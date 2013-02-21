package org.freeplane.plugin.webservice.v10;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.tools.ant.util.FileUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.docear.messages.Messages.AddNodeRequest;
import org.docear.messages.Messages.AddNodeResponse;
import org.docear.messages.Messages.ChangeNodeRequest;
import org.docear.messages.Messages.CloseAllOpenMapsRequest;
import org.docear.messages.Messages.CloseMapRequest;
import org.docear.messages.Messages.GetNodeRequest;
import org.docear.messages.Messages.GetNodeResponse;
import org.docear.messages.Messages.MindmapAsJsonReponse;
import org.docear.messages.Messages.MindmapAsJsonRequest;
import org.docear.messages.Messages.MindmapAsXmlRequest;
import org.docear.messages.Messages.MindmapAsXmlResponse;
import org.docear.messages.Messages.OpenMindMapRequest;
import org.docear.messages.Messages.RemoveNodeRequest;
import org.docear.messages.exceptions.MapNotFoundException;
import org.docear.messages.exceptions.NodeNotFoundException;
import org.freeplane.features.link.NodeLinks;
import org.freeplane.features.map.MapWriter;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mapio.MapIO;
import org.freeplane.features.mapio.mindmapmode.MMapIO;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.nodelocation.LocationModel;
import org.freeplane.plugin.webservice.WebserviceController;
import org.freeplane.plugin.webservice.v10.model.DefaultNodeModel;
import org.freeplane.plugin.webservice.v10.model.LockModel;
import org.freeplane.plugin.webservice.v10.model.MapModel;
import org.freeplane.plugin.webservice.v10.model.OpenMindmapInfo;

public class Webservice {

	static final Object lock = new Object();
	static final Map<String, OpenMindmapInfo> mapIdInfoMap = new HashMap<String, OpenMindmapInfo>();
	static final ObjectMapper objectMapper = new ObjectMapper();

	public static String getStatus() {
		return "Webservice V0.1";
	}

	/**
	 * returns a map as a JSON-Object
	 * @param id ID of map
	 * @param nodeCount soft limit of node count. When limit is reached, it only loads the outstanding child nodes of the current node.
	 * @return a map model
	 */
	public static MindmapAsJsonReponse getMapModelJson(MindmapAsJsonRequest request) throws MapNotFoundException {

		final int nodeCount = request.getNodeCount();
		final String mapId = request.getId();

		boolean loadAllNodes = nodeCount == -1;
		ModeController modeController = getModeController();
		
		if(!WebserviceHelper.selectMap(mapId)) {
			if(mapId.startsWith("test_")) { //FOR DEBUGING
				openTestMap(mapId);
				if(!WebserviceHelper.selectMap(mapId)) {
					throw new MapNotFoundException("Map not found!\n"+
							"Available test map ids: 'test_1','test_2','test_3','test_4','test_5'");
				}
			} else {
				throw new MapNotFoundException("Map not found");
			}
		}

		org.freeplane.features.map.MapModel freeplaneMap = modeController.getController().getMap();
		if(freeplaneMap == null) { //when not mapMode
			throw new AssertionError("Current mode not MapMode");
		}

		//create the MapModel for JSON
		MapModel mm = new MapModel(freeplaneMap,loadAllNodes);

		if(!loadAllNodes) {
			WebserviceHelper.loadNodesIntoModel(mm.root, nodeCount);
		}

		String result = buildJSON(mm); 
		return new MindmapAsJsonReponse(result);
	}

	private static void openTestMap(String id) {
		try {
			//create file
			Random ran = new Random();
			String filename = ""+System.currentTimeMillis()+ran.nextInt(100);
			File file = File.createTempFile(filename, ".mm");
			file.deleteOnExit();

			InputStream in = Webservice.class.getResourceAsStream("/mindmaps/"+id+".mm");
			//fill file from inputstream
			FileOutputStream out = new FileOutputStream(file);
			byte[] buffer = new byte[1024];
			int length;
			while((length = in.read(buffer, 0, buffer.length)) != -1) {
				out.write(buffer, 0, length);
			}
			out.flush();
			out.close();

			//put map in openMap Collection
			URL pathURL = file.toURI().toURL();
			mapIdInfoMap.put(id, new OpenMindmapInfo(pathURL));

			//open map
			ModeController modeController = getModeController();

			MMapIO mio = (MMapIO)modeController.getExtension(MapIO.class);
			mio.newMap(pathURL);
		} catch (Exception e) {}
	}

	/** 
	 * returns a map as a JSON-Object
	 * @param id ID of map
	 * @param nodeCount soft limit of node count. When limit is reached, it only loads the outstanding child nodes of the current node.
	 * @return a map model
	 * @throws MapNotFoundException 
	 * @throws IOException 
	 */
	public static MindmapAsXmlResponse getMapModelXml( MindmapAsXmlRequest request) throws MapNotFoundException, IOException {
		final String mapId = request.getMapId();
		
		selectMap(mapId);

		ModeController modeController = getModeController();
		org.freeplane.features.map.MapModel freeplaneMap = modeController.getController().getMap();
		if(freeplaneMap == null) { //when not mapMode
			throw new AssertionError("Current mode not MapMode");
		}

		StringWriter writer = new StringWriter();
		
		modeController.getMapController().getMapWriter().writeMapAsXml(freeplaneMap, writer, MapWriter.Mode.EXPORT, true, true);


		return new MindmapAsXmlResponse(writer.toString());
	}

	/**
	 * closes a map on the server
	 * @param id
	 * @return
	 */
	public static void closeMap(CloseMapRequest request) throws Exception{
		WebserviceHelper.closeMap(request.getMapId());
	}

	public static void openMindmap(OpenMindMapRequest request) {

		try {
			//create file
			Random ran = new Random();
			String filename = ""+System.currentTimeMillis()+ran.nextInt(100);
			File file = File.createTempFile(filename, ".mm");
			file.deleteOnExit();

			
			File received = request.getMindmapFile();
			org.apache.commons.io.FileUtils.copyFile(received, file);

			//put map in openMap Collection
			String mapId = WebserviceHelper.getMapIdFromFile(file);
			URL pathURL = file.toURI().toURL();
			mapIdInfoMap.put(mapId, new OpenMindmapInfo(pathURL));

			//open map
			ModeController modeController = getModeController();

			MMapIO mio = (MMapIO)modeController.getExtension(MapIO.class);
			mio.newMap(pathURL);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	
	public static void closeServer() {

		Set<String> ids = mapIdInfoMap.keySet(); 
		for(String mapId : ids) {
			try {
				WebserviceHelper.closeMap(mapId);
			} catch (Exception e) {

			}
		}

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.exit(0);

			}
		}).start();

	}

	/**
	 * returns a node as a JSON-Object
	 * @param id ID of node
	 * @param nodeCount soft limit of node count. When limit is reached, it only loads the outstanding child nodes of the current node.
	 * @return a node model
	 * @throws MapNotFoundException 
	 * @throws NodeNotFoundException 
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 */
	public static GetNodeResponse getNode(GetNodeRequest request) throws MapNotFoundException, NodeNotFoundException, JsonGenerationException, JsonMappingException, IOException {
		selectMap(request.getMapId());

		ModeController modeController = getModeController();
		boolean loadAllNodes = request.getNodeCount() == -1;


		NodeModel freeplaneNode = modeController.getMapController().getNodeFromID(request.getNodeId());

		if(freeplaneNode == null) {
			throw new NodeNotFoundException("Node with id '"+request.getNodeId()+"' not found.");
		}

		DefaultNodeModel node = new DefaultNodeModel(freeplaneNode,loadAllNodes);

		if(!loadAllNodes) {
			WebserviceHelper.loadNodesIntoModel(node, request.getNodeCount());
		}

		return new GetNodeResponse(objectMapper.writeValueAsString(node));
	}

	public static AddNodeResponse addNode(AddNodeRequest request) throws MapNotFoundException, NodeNotFoundException, JsonGenerationException, JsonMappingException, IOException {
		final String mapId = request.getMapId();
		final String parentNodeId = request.getParentNodeId(); 

		System.out.println("Selecting Map.");
		
		selectMap(mapId);
		
		System.out.println("Map selected.");

		ModeController modeController = getModeController();
		MMapController mapController = (MMapController) modeController.getMapController();
		//get map
		org.freeplane.features.map.MapModel mm = modeController.getController().getMap();

		//get parent Node
		NodeModel parentNode = mapController.getNodeFromID(parentNodeId);

		if(parentNode == null)
			throw new NodeNotFoundException("Node with id '"+parentNodeId+"' not found");

		System.out.println("got parent node: " + parentNode.getID());
		
		//create new node
		NodeModel node = modeController.getMapController().newNode("", mm);

		//insert node
		mapController.insertNode(node, parentNode);
		//mapController.fireMapChanged(new MapChangeEvent(new Object(), "node", "", ""));

		node.createID();
		System.out.println("created node." + node.getID() + " with text: \"" + node.getText() + "\"");

		return new AddNodeResponse(objectMapper.writeValueAsString(new DefaultNodeModel(node, false)));
		//return Response.ok(new DefaultNodeModel(node, false)).build();	
	}

	public static void changeNode(ChangeNodeRequest request) throws MapNotFoundException, NodeNotFoundException, JsonParseException, JsonMappingException, IOException, URISyntaxException {

		final String mapId = request.getMapId();

		final DefaultNodeModel node = objectMapper.readValue(request.getNodeAsJsonString(), DefaultNodeModel.class);

		selectMap(mapId);

		//get map
		ModeController modeController = getModeController();
		org.freeplane.features.map.MapModel mm = modeController.getController().getMap();

		//get node
		NodeModel freeplaneNode = mm.getNodeForID(node.id);
		if(freeplaneNode == null)
			throw new NodeNotFoundException("Node with id '"+node.id+"' not found");

		if(node.folded != null) {
			freeplaneNode.setFolded(node.folded);
		}
		if(node.isHtml != null) {
			freeplaneNode.setXmlText(node.nodeText);
		}
		if(node.attributes != null) {
			//TODO implement correctly
//			NodeAttributeTableModel attrTable;
//			AttributeController attrController = AttributeController.getController();
//			
//			if(node.attributes.size() > 0) {
//				attrTable = attrController.createAttributeTableModel(freeplaneNode);
//				for(Map.Entry<String, String> entry : node.attributes.entrySet()) {
//					//attrController.performInsertRow(attrTable, row, name, value)
//					attrTable.addRowNoUndo(new Attribute(entry.getKey(),entry.getValue()));
//				}
//				freeplaneNode.addExtension(attrTable);
//			} else if (node.attributes.size() == 0) {
//				if(freeplaneNode.getExtension(NodeAttributeTableModel.class) != null)
//					freeplaneNode.removeExtension(NodeAttributeTableModel.class);
//			}
		}
		if(node.hGap != null) {
			updateLocationModel(freeplaneNode, node.hGap, null);
		}
		if(node.icons != null) {
			//TODO handle
		}
		if(node.image != null) {
			//TODO handle
		}
		if(node.link != null) {
			
			NodeLinks nodeLinks = freeplaneNode.getExtension(NodeLinks.class);
			
			if(nodeLinks == null) {
				nodeLinks = new NodeLinks();
				freeplaneNode.addExtension(nodeLinks);				
			}
			
			nodeLinks.setHyperLink(new URI(node.link));
		}
		if(node.nodeText != null) {
			freeplaneNode.setText(node.nodeText);
		}
		if(node.shiftY != null) {
			updateLocationModel(freeplaneNode, null, node.shiftY);
		}
		//only for gui
		//freeplaneNode.fireNodeChanged(new NodeChangeEvent(freeplaneNode, "", "", ""));

		refreshLockAccessTime(freeplaneNode);

	}

	public static void removeNode(RemoveNodeRequest request) throws NodeNotFoundException, MapNotFoundException {
		selectMap(request.getMapId());
		ModeController modeController = getModeController();
		MMapController mapController = (MMapController) modeController.getMapController();
		NodeModel node = modeController.getMapController().getNodeFromID(request.getNodeId());
		if(node == null)
			throw new NodeNotFoundException("Node with id '"+request.getNodeId()+"' not found");

		//TODO works correct?
		try {
		mapController.deleteNode(node);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		//node.removeFromParent();
		
		//node.fireNodeChanged(new NodeChangeEvent(node, "parent", "", ""));
	}

	public static void refreshLock (String mapId, String nodeId) throws MapNotFoundException{
		selectMap(mapId);

		ModeController modeController = getModeController();
		NodeModel node = modeController.getMapController().getNodeFromID(nodeId);

		refreshLockAccessTime(node);
	}

	public static void requestLock (String mapId, String nodeId, String username) throws MapNotFoundException{
		selectMap(mapId);

		ModeController modeController = getModeController();
		NodeModel node = modeController.getMapController().getNodeFromID(nodeId);


		LockModel lm = node.getExtension(LockModel.class);//.setLastAccess(System.currentTimeMillis());
		if(lm != null) { //lock exists
			if(lm.getUsername().equals(username)) {
				return ;
			} else {
				throw new AssertionError("Node already locked.");
			}
		}
		//create lock
		lm = new LockModel(node,username,System.currentTimeMillis());
		node.addExtension(lm);


		//add to lock list

		getOpenMindMapInfo(mapId).getLockedNodes().add(node);
	}

	public static void releaseLock (String mapId, String nodeId) throws MapNotFoundException{
		selectMap(mapId);

		ModeController modeController = getModeController();
		NodeModel node = modeController.getMapController().getNodeFromID(nodeId);


		LockModel lm = node.getExtension(LockModel.class);//.setLastAccess(System.currentTimeMillis());
		if(lm == null) { //lock exists
			throw new AssertionError("No lock present.");
		}
		//release lock
		node.removeExtension(LockModel.class);


		//remove form to lock list
		getOpenMindMapInfo(mapId).getLockedNodes().remove(node);
	}

	public static String getExpiredLocks(String mapId, int sinceInMs) {
		if (!mapIdInfoMap.containsKey(mapId)){
			throw new AssertionError("Map not found.");
		}
		Set<NodeModel> nodes = getOpenMindMapInfo(mapId).getLockedNodes();
		Set<NodeModel> newNodes = new HashSet<NodeModel>();
		List<NodeModel> expiredNodes = new ArrayList<NodeModel>(); 

		for (NodeModel node : nodes){
			LockModel lock = node.getExtension(LockModel.class);
			long timeDiff = System.currentTimeMillis() - lock.getLastAccess();  
			if (timeDiff < sinceInMs){
				//Lock not expired
				newNodes.add(node);
			} else {
				//Lock expired
				node.removeExtension(LockModel.class);
				expiredNodes.add(node);
			}
		}
		nodes = newNodes;


		return buildJSON(expiredNodes.toArray());
	}
	
	public static void closeAllOpenMaps(CloseAllOpenMapsRequest request) {
		Set<String> ids = mapIdInfoMap.keySet(); 
		for(String mapId : ids) {
			try {
				WebserviceHelper.closeMap(mapId);
			} catch (Exception e) {

			}
		}
	}

	public static void closeUnusedMaps() {
		//TODO close maps and tell ZooKeeper
	}

	static ModeController getModeController() {
		return WebserviceController.getInstance().getModeController();
	}

	static org.freeplane.features.map.MapModel getOpenMap() {
		ModeController modeController = getModeController();
		return modeController.getMapController().getRootNode().getMap();
	}


	/**
	 * Select Map so getMapController() has right map. 
	 * @param mapId Id of Map
	 * @throws MapNotFoundException 
	 */
	private static void selectMap(String mapId) throws MapNotFoundException{
		if(!WebserviceHelper.selectMap(mapId)) {
			throw new MapNotFoundException("MapId: " + mapId);
		}
	}

	/**
	 * refresh lastAccesTime of node lock  
	 * @param node Node with lock
	 */
	private static void refreshLockAccessTime(NodeModel node){
		LockModel lm = node.getExtension(LockModel.class);
		if(lm != null) {
			lm.setLastAccess(System.currentTimeMillis());
		}
	}

	private static void updateLocationModel(NodeModel freeplaneNode, Integer hGap, Integer Shifty) {
		LocationModel lm = freeplaneNode.getExtension(LocationModel.class);
		if(lm == null) {
			lm = new LocationModel();
			freeplaneNode.addExtension(lm);
		}

		if(hGap != null) {
			lm.setHGap(hGap);
		}
		if(Shifty != null) {
			lm.setShiftY(Shifty);
		}
	}

	static OpenMindmapInfo getOpenMindMapInfo(String mapId) {
		if(!mapIdInfoMap.containsKey(mapId)) {
			return null;
		}
		return mapIdInfoMap.get(mapId);
	}

	private static String buildJSON(Object object) {
		String result = null;

		try {
			result = objectMapper.writeValueAsString(object);
		} catch (Exception e) {
			e.printStackTrace();
			throw new AssertionError(e);
		}		

		return result;
	}

}