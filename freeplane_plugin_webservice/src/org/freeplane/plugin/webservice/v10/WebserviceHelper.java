package org.freeplane.plugin.webservice.v10;

import java.io.File;
import java.net.URL;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.freeplane.features.mapio.MapIO;
import org.freeplane.features.mode.ModeController;
import org.freeplane.plugin.webservice.WebserviceController;
import org.freeplane.plugin.webservice.v10.model.NodeModelBase;
import org.w3c.dom.Document;

public final class WebserviceHelper {

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

	public static boolean selectMap(String id) throws Exception {
		if(!Webservice.openMapUrls.containsKey(id)) {
			return false;
		}

		URL pathURL = Webservice.openMapUrls.get(id);

		MapIO mio = WebserviceController.getInstance().getModeController().getExtension(MapIO.class);
		mio.newMap(pathURL);

		return true;
	}

	public static boolean closeMap(String id) throws Exception {
		ModeController modeController = Webservice.getModeController();
		//select map
		MapIO mio = modeController.getExtension(MapIO.class);
		mio.newMap(Webservice.openMapUrls.get(id));
		
		//close and remove map
		modeController.getController().close(true);
		Webservice.openMapUrls.remove(id);

		return true;
	}
}
