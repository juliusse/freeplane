package org.freeplane.plugin.client;

import java.util.Hashtable;

import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.main.osgi.IModeControllerExtensionProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator{

	@Override
	public void start(BundleContext context) {
		registerToFreeplaneStart(context);
	}

	private void registerToFreeplaneStart(final BundleContext context) {
		final Hashtable<String, String[]> props = new Hashtable<String, String[]>();
		
		props.put("mode", new String[] { MModeController.MODENAME });
		context.registerService(IModeControllerExtensionProvider.class.getName(),
				new IModeControllerExtensionProvider() {
			public void installExtension(ModeController modeController) {
				ClientController.getInstance();
			}
		}, props);
	}
	
	@Override
	public void stop(BundleContext context) {
		//Logger.getLogger().info("Activator.stop => Activator.stop called.");
		
		if (ClientController.isStarted()){
			//Logger.getLogger().info("Activator.stop => Stop running Remote.");
			ClientController.stop();
		}
	}
}
