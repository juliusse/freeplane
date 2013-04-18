package org.freeplane.plugin.client.listeners;

import java.util.concurrent.ExecutionException;

import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.IMapChangeListener;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.NodeModel;
import org.freeplane.plugin.client.ClientController;
import org.freeplane.plugin.client.services.WS;

public class MapChangeListener implements IMapChangeListener {

	@Override
	public void onPreNodeMoved(NodeModel oldParent, int oldIndex, NodeModel newParent, NodeModel child, int newIndex) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPreNodeDelete(NodeModel oldParent, NodeModel selectedNode, int index) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onNodeMoved(NodeModel oldParent, int oldIndex, NodeModel newParent, NodeModel child, int newIndex) {
		if (!isUpdating()) {
			LogUtils.info("Node Moved. Sending to Webservice");
			webservice().moveNodeTo("5", newParent.getID(), child.getID(), newIndex);
		}
	}

	@Override
	public void onNodeInserted(NodeModel parent, NodeModel child, int newIndex) {
		if (!isUpdating()) {
			LogUtils.info("Node Added. Sending to Webservice");
			try {
				final String newNodeId = webservice().createNode("5", parent.getID()).get();
				child.setID(newNodeId);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onNodeDeleted(NodeModel parent, NodeModel child, int index) {
		if (!isUpdating()) {
			LogUtils.info("Node Deleted. Sending to Webservice");
			webservice().removeNode("5", child.getID());
		}

	}

	@Override
	public void mapChanged(MapChangeEvent event) {

	}

	private WS webservice() {
		return ClientController.webservice();
	}

	private boolean isUpdating() {
		return ClientController.isUpdating();
	}

}
