package org.freeplane.plugin.client.listeners;

import java.awt.Container;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.INodeView;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.NodeChangeEvent;
import org.freeplane.features.map.NodeModel;
import org.freeplane.plugin.client.ClientController;
import org.freeplane.plugin.client.services.WS;
import org.freeplane.plugin.remote.v10.model.NodeModelDefault;
import org.freeplane.view.swing.features.filepreview.ExternalResource;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.NodeView;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

@SuppressWarnings("serial")
public class NodeViewListener extends NodeView implements INodeView {
	NodeModelDefault lastNodeState;

	public NodeViewListener(NodeModel model, MapView map, Container parent) {
		super(model, map, parent);
		lastNodeState = new NodeModelDefault(model, false);
	}

	@Override
	public void addContent(JComponent component, int pos) {
		// override to do nothing
	}

	@Override
	public void onPreNodeMoved(NodeModel oldParent, int oldIndex, NodeModel newParent, NodeModel child, int newIndex) {
		// not important
	}

	@Override
	public void onPreNodeDelete(NodeModel oldParent, NodeModel selectedNode, int index) {
		// not important
	}

	@Override
	public void onNodeMoved(NodeModel oldParent, int oldIndex, NodeModel newParent, NodeModel child, int newIndex) {
		// not important
	}

	@Override
	public void onNodeInserted(NodeModel parent, NodeModel child, int newIndex) {
		// not important
	}

	@Override
	public void onNodeDeleted(NodeModel parent, NodeModel child, int index) {
		// not important
	}

	@Override
	public void mapChanged(MapChangeEvent event) {
		// not important
	}

	@Override
	public void nodeChanged(final NodeChangeEvent event) {

		LogUtils.info("nodeChange called");
		if (!isUpdating()) {
			if (event != null && event.getProperty() != null)
				LogUtils.info("attribute: " + event.getProperty().toString());
			if (event != null && event.getNewValue() != null)
				LogUtils.info("value: " + event.getNewValue().toString());

			if (event.getProperty() != null) {
				final Object property = event.getProperty();
				if (property.toString().contains("FOLDING")) {
					LogUtils.info("folding");
					final ListenableFuture<Boolean> future = webservice().changeNode("5", event.getNode().getID(), "folded", event.getNewValue());
					Futures.addCallback(future, new FutureCallback<Boolean>() {

						@Override
						public void onFailure(Throwable t) {
							t.printStackTrace();
						}

						@Override
						public void onSuccess(Boolean success) {
							if (!success) {
								isUpdating(true);
								event.getNode().setFolded(!(Boolean) event.getNewValue());
								isUpdating(false);
							}
						}
					});
				}
				// images
				else if (property.equals(ExternalResource.class)) {
					// TODO handle images
					@SuppressWarnings("unused")
					final ExternalResource resource = (ExternalResource) event.getNewValue();
					// resource.getUri()
				}
				// send all because real change is unknown (only every 5
				// seconds)
				else if (property.equals(NodeModel.UNKNOWN_PROPERTY)) {
					long nowMillis = System.currentTimeMillis();
					if (lastMillis < 0 || nowMillis - lastMillis > 5000) {
						lastMillis = nowMillis;
						LogUtils.info("unkown property changed, creating diff");
						NodeModelDefault now = new NodeModelDefault(event.getNode(), false);
						Map<String, Object> attributeValueMap = getChangedAttributes(lastNodeState, now);
						lastNodeState = now;
						for (Map.Entry<String, Object> entry : attributeValueMap.entrySet()) {
							final ListenableFuture<Boolean> future = webservice().changeNode("5", event.getNode().getID(), entry.getKey(), entry.getValue());
							Futures.addCallback(future, new FutureCallback<Boolean>() {

								@Override
								public void onFailure(Throwable t) {
									t.printStackTrace();
								}

								@Override
								public void onSuccess(Boolean success) {
									if (!success) {
										// isUpdating(true);
										// event.
										// isUpdating(false);
									}
								}
							});
						}
					}
				}

			}
		}

	}

	private long lastMillis = -1;

	private Map<String, Object> getChangedAttributes(NodeModelDefault previousState, NodeModelDefault now) {
		final Map<String, Object> attributes = new HashMap<String, Object>();

		// nodeText is a recognized change
		// fold is a recognized change

		// moving is not recognized
		if (previousState.hGap != now.hGap) {
			LogUtils.info("hGap changed to " + now.hGap);
			attributes.put("hGap", now.hGap);
		}
		if (previousState.shiftY != now.shiftY) {
			LogUtils.info("hGap changed to " + now.shiftY);
			attributes.put("shiftY", now.shiftY);
		}

		return attributes;
	}

	private WS webservice() {
		return ClientController.webservice();
	}

	private boolean isUpdating() {
		return ClientController.isUpdating();
	}

	private void isUpdating(boolean value) {
		ClientController.isUpdating(value);
	}
}
