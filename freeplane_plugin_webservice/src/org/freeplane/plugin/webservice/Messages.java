package org.freeplane.plugin.webservice;

import java.io.Serializable;

public class Messages {

	public static class MindmapAsJsonRequest implements Serializable {

		private final String id;

		public MindmapAsJsonRequest(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
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
