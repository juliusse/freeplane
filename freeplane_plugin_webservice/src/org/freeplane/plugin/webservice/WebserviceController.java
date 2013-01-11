package org.freeplane.plugin.webservice;

import java.awt.Container;
import java.io.IOException;
import java.util.logging.Level;

import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.ui.INodeViewLifeCycleListener;
import org.freeplane.plugin.webservice.v10.Webservice;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.jersey.api.core.ClassNamesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.net.httpserver.HttpServer;



public class WebserviceController {

	private static WebserviceController webserviceController;	
	//private final ModeController modeController;
	private HttpServer server = null;
	
	public static WebserviceController getInstance() {
		return webserviceController;
	}

	WebserviceController() {
		webserviceController = this;
		//this.modeController = modeController;
		LogUtils.info("starting Webservice Plugin...");
		
		int port = 8080;
		try {
			port = Integer.parseInt(System.getenv("webservice_port"));
		} catch (Exception e) {}
	    
		this.registerListeners();
		
		//change class loader
		final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(WebserviceController.class.getClassLoader());
		
		try {
			ResourceConfig rc = new ClassNamesResourceConfig(
					Webservice.class
					);
			
			
			LogUtils.getLogger().log(Level.INFO, "Webservice address: http://localhost:"+port+"/rest");
			server = HttpServerFactory.create( "http://localhost:"+port+"/rest",rc );
			server.start();
			
		} catch (IOException e) {
			LogUtils.getLogger().log(Level.SEVERE, "Webservice could not be started.",e);
		} 
		finally {
			//set back to original class loader
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}

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
