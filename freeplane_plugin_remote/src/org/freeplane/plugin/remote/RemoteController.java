package org.freeplane.plugin.remote;

import java.awt.Container;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.docear.messages.Messages.CloseUnusedMaps;
import org.docear.messages.exceptions.MapNotFoundException;
import org.freeplane.features.mapio.MapIO;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.ui.INodeViewLifeCycleListener;
import org.freeplane.plugin.remote.actors.MainActor;
import org.freeplane.plugin.remote.v10.Utils;
import org.freeplane.plugin.remote.v10.model.OpenMindmapInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.PoisonPill;
import akka.actor.Props;

import com.typesafe.config.ConfigFactory;



public class RemoteController {

	private final Logger logger;
	
	private final ActorSystem system;
	private final ActorRef mainActor;
	private final Cancellable closeUnusedMapsJob;
	private final Map<String, OpenMindmapInfo> mapIdInfoMap = new HashMap<String, OpenMindmapInfo>();
	
	private static RemoteController instance;
	public static RemoteController getInstance() {
		if(instance == null)
			instance = new RemoteController();
		return instance;
	}

	private RemoteController() {		
		//change class loader
		final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(Activator.class.getClassLoader());
		//create logger
		logger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		
		logger.info("starting Remote Plugin...");
		
		ActorSystem preSystem = null;
		try {
			preSystem = ActorSystem.create("freeplaneRemote", ConfigFactory.load().getConfig("listener"));
		} catch (org.jboss.netty.channel.ChannelException ex) {
			logger.error(ex.getMessage());
			System.exit(1);
		}
		
		system = preSystem;
		mainActor = system.actorOf(new Props(MainActor.class), "main");
		logger.info("Main Actor running at path='{}'", mainActor.path());

		closeUnusedMapsJob = 
				system.scheduler().schedule(
						Duration.Zero(), 
						Duration.apply(10, TimeUnit.MINUTES), 
						new Runnable() {
							@Override
							public void run() {
								logger.info("Scheduling closing of unused maps.");
								mainActor.tell(new CloseUnusedMaps(600000), null); // ten minutes
							}
						}, system.dispatcher());

		
		this.registerListeners();
		//set back to original class loader
		Thread.currentThread().setContextClassLoader(contextClassLoader);
	}

	/**
	 * registers all listeners to react on necessary events like created nodes
	 * Might belong into a new plugin, which sends changes to the server (And this IS the server)
	 */
	private void registerListeners() {
		getModeController().addINodeViewLifeCycleListener(new INodeViewLifeCycleListener() {

			@Override
			public void onViewRemoved(Container nodeView) {

			}

			@Override
			public void onViewCreated(Container nodeView) {				
			}
		});
	}

	public static void stop() {
		getLogger().info("Shutting down remote plugin...");
		RemoteController controller = getInstance();
		controller.closeUnusedMapsJob.cancel();
		controller.mainActor.tell(PoisonPill.getInstance(), null);
		controller.system.shutdown();
		controller.closeMapsAndDestroyFiles();
	}
	
	private void closeMapsAndDestroyFiles() {
		Set<String> idSet = new HashSet<String>(this.mapIdInfoMap.keySet());
		for(String id: idSet) {
			try {
				Utils.closeMap(id);
			} catch (MapNotFoundException e) {
				logger.warn("could not find map with id '{}'",id);
			}
		}
	}

	public static ModeController getModeController() {
		//Controller.getCurrentController().selectMode(MModeController.getMModeController());
		return MModeController.getMModeController();
	}
	
	public static MapIO getMapIO() {
		return getModeController().getExtension(MapIO.class);
	}

	public static Map<String, OpenMindmapInfo> getMapIdInfoMap() {
		return getInstance().mapIdInfoMap;
	}
	
	public static Logger getLogger() {
		return getInstance().logger;
	}
}
