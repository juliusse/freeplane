//package org.freeplane.plugin.client.jobs;
//
//import static org.freeplane.plugin.remote.RemoteUtils.addNodeToOpenMap;
//import static org.freeplane.plugin.remote.RemoteUtils.changeNodeAttribute;
//import static org.freeplane.plugin.remote.RemoteUtils.getNodeFromOpenMapById;
//
//import org.docear.messages.exceptions.NodeNotFoundException;
//import org.freeplane.core.util.LogUtils;
//import org.freeplane.features.map.NodeModel;
//import org.freeplane.features.map.mindmapmode.MMapController;
//import org.freeplane.plugin.client.ClientController;
//import org.freeplane.plugin.client.services.GetUpdatesResponse;
//import org.freeplane.plugin.client.services.WS;
//import org.freeplane.plugin.remote.RemoteUtils;
//import org.freeplane.plugin.remote.v10.model.updates.AddNodeUpdate;
//import org.freeplane.plugin.remote.v10.model.updates.AddNodeUpdate.Side;
//import org.freeplane.plugin.remote.v10.model.updates.ChangeNodeAttributeUpdate;
//import org.freeplane.plugin.remote.v10.model.updates.DeleteNodeUpdate;
//import org.freeplane.plugin.remote.v10.model.updates.MapUpdate;
//import org.freeplane.plugin.remote.v10.model.updates.MoveNodeUpdate;
//
//import com.google.common.util.concurrent.Futures;
//
//public class ListenForUpdatesJob implements Runnable {
//
//	private String currentMapId;
//	private boolean terminate = false;
//	private int currentRevision;
//
//	public ListenForUpdatesJob(String currentMapId, int currentRevision) {
//		super();
//		this.currentMapId = currentMapId;
//		this.currentRevision = currentRevision;
//	}
//
//	@Override
//	public void run() {
//		while (!terminate) {
//			final String mapIdForThisExecution = currentMapId;
//			try {
//				waitForUpdates(mapIdForThisExecution);
//				LogUtils.info("updates occured");
//				if (mapIdForThisExecution.equals(currentMapId)) {
//					getAndApplyUpdates(currentMapId, currentRevision);
//				}
//			} catch (NodeNotFoundException e) {
//				e.printStackTrace();
//			} catch (Throwable t) {
//				// catch all (will be removed later!!)
//				t.printStackTrace();
//			}
//		}
//	}
//
//	private void waitForUpdates(String mapId) {
//		boolean updateOccured = false;
//		while (!updateOccured) {
//			updateOccured = Futures.getUnchecked(webservice().listenIfUpdatesOccur(mapId));
//		}
//
//	}
//
//	public void getAndApplyUpdates(String mapId, int currentRevision) throws NodeNotFoundException {
//		isUpdating(true);
//		final GetUpdatesResponse response = Futures.getUnchecked(webservice().getUpdatesSinceRevision(mapId, currentRevision));
//		currentRevision = response.getCurrentRevision();
//
//		for (MapUpdate mapUpdate : response.getOrderedUpdates()) {
//			if (mapUpdate.getUsername().equals(loggedInUser()) && mapUpdate.getSource().equals(source())) {
//				// update was done by this instance
//				continue;
//			}
//			if (mapUpdate instanceof AddNodeUpdate) {
//				final AddNodeUpdate update = (AddNodeUpdate) mapUpdate;
//
//				final String newNodeId = update.getNewNodeId();
//				final String parentNodeId = update.getParentNodeId();
//
//				final NodeModel parentNode = getNodeFromOpenMapById(mmapController(), parentNodeId);
//
//				boolean nodePresent = true;
//				try {
//					getNodeFromOpenMapById(mmapController(), newNodeId);
//				} catch (NodeNotFoundException e) {
//					nodePresent = false;
//				}
//
//				if (!nodePresent) { // otherwise it already exists
//					final NodeModel newNode = addNodeToOpenMap(mmapController(), parentNode);
//					newNode.setID(newNodeId);
//					if (update.getSide() != null) {
//						newNode.setLeft(update.getSide() == Side.Left);
//					}
//				}
//			} else if (mapUpdate instanceof DeleteNodeUpdate) {
//				final DeleteNodeUpdate update = (DeleteNodeUpdate) mapUpdate;
//				final String nodeId = update.getNodeId();
//				final NodeModel node = getNodeFromOpenMapById(mmapController(), nodeId);
//				mmapController().deleteNode(node);
//			} else if (mapUpdate instanceof ChangeNodeAttributeUpdate) {
//				final ChangeNodeAttributeUpdate update = (ChangeNodeAttributeUpdate) mapUpdate;
//				try {
//					final NodeModel freeplaneNode = getNodeFromOpenMapById(mmapController(), update.getNodeId());
//					changeNodeAttribute(freeplaneNode, update.getAttribute(), update.getValue());
//					if(ClientController.selectedNodesMap().containsKey(freeplaneNode)) {
//						ClientController.selectedNodesMap().get(freeplaneNode).updateCurrentState();
//					}
//					ClientController.mmapController().nodeChanged(freeplaneNode);
//				} catch (NodeNotFoundException e) {
//					// Do nothing, indicates that this app was sender
//				} catch (NullPointerException e) {
//					// Do nothing, but happens very often in freeplane view stuff
//				}
//			} else if (mapUpdate instanceof MoveNodeUpdate) {
//				final MoveNodeUpdate update = (MoveNodeUpdate) mapUpdate;
//
//				RemoteUtils.moveNodeTo(mmapController(), update.getNewParentNodeId(), update.getNodetoMoveId(), update.getNewIndex());
//			}
//		}
//		isUpdating(false);
//	}
//
//	public int getCurrentRevision() {
//		return currentRevision;
//	}
//
//	public String getCurrentMapId() {
//		return currentMapId;
//	}
//
//	public void changeMap(String mapId, int currentRevision) {
//		this.currentMapId = mapId;
//		this.currentRevision = currentRevision;
//	}
//
//	public void stop() {
//		terminate = true;
//	}
//
//	private WS webservice() {
//		return ClientController.webservice();
//	}
//
//	// private boolean isUpdating() {
//	// return ClientController.isUpdating();
//	// }
//
//	private void isUpdating(boolean value) {
//		ClientController.isUpdating(value);
//	}
//
//	private MMapController mmapController() {
//		return ClientController.mmapController();
//	}
//
//	private String loggedInUser() {
//		return ClientController.loggedInUserName();
//	}
//
//	private String source() {
//		return ClientController.source();
//	}
//
//}
