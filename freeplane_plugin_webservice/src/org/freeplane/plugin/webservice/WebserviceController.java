package org.freeplane.plugin.webservice;

import java.awt.Container;
import java.io.IOException;
import java.util.logging.Level;

import org.freeplane.core.util.LogUtils;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.ui.INodeViewLifeCycleListener;
import org.freeplane.plugin.webservice.actors.MainActor;
import org.freeplane.plugin.webservice.v10.Webservice;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.jersey.api.core.ClassNamesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.net.httpserver.HttpServer;
import com.typesafe.config.ConfigFactory;



public class WebserviceController {

	private final ActorSystem system;
	private static WebserviceController webserviceController;

	public static WebserviceController getInstance() {
		return webserviceController;
	}

	WebserviceController() {
		webserviceController = this;

		LogUtils.info("starting Webservice Plugin...");

		int port = 8080;
		try {
			port = Integer.parseInt(System.getenv("webservice_port"));
		} catch (Exception e) {}

		this.registerListeners();

		//change class loader
		final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(WebserviceController.class.getClassLoader());



		system = ActorSystem.create("freeplaneRemote", ConfigFactory.load().getConfig("listener"));
		ActorRef actor = system.actorOf(new Props(MainActor.class), "main");
		System.out.println("path=" + actor.path());

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
