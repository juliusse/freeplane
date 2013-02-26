package org.freeplane.plugin.remote.v10;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.LinkedList;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.docear.messages.Messages.OpenMindMapRequest;
import org.docear.messages.exceptions.MapNotFoundException;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.mapio.MapIO;
import org.freeplane.features.mode.ModeController;
import org.freeplane.plugin.remote.RemoteController;
import org.freeplane.plugin.remote.v10.model.NodeModelBase;
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

	public static String getMapIdFromFile(File mindmapFile) {
		try {
			DocumentBuilderFactory dbFactory =  DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(mindmapFile);

			doc.getDocumentElement().normalize();

			return doc.getDocumentElement().getAttribute("dcr_id");

		} catch (Exception e) {}


		return null;
	}

	public static void selectMap(String id) throws MapNotFoundException {
		if(!RemoteController.getMapIdInfoMap().containsKey(id)) {
			throw new MapNotFoundException("Map with id "+ id+ " is not present.");
		}
		
		LogUtils.info("Changing map to "+id);
		URL pathURL = RemoteController.getMapIdInfoMap().get(id).getMapUrl();

		try{
			final MapIO mio = RemoteController.getMapIO();
			mio.newMap(pathURL);
		} catch (Exception e) {
			LogUtils.severe(e);
			throw new MapNotFoundException("Could not open Map with id "+ id);
		}
	}

	public static void closeMap(String id) throws MapNotFoundException {
		ModeController modeController = RemoteController.getModeController();
		//select map
		selectMap(id);
		
		//close and remove map
		modeController.getController().close(true);
		RemoteController.getMapIdInfoMap().remove(id);
	}
	
	public static void openTestMap(String id) {
		try {
			//create file
			Random ran = new Random();
			String filename = ""+System.currentTimeMillis()+ran.nextInt(100);
			File file = File.createTempFile(filename, ".mm");
			file.deleteOnExit();

			InputStream in = RemoteController.class.getResourceAsStream("/mindmaps/"+id+".mm");
			StringWriter writer = new StringWriter();
			IOUtils.copy(in, writer);
			String xmlMap = writer.toString();
			
			Actions.openMindmap(new OpenMindMapRequest(xmlMap));
		} catch (Exception e) {}
	}
}
