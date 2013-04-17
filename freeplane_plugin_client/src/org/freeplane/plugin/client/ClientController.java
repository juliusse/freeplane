package org.freeplane.plugin.client;

import static org.freeplane.plugin.remote.RemoteUtils.addNodeToOpenMap;
import static org.freeplane.plugin.remote.RemoteUtils.changeNodeAttribute;
import static org.freeplane.plugin.remote.RemoteUtils.getNodeFromOpenMapById;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.docear.messages.exceptions.NodeNotFoundException;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.IMapChangeListener;
import org.freeplane.features.map.IMapLifeCycleListener;
import org.freeplane.features.map.INodeChangeListener;
import org.freeplane.features.map.INodeView;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeChangeEvent;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mapio.MapIO;
import org.freeplane.features.mapio.mindmapmode.MMapIO;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.n3.nanoxml.XMLException;
import org.freeplane.plugin.client.services.DocearOnlineWs;
import org.freeplane.plugin.client.services.GetUpdatesResponse;
import org.freeplane.plugin.client.services.WS;
import org.freeplane.plugin.remote.RemoteUtils;
import org.freeplane.plugin.remote.v10.model.updates.AddNodeUpdate;
import org.freeplane.plugin.remote.v10.model.updates.AddNodeUpdate.Side;
import org.freeplane.plugin.remote.v10.model.updates.ChangeNodeAttributeUpdate;
import org.freeplane.plugin.remote.v10.model.updates.DeleteNodeUpdate;
import org.freeplane.plugin.remote.v10.model.updates.MapUpdate;
import org.freeplane.plugin.remote.v10.model.updates.MoveNodeUpdate;

public class ClientController {

	private WS webservice;
	private static ClientController instance;

	public static ClientController getInstance() {
		if (instance == null)
			instance = new ClientController();
		return instance;
	}

	private ClientController() {

		// change class loader
		final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(Activator.class.getClassLoader());

		LogUtils.info("starting Client Plugin...");

		webservice = new DocearOnlineWs();

		this.registerListeners();
		// set back to original class loader
		Thread.currentThread().setContextClassLoader(contextClassLoader);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(2000);
					openMindmap("5");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}).start();

		// check for changes
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						waitForUpdates("5");
						getAndApplyUpdates("5", currentRevision);
					} catch (NodeNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}).start();

	}

	/**
	 * registers all listeners to react on necessary events like created nodes
	 * Might belong into a new plugin, which sends changes to the server (And
	 * this IS the server)
	 */
	private void registerListeners() {
		mmapController().addMapLifeCycleListener(new IMapLifeCycleListener() {

			@Override
			public void onRemove(MapModel map) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onCreate(MapModel map) {
				LogUtils.info("map changed");
				for (NodeModel node : map.getRootNode().getChildren()) {

					node.addViewer(new INodeView() {

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
							// TODO Auto-generated method stub

						}

						@Override
						public void onNodeInserted(NodeModel parent, NodeModel child, int newIndex) {
							// TODO Auto-generated method stub

						}

						@Override
						public void onNodeDeleted(NodeModel parent, NodeModel child, int index) {
							// TODO Auto-generated method stub

						}

						@Override
						public void mapChanged(MapChangeEvent event) {
							// TODO Auto-generated method stub

						}

						@Override
						public void nodeChanged(NodeChangeEvent event) {
							LogUtils.info("nodeChange called");
							if (!isUpdating) {
								if (event != null && event.getProperty() != null)
									LogUtils.info("attribute: " + event.getProperty().toString());
								if (event != null && event.getNewValue() != null)
									LogUtils.info("value: " + event.getNewValue().toString());

//								if(event.getProperty() != null && event.getProperty().toString().contains("FOLDING")) {
//									LogUtils.info("folding");
//									webservice().changeNode("5", event.getNode().getID(), "folded", event.getNewValue());
//								}
								// if (event.getProperty() != null &&
								// event.getProperty().equals("node_text")) {
								// webservice().changeNode("5",
								// event.getNode().getID(),
								// "nodeText", event.getNewValue());
								// }
								// node_text
							}

						}
					});
				}

			}
		});
		
		mmapController().addMapChangeListener(new IMapChangeListener() {
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
				if (!isUpdating) {
					LogUtils.info("Node Moved. Sending to Webservice");
					webservice().moveNodeTo("5", newParent.getID(), child.getID(), newIndex);
				}
			}

			@Override
			public void onNodeInserted(NodeModel parent, NodeModel child, int newIndex) {
				if (!isUpdating) {
					LogUtils.info("Node Added. Sending to Webservice");
					final String newNodeId = webservice().createNode("5", parent.getID());
					child.setID(newNodeId);
				}
			}

			@Override
			public void onNodeDeleted(NodeModel parent, NodeModel child, int index) {
				if (!isUpdating) {
					LogUtils.info("Node Deleted. Sending to Webservice");
					webservice().removeNode("5", child.getID());
				}

			}

			@Override
			public void mapChanged(MapChangeEvent event) {
				
			}
		});

		// mmapController().toggleFolded()

		mmapController().addNodeChangeListener(new INodeChangeListener() {

			@Override
			public void nodeChanged(NodeChangeEvent event) {
				if (!isUpdating) {
					if (event != null && event.getProperty() != null)
						LogUtils.info("attribute: " + event.getProperty().toString());
					if (event != null && event.getNewValue() != null)
						LogUtils.info("value: " + event.getNewValue().toString());

					// if (event.getProperty() != null &&
					// event.getProperty().equals("node_text")) {
					// webservice().changeNode("5", event.getNode().getID(),
					// "nodeText", event.getNewValue());
					// }
					// node_text
				}
			}
		});

	}

	public static boolean isStarted() {
		return instance != null;
	}

	public static void stop() {
		// getLogger().info("Shutting down client plugin...");
	}

	public static ModeController getModeController() {
		// Controller.getCurrentController().selectMode(MModeController.getMModeController());
		return MModeController.getMModeController();
	}

	public static MapIO getMapIO() {
		return getModeController().getExtension(MapIO.class);
	}

	// public static Logger getLogger() {
	// return org.freeplane.plugin.client.Logger.getLogger();
	// }

	// private final Map<String, Cookie> cookies = new HashMap<String,
	// Cookie>();
	// private void setAndRefreshCookis(List<NewCookie> newCookies) {
	// for(NewCookie c : newCookies) {
	// cookies.put(c.getName(), c.toCookie());
	// }
	// }
	//
	// private void applyCookies(WebResource wr) {
	// for(Cookie c : cookies.values()) {
	// wr.getRequestBuilder().cookie(c);
	// }
	// }

	public void openMindmap(final String mapId) {

		// webservice.login("online-demo", "online-demo");
		webservice.login("Julius", "secret");

		final JsonNode responseNode = webservice().getMapAsXml(mapId);

		currentRevision = responseNode.get("currentRevision").asInt();
		final String xmlString = responseNode.get("xmlString").asText();
		final Random ran = new Random();
		final String filename = "" + System.currentTimeMillis() + ran.nextInt(100);
		final String tempDirPath = System.getProperty("java.io.tmpdir");
		final File file = new File(tempDirPath + "/docear/" + filename + ".mm");

		try {
			// logger().debug("Actions.openMindmap => writing mindmap content to file");
			FileUtils.writeStringToFile(file, xmlString);

			// put map in openMap Collection
			final URL pathURL = file.toURI().toURL();
			// logger().debug("Actions.openMindmap => mindmap was put into openMindmapInfoMap ({} => {})",
			// mapId, info.getMapUrl());

			// open map
			// logger().debug("Actions.openMindmap => opening mindmap...");

			final MMapIO mio = (MMapIO) ClientController.getMapIO();
			mio.newMap(pathURL);
			// logger().debug("Actions.openMindmap => map successfully loaded and opened!");
		} catch (IOException e) {
			throw new AssertionError(e);
		} catch (URISyntaxException e) {
			throw new AssertionError(e);
		} catch (XMLException e) {
			throw new AssertionError(e);
		} finally {
			// logger().debug("Actions.closeMap => removing temporary file from file system");
			// file.delete();
		}

		// final String mapContent = request.getMindmapFileContent();
		// final String mapName = request.getMindmapFileName();
		// logger().debug("Actions.openMindmap => mapId: {}; mapName: {}; content:'{}...'",
		// mapId, mapName, mapContent.substring(0, Math.min(mapContent.length(),
		// 20)));
		//

		//

		//
		// return new OpenMindMapResponse(true);
	}

	private int currentRevision;

	public void waitForUpdates(String mapId) {
		boolean updateOccured = false;
		while (!updateOccured) {
			updateOccured = webservice().listenIfUpdatesOccur(mapId);
		}

	}

	private boolean isUpdating = false;

	public void getAndApplyUpdates(String mapId, int currentRevision) throws NodeNotFoundException {
		isUpdating = true;
		final GetUpdatesResponse response = webservice().getUpdatesSinceRevision(mapId, currentRevision);
		currentRevision = response.getCurrentRevision();

		for (MapUpdate mapUpdate : response.getOrderedUpdates()) {
			if (mapUpdate instanceof AddNodeUpdate) {
				final AddNodeUpdate update = (AddNodeUpdate) mapUpdate;

				final String newNodeId = update.getNewNodeId();
				final String parentNodeId = update.getParentNodeId();

				final NodeModel parentNode = getNodeFromOpenMapById(mmapController(), parentNodeId);

				boolean nodePresent = true;
				try {
					getNodeFromOpenMapById(mmapController(), newNodeId);
				} catch (NodeNotFoundException e) {
					nodePresent = false;
				}

				if (!nodePresent) { // otherwise it already exists
					// TODO use real text
					final NodeModel newNode = addNodeToOpenMap(mmapController(), parentNode);
					newNode.setID(newNodeId);
					if (update.getSide() != null) {
						newNode.setLeft(update.getSide() == Side.Left);
					}
				}
			} else if (mapUpdate instanceof DeleteNodeUpdate) {
				final DeleteNodeUpdate update = (DeleteNodeUpdate) mapUpdate;
				final String nodeId = update.getNodeId();
				final NodeModel node = getNodeFromOpenMapById(mmapController(), nodeId);
				mmapController().deleteNode(node);
			} else if (mapUpdate instanceof ChangeNodeAttributeUpdate) {
				final ChangeNodeAttributeUpdate update = (ChangeNodeAttributeUpdate) mapUpdate;
				try {
					final NodeModel freeplaneNode = getNodeFromOpenMapById(mmapController(), update.getNodeId());
					changeNodeAttribute(freeplaneNode, update.getAttribute(), update.getValue());
				} catch (NodeNotFoundException e) {
					// Do nothing, indicates that this app was sender
				}
			} else if (mapUpdate instanceof MoveNodeUpdate) {
				final MoveNodeUpdate update = (MoveNodeUpdate) mapUpdate;

				RemoteUtils.moveNodeTo(mmapController(), update.getNewParentNodeId(), update.getNodetoMoveId(), update.getNewIndex());
			}
		}
		isUpdating = false;
	}

	public static MMapController mmapController() {
		return (MMapController) getModeController().getMapController();
	}

	public static WS webservice() {
		return getInstance().webservice;
	}
}
