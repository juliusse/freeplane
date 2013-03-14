package org.freeplane.plugin.remote;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.freeplane.core.util.Compat;
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
		final Bundle systemBundle = context.getBundle(0);
		
		saveAutoProperties();
		registerToFreeplaneStart(context);


		registerShutDownHook(context);

		generatePIDFile(systemBundle);

	}
	
	private void registerToFreeplaneStart(final BundleContext context) {
		final Hashtable<String, String[]> props = new Hashtable<String, String[]>();
		final Bundle systemBundle = context.getBundle(0);
		
		props.put("mode", new String[] { MModeController.MODENAME });
		context.registerService(IModeControllerExtensionProvider.class.getName(),
				new IModeControllerExtensionProvider() {
			public void installExtension(ModeController modeController) {
				try {
					RemoteController.getInstance();
				} catch(ChannelException e) {
					Logger.getLogger().error("Error while starting RemotePlugin!\n",e);
					stop(context);
					try{systemBundle.stop();} catch(Exception e1) {}
					System.exit(1);
				}
			}
		}, props);
	}
	
	private void registerShutDownHook(BundleContext context) {
		final Bundle systemBundle = context.getBundle(0);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override 	
			public void run() {
				Logger.getLogger().info("Shutdown initiated...");
				try {
					systemBundle.stop();
				} catch (BundleException e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void saveAutoProperties() {
		//send auto.properties to correct location
		final String freeplaneDirectory = Compat.getFreeplaneUserDirectory();
		final File userPropertiesFolder = new File(freeplaneDirectory);
		final File autoPropertiesFile = new File(userPropertiesFolder, "auto.properties");
		Logger.getLogger().info("Activator.start => trying to save auto.properties to '{}'.",autoPropertiesFile.getAbsolutePath());
		InputStream in = null;
		OutputStream out = null;
		try {
			in = RemoteController.class.getResourceAsStream("/auto.properties");
			out = new FileOutputStream(autoPropertiesFile);
			IOUtils.copy(in, out);
		} catch (Exception e) {
			Logger.getLogger().error("could not save auto.properties. ",e);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}
	}

//	@Override
//	public void configure(BundleContext arg0, ActorSystem arg1) {
//		RemoteController.stop();
//	}
	
	private void generatePIDFile(Bundle systemBundle){
		RuntimeMXBean rtb = ManagementFactory.getRuntimeMXBean();
		String name = rtb.getName();
		Integer pid = Integer.parseInt(name.substring(0, name.indexOf("@")));
		
		Logger.getLogger().info("OSGI pid: {}",pid);
		
		
        FileWriter fileWriter = null;
        try {
            File pidFile = new File("RUNNING_PID");
            Logger.getLogger().info("Path of RUNNING_PID = '{}'",pidFile.getAbsolutePath());
            if (pidFile.exists()){
            	Logger.getLogger().error("RUNNING_PID already exists. Abort start.");
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
		Logger.getLogger().info("Activator.stop => stopping remote plugin.");
		
		if (RemoteController.isStarted()){
			RemoteController.stop();
		}

		try{			 
    		File file = new File("./RUNNING_PID");
    		if(!file.delete()){
    			Logger.getLogger().error("Error while deleting RUNNING_PID");
    		}
    	}catch(Exception e){
    		Logger.getLogger().error("Error while deleting RUNNING_PID",e);
    	}
	}
}
