package org.freeplane.plugin.remote.v10.model.updates;

public class MoveNodeUpdate extends MapUpdate {
	private final String newParentNodeId;
	private final String nodetoMoveId;
	private final Integer newIndex;

	public MoveNodeUpdate(String newParentNodeId, String nodetoMoveId, Integer newIndex) {
		super(Type.MoveNode);
		this.newParentNodeId = newParentNodeId;
		this.nodetoMoveId = nodetoMoveId;
		this.newIndex = newIndex;
	}

	public String getNewParentNodeId() {
		return newParentNodeId;
	}

	public String getNodetoMoveId() {
		return nodetoMoveId;
	}

	public Integer getNewIndex() {
		return newIndex;
	}

}
