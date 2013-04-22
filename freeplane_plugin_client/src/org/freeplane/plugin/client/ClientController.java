package org.freeplane.plugin.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.INodeSelectionListener;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mapio.MapIO;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.plugin.client.actors.ApplyChangesActor;
import org.freeplane.plugin.client.actors.InitCollaborationActor;
import org.freeplane.plugin.client.actors.ListenForUpdatesActor;
import org.freeplane.plugin.client.listeners.MapChangeListener;
import org.freeplane.plugin.client.listeners.NodeChangeListener;
import org.freeplane.plugin.client.listeners.NodeViewListener;
import org.freeplane.plugin.client.services.DocearOnlineWs;
import org.freeplane.plugin.client.services.WS;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.TypedActor;
import akka.actor.TypedProps;
import akka.util.Timeout;

import com.typesafe.config.ConfigFactory;

public class ClientController {

	// Temp variables, only for developping
	public static final String USER = "Julius";
	public static final String PW = "secret";

	private final ActorSystem system;
	private ActorRef listenForUpdatesActor = null;
	private ActorRef applyChangeActor = null;
	private ActorRef initCollaborationactor = null;

	//private final ListeningScheduledExecutorService executor;
	
	private WS webservice;
	private static ClientController instance;
	private final String sourceString;
	private boolean isUpdating = false;
	
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
		//executor = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(10));

		// change class loader
		final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(Activator.class.getClassLoader());

		LogUtils.info("starting Client Plugin...");

		system = ActorSystem.create("freeplaneClient", ConfigFactory.load().getConfig("local"));
		listenForUpdatesActor = system.actorOf(new Props(ListenForUpdatesActor.class), "updateListener");
		applyChangeActor = system.actorOf(new Props(ApplyChangesActor.class), "changeApplier");
		initCollaborationactor = system.actorOf(new Props(InitCollaborationActor.class), "initCollaboration");
		
		
		webservice = TypedActor.get(system).typedActorOf(
				new TypedProps<DocearOnlineWs>(WS.class, DocearOnlineWs.class).withTimeout(Timeout.apply(3, TimeUnit.MINUTES)));


		this.registerListeners();

		initCollaborationactor.tell(new InitCollaborationActor.Messages.InitCollaborationMode("5",USER,PW), null);
		
//		system.scheduler().scheduleOnce(
//				Duration.create(1, TimeUnit.SECONDS), 
//				new MapInitRunnable("5", USER, PW), 
//				system.dispatcher());


		// set back to original class loader
		Thread.currentThread().setContextClassLoader(contextClassLoader);
	}

	public static final class CheckForChangesRunnable implements Runnable {

		@Override
		public void run() {
			final Map<NodeModel, NodeViewListener> selectedNodesMap = ClientController.selectedNodesMap();
			for (Map.Entry<NodeModel, NodeViewListener> nodePair : selectedNodesMap.entrySet()) {
				final Map<String, Object> attributeValueMap = nodePair.getValue().getChangedAttributes();

				for (Map.Entry<String, Object> entry : attributeValueMap.entrySet()) {
					webservice().changeNode("5", nodePair.getKey().getID(), entry.getKey(), entry.getValue());
				}
				
				nodePair.getValue().updateCurrentState();
			}
		}

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

	public static ActorRef applyChangesActor() {
		return getInstance().applyChangeActor;
	}

	public static ActorRef listenForUpdatesActor() {
		return getInstance().listenForUpdatesActor;
	}

	public static Map<NodeModel, NodeViewListener> selectedNodesMap() {
		return getInstance().selectedNodesMap;
	}
	
	public static ActorSystem system() {
		return getInstance().system;
	}
}
