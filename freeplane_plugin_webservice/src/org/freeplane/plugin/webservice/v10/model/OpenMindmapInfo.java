package org.freeplane.plugin.webservice.v10.model;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.freeplane.features.map.NodeModel;

public class OpenMindmapInfo {

	private final URL mapUrl;
	private final Set<NodeModel> lockedNodes;
	private long lastAccessTime;

	public OpenMindmapInfo(URL mapUrl) {
		this.mapUrl = mapUrl;
		this.lockedNodes = new HashSet<NodeModel>();
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
	
	private void updateAccessTime() {
		lastAccessTime = System.currentTimeMillis();
	}
	
}
