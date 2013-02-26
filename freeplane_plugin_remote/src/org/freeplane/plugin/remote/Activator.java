package org.freeplane.plugin.remote;

import java.util.Hashtable;

import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.main.osgi.IModeControllerExtensionProvider;
import org.osgi.framework.BundleContext;

import akka.actor.ActorSystem;
import akka.osgi.ActorSystemActivator;



public class Activator extends ActorSystemActivator {

	@Override
	public void start(BundleContext context) {
		
		final Hashtable<String, String[]> props = new Hashtable<String, String[]>();
		props.put("mode", new String[] { MModeController.MODENAME });
		context.registerService(IModeControllerExtensionProvider.class.getName(),
		    new IModeControllerExtensionProvider() {
			    public void installExtension(ModeController modeController) {
			    	RemoteController.getInstance();
			    }
		    }, props);
		//registerMindMapModeExtension(context);
	}
	

	@Override
	public void configure(BundleContext arg0, ActorSystem arg1) {
		RemoteController.stop();
	}
}
