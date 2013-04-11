package org.freeplane.plugin.remote.v10;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.LinkedList;

import org.apache.commons.io.IOUtils;
import org.docear.messages.Messages.OpenMindMapRequest;
import org.docear.messages.exceptions.CannotRetrieveMapIdException;
import org.docear.messages.exceptions.MapNotFoundException;
import org.freeplane.features.mapio.MapIO;
import org.freeplane.plugin.remote.RemoteController;
import org.freeplane.plugin.remote.v10.model.NodeModelBase;
import org.slf4j.Logger;

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

	public static void selectMap(final String mapId) throws MapNotFoundException {
		logger().debug("Utils.selectMap => mapId:'{}'",mapId);
		
		if(!RemoteController.getMapIdInfoMap().containsKey(mapId)) {
			logger().error("Utils.selectMap => map not found");
			throw new MapNotFoundException("Map with id "+ mapId+ " is not present.",mapId);
		}
		
		logger().debug("Utils.selectMap => Changing map to '{}'",mapId);
		URL pathURL = RemoteController.getMapIdInfoMap().get(mapId).getMapUrl();

		try{
			final MapIO mio = RemoteController.getMapIO();
			mio.newMap(pathURL);
			logger().debug("Utils.selectMap => Map succesfully selected");
		} catch (Exception e) {
			logger().error("Utils.selectMap => Error while selecting map with id '{}'",mapId);
			throw new MapNotFoundException("Could not open Map with id "+ mapId,e,mapId);
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
		
		logger().debug("Utils.closeMap => removing map info from MapIdInfoMap");
		RemoteController.getMapIdInfoMap().remove(mapId);
	}
	
	public static void openTestMap(String id) throws CannotRetrieveMapIdException {
		InputStream in = null;
		try {
			final String mapName = id+".mm";
			in = RemoteController.class.getResourceAsStream("/mindmaps/"+mapName);
			StringWriter writer = new StringWriter();
			IOUtils.copy(in, writer);
			final String xmlMap = writer.toString();
			
			Actions.openMindmap(new OpenMindMapRequest(id, xmlMap,mapName));
		} catch (IOException e) {}
		finally {
			IOUtils.closeQuietly(in);
		}
	}
	
	private static Logger logger() {
		return RemoteController.getLogger();
	}

}
