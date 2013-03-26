package org.freeplane.plugin.remote.v10;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.docear.messages.Messages.AddNodeRequest;
import org.docear.messages.Messages.AddNodeResponse;
import org.docear.messages.Messages.ChangeNodeRequest;
import org.docear.messages.Messages.ChangeNodeResponse;
import org.docear.messages.Messages.CloseAllOpenMapsRequest;
import org.docear.messages.Messages.CloseMapRequest;
import org.docear.messages.Messages.CloseServerRequest;
import org.docear.messages.Messages.CloseUnusedMaps;
import org.docear.messages.Messages.GetNodeRequest;
import org.docear.messages.Messages.GetNodeResponse;
import org.docear.messages.Messages.ListenToUpdateOccurrenceRequest;
import org.docear.messages.Messages.ListenToUpdateOccurrenceRespone;
import org.docear.messages.Messages.MindmapAsJsonReponse;
import org.docear.messages.Messages.MindmapAsJsonRequest;
import org.docear.messages.Messages.MindmapAsXmlRequest;
import org.docear.messages.Messages.MindmapAsXmlResponse;
import org.docear.messages.Messages.OpenMindMapRequest;
import org.docear.messages.Messages.ReleaseLockRequest;
import org.docear.messages.Messages.ReleaseLockResponse;
import org.docear.messages.Messages.RemoveNodeRequest;
import org.docear.messages.Messages.RemoveNodeResponse;
import org.docear.messages.Messages.RequestLockRequest;
import org.docear.messages.Messages.RequestLockResponse;
import org.docear.messages.exceptions.CannotRetrieveMapIdException;
import org.docear.messages.exceptions.LockNotFoundException;
import org.docear.messages.exceptions.MapNotFoundException;
import org.docear.messages.exceptions.NodeAlreadyLockedException;
import org.docear.messages.exceptions.NodeNotFoundException;
import org.docear.messages.exceptions.NodeNotLockedByUserException;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.link.NodeLinks;
import org.freeplane.features.map.MapWriter;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mapio.mindmapmode.MMapIO;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.nodelocation.LocationModel;
import org.freeplane.plugin.remote.InternalMessages.ReleaseTimedOutLocks;
import org.freeplane.plugin.remote.RemoteController;
import org.freeplane.plugin.remote.v10.model.DefaultNodeModel;
import org.freeplane.plugin.remote.v10.model.LockModel;
import org.freeplane.plugin.remote.v10.model.MapModel;
import org.freeplane.plugin.remote.v10.model.OpenMindmapInfo;
import org.freeplane.plugin.remote.v10.model.updates.AddNodeUpdate;
import org.freeplane.plugin.remote.v10.model.updates.ChangeNodeUpdate;
import org.freeplane.plugin.remote.v10.model.updates.DeleteNodeUpdate;
import org.slf4j.Logger;

import scala.concurrent.Future;
import akka.dispatch.Futures;

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
		final OpenMindmapInfo info = RemoteController.getMapIdInfoMap().get(mapId);
		final String mapName = info.getName();
		final Long revision = info.getCurrentRevision();
		MapModel mm = new MapModel(freeplaneMap,mapName,revision, loadAllNodes);

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

	public static void openMindmap(final OpenMindMapRequest request) throws CannotRetrieveMapIdException {
		final String mapContent = request.getMindmapFileContent();
		final String mapName = request.getMindmapFileName();
		logger().debug("Actions.openMindmap => mindmapFileContent:'{}...'",mapContent.substring(0,Math.min(mapContent.length(), 20)));


		//create file
		final Random ran = new Random();
		final String filename = ""+System.currentTimeMillis()+ran.nextInt(100);
		final String tempDirPath = System.getProperty("java.io.tmpdir");
		final File file = new File(tempDirPath+"/docear/"+filename+".mm");
		logger().debug("Actions.openMindmap => temporary file '{}' was created",file.getAbsolutePath());

		try {
			logger().debug("Actions.openMindmap => writing mindmap content to file");
			FileUtils.writeStringToFile(file, mapContent);

			//put map in openMap Collection
			final String mapId = Utils.getMapIdFromFile(file);
			final URL pathURL = file.toURI().toURL();
			final OpenMindmapInfo info = new OpenMindmapInfo(pathURL,mapName);
			getOpenMindmapInfoMap().put(mapId, info);
			logger().debug("Actions.openMindmap => mindmap was put into openMindmapInfoMap ({} => {})",mapId,info.getMapUrl());

			//open map
			logger().debug("Actions.openMindmap => opening mindmap...");
			final MMapIO mio = (MMapIO)RemoteController.getMapIO();
			mio.newMap(pathURL);
			logger().debug("Actions.openMindmap => map successfully loaded and opened!");
		} catch(CannotRetrieveMapIdException e) {
			throw e;
		} catch (Exception e) {
			throw new AssertionError(e);
		} finally {
			logger().debug("Utils.closeMap => removing temporary file from file system");
			file.delete();
		}
	}

	public static Future<ListenToUpdateOccurrenceRespone> listenIfUpdateOccurs(ListenToUpdateOccurrenceRequest request) throws MapNotFoundException {
		final String mapId = request.getMapId();
		final OpenMindmapInfo info = getOpenMindMapInfo(mapId); 
		if(info == null)
			throw new MapNotFoundException("Map with id "+mapId+" was not present");

		//Polling to check for changes
		final long revision = info.getCurrentRevision();
		Future<ListenToUpdateOccurrenceRespone> future = Futures.future(new Callable<ListenToUpdateOccurrenceRespone>() {

			@Override
			public ListenToUpdateOccurrenceRespone call() throws Exception {
				final long pollingStart = System.currentTimeMillis();
				final long pollingDuration = 60000;
				final long pollingInterval = 25;

				while(System.currentTimeMillis() - pollingStart < pollingDuration) {
					if(info.getCurrentRevision() != revision) {
						logger().debug("listenIfUpdateOccurs => update occured at map {}",mapId);
						return new ListenToUpdateOccurrenceRespone(true);
					}
					Thread.sleep(pollingInterval);
				}
				return new ListenToUpdateOccurrenceRespone(false);
			}
		}, RemoteController.getActorSystem().dispatcher());


		return future;
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
		final String nodeAsJson = new DefaultNodeModel(node, false).toJsonString();
		getOpenMindMapInfo(mapId).addUpdate(new AddNodeUpdate(parentNodeId, nodeAsJson));
		return new AddNodeResponse(nodeAsJson);
	}

	public static ChangeNodeResponse changeNode(final ChangeNodeRequest request) throws MapNotFoundException, NodeNotFoundException,NodeNotLockedByUserException {
		final String mapId = request.getMapId();
		final DefaultNodeModel node = getObjectFromJsonString(DefaultNodeModel.class, request.getNodeAsJsonString());
		final String nodeId = node.id;
		final String username = request.getUsername();
		logger().debug("Actions.changeNode => mapId:'{}'; nodeAsJson:'{}'",mapId,request.getNodeAsJsonString());
		
		logger().debug("Actions.changeNode => selecting map");
		selectMap(mapId);

		//get node
		logger().debug("Actions.changeNode => retrieving node");
		final NodeModel freeplaneNode = getNodeFromOpenMapById(nodeId);
		//check if user has lock
		if(!hasUserLockOnNode(mapId, freeplaneNode, username)) {
			throw new NodeNotLockedByUserException("User has no lock on node");
		}

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

			try {
			nodeLinks.setHyperLink(new URI(node.link));
			} catch (URISyntaxException e) {
				logger().error("problem saving hyperlink",e);
			}
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

		final DefaultNodeModel changedNode = new DefaultNodeModel(freeplaneNode, false);
		final String nodeAsJson = changedNode.toJsonString();
		final ChangeNodeUpdate update = new ChangeNodeUpdate(nodeAsJson);

		getOpenMindMapInfo(mapId).addUpdate(update);
		return new ChangeNodeResponse(nodeAsJson);
	}

	public static RemoveNodeResponse removeNode(RemoveNodeRequest request) throws NodeNotFoundException, MapNotFoundException {
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

		final OpenMindmapInfo info = getOpenMindMapInfo(mapId);
		info.addUpdate(new DeleteNodeUpdate(nodeId));

		return new RemoveNodeResponse(true);
	}

//	public static RefreshLockResponse refreshLock (RefreshLockRequest request) throws MapNotFoundException, NodeNotFoundException{
//		final String mapId = request.getMapId();
//		final String nodeId = request.getNodeId();
//		logger().debug("Actions.refreshLock => mapId:'{}'; nodeId:'{}'",mapId,nodeId);
//
//		logger().debug("Actions.refreshLock => selecting map");
//		selectMap(request.getMapId());
//
//		logger().debug("Actions.refreshLock => retrieving node");
//		final NodeModel node = getNodeFromOpenMapById(nodeId);
//
//		refreshLockAccessTime(node);
//		return new RefreshLockResponse(true);
//	}

	public static RequestLockResponse requestLock (RequestLockRequest request) throws MapNotFoundException, NodeAlreadyLockedException, NodeNotFoundException{
		final String mapId = request.getMapId();
		final String nodeId = request.getNodeId();
		final String username = request.getUsername();
		logger().debug("Actions.requestLock => mapId:'{}'; nodeId:'{}'; username:'{}'",mapId,nodeId,username);

		logger().debug("Actions.requestLock => selecting map");
		selectMap(mapId);

		logger().debug("Actions.requestLock => retrieving freeplane node");
		final NodeModel node = getNodeFromOpenMapById(nodeId);

		logger().debug("Actions.requestLock => retrieving lock model");
		final LockModel lockModel = node.getExtension(LockModel.class);

		if(lockModel == null) { //no lock present
			logger().debug("Actions.requestLock => no lock on node, creating lock...");
			final String changedNodeJson = addLockToNode(mapId, node, username);
			return new RequestLockResponse(true, changedNodeJson);
		} else if(username.equals(lockModel.getUsername())) { //refresh from locking user 
			refreshLockAccessTime(node);
			return new RequestLockResponse(true, null);
		} else { //already locked by someone else
			return new RequestLockResponse(false, null);
		}
	}

	public static ReleaseLockResponse releaseLock (ReleaseLockRequest request) throws MapNotFoundException, LockNotFoundException, NodeNotFoundException{
		final String mapId = request.getMapId();
		final String nodeId = request.getNodeId();
		final String username = request.getUsername();
		logger().debug("Actions.releaseLock => mapId:'{}'; nodeId:'{}'; username: {}",mapId,nodeId,username);

		logger().debug("Actions.releaseLock => selecting map");
		selectMap(mapId);

		logger().debug("Actions.releaseLock => retrieving node");
		final NodeModel node = getNodeFromOpenMapById(nodeId);

		logger().debug("Actions.releaseLock => retrieving lock");
		final LockModel lm = node.getExtension(LockModel.class);
		if(lm == null) { 
			// No lock available, nothing to release... just quit
			logger().warn("Actions.releaseLock => no lock present");
			//throw new LockNotFoundException("Lock for nodeId " + request.getNodeId() + " not found.");
			return new ReleaseLockResponse(true, null);
		}

		//check if correct user
		if(username.equals(lm.getUsername())) {
			//release lock
			logger().debug("Actions.releaseLock => releasing lock");
			final String nodeAsJson = releaseLockFromNode(mapId, node);

			return new ReleaseLockResponse(true, nodeAsJson);
		} else {
			return new ReleaseLockResponse(false, null);
		}
	}


	public static void releaseTimedOutLocks(ReleaseTimedOutLocks request) throws MapNotFoundException, JsonGenerationException, JsonMappingException, IOException{
		final Long millisSinceRequest = request.getMillisecondsSinceRequest();

		for(Entry<String, OpenMindmapInfo> entry : getOpenMindmapInfoMap().entrySet()) {
			final String mapId = entry.getKey();
			final OpenMindmapInfo info = entry.getValue();
			final Set<NodeModel> lockedNodes = new HashSet<NodeModel>(info.getLockedNodes());

			for(NodeModel lockedNode : lockedNodes) {
				LockModel lock = lockedNode.getExtension(LockModel.class);
				long timeDiff = System.currentTimeMillis() - lock.getLastAccess();  
				if (timeDiff > millisSinceRequest){
					releaseLockFromNode(mapId, lockedNode);
				} 
			}

		}
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
				try {
					Utils.openTestMap(mapId);
				} catch (CannotRetrieveMapIdException e) {
					throw new AssertionError(e);
				}
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

	private static String addLockToNode(String mapId, NodeModel freeplaneNode, String username) {
		final OpenMindmapInfo info = getOpenMindMapInfo(mapId);
		if(freeplaneNode.getExtension(LockModel.class) == null) {
			final LockModel lockModel = new LockModel(freeplaneNode,username,System.currentTimeMillis());
			
			//add node to locked list
			logger().debug("Actions.addLockToNode => adding node to locked node list");
			info.addLockedNode(freeplaneNode);
			//add lock
			freeplaneNode.addExtension(lockModel);

			final String nodeAsJson = new DefaultNodeModel(freeplaneNode,false).toJsonString();
			//add change to revision list
			info.addUpdate(new ChangeNodeUpdate(nodeAsJson));
			return nodeAsJson;
		} else {
			throw new AssertionError("Tried to add Lock to a Node with a Lock present");
		}
	}

	private static String releaseLockFromNode(String mapId, NodeModel freeplaneNode) {
		final OpenMindmapInfo info = getOpenMindMapInfo(mapId);
		if(freeplaneNode.getExtension(LockModel.class) != null) {
			
			//remove node from locked list
			logger().debug("Actions.releaseLockFromNode => remove node from locked list");
			info.removeLockedNode(freeplaneNode);
			//remove lock
			freeplaneNode.removeExtension(LockModel.class);

			final String nodeAsJson = new DefaultNodeModel(freeplaneNode,false).toJsonString();
			//add change to revision list
			info.addUpdate(new ChangeNodeUpdate(nodeAsJson));
			return nodeAsJson;
		} else {
			throw new AssertionError("Tried to remove Lock from a Node without a Lock");
		}
	}
	
	private static boolean hasUserLockOnNode(String mapId, NodeModel node, String userName) {
		LockModel lm = node.getExtension(LockModel.class);
		if(lm == null) { //no lock at all
			return false;
		} else if(userName.equals(lm.getUsername())) {
			return true;
		} else { //locked by someone else
			return false;
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

	private static OpenMindmapInfo getOpenMindMapInfo(String mapId) {
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
	
	private static <A> A getObjectFromJsonString(Class<A> clazz, String json) {
		A result = null;

		try {
			result = objectMapper.readValue(json, clazz);
		} catch (Exception e) {
			LogUtils.severe("Error while parsing JSON-String to object!", e);
			throw new AssertionError(e);
		}		

		return result;
	}

	private static Logger logger() {
		return RemoteController.getLogger();
	}

}