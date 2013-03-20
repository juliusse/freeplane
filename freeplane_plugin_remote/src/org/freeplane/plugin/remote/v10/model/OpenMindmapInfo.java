package org.freeplane.plugin.remote.v10.model;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.freeplane.features.map.NodeModel;

public class OpenMindmapInfo implements Serializable {
	private static final long serialVersionUID = 1L;

	private final URL mapUrl;
	private final Set<NodeModel> lockedNodes;
	private long lastAccessTime;
	private long lastUpdateTime;
	private final String name;
	private final List<String> updateList;

	public OpenMindmapInfo(URL mapUrl, String name) {
		this.mapUrl = mapUrl;
		this.name = name;
		this.lockedNodes = new HashSet<NodeModel>();
		this.updateList = new ArrayList<String>();
		updateAccessTime();
	}

	public URL getMapUrl() {
		updateAccessTime();
		return mapUrl;
	}

	public Set<NodeModel> getLockedNodes() {
		updateAccessTime();
		return lockedNodes;
	}

	public long getLastAccessTime() {
		return lastAccessTime;
	}
	
	public long getLastUpdateTime() {
		return lastUpdateTime;
	}

	public String getName() {
		return name;
	}
	
	private void updateAccessTime() {
		lastAccessTime = System.currentTimeMillis();
	}
	
	//update and revision related
	private void updateUpdateTime() {
		lastUpdateTime = System.currentTimeMillis();
	}
	
	public long getCurrentRevision() {
		return updateList.size();
	}
	
	public void addUpdate(String updateStatement) {
		updateList.add(updateStatement);
		updateUpdateTime();
	}
}
