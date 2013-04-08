package org.freeplane.plugin.remote.v10.model.updates;

public class AddNodeUpdate extends MapUpdate {
	public enum Side {Left, Right}

	private final String parentNodeId;
	private final String nodeAsJson;
	private final Side side;

	// Do not remove! Needed for jackson (in testing project)
	@SuppressWarnings("unused")
	private AddNodeUpdate() {
		this("","",null);
	}


	/**
	 * Constructor for default nodes (don't need side)
	 */
	public AddNodeUpdate(String parentNodeId, String nodeAsJson) {
		this(parentNodeId, nodeAsJson, null);
	}


	/**
	 * For node that is added to root node
	 * @param side only required at root node level
	 */
	public AddNodeUpdate(String parentNodeId, String nodeAsJson, Side side) {
		super(Type.AddNode);
		this.parentNodeId = parentNodeId;
		this.nodeAsJson = nodeAsJson;
		this.side = side;
	}



	public Side getSide() {
		return side;
	}



	public String getNodeAsJson() {
		return nodeAsJson;
	}

	public String getParentNodeId() {
		return parentNodeId;
	}

}
