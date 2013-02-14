package org.freeplane.plugin.webservice;

import java.io.Serializable;

import org.freeplane.plugin.webservice.v10.model.DefaultNodeModel;

public class Messages {

	public static class MindmapAsJsonRequest implements Serializable {

		private final String id;
		private final int nodeCount;

		public MindmapAsJsonRequest(String id) {
			this(id,-1);
		}
		
		public MindmapAsJsonRequest(String id, int nodeCount) {
			this.id = id;
			this.nodeCount = nodeCount;
		}

		public String getId() {
			return id;
		}

		public int getNodeCount() {
			return nodeCount;
		}
		
	}

	public static class MindmapAsJsonReponse implements Serializable {
		private final String jsonString;

		public MindmapAsJsonReponse(String jsonString) {
			this.jsonString = jsonString;
		}

		public String getJsonString() {
			return jsonString;
		}
		
	}
	
	public static class AddNodeRequest implements Serializable {
		private final String mapId;
		private final String parentNodeId;
		
		public AddNodeRequest(String mapId, String parentNodeId) {
			super();
			this.mapId = mapId;
			this.parentNodeId = parentNodeId;
		}

		public String getMapId() {
			return mapId;
		}

		public String getParentNodeId() {
			return parentNodeId;
		}
		
	}
	
	public static class AddNodeResponse implements Serializable {
		private final DefaultNodeModel node;

		public AddNodeResponse(DefaultNodeModel node) {
			super();
			this.node = node;
		}

		public DefaultNodeModel getNode() {
			return node;
		}
	}
	
	public static class ErrorMessage implements Serializable {
		private final Exception e;

		public ErrorMessage(Exception e) {
			super();
			this.e = e;
		}

		public Exception getE() {
			return e;
		}
	}
}
