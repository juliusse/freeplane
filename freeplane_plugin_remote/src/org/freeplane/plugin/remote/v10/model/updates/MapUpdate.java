package org.freeplane.plugin.remote.v10.model.updates;

import org.codehaus.jackson.map.ObjectMapper;

public abstract class MapUpdate {
	public enum Type {
		ChangeNodeAttribute, AddNode, DeleteNode, MoveNode
	}

	private final Type type;

	public MapUpdate(Type type) {
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	public String toJson() {
		try {
			final ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(this);
		} catch (Exception e) {
			throw new AssertionError("Could not serialize MapUpdate from type "
					+ this.getClass().getSimpleName());
		}

	}
}
