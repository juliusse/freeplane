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
import org.jboss.netty.channel.ChannelException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class Activator implements BundleActivator{

	@Override
	public void start(BundleContext context) {
		final Hashtable<String, String[]> props = new Hashtable<String, String[]>();
		final Bundle systemBundle = context.getBundle(0);
		props.put("mode", new String[] { MModeController.MODENAME });
		context.registerService(IModeControllerExtensionProvider.class.getName(),
		    new IModeControllerExtensionProvider() {
			    public void installExtension(ModeController modeController) {
			    	try {
			    		RemoteController.getInstance();
			    	} catch (ChannelException e){
			    		try {
							systemBundle.stop();
							throw e;
						} catch (BundleException e1) {
							e1.printStackTrace();
						}
			    	}
			    }
		    }, props);
		
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override 	
		    public void run() {
		    	System.err.println("Shutdown hook for remote called.");
		    	try {
					systemBundle.stop();
				} catch (BundleException e) {
					e.printStackTrace();
				}
		    }
		});
		
		generatePIDFile(systemBundle);
	}
	
	private void generatePIDFile(Bundle systemBundle){
		RuntimeMXBean rtb = ManagementFactory.getRuntimeMXBean();
		String name = rtb.getName();
		Integer pid = Integer.parseInt(name.substring(0, name.indexOf("@")));
		
		System.err.println("pid :" + pid);
		
        FileWriter fileWriter = null;
        try {
            File pidFile = new File("./RUNNING_PID");
            if (pidFile.exists()){
            	System.err.println("RUNNING_PID already exists. Abort start.");
            	systemBundle.stop();
            	System.exit(1);
            }
            fileWriter = new FileWriter(pidFile);
            fileWriter.write(pid.toString());
            fileWriter.close();
        } catch (IOException ex){
        	System.err.println(ex.getMessage());
		} catch (BundleException e) {
			e.printStackTrace();
		} finally {
            try {
                fileWriter.close();
            } catch (IOException ex) {
            	System.err.println(ex.getMessage());
            }
        }
	}
	
	@Override
	public void stop(BundleContext context) {
		System.err.println("STOPPING REMOTE");
		if (RemoteController.isStarted()){
			RemoteController.stop();
		}

		try{			 
    		File file = new File("./RUNNING_PID");
    		if(!file.delete()){
    			System.out.println("Error while deleting RUNNING_PID");
    		}
    	}catch(Exception e){
    		e.printStackTrace();
    	}
	}
	
	public void stopSilent() {
		System.err.println("STOPPING REMOTE silent.");
		RemoteController.stop();
	}
}
