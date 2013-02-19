package org.freeplane.plugin.webservice.v10.model;

import java.io.Serializable;

import org.freeplane.core.extension.IExtension;
import org.freeplane.features.map.NodeModel;

public class LockModel implements IExtension, Serializable {

	private NodeModel nodeId;
	private String username;
	private long lastAccess;
	
	public LockModel() {
		
	}
	
	public LockModel(NodeModel nodeId, String username, long lastAccess) {
		super();
		this.nodeId = nodeId;
		this.username = username;
		this.lastAccess = lastAccess;
	}
	public NodeModel getNodeId() {
		return nodeId;
	}
	public void setNodeId(NodeModel nodeId) {
		this.nodeId = nodeId;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public long getLastAccess() {
		return lastAccess;
	}
	public void setLastAccess(long lastAccess) {
		this.lastAccess = lastAccess;
	}
	
	
}
