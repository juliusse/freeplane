package org.freeplane.plugin.remote.v10.model.updates;

public class AddNodeUpdate extends MapUpdate {

	private final String parentNodeId;
	private final String nodeAsJson;

	// Do not remove! Needed for jackson (in testing project)
	private AddNodeUpdate() {
		super(Type.AddNode);
		parentNodeId = "";
		nodeAsJson = "";
	}
	
	public AddNodeUpdate(String parentNodeId, String nodeAsJson) {
		super(Type.AddNode);
		this.parentNodeId = parentNodeId;
		this.nodeAsJson = nodeAsJson;
	}

	public String getNodeAsJson() {
		return nodeAsJson;
	}

	public String getParentNodeId() {
		return parentNodeId;
	}
	
}
