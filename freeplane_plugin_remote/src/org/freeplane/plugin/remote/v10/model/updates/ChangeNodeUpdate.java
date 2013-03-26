package org.freeplane.plugin.remote.v10.model.updates;

public class ChangeNodeUpdate extends MapUpdate {

	private final String nodeAsJson;
	
	public ChangeNodeUpdate(String nodeAsJson) {
		super(Type.ChangeNode);
		
		this.nodeAsJson = nodeAsJson;
	}

	public String getNodeAsJson() {
		return nodeAsJson;
	}
}
