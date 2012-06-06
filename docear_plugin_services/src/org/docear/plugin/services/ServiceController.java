package org.docear.plugin.services;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;

import javax.swing.JMenu;
import javax.swing.SwingUtilities;

import org.docear.plugin.communications.CommunicationsController;
import org.docear.plugin.core.DocearController;
import org.docear.plugin.services.actions.DocearAllowUploadChooserAction;
import org.docear.plugin.services.actions.DocearCheckForUpdatesAction;
import org.docear.plugin.services.actions.DocearClearUserDataAction;
import org.docear.plugin.services.features.UpdateCheck;
import org.docear.plugin.services.features.elements.Application;
import org.docear.plugin.services.listeners.DocearEventListener;
import org.docear.plugin.services.listeners.MapLifeCycleListener;
import org.docear.plugin.services.recommendations.actions.ShowRecommendationsAction;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.IMenuContributor;
import org.freeplane.core.ui.MenuBuilder;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.IMapLifeCycleListener;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;

public class ServiceController {
	public static final String DOCEAR_INFORMATION_RETRIEVAL = "docear_information_retrieval";
	public static final String DOCEAR_SAVE_BACKUP = "docear_save_backup";

	private final static ServiceController serviceController = new ServiceController();
	
	private final ServiceRunner backupRunner = new ServiceRunner();
	private final File backupFolder = new File(CommunicationsController.getController().getCommunicationsQueuePath(), "mindmaps");
	
	private final IMapLifeCycleListener mapLifeCycleListener = new MapLifeCycleListener();
	public static final int ALLOW_RECOMMENDATIONS = 8;
	public static final int ALLOW_USAGE_MINING = 4;
	public static final int ALLOW_INFORMATION_RETRIEVAL = 2;
	public static final int ALLOW_RESEARCH = 1;
	
	private Application application;

	private static FileFilter zipFilter = new FileFilter() {
		public boolean accept(File f) {
			return (f != null && f.getName().toLowerCase().endsWith(".zip"));
		}		
	};
	
	public ServiceController() {
		LogUtils.info("starting DocearBackupStarter()");
		initListeners();
		
		new ServiceConfiguration();	    
		new ServicePreferences();
		
		addPluginDefaults();
		addMenuEntries();
		Controller.getCurrentController().addAction(new DocearClearUserDataAction());
		Controller.getCurrentController().addAction(new DocearAllowUploadChooserAction());
		Controller.getCurrentController().addAction(new DocearCheckForUpdatesAction());
		Controller.getCurrentController().addAction(new ShowRecommendationsAction());
	
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {				
				backupRunner.run();
				new UpdateCheck();
			}		
		});			
	}
	
	public void initListeners() {
		DocearController.getController().addDocearEventListener(new DocearEventListener());
		Controller.getCurrentModeController().getMapController().addMapLifeCycleListener(mapLifeCycleListener);
	}
	
	public static ServiceController getController() {
		return serviceController;
	}
	
	public ServiceRunner getBackupRunner() {
		return backupRunner;
	}

	private void addPluginDefaults() {
		final URL defaults = this.getClass().getResource(ResourceController.PLUGIN_DEFAULTS_RESOURCE);
		if (defaults == null)
			throw new RuntimeException("cannot open " + ResourceController.PLUGIN_DEFAULTS_RESOURCE);
		Controller.getCurrentController().getResourceController().addDefaults(defaults);		
	}
	
	public boolean isBackupEnabled() {
		return ResourceController.getResourceController().getBooleanProperty(DOCEAR_SAVE_BACKUP);
	}
	
	public void setBackupEnabled(boolean b) {
		ResourceController.getResourceController().setProperty(DOCEAR_SAVE_BACKUP, b);
	}
	
	public int getInformationRetrievalCode() {
		return Integer.parseInt(ResourceController.getResourceController().getProperty(DOCEAR_INFORMATION_RETRIEVAL, "0"));
	}
	
	public boolean isResearchAllowed() {
		return (getInformationRetrievalCode() & ALLOW_RESEARCH) > 0;
	}
	
	public boolean isInformationRetrievalSelected() {
		return (getInformationRetrievalCode() & ALLOW_INFORMATION_RETRIEVAL) > 0;
	}
	
	public boolean isUsageMiningAllowed() {
		return (getInformationRetrievalCode() & ALLOW_USAGE_MINING) > 0;
	}
	
	public boolean isRecommendationsAllowed() {
		return (getInformationRetrievalCode() & ALLOW_RECOMMENDATIONS) > 0;
	}
	
	public void setInformationRetrievalCode(int code) {
		ResourceController.getResourceController().setProperty(DOCEAR_INFORMATION_RETRIEVAL, ""+code);
	}
	
	public File getBackupDirectory() {		
		if (!backupFolder.exists()) {
			backupFolder.mkdirs();
		}
		return backupFolder;
	}
	
	
	public File[] getBackupQueue() {
		return getBackupDirectory().listFiles(zipFilter);
	}
	
	public boolean isBackupAllowed() {
		CommunicationsController commCtrl = CommunicationsController.getController();
		return isBackupEnabled() && commCtrl.allowTransmission() && !isEmpty(commCtrl.getRegisteredAccessToken()) && !isEmpty(commCtrl.getRegisteredUserName());
	}
	
	public boolean isInformationRetrievalAllowed() {
		CommunicationsController commCtrl = CommunicationsController.getController();
		boolean needUser = getInformationRetrievalCode()>0 && commCtrl.allowTransmission();
		
		return needUser && (!isEmpty(commCtrl.getAccessToken()) || !isEmpty(commCtrl.getUserName()));
	}
	
	private boolean isEmpty(String s) {
		return s == null || s.trim().length() == 0;
	}

	public Application getApplication() {
		return application;
	}

	public void setApplication(Application application) {
		this.application = application;
	}
	
	private void addMenuEntries() {

		Controller.getCurrentModeController().addMenuContributor(new IMenuContributor() {
			public void updateMenus(ModeController modeController, MenuBuilder builder) { // /EditDetailsInDialogAction
				builder.addMenuItem("/menu_bar/extras",new JMenu(TextUtils.getText("docear.recommendations.menu")), "/menu_bar/recommendations", MenuBuilder.BEFORE);
				builder.addAction("/menu_bar/recommendations", new ShowRecommendationsAction(),	MenuBuilder.AS_CHILD);				
				builder.addMenuItem("/node_popup",new JMenu(TextUtils.getText("docear.recommendations.menu")), "/node_popup/recommendations", MenuBuilder.PREPEND);
				builder.addAction("/node_popup/recommendations", new ShowRecommendationsAction(), MenuBuilder.AS_CHILD);
			}
		});
	}
}
