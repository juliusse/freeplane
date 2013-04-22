package org.freeplane.plugin.client.listeners;

import org.freeplane.features.map.IMapLifeCycleListener;
import org.freeplane.features.map.MapModel;

public class MapLifeCycleListener implements IMapLifeCycleListener{
	@Override
	public void onRemove(MapModel map) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCreate(MapModel map) {
//		LogUtils.info("map changed");
//
//		final MapView mapView = new MapView(map, ClientController.getModeController());
//		final NodeModel rootNode = map.getRootNode();
//		final NodeViewListener listener = new NodeViewListener(rootNode, mapView, mapView);
//		rootNode.addViewer(listener);
//		addNodeViewListeners(rootNode, listener, mapView);

	}

//	private static void addNodeViewListeners(NodeModel parentNode, NodeViewListener parentNodeView, MapView mapView) {
//		for (NodeModel node : parentNode.getChildren()) {
//			final NodeViewListener listener = new NodeViewListener(parentNode, mapView, parentNodeView);
//			node.addViewer(listener);
//			addNodeViewListeners(node, listener, mapView);
//		}
//	}
}
