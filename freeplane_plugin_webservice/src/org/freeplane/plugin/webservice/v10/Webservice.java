package org.freeplane.plugin.webservice.v10;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapWriter;
import org.freeplane.features.map.NodeChangeEvent;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mapio.MapIO;
import org.freeplane.features.mapio.mindmapmode.MMapIO;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.nodelocation.LocationModel;
import org.freeplane.plugin.webservice.Messages.AddNodeRequest;
import org.freeplane.plugin.webservice.Messages.AddNodeResponse;
import org.freeplane.plugin.webservice.Messages.MindmapAsJsonRequest;
import org.freeplane.plugin.webservice.WebserviceController;
import org.freeplane.plugin.webservice.v10.exceptions.MapNotFoundException;
import org.freeplane.plugin.webservice.v10.exceptions.NodeNotFoundException;
import org.freeplane.plugin.webservice.v10.model.DefaultNodeModel;
import org.freeplane.plugin.webservice.v10.model.LockModel;
import org.freeplane.plugin.webservice.v10.model.MapModel;
import org.freeplane.plugin.webservice.v10.model.OpenMindmapInfo;

import com.sun.jersey.api.client.ClientResponse.Status;

@Path("/v1")
@Produces(MediaType.APPLICATION_JSON)
public class Webservice {

	static final Object lock = new Object();
	static final Map<String, OpenMindmapInfo> mapIdInfoMap = new HashMap<String, OpenMindmapInfo>();

	@GET
	@Path("status")
	public Response getStatus() {
		return Response.ok("Webservice V0.05").build();
	}

	/**
	 * returns a map as a JSON-Object
	 * @param id ID of map
	 * @param nodeCount soft limit of node count. When limit is reached, it only loads the outstanding child nodes of the current node.
	 * @return a map model
	 */
	public static String getMapModel(MindmapAsJsonRequest request) throws MapNotFoundException {

		final int nodeCount = request.getNodeCount();
		final String mapId = request.getId();

		boolean loadAllNodes = nodeCount == -1;
		ModeController modeController = getModeController();

		try {
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
		} catch(Exception e) {
			//TODO real handling
			throw new RuntimeException();
			//return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
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
		if(result != null) 
			return result;
		else
			throw new AssertionError("buildJSON");
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
	@GET
	@Path("map/{mapId}/xml")
	@Produces(MediaType.APPLICATION_XML)
	public synchronized Response getMapModelXml(
			@PathParam("mapId") String mapId) throws MapNotFoundException {
		selectMap(mapId);

		ModeController modeController = getModeController();
		org.freeplane.features.map.MapModel freeplaneMap = modeController.getController().getMap();
		if(freeplaneMap == null) { //when not mapMode
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Current mode not MapMode").build();
		}

		StringWriter writer = new StringWriter();
		try {
			modeController.getMapController().getMapWriter().writeMapAsXml(freeplaneMap, writer, MapWriter.Mode.EXPORT, true, true);
		} catch (IOException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
		}

		return Response.ok(writer.toString()).build();
	}

	/**
	 * closes a map on the server
	 * @param id
	 * @return
	 */
	@DELETE
	@Path("map/{mapId}")
	public synchronized Response closeMap(@PathParam("mapId") String mapId) {
		try {
			WebserviceHelper.closeMap(mapId);
			return Response.ok().build();
		} catch (Exception e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
		}
	}

	@PUT
	@Path("map")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public synchronized Response openMindmap(InputStream uploadedInputStream) {

		try {
			//create file
			Random ran = new Random();
			String filename = ""+System.currentTimeMillis()+ran.nextInt(100);
			File file = File.createTempFile(filename, ".mm");
			file.deleteOnExit();

			//fill file from inputstream
			FileOutputStream out = new FileOutputStream(file);
			byte[] buffer = new byte[1024];
			int length;
			while((length = uploadedInputStream.read(buffer, 0, buffer.length)) != -1) {
				out.write(buffer, 0, length);
			}
			out.flush();
			out.close();

			//put map in openMap Collection
			String mapId = WebserviceHelper.getMapIdFromFile(file);
			URL pathURL = file.toURI().toURL();
			mapIdInfoMap.put(mapId, new OpenMindmapInfo(pathURL));

			//open map
			ModeController modeController = getModeController();

			MMapIO mio = (MMapIO)modeController.getExtension(MapIO.class);
			mio.newMap(pathURL);
		} catch (Exception e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
		}

		return Response.status(200).entity("File uploaded succesfully").build();
	}

	@GET
	@Path("shutdown")
	public synchronized Response closeServer() {

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

		return Response.ok().build();
	}

	/**
	 * returns a node as a JSON-Object
	 * @param id ID of node
	 * @param nodeCount soft limit of node count. When limit is reached, it only loads the outstanding child nodes of the current node.
	 * @return a node model
	 * @throws MapNotFoundException 
	 */
	@GET
	@Path("map/{mapId}/node/{nodeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public synchronized Response getNode(
			@PathParam("mapId") String mapId,
			@PathParam("nodeId") String nodeId, 
			@QueryParam("nodeCount") @DefaultValue("-1") int nodeCount) throws MapNotFoundException {

		selectMap(mapId);

		ModeController modeController = getModeController();
		boolean loadAllNodes = nodeCount == -1;


		NodeModel freeplaneNode = modeController.getMapController().getNodeFromID(nodeId);

		if(freeplaneNode == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(new NodeNotFoundException("Node with id '"+nodeId+"' not found.")).build();
		}

		DefaultNodeModel node = new DefaultNodeModel(freeplaneNode,loadAllNodes);

		if(!loadAllNodes) {
			WebserviceHelper.loadNodesIntoModel(node, nodeCount);
		}

		return Response.ok(node).build();
	}

	public static AddNodeResponse addNode(AddNodeRequest request) throws MapNotFoundException, NodeNotFoundException {
		final String mapId = request.getMapId();
		final String parentNodeId = request.getParentNodeId(); 
		
		selectMap(mapId);

		ModeController modeController = getModeController();
		MapController mapController = modeController.getMapController();
		//get map
		org.freeplane.features.map.MapModel mm = modeController.getController().getMap();

		//get parent Node
		NodeModel parentNode = mapController.getNodeFromID(parentNodeId);

		if(parentNode == null)
			throw new NodeNotFoundException("Node with id '"+parentNodeId+"' not found");

		//create new node
		NodeModel node = modeController.getMapController().newNode("", mm);

		//insert node
		mapController.insertNodeIntoWithoutUndo(node, parentNode);
		mapController.fireMapChanged(new MapChangeEvent(null, "node", "", ""));

		node.createID();
		
		return new AddNodeResponse(new DefaultNodeModel(node, false));
		//return Response.ok(new DefaultNodeModel(node, false)).build();	
	}

	@PUT
	@Path("map/{mapId}/node")
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({ MediaType.APPLICATION_JSON })
	public synchronized Response changeNode(@PathParam("mapId") String mapId, DefaultNodeModel node) throws MapNotFoundException {
		selectMap(mapId);

		//get map
		ModeController modeController = getModeController();
		org.freeplane.features.map.MapModel mm = modeController.getController().getMap();

		//get node
		NodeModel freeplaneNode = mm.getNodeForID(node.id);
		if(freeplaneNode == null)
			return Response.status(Status.BAD_REQUEST).entity("Node with id '"+node.id+"' not found").build();

		if(node.folded != null) {
			freeplaneNode.setFolded(node.folded);
		}
		if(node.isHtml != null) {
			freeplaneNode.setXmlText(node.nodeText);
		}
		if(node.attributes != null) {
			//TODO set attributes right
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
			//TODO handle
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

		//Response.ok(new DefaultNodeModel(node)).
		return Response.ok().build();	
	}

	@DELETE
	@Path("map/{mapId}/node")
	@Produces({ MediaType.APPLICATION_JSON })
	public synchronized Response removeNode(String id) throws NodeNotFoundException {
		ModeController modeController = getModeController();
		NodeModel node = modeController.getMapController().getNodeFromID(id);
		if(node == null)
			throw new NodeNotFoundException("Node with id '"+id+"' not found");

		//TODO works correct?
		node.removeFromParent();
		node.fireNodeChanged(new NodeChangeEvent(node, "parent", "", ""));
		return Response.ok(new Boolean(true).toString()).build();
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

	private void updateLocationModel(NodeModel freeplaneNode, Integer hGap, Integer Shifty) {
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
		ObjectMapper mapper = new ObjectMapper();
		String result = null;

		try {
			result = mapper.writeValueAsString(object);
		} catch (Exception e) {
			e.printStackTrace();
		}		

		return result;
	}

}