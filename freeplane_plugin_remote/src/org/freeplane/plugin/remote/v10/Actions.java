package org.freeplane.plugin.remote.v10;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.docear.messages.Messages.AddNodeRequest;
import org.docear.messages.Messages.AddNodeResponse;
import org.docear.messages.Messages.ChangeNodeRequest;
import org.docear.messages.Messages.CloseAllOpenMapsRequest;
import org.docear.messages.Messages.CloseMapRequest;
import org.docear.messages.Messages.CloseServerRequest;
import org.docear.messages.Messages.CloseUnusedMaps;
import org.docear.messages.Messages.GetExpiredLocksRequest;
import org.docear.messages.Messages.GetExpiredLocksResponse;
import org.docear.messages.Messages.GetNodeRequest;
import org.docear.messages.Messages.GetNodeResponse;
import org.docear.messages.Messages.MindmapAsJsonReponse;
import org.docear.messages.Messages.MindmapAsJsonRequest;
import org.docear.messages.Messages.MindmapAsXmlRequest;
import org.docear.messages.Messages.MindmapAsXmlResponse;
import org.docear.messages.Messages.OpenMindMapRequest;
import org.docear.messages.Messages.RefreshLockRequest;
import org.docear.messages.Messages.ReleaseLockRequest;
import org.docear.messages.Messages.RemoveNodeRequest;
import org.docear.messages.Messages.RequestLockRequest;
import org.docear.messages.exceptions.CannotRetrieveMapIdException;
import org.docear.messages.exceptions.LockNotFoundException;
import org.docear.messages.exceptions.MapNotFoundException;
import org.docear.messages.exceptions.NodeAlreadyLockedException;
import org.docear.messages.exceptions.NodeNotFoundException;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.link.NodeLinks;
import org.freeplane.features.map.MapWriter;
import org.freeplane.features.map.NodeChangeEvent;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mapio.mindmapmode.MMapIO;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.nodelocation.LocationModel;
import org.freeplane.plugin.remote.RemoteController;
import org.freeplane.plugin.remote.v10.model.DefaultNodeModel;
import org.freeplane.plugin.remote.v10.model.LockModel;
import org.freeplane.plugin.remote.v10.model.MapModel;
import org.freeplane.plugin.remote.v10.model.OpenMindmapInfo;
import org.slf4j.Logger;

public class Actions {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * returns a map as a JSON-Object
	 * @param id ID of map
	 * @param nodeCount soft limit of node count. When limit is reached, it only loads the outstanding child nodes of the current node.
	 * @return a map model
	 */
	public static MindmapAsJsonReponse getMapModelJson(MindmapAsJsonRequest request) throws MapNotFoundException {

		final int nodeCount = request.getNodeCount();
		final String mapId = request.getId();

		final boolean loadAllNodes = nodeCount == -1;

		logger().debug("getMapModelJson => mapId:'{}'; nodeCount:{}; loadAllNodes:{}",mapId,nodeCount,loadAllNodes);
		final ModeController modeController = getModeController();

		logger().debug("getMapModelJson => selecting map");
		selectMap(mapId);

		logger().debug("getMapModelJson => retrieving freeplane map");
		org.freeplane.features.map.MapModel freeplaneMap = modeController.getController().getMap();
		if(freeplaneMap == null) { //when not mapMode
			logger().error("getMapModelJson => Current mode not MapMode!");
			throw new AssertionError("Current mode not MapMode");
		}

		//create the MapModel for JSON
		logger().debug("getMapModelJson => creating mapmodel for JSON-convertion");
		MapModel mm = new MapModel(freeplaneMap,loadAllNodes);

		if(!loadAllNodes) {
			Utils.loadNodesIntoModel(mm.root, nodeCount);
		}

		logger().debug("getMapModelJson => creating JSON string");
		String result = buildJSON(mm);
		//LogUtils.getLogger().log(Level.FINE, "getMapModel called for mapId '"+request.getId()+"' with nodeCount: "+request.getNodeCount());
		logger().debug("getMapModelJson => returning JSON string");
		return new MindmapAsJsonReponse(result);
	}



	/** 
	 * returns a map as a JSON-Object
	 * @param id ID of map
	 * @param nodeCount soft limit of node count. When limit is reached, it only loads the outstanding child nodes of the current node.
	 * @return a map model
	 * @throws MapNotFoundException 
	 * @throws IOException 
	 */
	public static MindmapAsXmlResponse getMapModelXml(final MindmapAsXmlRequest request) throws MapNotFoundException, IOException {
		final String mapId = request.getMapId();
		logger().debug("getMapModelXml => mapId:'{}'",mapId);

		logger().debug("getMapModelXml => selecting map");
		selectMap(mapId);

		final ModeController modeController = getModeController();
		final org.freeplane.features.map.MapModel freeplaneMap = modeController.getController().getMap();
		if(freeplaneMap == null) { //when not mapMode
			logger().error("getMapModelXml => current mode not MapMode!");
			throw new AssertionError("Current mode not MapMode");
		}


		logger().debug("getMapModelXml => serialising map to XML");
		final StringWriter writer = new StringWriter();
		modeController.getMapController()
		.getMapWriter().writeMapAsXml(freeplaneMap, writer, MapWriter.Mode.EXPORT, true, true);

		logger().debug("getMapModelXml => returning map as XML string");
		return new MindmapAsXmlResponse(writer.toString());
	}

	/**
	 * closes a map on the server
	 * @param id
	 * @return
	 */
	public static void closeMap(final CloseMapRequest request) throws MapNotFoundException{
		logger().debug("closeMap => mapId:'{}'",request.getMapId());
		Utils.closeMap(request.getMapId());
	}

	public static void openMindmap(OpenMindMapRequest request) throws CannotRetrieveMapIdException {

		logger().debug("openMindmap => mindmapFileContent:'{}...'",request.getMindmapFileContent().substring(0, 20));
		try {
			//create file
			final Random ran = new Random();
			final String filename = ""+System.currentTimeMillis()+ran.nextInt(100);
			final String tempDirPath = System.getProperty("java.io.tmpdir");
			final File file = new File(tempDirPath+"/docear/"+filename+".mm");
			logger().debug("openMindmap => temporary file '{}' was created",file.getAbsolutePath());

			logger().debug("openMindmap => writing mindmap content to file");
			FileUtils.writeStringToFile(file, request.getMindmapFileContent());

			//put map in openMap Collection
			final String mapId = Utils.getMapIdFromFile(file);
			final URL pathURL = file.toURI().toURL();
			final OpenMindmapInfo ommi = new OpenMindmapInfo(pathURL);
			getOpenMindmapInfoMap().put(mapId, ommi);
			logger().debug("openMindmap => mindmap was put into openMindmapInfoMap ({} => {})",mapId,ommi.getMapUrl());

			//open map
			logger().debug("openMindmap => opening mindmap...");
			final MMapIO mio = (MMapIO)RemoteController.getMapIO();
			mio.newMap(pathURL);
			logger().debug("openMindmap => map successfully loaded and opened!");
		} catch(CannotRetrieveMapIdException e) {
			throw e;
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}


	public static void closeServer(CloseServerRequest request) {
		logger().debug("closeServer => no parameters");

		logger().debug("closeServer => closing open maps");
		Set<String> ids = getOpenMindmapInfoMap().keySet(); 
		for(String mapId : ids) {
			try {
				Utils.closeMap(mapId);
			} catch (Exception e) {

			}
		}
		logger().debug("closeServer => Starting Thread to shutdown App in 2 seconds");
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(2000);

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				logger().debug("closeServer => shutting down");
				System.exit(15);

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
	public static GetNodeResponse getNode(final GetNodeRequest request) throws MapNotFoundException, NodeNotFoundException, JsonGenerationException, JsonMappingException, IOException {
		final String mapId = request.getMapId();
		final String nodeId = request.getNodeId();
		final int nodeCount = request.getNodeCount();
		final boolean loadAllNodes = nodeCount == -1;

		logger().debug("getNode => mapId:'{}'; nodeId:'{}'; nodeCount:{}; loadAllNodes:{}",mapId,nodeId,nodeCount,loadAllNodes);

		logger().debug("getNode => selecting map");
		selectMap(mapId);

		logger().debug("getNode => retrieving freeplane node");
		final NodeModel freeplaneNode = getModeController().getMapController().getNodeFromID(request.getNodeId());

		if(freeplaneNode == null) {
			logger().error("getNode => requested node not found; throwing exception");
			throw new NodeNotFoundException("Node with id '"+request.getNodeId()+"' not found.");
		}


		logger().debug("getNode => loading into model to convert to JSON");
		final DefaultNodeModel node = new DefaultNodeModel(freeplaneNode,loadAllNodes);
		if(!loadAllNodes) {
			Utils.loadNodesIntoModel(node, request.getNodeCount());
		}

		logger().debug("getNode => returning node as JSON");
		return new GetNodeResponse(buildJSON(node));
	}

	public static AddNodeResponse addNode(AddNodeRequest request) throws MapNotFoundException, NodeNotFoundException, JsonGenerationException, JsonMappingException, IOException {
		final String mapId = request.getMapId();
		final String parentNodeId = request.getParentNodeId(); 
		logger().debug("addNode => mapId:'{}'; parentNodeId:'{}'",mapId,parentNodeId);

		logger().debug("addNode => selecting map");
		selectMap(mapId);

		final ModeController modeController = getModeController();
		final MMapController mapController = (MMapController) modeController.getMapController();

		//get map
		logger().debug("addNode => retrieving freeplane map");
		org.freeplane.features.map.MapModel mm = modeController.getController().getMap();

		//get parent Node
		logger().debug("addNode => retrieving freeplane parent node");
		NodeModel parentNode = mapController.getNodeFromID(parentNodeId);
		if(parentNode == null) {
			logger().error("addNode => parent node not found, throwing exception"); 
			throw new NodeNotFoundException("Node with id '"+parentNodeId+"' not found");
		}

		//create new node
		logger().debug("addNode => creating new node");
		NodeModel node = mapController.newNode("", mm);

		//insert node
		logger().debug("addNode => inserting new node");
		mapController.insertNode(node, parentNode);
		//mapController.fireMapChanged(new MapChangeEvent(new Object(), "node", "", ""));

		node.createID();
		logger().debug("addNode => node with id '{}' successfully created",node.getID()); 

		logger().debug("addNode => returning response with new node as json");
		return new AddNodeResponse(buildJSON(new DefaultNodeModel(node, false)));
	}

	public static void changeNode(final ChangeNodeRequest request) throws MapNotFoundException, NodeNotFoundException, JsonParseException, JsonMappingException, IOException, URISyntaxException {
		final String mapId = request.getMapId();
		final DefaultNodeModel node = objectMapper.readValue(request.getNodeAsJsonString(), DefaultNodeModel.class);
		logger().debug("changeNode => mapId:'{}'; nodeAsJson:'{}'",mapId,request.getNodeAsJsonString());

		logger().debug("changeNode => selecting map");
		selectMap(mapId);

		//get map
		logger().debug("changeNode => retrieving map");
		org.freeplane.features.map.MapModel mm = getModeController().getController().getMap();

		//get node
		logger().debug("changeNode => retrieving node");
		NodeModel freeplaneNode = mm.getNodeForID(node.id);
		if(freeplaneNode == null) {
			logger().error("changeNode => node not found, throwing exception"); 
			throw new NodeNotFoundException("Node with id '"+node.id+"' not found");
		}

		if(node.folded != null) {
			logger().debug("changeNode => folded changed to {}",node.folded);
			freeplaneNode.setFolded(node.folded);
		}
		if(node.isHtml != null) {
			logger().debug("changeNode => isHtml changed to {}",node.isHtml);
			freeplaneNode.setXmlText(node.nodeText);
		}
		if(node.attributes != null) {
			logger().error("changeNode => attributes are not implemented yet");
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
			logger().debug("changeNode => hGap changed to {}",node.hGap);
			updateLocationModel(freeplaneNode, node.hGap, null);
		}
		if(node.icons != null) {
			//TODO handle
		}
		if(node.image != null) {
			//TODO handle
		}
		if(node.link != null) {
			logger().debug("changeNode => link changed to {}",node.link);
			NodeLinks nodeLinks = freeplaneNode.getExtension(NodeLinks.class);

			if(nodeLinks == null) {
				nodeLinks = new NodeLinks();
				freeplaneNode.addExtension(nodeLinks);				
			}

			nodeLinks.setHyperLink(new URI(node.link));
		}
		if(node.nodeText != null) {
			logger().debug("changeNode => nodeText changed to {}",node.nodeText);
			freeplaneNode.setText(node.nodeText);
		}
		if(node.shiftY != null) {
			logger().debug("changeNode => shiftY changed to {}",node.shiftY);
			updateLocationModel(freeplaneNode, null, node.shiftY);
		}
		//only for gui
		//freeplaneNode.fireNodeChanged(new NodeChangeEvent(freeplaneNode, "", "", ""));

		logger().debug("changeNode => refreshing lock access time");
		refreshLockAccessTime(freeplaneNode);
	}

	public static void removeNode(RemoveNodeRequest request) throws NodeNotFoundException, MapNotFoundException {
		final String mapId = request.getMapId();
		final String nodeId = request.getNodeId();
		logger().debug("removeNode => mapId:'{}'; nodeId:'{}'",mapId,nodeId);

		logger().debug("removeNode => selecting map");
		selectMap(request.getMapId());

		logger().debug("removeNode => retrieving node");
		final MMapController mapController = (MMapController) getModeController().getMapController();
		NodeModel node = mapController.getNodeFromID(request.getNodeId());
		if(node == null) {
			logger().error("removeNode => node not found, throwing exception");
			throw new NodeNotFoundException("Node with id '"+request.getNodeId()+"' not found");
		}

		logger().debug("removeNode => deleting node");
		mapController.deleteNode(node);
	}

	public static void refreshLock (RefreshLockRequest request) throws MapNotFoundException, NodeNotFoundException{
		final String mapId = request.getMapId();
		final String nodeId = request.getNodeId();
		logger().debug("refreshLock => mapId:'{}'; nodeId:'{}'",mapId,nodeId);
		
		logger().debug("refreshLock => selecting map");
		selectMap(request.getMapId());

		logger().debug("refreshLock => retrieving node");
		NodeModel node = getModeController().getMapController().getNodeFromID(request.getNodeId());
		if(node == null) {
			logger().error("refreshLock => node not found, throwing exception");
			throw new NodeNotFoundException("Node with id '"+request.getNodeId()+"' not found");
		}

		refreshLockAccessTime(node);
	}

	public static void requestLock (RequestLockRequest request) throws MapNotFoundException, NodeAlreadyLockedException{
		selectMap(request.getMapId());

		ModeController modeController = getModeController();
		NodeModel node = modeController.getMapController().getNodeFromID(request.getNodeId());


		LockModel lm = node.getExtension(LockModel.class);//.setLastAccess(System.currentTimeMillis());
		if(lm != null) { 
			// Lock exists. Check if it's own lock
			if(lm.getUsername().equals(request.getUsername())) {
				return ;
			} else {
				throw new NodeAlreadyLockedException("Locked by " + lm.getUsername() + ".");
			}
		}
		//create lock
		lm = new LockModel(node,request.getUsername(),System.currentTimeMillis());
		node.addExtension(lm);

		//add to lock list
		getOpenMindMapInfo(request.getMapId()).getLockedNodes().add(node);
	}

	public static void releaseLock (ReleaseLockRequest request) throws MapNotFoundException, LockNotFoundException{
		selectMap(request.getMapId());

		ModeController modeController = getModeController();
		NodeModel node = modeController.getMapController().getNodeFromID(request.getNodeId());


		LockModel lm = node.getExtension(LockModel.class);//.setLastAccess(System.currentTimeMillis());
		if(lm == null) { 
			// No lock available
			throw new LockNotFoundException("Lock for nodeId " + request.getNodeId() + " not found.");
		}
		//release lock
		node.removeExtension(LockModel.class);

		//remove form to lock list
		getOpenMindMapInfo(request.getMapId()).getLockedNodes().remove(node);
	}

	public static GetExpiredLocksResponse getExpiredLocks(GetExpiredLocksRequest request) throws MapNotFoundException, JsonGenerationException, JsonMappingException, IOException{
		if (!getOpenMindmapInfoMap().containsKey(request.getMapId())){
			throw new MapNotFoundException("MapId: " + request.getMapId());
		}
		Set<NodeModel> nodes = getOpenMindMapInfo(request.getMapId()).getLockedNodes();
		Set<NodeModel> newNodes = new HashSet<NodeModel>();
		List<NodeModel> expiredNodes = new ArrayList<NodeModel>(); 

		for (NodeModel node : nodes){
			LockModel lock = node.getExtension(LockModel.class);
			long timeDiff = System.currentTimeMillis() - lock.getLastAccess();  
			if (timeDiff < request.getDeltaTimeInMs()){
				//Lock not expired
				newNodes.add(node);
			} else {
				//Lock expired
				node.removeExtension(LockModel.class);
				expiredNodes.add(node);
			}
		}
		nodes = newNodes;

		return new GetExpiredLocksResponse(objectMapper.writeValueAsString(expiredNodes));
	}

	public static void closeAllOpenMaps(CloseAllOpenMapsRequest request) {
		Set<String> ids = getOpenMindmapInfoMap().keySet(); 
		for(String mapId : ids) {
			try {
				Utils.closeMap(mapId);
			} catch (Exception e) {

			}
		}
	}

	public static void closeUnusedMaps(CloseUnusedMaps request) throws Exception {

		final long allowedMsSinceLastAccess = request.getUnusedSinceInMs();
		final long now = System.currentTimeMillis();
		for(Map.Entry<String, OpenMindmapInfo> entry : getOpenMindmapInfoMap().entrySet()) {
			final long lastAccessTime = entry.getValue().getLastAccessTime();
			final long sinceLastAccess = now - lastAccessTime;
			final long sinceLastAccessInMinutes = sinceLastAccess / 60000;

			if(sinceLastAccess > allowedMsSinceLastAccess) {
				//TODO tell ZooKeeper and save to hadoop
				final String mapId = entry.getKey();
				closeMap(new CloseMapRequest(mapId));
				LogUtils.info("Map "+mapId+" was closed, because it havent been used for about "+sinceLastAccessInMinutes+" minutes.");
			}
		}
	}

	static ModeController getModeController() {
		return RemoteController.getModeController();
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
		checkForDebugMap(mapId);
		Utils.selectMap(mapId);
	}

	/**
	 * checks if requested map is a map used for debugging
	 * @param mapId
	 * @throws MapNotFoundException
	 */
	private static void checkForDebugMap(final String mapId) throws MapNotFoundException {
		if(!getOpenMindmapInfoMap().containsKey(mapId) && mapId.startsWith("test_")) {
			//check if testmap exists
			if(mapId.equals("test_1") || mapId.equals("test_2") || 
					mapId.equals("test_3") || mapId.equals("test_5")) {
				Utils.openTestMap(mapId);
			} else {
				throw new MapNotFoundException("Map not found!\n"+
						"Available test map ids: 'test_1','test_2','test_3','test_5'");
			}
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

	private static Map<String, OpenMindmapInfo> getOpenMindmapInfoMap() {
		return RemoteController.getMapIdInfoMap();
	}

	static OpenMindmapInfo getOpenMindMapInfo(String mapId) {
		if(!getOpenMindmapInfoMap().containsKey(mapId)) {
			return null;
		}
		return getOpenMindmapInfoMap().get(mapId);
	}

	private static String buildJSON(Object object) {
		String result = null;

		try {
			result = objectMapper.writeValueAsString(object);
		} catch (Exception e) {
			LogUtils.severe("Error while parsing object to JSON-String!", e);
			throw new AssertionError(e);
		}		

		return result;
	}

	private static Logger logger() {
		return RemoteController.getLogger();
	}

}