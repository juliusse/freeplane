package org.freeplane.plugin.remote;

import java.awt.Container;
import java.util.concurrent.TimeUnit;

import org.docear.messages.Messages.CloseUnusedMaps;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.ui.INodeViewLifeCycleListener;
import org.freeplane.plugin.remote.actors.MainActor;

import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Props;

import com.typesafe.config.ConfigFactory;



public class WebserviceController {

	private final ActorSystem system;
	private final ActorRef mainActor;
	private final Cancellable closeUnusedMapsJob;
	private static WebserviceController webserviceController;


	public static WebserviceController getInstance() {
		return webserviceController;
	}

	WebserviceController() {
		webserviceController = this;

		LogUtils.info("starting Webservice Plugin...");

		//		int port = 8080;
		//		try {
		//			port = Integer.parseInt(System.getenv("webservice_port"));
		//		} catch (Exception e) {}

		this.registerListeners();
		//change class loader
		final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(Activator.class.getClassLoader());


		system = ActorSystem.create("freeplaneRemote", ConfigFactory.load().getConfig("listener"));
		mainActor = system.actorOf(new Props(MainActor.class), "main");
		LogUtils.info("Main Actor running at path=" + mainActor.path());

		closeUnusedMapsJob = 
				system.scheduler().schedule(
						Duration.Zero(), 
						Duration.apply(10, TimeUnit.MINUTES), 
						new Runnable() {
							@Override
							public void run() {
								mainActor.tell(new CloseUnusedMaps(1), null);
							}
						}, system.dispatcher());

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


	public ModeController getModeController() {
		//Controller.getCurrentController().selectMode(MModeController.getMModeController());
		return MModeController.getMModeController();
	}

}
