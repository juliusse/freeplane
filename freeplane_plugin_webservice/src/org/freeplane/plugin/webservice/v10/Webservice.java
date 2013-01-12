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

import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapWriter;
import org.freeplane.features.map.NodeChangeEvent;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mapio.MapIO;
import org.freeplane.features.mapio.mindmapmode.MMapIO;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.nodelocation.LocationModel;
import org.freeplane.plugin.webservice.WebserviceController;
import org.freeplane.plugin.webservice.v10.exceptions.MapNotFoundException;
import org.freeplane.plugin.webservice.v10.exceptions.NodeNotFoundException;
import org.freeplane.plugin.webservice.v10.model.DefaultNodeModel;
import org.freeplane.plugin.webservice.v10.model.LockModel;
import org.freeplane.plugin.webservice.v10.model.MapModel;

import com.sun.jersey.api.client.ClientResponse.Status;

@Path("/v1")
@Produces(MediaType.APPLICATION_JSON)
public class Webservice {

	//TODO add lock mechanism to Freeplane Node and our Node (discuss with dimitry?)

	static final Map<String, URL> openMapUrls = new HashMap<String, URL>();
	static final Map<String, Set<NodeModel>> lockedNodes = new HashMap<String, Set<NodeModel>>();

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
	@GET
	@Path("map/{mapId}/json")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMapModel(
			@PathParam("mapId") String mapId, 
			@QueryParam("nodeCount") @DefaultValue("-1") int nodeCount) 
					throws MapNotFoundException {

		boolean loadAllNodes = nodeCount == -1;
		ModeController modeController = getModeController();

		try {
			if(!WebserviceHelper.selectMap(mapId)) {
				if(mapId.startsWith("test_")) { //FOR DEBUGING
					openTestMap(mapId);
					if(!WebserviceHelper.selectMap(mapId)) {
						return Response.status(Status.NOT_FOUND).entity("Map not found!\n"+
								"Available test map ids: 'test_1','test_2','test_3','test_4','test_5'").build();
					}
				} else {
					return Response.status(Status.NOT_FOUND).entity("Map not found").build();
				}
			}
		} catch(Exception e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
		}


		org.freeplane.features.map.MapModel freeplaneMap = modeController.getController().getMap();
		if(freeplaneMap == null) { //when not mapMode
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Current mode not MapMode").build();
		}

		//create the MapModel for JSON
		MapModel mm = new MapModel(freeplaneMap,loadAllNodes);

		if(!loadAllNodes) {
			WebserviceHelper.loadNodesIntoModel(mm.root, nodeCount);
		}

		return Response.ok(mm).build();
	}

	private void openTestMap(String id) {
		try {
			//create file
			Random ran = new Random();
			String filename = ""+System.currentTimeMillis()+ran.nextInt(100);
			File file = File.createTempFile(filename, ".mm");
			file.deleteOnExit();

			InputStream in = this.getClass().getResourceAsStream("/mindmaps/"+id+".mm");
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
			openMapUrls.put(id, pathURL);

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
	 * @throws IOException 
	 */
	@GET
	@Path("map/{mapId}/xml")
	@Produces(MediaType.APPLICATION_XML)
	public Response getMapModelXml(
			@PathParam("mapId") String mapId) {
		Response selectMapResponse = selectMap(mapId);
		if (selectMapResponse != null){
			return selectMapResponse;
		}
		
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
	public Response closeMap(@PathParam("mapId") String mapId) {
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
	public Response openMindmap(InputStream uploadedInputStream) {

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
			String id = WebserviceHelper.getMapIdFromFile(file);
			URL pathURL = file.toURI().toURL();
			openMapUrls.put(id, pathURL);

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
	public Response closeServer() {

		Set<String> ids = openMapUrls.keySet(); 
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
	 */
	@GET
	@Path("map/{mapId}/node/{nodeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNode(
			@PathParam("mapId") String mapId,
			@PathParam("nodeId") String nodeId, 
			@QueryParam("nodeCount") @DefaultValue("-1") int nodeCount) {
		
		Response selectMapResponse = selectMap(mapId);
		if (selectMapResponse != null){
			return selectMapResponse;
		}
		
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


	@POST
	@Path("map/{mapId}/node/create")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response addNode(@PathParam("mapId") String mapId, String parentNodeId) { 
		Response selectMapResponse = selectMap(mapId);
		if (selectMapResponse != null){
			return selectMapResponse;
		}
		
		// TODO correct method handling
		// TODO how to call method correctly?

		ModeController modeController = getModeController();
		MapController mapController = modeController.getMapController();
		//get map
		org.freeplane.features.map.MapModel mm = modeController.getController().getMap();

		//get parent Node
		NodeModel parentNode = mapController.getNodeFromID(parentNodeId);

		if(parentNode == null)
			return Response.status(Status.BAD_REQUEST).entity("Node with id '"+parentNodeId+"' not found").build();

		//create new node
		NodeModel node = modeController.getMapController().newNode("", mm);

		//insert node
		mapController.insertNodeIntoWithoutUndo(node, parentNode);
		mapController.fireMapChanged(new MapChangeEvent(this, "node", "", ""));

		node.createID();
		return Response.ok(new DefaultNodeModel(node, false)).build();	
	}

	@PUT
	@Path("map/{mapId}/node")
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({ MediaType.APPLICATION_JSON })
	public Response changeNode(@PathParam("mapId") String mapId, DefaultNodeModel node) {
		Response response = selectMap(mapId);
		if(response != null) 
			return response;

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

		freeplaneNode.fireNodeChanged(new NodeChangeEvent(freeplaneNode, "", "", ""));
		//refreshLockAccessTime(NodeModel node);

		//TODO make changes according to data send with node
		//TODO add LockExtension if lock is in node
		//Response.ok(new DefaultNodeModel(node)).
		return Response.ok(new DefaultNodeModel(freeplaneNode,false)).build();	
	}

	@DELETE
	@Path("map/{mapId}/node")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response removeNode(String id) throws NodeNotFoundException {
		ModeController modeController = getModeController();
		NodeModel node = modeController.getMapController().getNodeFromID(id);
		if(node == null)
			throw new NodeNotFoundException("Node with id '"+id+"' not found");

		//TODO works correct?
		node.removeFromParent();
		node.fireNodeChanged(new NodeChangeEvent(node, "parent", "", ""));
		return Response.ok(new Boolean(true).toString()).build();
	}

	@PUT
	@Path("map/{mapId}/node/{nodeId}/refreshLock") 
	public Response refreshLock (@PathParam("mapId") String mapId, @PathParam("nodeId") String nodeId){
		Response selectMapResponse = selectMap(mapId);
		if (selectMapResponse != null){
			return selectMapResponse;
		}
		ModeController modeController = getModeController();
		NodeModel node = modeController.getMapController().getNodeFromID(nodeId);

		//node.getExtension(LockModel.class).setLastAccess(System.currentTimeMillis());
		refreshLockAccessTime(node);
		

		return Response.ok().build();
	}

	@GET
	@Path("map/{mapId}/unlockExpired/{sinceInMs}")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getExpiredLocks(@PathParam("mapId") String mapId, @PathParam("sinceInMs") int sinceInMs) {
		if (!lockedNodes.containsKey(mapId)){
			return Response.status(Status.NOT_FOUND).entity("Map not found.").build();
		}
		Set<NodeModel> nodes = lockedNodes.get(mapId);
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
		return Response.ok(expiredNodes.toArray()).build();
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
	 * @return
	 */
	private Response selectMap(String mapId){
		if(!WebserviceHelper.selectMap(mapId)) {
			return Response.status(Status.NOT_FOUND).entity("Map not found.").build();
		}
		return null;
	}
	
	/**
	 * refresh lastAccesTime of node lock  
	 * @param node Node with lock
	 */
	private void refreshLockAccessTime(NodeModel node){
		node.getExtension(LockModel.class).setLastAccess(System.currentTimeMillis());
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

}