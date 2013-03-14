package org.freeplane.plugin.remote.v10.model;

import java.io.Serializable;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.freeplane.features.map.NodeModel;

public class OpenMindmapInfo implements Serializable {

	private final URL mapUrl;
	private final Set<NodeModel> lockedNodes;
	private long lastAccessTime;
	private final String name;

	public OpenMindmapInfo(URL mapUrl, String name) {
		this.mapUrl = mapUrl;
		this.name = name;
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

	public String getName() {
		return name;
	}
	
}
