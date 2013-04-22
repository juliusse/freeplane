package org.freeplane.plugin.client;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.INodeSelectionListener;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mapio.MapIO;
import org.freeplane.features.mapio.mindmapmode.MMapIO;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.n3.nanoxml.XMLException;
import org.freeplane.plugin.client.jobs.ListenForUpdatesJob;
import org.freeplane.plugin.client.listeners.MapChangeListener;
import org.freeplane.plugin.client.listeners.NodeChangeListener;
import org.freeplane.plugin.client.listeners.NodeViewListener;
import org.freeplane.plugin.client.services.DocearOnlineWs;
import org.freeplane.plugin.client.services.WS;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class ClientController {

	// Temp variables, only for developping
	public static final String USER = "Julius";
	public static final String PW = "secret";

	private final ListeningScheduledExecutorService executor;
	private WS webservice;
	private static ClientController instance;
	private final String sourceString;
	private boolean isUpdating = false;
	private ListenForUpdatesJob listenForUpdatesJob = null;
	private final Map<NodeModel, NodeViewListener> selectedNodesMap = new HashMap<NodeModel, NodeViewListener>();

	public static ClientController getInstance() {
		if (instance == null)
			instance = new ClientController();
		return instance;
	}

	private ClientController() {

		// set sourceString, used to identify for updates
		try {
			final String computername = InetAddress.getLocalHost().getHostName();
			sourceString = computername + "_" + System.currentTimeMillis();
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}

		// create Threadpool
		executor = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(10));

		// change class loader
		// final ClassLoader contextClassLoader =
		// Thread.currentThread().getContextClassLoader();
		// Thread.currentThread().setContextClassLoader(Activator.class.getClassLoader());

		LogUtils.info("starting Client Plugin...");

		webservice = new DocearOnlineWs();

		this.registerListeners();
		// // set back to original class loader
		// Thread.currentThread().setContextClassLoader(contextClassLoader);
		executor.schedule((new Runnable() {

			@Override
			public void run() {
				try {
					while (!getModeController().getModeName().equals("MindMap")) {
						Thread.sleep(20);
					}
					Futures.getUnchecked(webservice.login(USER, PW));
					final int currentRevision = openMindmap("5");
					listenForUpdatesJob = new ListenForUpdatesJob("5", currentRevision);
					executor.submit(listenForUpdatesJob);
				} catch (InterruptedException e) {
				} catch (Throwable t) {
					t.printStackTrace();
				}

			}
		}),2,TimeUnit.SECONDS);
		executor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				for (Map.Entry<NodeModel, NodeViewListener> nodePair : selectedNodesMap.entrySet()) {
					final Map<String, Object> attributeValueMap = nodePair.getValue().getChangedAttributes();

					for (Map.Entry<String, Object> entry : attributeValueMap.entrySet()) {
						webservice().changeNode("5", nodePair.getKey().getID(), entry.getKey(), entry.getValue());
					}
					nodePair.getValue().updateCurrentState();
				}
			}
		}, 5, 1, TimeUnit.SECONDS);
	}

	/**
	 * registers all listeners to react on necessary events like created nodes
	 * Might belong into a new plugin, which sends changes to the server
	 */
	private void registerListeners() {
		// mmapController().addMapLifeCycleListener(new MapLifeCycleListener());
		mmapController().addMapChangeListener(new MapChangeListener());
		mmapController().addNodeChangeListener(new NodeChangeListener());
		mmapController().addNodeSelectionListener(new INodeSelectionListener() {

			@Override
			public void onSelect(NodeModel node) {
				final NodeViewListener listener = new NodeViewListener(node, null, null);
				node.addViewer(listener);
				selectedNodesMap.put(node, listener);
			}

			@Override
			public void onDeselect(NodeModel node) {
				final NodeViewListener listener = selectedNodesMap.remove(node);
				if (listener != null) {
					final Map<String, Object> attributeValueMap = listener.getChangedAttributes();

					for (Map.Entry<String, Object> entry : attributeValueMap.entrySet()) {
						webservice().changeNode("5", node.getID(), entry.getKey(), entry.getValue());
						
					}

					node.removeViewer(listener);
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
		return MModeController.getMModeController();
	}

	public static MapIO getMapIO() {
		return getModeController().getExtension(MapIO.class);
	}

	public int openMindmap(final String mapId) {

		final JsonNode responseNode = Futures.getUnchecked(webservice().getMapAsXml(mapId));

		final int currentRevision = responseNode.get("currentRevision").asInt();
		final String xmlString = responseNode.get("xmlString").asText();
		final Random ran = new Random();
		final String filename = "" + System.currentTimeMillis() + ran.nextInt(100);
		final String tempDirPath = System.getProperty("java.io.tmpdir");
		final File file = new File(tempDirPath + "/docear/" + filename + ".mm");

		try {
			FileUtils.writeStringToFile(file, xmlString);
			final URL pathURL = file.toURI().toURL();

			final MMapIO mio = (MMapIO) ClientController.getMapIO();
			mio.newMap(pathURL);
		} catch (IOException e) {
			throw new AssertionError(e);
		} catch (URISyntaxException e) {
			throw new AssertionError(e);
		} catch (XMLException e) {
			throw new AssertionError(e);
		} finally {
			file.delete();
		}

		return currentRevision;
	}

	public static MMapController mmapController() {
		return (MMapController) getModeController().getMapController();
	}

	public static WS webservice() {
		return getInstance().webservice;
	}

	public static String source() {
		return getInstance().sourceString;
	}

	public static boolean isUpdating() {
		return getInstance().isUpdating;
	}

	public static void isUpdating(boolean value) {
		getInstance().isUpdating = value;
	}

	public static String loggedInUserName() {
		return USER;
	}

	public static ListeningScheduledExecutorService executor() {
		return getInstance().executor;
	}
	
	public static Map<NodeModel,NodeViewListener> selectedNodesMap() {
		return getInstance().selectedNodesMap;
	}
}
