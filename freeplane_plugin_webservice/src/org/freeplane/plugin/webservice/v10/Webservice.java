package org.freeplane.plugin.webservice.v10;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
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
import org.freeplane.plugin.webservice.WebserviceController;
import org.freeplane.plugin.webservice.v10.exceptions.MapNotFoundException;
import org.freeplane.plugin.webservice.v10.exceptions.NodeNotFoundException;
import org.freeplane.plugin.webservice.v10.model.DefaultNodeModel;
import org.freeplane.plugin.webservice.v10.model.MapModel;

import com.sun.jersey.api.client.ClientResponse.Status;

@Path("/v1")
@Produces(MediaType.APPLICATION_JSON)
public class Webservice {

	static final Map<String, URL> openMapUrls;
	static {
		openMapUrls = new HashMap<String, URL>();
	}
	
	@GET
	@Path("status")
	public Response getStatus() {
		return Response.ok("Webservice V0.01").build();
	}

	/**
	 * returns a map as a JSON-Object
	 * @param id ID of map
	 * @param nodeCount soft limit of node count. When limit is reached, it only loads the outstanding child nodes of the current node.
	 * @return a map model
	 */
	@GET
	@Path("map/json/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMapModel(
			@PathParam("id") String id, 
			@QueryParam("nodeCount") @DefaultValue("-1") int nodeCount) 
					throws MapNotFoundException {

		boolean loadAllNodes = nodeCount == -1;
		ModeController modeController = getModeController();

		try {
			if(!WebserviceHelper.selectMap(id)) {
				if(id.startsWith("test_")) { //FOR DEBUGING
					openTestMap(id);
					if(!WebserviceHelper.selectMap(id)) {
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
	@Path("map/xml/{id}")
	@Produces(MediaType.APPLICATION_XML)
	public Response getMapModelXml(
			@PathParam("id") String id) {
		ModeController modeController = getModeController();

		try {
			if(!WebserviceHelper.selectMap(id)) {
				return Response.status(Status.NOT_FOUND).entity("Map not found").build();
			}
		} catch(Exception e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
		}

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
	@Path("map/{id}")
	public Response closeMap(@PathParam("id") String id) {

		try {
			WebserviceHelper.closeMap(id);
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
	@Path("close")
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
	@Path("node/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNode(
			@PathParam("id") String id, 
			@QueryParam("nodeCount") @DefaultValue("-1") int nodeCount) {
		ModeController modeController = getModeController();
		boolean loadAllNodes = nodeCount == -1;

		
		NodeModel freeplaneNode = modeController.getMapController().getNodeFromID(id);
		if(freeplaneNode == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(new NodeNotFoundException("Node with id '"+id+"' not found.")).build();
		}

		DefaultNodeModel node = new DefaultNodeModel(freeplaneNode,loadAllNodes);

		if(!loadAllNodes) {
			WebserviceHelper.loadNodesIntoModel(node, nodeCount);
		}

		return Response.ok(node).build();
	}

	@POST
	@Path("node/{parentNodeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public DefaultNodeModel addNode(@PathParam("parentNodeId") String parentNodeId,
			@QueryParam("nodeText") @DefaultValue("new Node") String nodeText, 
			@QueryParam("isHtml") @DefaultValue("false") Boolean isHtml) throws NodeNotFoundException {

		ModeController modeController = getModeController();
		MapController mapController = modeController.getMapController();
		//get map
		org.freeplane.features.map.MapModel mm = modeController.getController().getMap();

		//get parent Node
		NodeModel parentNode = mapController.getNodeFromID(parentNodeId);

		if(parentNode == null)
			throw new NodeNotFoundException("Node with id '"+parentNodeId+"' not found");

		//create new node
		NodeModel node = modeController.getMapController().newNode(nodeText, mm);

		//insert node
		mapController.insertNodeIntoWithoutUndo(node, parentNode);
		mapController.fireMapChanged(new MapChangeEvent(this, "node", "", ""));

		node.createID();
		return new DefaultNodeModel(node, false);	
	}

	@POST
	@Path("node/{nodeId}")
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({ MediaType.APPLICATION_JSON })
	public DefaultNodeModel changeNode(DefaultNodeModel node) throws NodeNotFoundException {
		//get map
		ModeController modeController = getModeController();
		org.freeplane.features.map.MapModel mm = modeController.getController().getMap();
		//get node
		NodeModel freeplaneNode = mm.getNodeForID(node.id);
		if(freeplaneNode == null)
			throw new NodeNotFoundException("Node with id '"+node.id+"' not found");

		//TODO do stuff
		//Response.ok(new DefaultNodeModel(node)).
		return new DefaultNodeModel(freeplaneNode,false);	
	}


	@DELETE
	@Path("removeNode/{id}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String removeNode(@PathParam("id")String id) throws NodeNotFoundException {
		ModeController modeController = getModeController();
		NodeModel node = modeController.getMapController().getNodeFromID(id);
		if(node == null)
			throw new NodeNotFoundException("Node with id '"+id+"' not found");

		node.removeFromParent();
		node.fireNodeChanged(new NodeChangeEvent(node, "parent", "", ""));
		return new Boolean(true).toString();
	}

	static ModeController getModeController() {
		return WebserviceController.getInstance().getModeController();
	}

	static org.freeplane.features.map.MapModel getOpenMap() {
		ModeController modeController = getModeController();
		return modeController.getMapController().getRootNode().getMap();
	}


}
