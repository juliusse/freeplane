package org.freeplane.plugin.webservice;

import java.util.Hashtable;

import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.main.osgi.IModeControllerExtensionProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import akka.actor.*;
import akka.osgi.ActorSystemActivator;



public class Activator extends ActorSystemActivator {

	//private ServiceTracker httpServiceTracker;


//	public void startService(BundleContext context, ModeController modeController) {
//		new WebserviceController(modeController,context);
//	}

//	protected Collection<IControllerExtensionProvider> getControllerExtensions() {
//		return null;
//	}

	@Override
	public void start(BundleContext context) {
		
		final Hashtable<String, String[]> props = new Hashtable<String, String[]>();
		props.put("mode", new String[] { MModeController.MODENAME });
		context.registerService(IModeControllerExtensionProvider.class.getName(),
		    new IModeControllerExtensionProvider() {
			    public void installExtension(ModeController modeController) {
			    	new WebserviceController();
			    }
		    }, props);
		//registerMindMapModeExtension(context);
	}
	
//	private void registerMindMapModeExtension(final BundleContext context) {
//		final Hashtable<String, String[]> props = new Hashtable<String, String[]>();
//		props.put("mode", new String[] { MModeController.MODENAME });
//		context.registerService(IModeControllerExtensionProvider.class.getName(),
//		    new IModeControllerExtensionProvider() {
//			    public void installExtension(final ModeController modeController) {
//			    	new WebserviceController();
//			    }
//		    }, props);
//	}

	@Override
	public void configure(BundleContext arg0, ActorSystem arg1) {
		
	}
}
