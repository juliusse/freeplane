package org.freeplane.plugin.client.listeners;

import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.INodeChangeListener;
import org.freeplane.features.map.NodeChangeEvent;
import org.freeplane.plugin.client.ClientController;
import org.freeplane.plugin.client.services.WS;

public class NodeChangeListener implements INodeChangeListener {

	@Override
	public void nodeChanged(final NodeChangeEvent event) {
		if (!isUpdating()) {
			if (event != null && event.getProperty() != null)
				LogUtils.info("attribute: " + event.getProperty().toString());
			if (event != null && event.getNewValue() != null)
				LogUtils.info("value: " + event.getNewValue().toString());

			if (event.getProperty() != null && event.getProperty().equals("node_text")) {
				LogUtils.info("node_text");
				webservice().changeNode("5", event.getNode().getID(), "nodeText", event.getNewValue());
//				final ListenableFuture<Boolean> future = webservice().changeNode("5", event.getNode().getID(), "nodeText", event.getNewValue());
//				Futures.addCallback(future, new FutureCallback<Boolean>() {
//					@Override
//					public void onFailure(Throwable t) {
//						t.printStackTrace();
//					}
//
//					@Override
//					public void onSuccess(Boolean success) {
//						if (!success) {
//							isUpdating(true);
//							event.getNode().setText(event.getOldValue().toString());
//							isUpdating(false);
//						}
//					}
//				});
			}
			// node_text

		}
	}

	public static WS webservice() {
		return ClientController.webservice();
	}

	public static boolean isUpdating() {
		return ClientController.isUpdating();
	}

	public static void isUpdating(boolean value) {
		ClientController.isUpdating(value);
	}
}
