package org.freeplane.plugin.remote.v10.model;

import java.io.Serializable;

import org.freeplane.core.extension.IExtension;
import org.freeplane.features.map.NodeModel;

public class LockModel implements IExtension, Serializable {
	private static final long serialVersionUID = 1L;
	
	private NodeModel node;
	private String username;
	private long lastAccess;
	
	public LockModel() {
		
	}
	
	public LockModel(NodeModel node, String username, long lastAccess) {
		super();
		this.node = node;
		this.username = username;
		this.lastAccess = lastAccess;
	}
//	public NodeModel getNodeId() {
//		return node;
//	}
//	public void setNodeId(NodeModel nodeId) {
//		this.node = nodeId;
//	}
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
