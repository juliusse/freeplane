package org.freeplane.plugin.remote.v10;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.docear.messages.Messages.OpenMindMapRequest;
import org.docear.messages.exceptions.CannotRetrieveMapIdException;
import org.docear.messages.exceptions.MapNotFoundException;
import org.freeplane.features.mapio.MapIO;
import org.freeplane.plugin.remote.RemoteController;
import org.freeplane.plugin.remote.v10.model.NodeModelBase;
import org.freeplane.plugin.remote.v10.model.OpenMindmapInfo;
import org.slf4j.Logger;
import org.w3c.dom.Document;

public final class Utils {

	public static void loadNodesIntoModel(NodeModelBase node, int nodeCount) {
		LinkedList<NodeModelBase> nodeQueue = new LinkedList<NodeModelBase>();
		nodeQueue.add(node);
		while(nodeCount > 0 && !nodeQueue.isEmpty()) {
			NodeModelBase curNode = nodeQueue.pop();

			nodeCount -= curNode.loadChildren(false);
			for(NodeModelBase child : curNode.getAllChildren()) {
				nodeQueue.add(child);
			}
		}
	}

	public static String getMapIdFromFile(File mindmapFile) throws CannotRetrieveMapIdException {
		logger().debug("Utils.getMapIdFromFile => retrieving mapId from '{}'",mindmapFile.getAbsolutePath());
		try {
			logger().debug("Utils.getMapIdFromFile => parsing document as XML");
			DocumentBuilderFactory dbFactory =  DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(mindmapFile);

			doc.getDocumentElement().normalize();
			logger().debug("Utils.getMapIdFromFile => sending value of attribute 'dcr_id'");
			return doc.getDocumentElement().getAttribute("dcr_id");

		} catch(Exception e) {
			logger().error("Utils.getMapIdFromFile => exception catched, CannotRetrieveMapIdException thrown");
			throw new CannotRetrieveMapIdException("Cannot retrieve map id from file",e);
		}
	}

	public static void selectMap(final String mapId) throws MapNotFoundException {
		logger().debug("Utils.selectMap => mapId:'{}'",mapId);
		
		if(!RemoteController.getMapIdInfoMap().containsKey(mapId)) {
			logger().error("Utils.selectMap => map not found");
			throw new MapNotFoundException("Map with id "+ mapId+ " is not present.");
		}
		
		logger().debug("Utils.selectMap => Changing map to '{}'",mapId);
		URL pathURL = RemoteController.getMapIdInfoMap().get(mapId).getMapUrl();

		try{
			final MapIO mio = RemoteController.getMapIO();
			mio.newMap(pathURL);
			logger().debug("Utils.selectMap => Map succesfully selected");
		} catch (Exception e) {
			logger().error("Utils.selectMap => Error while selecting map with id '{}'",mapId);
			throw new MapNotFoundException("Could not open Map with id "+ mapId,e);
		}
	}

	public static void closeMap(final String mapId) throws MapNotFoundException {
		logger().debug("Utils.closeMap => mapId:'{}'",mapId);
		
		//select map
		logger().debug("Utils.closeMap => selecting map");
		selectMap(mapId);
		
		//close and remove map
		logger().debug("Utils.closeMap => closing map");
		RemoteController.getModeController().getController().close(true);
		
		//remove file from filesystem
		try {
			final OpenMindmapInfo mmInfos = RemoteController.getMapIdInfoMap().get(mapId);
			logger().debug("Utils.closeMap => removing temporary file '{}' from file system",mmInfos.getMapUrl());
			new File(mmInfos.getMapUrl().toURI()).delete();
		} catch (Exception e) {
			logger().error("Utils.closeMap => could not delete map file.",e);
		}
		
		logger().debug("Utils.closeMap => removing map info from MapIdInfoMap");
		RemoteController.getMapIdInfoMap().remove(mapId);
	}
	
	public static void openTestMap(String id) {
		InputStream in = null;
		try {
			final String mapName = id+".mm";
			in = RemoteController.class.getResourceAsStream("/mindmaps/"+mapName);
			StringWriter writer = new StringWriter();
			IOUtils.copy(in, writer);
			final String xmlMap = writer.toString();
			
			Actions.openMindmap(new OpenMindMapRequest(xmlMap,mapName));
		} catch (Exception e) {}
		finally {
			try{in.close();}catch(Exception e){}
		}
	}
	
	private static Logger logger() {
		return RemoteController.getLogger();
	}

}
