package org.freeplane.plugin.remote.v10.model.updates;

public class DeleteNodeUpdate extends MapUpdate {

	private final String nodeId;
	
	public DeleteNodeUpdate(String nodeId) {
		super(Type.DeleteNode);
		
		this.nodeId = nodeId;
	}

	public String getNodeId() {
		return nodeId;
	}

	
}
