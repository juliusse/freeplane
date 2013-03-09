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

import scala.collection.mutable.HashMap;

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

		logger().debug("Actions.getMapModelJson => mapId:'{}'; nodeCount:{}; loadAllNodes:{}",mapId,nodeCount,loadAllNodes);
		final ModeController modeController = getModeController();

		logger().debug("Actions.getMapModelJson => selecting map");
		selectMap(mapId);

		logger().debug("Actions.getMapModelJson => retrieving freeplane map");
		org.freeplane.features.map.MapModel freeplaneMap = modeController.getController().getMap();
		if(freeplaneMap == null) { //when not mapMode
			logger().error("Actions.getMapModelJson => Current mode not MapMode!");
			throw new AssertionError("Current mode not MapMode");
		}

		//create the MapModel for JSON
		logger().debug("Actions.getMapModelJson => creating mapmodel for JSON-convertion");
		MapModel mm = new MapModel(freeplaneMap,loadAllNodes);

		if(!loadAllNodes) {
			Utils.loadNodesIntoModel(mm.root, nodeCount);
		}

		logger().debug("Actions.getMapModelJson => creating JSON string");
		String result = mm.toJsonString();
		
		logger().debug("Actions.getMapModelJson => returning JSON string");
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
		logger().debug("Actions.getMapModelXml => mapId:'{}'",mapId);

		logger().debug("Actions.getMapModelXml => selecting map");
		selectMap(mapId);

		final ModeController modeController = getModeController();
		final org.freeplane.features.map.MapModel freeplaneMap = modeController.getController().getMap();
		if(freeplaneMap == null) { //when not mapMode
			logger().error("Actions.getMapModelXml => current mode not MapMode!");
			throw new AssertionError("Current mode not MapMode");
		}


		logger().debug("Actions.getMapModelXml => serialising map to XML");
		final StringWriter writer = new StringWriter();
		modeController.getMapController()
		.getMapWriter().writeMapAsXml(freeplaneMap, writer, MapWriter.Mode.EXPORT, true, true);

		logger().debug("Actions.getMapModelXml => returning map as XML string");
		return new MindmapAsXmlResponse(writer.toString());
	}

	/**
	 * closes a map on the server
	 * @param id
	 * @return
	 */
	public static void closeMap(final CloseMapRequest request) throws MapNotFoundException{
		logger().debug("Actions.closeMap => mapId:'{}'",request.getMapId());
		Utils.closeMap(request.getMapId());
	}

	public static void openMindmap(OpenMindMapRequest request) throws CannotRetrieveMapIdException {

		logger().debug("Actions.openMindmap => mindmapFileContent:'{}...'",request.getMindmapFileContent().substring(0, 20));
		try {
			//create file
			final Random ran = new Random();
			final String filename = ""+System.currentTimeMillis()+ran.nextInt(100);
			final String tempDirPath = System.getProperty("java.io.tmpdir");
			final File file = new File(tempDirPath+"/docear/"+filename+".mm");
			logger().debug("Actions.openMindmap => temporary file '{}' was created",file.getAbsolutePath());

			logger().debug("Actions.openMindmap => writing mindmap content to file");
			FileUtils.writeStringToFile(file, request.getMindmapFileContent());

			//put map in openMap Collection
			final String mapId = Utils.getMapIdFromFile(file);
			final URL pathURL = file.toURI().toURL();
			final OpenMindmapInfo ommi = new OpenMindmapInfo(pathURL);
			getOpenMindmapInfoMap().put(mapId, ommi);
			logger().debug("Actions.openMindmap => mindmap was put into openMindmapInfoMap ({} => {})",mapId,ommi.getMapUrl());

			//open map
			logger().debug("Actions.openMindmap => opening mindmap...");
			final MMapIO mio = (MMapIO)RemoteController.getMapIO();
			mio.newMap(pathURL);
			logger().debug("Actions.openMindmap => map successfully loaded and opened!");
		} catch(CannotRetrieveMapIdException e) {
			throw e;
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}


	public static void closeServer(CloseServerRequest request) {
		logger().debug("Actions.closeServer => no parameters");

		logger().debug("Actions.closeServer => closing open maps");
		Set<String> ids = getOpenMindmapInfoMap().keySet(); 
		for(String mapId : ids) {
			try {
				Utils.closeMap(mapId);
			} catch (Exception e) {

			}
		}
		logger().debug("Actions.closeServer => Starting Thread to shutdown App in 2 seconds");
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(2000);

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				logger().debug("Actions.closeServer => shutting down");
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

		logger().debug("Actions.getNode => mapId:'{}'; nodeId:'{}'; nodeCount:{}; loadAllNodes:{}",mapId,nodeId,nodeCount,loadAllNodes);

		logger().debug("Actions.getNode => selecting map");
		selectMap(mapId);

		logger().debug("Actions.getNode => retrieving freeplane node");
		final NodeModel freeplaneNode = getNodeFromOpenMapById(nodeId);


		logger().debug("Actions.getNode => loading into model to convert to JSON");
		final DefaultNodeModel node = new DefaultNodeModel(freeplaneNode,loadAllNodes);
		if(!loadAllNodes) {
			Utils.loadNodesIntoModel(node, request.getNodeCount());
		}

		logger().debug("Actions.getNode => returning node as JSON");
		return new GetNodeResponse(buildJSON(node));
	}

	public static AddNodeResponse addNode(AddNodeRequest request) throws MapNotFoundException, NodeNotFoundException, JsonGenerationException, JsonMappingException, IOException {
		final String mapId = request.getMapId();
		final String parentNodeId = request.getParentNodeId(); 
		logger().debug("Actions.addNode => mapId:'{}'; parentNodeId:'{}'",mapId,parentNodeId);

		logger().debug("Actions.addNode => selecting map");
		selectMap(mapId);

		final MMapController mapController = (MMapController) getModeController().getMapController();

		//get map
		logger().debug("Actions.addNode => retrieving freeplane map");
		org.freeplane.features.map.MapModel mm = getOpenMap();

		//get parent Node
		logger().debug("Actions.addNode => retrieving freeplane parent node");
		final NodeModel parentNode = getNodeFromOpenMapById(parentNodeId);

		//create new node
		logger().debug("Actions.addNode => creating new node");
		NodeModel node = mapController.newNode("", mm);

		//insert node
		logger().debug("Actions.addNode => inserting new node");
		mapController.insertNode(node, parentNode);
		//mapController.fireMapChanged(new MapChangeEvent(new Object(), "node", "", ""));

		node.createID();
		logger().debug("Actions.addNode => node with id '{}' successfully created",node.getID()); 

		logger().debug("Actions.addNode => returning response with new node as json");
		return new AddNodeResponse(buildJSON(new DefaultNodeModel(node, false)));
	}

	public static void changeNode(final ChangeNodeRequest request) throws MapNotFoundException, NodeNotFoundException, JsonParseException, JsonMappingException, IOException, URISyntaxException {
		final String mapId = request.getMapId();
		final DefaultNodeModel node = objectMapper.readValue(request.getNodeAsJsonString(), DefaultNodeModel.class);
		logger().debug("Actions.changeNode => mapId:'{}'; nodeAsJson:'{}'",mapId,request.getNodeAsJsonString());

		logger().debug("Actions.changeNode => selecting map");
		selectMap(mapId);

		//get node
		logger().debug("Actions.changeNode => retrieving node");
		final NodeModel freeplaneNode = getNodeFromOpenMapById(node.id);
		

		if(node.folded != null) {
			logger().debug("Actions.changeNode => folded changed to {}",node.folded);
			freeplaneNode.setFolded(node.folded);
		}
		if(node.isHtml != null) {
			logger().debug("Actions.changeNode => isHtml changed to {}",node.isHtml);
			freeplaneNode.setXmlText(node.nodeText);
		}
		if(node.attributes != null) {
			logger().error("Actions.changeNode => attributes are not implemented yet");
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
			logger().debug("Actions.changeNode => hGap changed to {}",node.hGap);
			updateLocationModel(freeplaneNode, node.hGap, null);
		}
		if(node.icons != null) {
			//TODO handle
		}
		if(node.image != null) {
			//TODO handle
		}
		if(node.link != null) {
			logger().debug("Actions.changeNode => link changed to {}",node.link);
			NodeLinks nodeLinks = freeplaneNode.getExtension(NodeLinks.class);

			if(nodeLinks == null) {
				nodeLinks = new NodeLinks();
				freeplaneNode.addExtension(nodeLinks);				
			}

			nodeLinks.setHyperLink(new URI(node.link));
		}
		if(node.nodeText != null) {
			logger().debug("Actions.changeNode => nodeText changed to {}",node.nodeText);
			freeplaneNode.setText(node.nodeText);
		}
		if(node.shiftY != null) {
			logger().debug("Actions.changeNode => shiftY changed to {}",node.shiftY);
			updateLocationModel(freeplaneNode, null, node.shiftY);
		}
		//only for gui
		//freeplaneNode.fireNodeChanged(new NodeChangeEvent(freeplaneNode, "", "", ""));

		logger().debug("Actions.changeNode => refreshing lock access time");
		refreshLockAccessTime(freeplaneNode);
	}

	public static void removeNode(RemoveNodeRequest request) throws NodeNotFoundException, MapNotFoundException {
		final String mapId = request.getMapId();
		final String nodeId = request.getNodeId();
		logger().debug("Actions.removeNode => mapId:'{}'; nodeId:'{}'",mapId,nodeId);

		logger().debug("Actions.removeNode => selecting map");
		selectMap(request.getMapId());

		logger().debug("Actions.removeNode => retrieving node");
		final MMapController mapController = (MMapController) getModeController().getMapController();
		final NodeModel node = getNodeFromOpenMapById(nodeId);

		logger().debug("Actions.removeNode => deleting node");
		mapController.deleteNode(node);
	}

	public static void refreshLock (RefreshLockRequest request) throws MapNotFoundException, NodeNotFoundException{
		final String mapId = request.getMapId();
		final String nodeId = request.getNodeId();
		logger().debug("Actions.refreshLock => mapId:'{}'; nodeId:'{}'",mapId,nodeId);

		logger().debug("Actions.refreshLock => selecting map");
		selectMap(request.getMapId());

		logger().debug("Actions.refreshLock => retrieving node");
		final NodeModel node = getNodeFromOpenMapById(nodeId);

		refreshLockAccessTime(node);
	}

	public static void requestLock (RequestLockRequest request) throws MapNotFoundException, NodeAlreadyLockedException, NodeNotFoundException{
		final String mapId = request.getMapId();
		final String nodeId = request.getNodeId();
		final String username = request.getUsername();
		logger().debug("Actions.requestLock => mapId:'{}'; nodeId:'{}'; username:'{}'",mapId,nodeId,username);

		logger().debug("Actions.requestLock => selecting map");
		selectMap(request.getMapId());

		logger().debug("Actions.requestLock => retrieving freeplane node");
		final NodeModel node = getNodeFromOpenMapById(nodeId);

		logger().debug("Actions.requestLock => retrieving lock model");
		final LockModel lm = node.getExtension(LockModel.class);//.setLastAccess(System.currentTimeMillis());
		if(lm != null) { 
			// Lock exists. Check if it's own lock
			if(lm.getUsername().equals(request.getUsername())) {
				logger().info("Actions.requestLock => user requested lock for node he already locks");
				return;
			} else {
				logger().info("Actions.requestLock => node already locked"); 
				throw new NodeAlreadyLockedException("Locked by " + lm.getUsername() + ".");
			}
		} else {
			logger().debug("Actions.requestLock => no lock on node, creating lock...");
			//create lock
			final LockModel newLock = new LockModel(node,request.getUsername(),System.currentTimeMillis());
			node.addExtension(newLock);

			//add to lock list
			logger().debug("Actions.requestLock => adding node to locked node list");
			getOpenMindMapInfo(request.getMapId()).getLockedNodes().add(node);
		}
	}

	//TODO: Maybe send username as some kind of validation, too? (js)
	public static void releaseLock (ReleaseLockRequest request) throws MapNotFoundException, LockNotFoundException, NodeNotFoundException{
		final String mapId = request.getMapId();
		final String nodeId = request.getNodeId();
		logger().debug("Actions.releaseLock => mapId:'{}'; nodeId:'{}'",mapId,nodeId);

		logger().debug("Actions.releaseLock => selecting map");
		selectMap(mapId);

		logger().debug("Actions.releaseLock => retrieving node");
		final NodeModel node = getNodeFromOpenMapById(nodeId);

		logger().debug("Actions.releaseLock => retrieving lock");
		final LockModel lm = node.getExtension(LockModel.class);//.setLastAccess(System.currentTimeMillis());
		if(lm == null) { 
			// No lock available
			logger().error("Actions.releaseLock => lock not found, throwing exception");
			throw new LockNotFoundException("Lock for nodeId " + request.getNodeId() + " not found.");
		}

		//release lock
		logger().debug("Actions.releaseLock => releasing lock");
		node.removeExtension(LockModel.class);

		//remove form to lock list
		logger().debug("Actions.releaseLock => remove node from locked list");
		getOpenMindMapInfo(request.getMapId()).getLockedNodes().remove(node);
	}

	//TODO naming varies from action
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

	public static void closeAllOpenMaps(CloseAllOpenMapsRequest request) throws MapNotFoundException {
		logger().debug("Actions.closeAllOpenMaps => no parameters");
		Set<String> ids = new HashSet<String>(getOpenMindmapInfoMap().keySet()); 
		for(String mapId : ids) {
			logger().debug("Actions.closeAllOpenMaps => closing map with id '{}'",mapId);
			Utils.closeMap(mapId);
		}
	}

	public static void closeUnusedMaps(CloseUnusedMaps request) throws Exception {
		final long allowedMsSinceLastAccess = request.getUnusedSinceInMs();
		logger().debug("Actions.closeUnusedMaps => max ms since last access:'{}'",allowedMsSinceLastAccess);

		final long now = System.currentTimeMillis();
		for(final String mapId : new HashSet<String>(getOpenMindmapInfoMap().keySet())) {
			final OpenMindmapInfo omi = getOpenMindMapInfo(mapId);
			final long lastAccessTime = omi.getLastAccessTime();
			final long sinceLastAccess = now - lastAccessTime;
			final long sinceLastAccessInMinutes = sinceLastAccess / 60000;
			logger().debug("Actions.closeUnusedMaps => mapId:'{}'; lastAccess:{}; sinceLastAccess:{}",mapId,lastAccessTime,sinceLastAccess);

			if(sinceLastAccess > allowedMsSinceLastAccess) {
				//TODO tell ZooKeeper and save to hadoop
				closeMap(new CloseMapRequest(mapId));
				logger().info("Actions.closeUnusedMaps => map was closed, because it havent been used for about {} minutes.",sinceLastAccessInMinutes);
			}
		}
	}

	private static ModeController getModeController() {
		return RemoteController.getModeController();
	}

	private static org.freeplane.features.map.MapModel getOpenMap() {
		ModeController modeController = getModeController();
		return modeController.getMapController().getRootNode().getMap();
	}

	private static org.freeplane.features.map.NodeModel getNodeFromOpenMapById(final String nodeId) throws NodeNotFoundException {
		logger().debug("Actions.getNodeFromOpenMapById => nodeId: {}",nodeId);
		final NodeModel freeplaneNode = getModeController().getMapController().getNodeFromID(nodeId);

		if(freeplaneNode == null) {
			logger().error("Actions.getNodeFromOpenMapById => requested node not found; throwing exception");
			throw new NodeNotFoundException("Node with id '"+nodeId+"' not found.");
		}
		
		return freeplaneNode;
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