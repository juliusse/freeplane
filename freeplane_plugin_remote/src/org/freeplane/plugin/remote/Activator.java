package org.freeplane.plugin.remote;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Hashtable;

import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.main.osgi.IModeControllerExtensionProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import akka.actor.ActorSystem;
import akka.osgi.ActorSystemActivator;



public class Activator extends ActorSystemActivator implements BundleActivator{

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
		
		final Bundle systemBundle = context.getBundle(0);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override 	
		    public void run() {
		    	System.err.println("Shutdown HOOK");
		    	try {
					systemBundle.stop();
				} catch (BundleException e) {
					e.printStackTrace();
				}
		    }
		});
		
		generatePIDFile();
		
		//registerMindMapModeExtension(context);
	}
	
	private void generatePIDFile(){
		RuntimeMXBean rtb = ManagementFactory.getRuntimeMXBean();
		String name = rtb.getName();
		Integer pid = Integer.parseInt(name.substring(0, name.indexOf("@")));
		
		System.err.println("pid :" + pid);
		
        FileWriter fileWriter = null;
        try {
            File pidFile = new File("RUNNING_PID");
            fileWriter = new FileWriter(pidFile);
            fileWriter.write(pid.toString());
            fileWriter.close();
        } catch (IOException ex) {
        	System.err.println(ex.getMessage());
        } finally {
            try {
                fileWriter.close();
            } catch (IOException ex) {
            	System.err.println(ex.getMessage());
            }
        }
	}

	@Override
	public void configure(BundleContext arg0, ActorSystem arg1) {
		RemoteController.stop();
	}
	
	@Override
	public void stop(BundleContext context) {
		System.err.println("STOPPING REMOTE");
		RemoteController.stop();
		super.stop(context);
		
		try{			 
    		File file = new File("RUNNING_PID");
    		if(!file.delete()){
    			System.out.println("Error while deleting RUNNING_PID");
    		}
    	}catch(Exception e){
    		e.printStackTrace();
    	}
	}
}