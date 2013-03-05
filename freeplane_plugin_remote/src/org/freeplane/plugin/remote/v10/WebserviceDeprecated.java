package org.freeplane.plugin.remote.v10;

import java.io.IOException;
import java.io.StringWriter;

import org.docear.messages.exceptions.MapNotFoundException;
import org.freeplane.features.map.MapWriter;
import org.freeplane.features.mode.ModeController;
import org.freeplane.plugin.remote.RemoteController;
import org.freeplane.plugin.remote.v10.model.MapModel;

//@Path("/v1")
//@Produces(MediaType.APPLICATION_JSON)
public class WebserviceDeprecated {
	
	/**
	 * returns the current map as a JSON-Object
	 * @param id ID of map
	 * @param nodeCount soft limit of node count. When limit is reached, it only loads the outstanding child nodes of the current node.
	 * @return a map model
	 */
//	@GET
//	@Path("map.json")
//	@Produces(MediaType.APPLICATION_JSON)
	public MapModel getOpenMapAsJson( 
			//@QueryParam("nodeCount") @DefaultValue("-1") 
			int nodeCount) throws MapNotFoundException {
		boolean loadAllNodes = nodeCount == -1;
		ModeController modeController = RemoteController.getModeController();


		org.freeplane.features.map.MapModel freeplaneMap = modeController.getController().getMap();

		MapModel mm = new MapModel(freeplaneMap,loadAllNodes);
		if(!loadAllNodes) {
			Utils.loadNodesIntoModel(mm.root, nodeCount);
		}

		return mm;
	}
	
	/**
	 * returns a map as a JSON-Object
	 * @param id ID of map
	 * @param nodeCount soft limit of node count. When limit is reached, it only loads the outstanding child nodes of the current node.
	 * @return a map model
	 * @throws IOException 
	 */
//	@GET
//	@Path("map.xml")
//	@Produces(MediaType.APPLICATION_ATOM_XML)
	public String getOpenMapAsXml() throws MapNotFoundException, IOException {
		ModeController modeController = RemoteController.getModeController();
		org.freeplane.features.map.MapModel freeplaneMap = modeController.getController().getMap();

		StringWriter writer = new StringWriter();
		modeController.getMapController().getMapWriter().writeMapAsXml(freeplaneMap, writer, MapWriter.Mode.EXPORT, true, true);


		return writer.toString();
	}
	
//	@PUT
//	@Path("selectMindmap/{id}")
//	public Response selectMindmap(//@PathParam("id") 
//			String id) {
//		URL mapUrl = Actions.getOpenMindMapInfo(id).getMapUrl();
//		MapIO mio = RemoteController.getMapIO();
//		try {
//			mio.newMap(mapUrl);
//		} catch (Exception e) {
//			return Response.status(Status.NOT_ACCEPTABLE).entity("ID not found").build();
//		}
//		return Response.ok().build();
//	}
	
//	@GET
//	@Produces({ MediaType.TEXT_PLAIN  })
//	@Path("search/mapId/{mapId}/query/{query}")
//	public String search(@PathParam("mapId") Long mapId, @PathParam("query") String query) {
//		return String.format("mapId = %s, searchString = %s", mapId, query);
//	}

//	@GET
//	@Path("addNodeToRootNode")
//	@Produces({ MediaType.APPLICATION_JSON })
//	public String addNodeToRootNode( @DefaultValue("Sample Text") @QueryParam("text")String text) {
//		ModeController modeController = Actions.getModeController();
//		org.freeplane.features.map.MapModel mm = Actions.getOpenMap();
//		NodeModel root = modeController.getMapController().getRootNode();
//		NodeModel node = modeController.getMapController().newNode(text, mm);
//		root.insert(node);
//		modeController.getMapController().fireMapChanged(new MapChangeEvent(this, "node", "", ""));
//		node.createID();
//		return node.getID();
//	}	


//	@GET
//	@Path("addNodeToSelectedNode/query")
//	@Produces({ MediaType.APPLICATION_JSON })
//	public String addNodeToSelectedNodeQuery(@QueryParam("text")String text) {
//		ModeController modeController = Actions.getModeController();
//		org.freeplane.features.map.MapModel mm = Actions.getOpenMap();
//		NodeModel selectedNode = modeController.getMapController().getSelectedNodes().iterator().next();
//		if (text == null) text = "New Node.";
//		NodeModel node = modeController.getMapController().newNode(text, mm);
//		selectedNode.insert(node);
//		modeController.getMapController().fireMapChanged(new MapChangeEvent(this, "node", "", ""));
//		node.createID();
//		return node.getID();
//	}


	// testing methods
//	@GET
//	@Path("addNodeToRootNode/query")
//	@Produces({ MediaType.APPLICATION_JSON })
//	public String addNodeToRootNodeQuery(@QueryParam("text")String text) {
//		ModeController modeController = Actions.getModeController();
//		org.freeplane.features.map.MapModel mm = Actions.getOpenMap();
//		NodeModel root = modeController.getMapController().getRootNode();
//		if (text == null) text = "New Node.";
//		NodeModel node = modeController.getMapController().newNode(text, mm);
//		root.insert(node);
//		modeController.getMapController().fireMapChanged(new MapChangeEvent(this, "node", "", ""));
//		node.createID();
//		return node.getID();
//	}

//	@GET
//	@Path("addNodeToSelectedNode")
//	@Produces({ MediaType.APPLICATION_JSON })
//	public String addNodeToSelectedNode(@DefaultValue("Sample Text") @QueryParam("text")String text) {
//		ModeController modeController = Actions.getModeController();
//		org.freeplane.features.map.MapModel mm = Actions.getOpenMap();
//		NodeModel selectedNode = modeController.getMapController().getSelectedNodes().iterator().next();
//		NodeModel node = modeController.getMapController().newNode(text, mm);
//		selectedNode.insert(node);
//		modeController.getMapController().fireMapChanged(new MapChangeEvent(this, "node", "", ""));
//		node.createID();
//		return node.getID();
//	}

//	@GET
//	@Path("sampleNode")
//	@Produces({ MediaType.APPLICATION_JSON })
//	public Boolean sampleNode() {
//		ModeController modeController = RemoteController.getModeController();
//
//
//
//		org.freeplane.features.map.MapModel mm = Actions.getOpenMap();
//		NodeModel root = modeController.getMapController().getRootNode();
//		modeController.getMapController().select(modeController.getMapController().getRootNode());
//		NodeModel node = modeController.getMapController().newNode("Sample Text", mm);
//		modeController.getMapController().insertNodeIntoWithoutUndo(node, root);
//		modeController.getMapController().fireMapChanged(new MapChangeEvent(this, node, null, node));
//		//					AFreeplaneAction action = modeController.getAction("NewChildAction");
//		//					action.actionPerformed(null);
//
//		node.setUserObject("3 Seconds to deletion");
//		node.fireNodeChanged(new NodeChangeEvent(node, "userObject", "blub", "bla"));
//		modeController.getMapController().fireMapChanged(new MapChangeEvent(this, node, null, node));
//		try {Thread.sleep(1000);} catch(Throwable t) {}
//
//		node.setUserObject("2 Seconds to deletion");
//		node.fireNodeChanged(new NodeChangeEvent(node, "userObject", "blub", "bla"));
//		try {Thread.sleep(1000);} catch(Throwable t) {}
//		node.setUserObject("1 Seconds to deletion");
//		node.fireNodeChanged(new NodeChangeEvent(node, "userObject", "blub", "bla"));
//		try {Thread.sleep(1000);} catch(Throwable t) {}
//
//		node.removeFromParent();
//		modeController.getMapController().fireMapChanged(new MapChangeEvent(this, node, null, node));
//		try {Thread.sleep(3000);} catch(Throwable t) {}		
//		return Boolean.TRUE;
//	}
	
}
