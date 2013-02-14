package org.freeplane.plugin.webservice;

import java.io.Serializable;

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
			super();
			this.jsonString = jsonString;
		}

		public String getJsonString() {
			return jsonString;
		}
		
	}
}
