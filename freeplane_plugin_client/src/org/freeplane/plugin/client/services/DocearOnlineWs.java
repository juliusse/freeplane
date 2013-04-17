package org.freeplane.plugin.client.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.freeplane.core.util.LogUtils;
import org.freeplane.plugin.remote.v10.model.updates.AddNodeUpdate;
import org.freeplane.plugin.remote.v10.model.updates.ChangeNodeAttributeUpdate;
import org.freeplane.plugin.remote.v10.model.updates.DeleteNodeUpdate;
import org.freeplane.plugin.remote.v10.model.updates.MapUpdate;
import org.freeplane.plugin.remote.v10.model.updates.MoveNodeUpdate;

import com.sun.jersey.api.client.AsyncWebResource;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class DocearOnlineWs implements WS {

	private final String serviceUrl = "http://localhost:9000";
	private final Client restClient;

	public DocearOnlineWs() {

		restClient = ApacheHttpClient.create();
		restClient.addFilter(new ClientFilter() {
			private ArrayList<Object> cookies;

			@Override
			public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
				if (cookies != null) {
					request.getHeaders().put("Cookie", cookies);
				}
				ClientResponse response = getNext().handle(request);
				if (response.getCookies() != null) {
					if (cookies == null) {
						cookies = new ArrayList<Object>();
					}
					// simple addAll just for illustration (should probably
					// check for duplicates and expired cookies)
					cookies.addAll(response.getCookies());
				}
				return response;
			}
		});
	}

	@Override
	public boolean login(String username, String password) {
		final WebResource loginResource = restClient.resource(serviceUrl).path("rest/login");
		MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("username", username);
		formData.add("password", password);
		final ClientResponse loginResponse = loginResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);

		return loginResponse.getStatus() == 200;
	}

	@Override
	public boolean listenIfUpdatesOccur(String mapId) {
		final WebResource resource = restClient.resource(serviceUrl).path("map/" + mapId + "/listen");

		final ClientResponse loginResponse = resource.get(ClientResponse.class);

		return loginResponse.getStatus() == 200;
	}

	@Override
	public JsonNode getMapAsXml(String mapId) {
		try {
			final WebResource mapAsXmlResource = restClient.resource(serviceUrl).path("map/" + mapId + "/xml");
			final JsonNode response = new ObjectMapper().readTree(mapAsXmlResource.get(String.class));
			return response;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public GetUpdatesResponse getUpdatesSinceRevision(String mapId, int sinceRevision) {
		int currentRevision = -1;
		List<MapUpdate> updates = new ArrayList<MapUpdate>();
		final WebResource fetchUpdates = restClient.resource(serviceUrl).path("map/" + mapId + "/updates/" + sinceRevision);
		final ClientResponse response = fetchUpdates.get(ClientResponse.class);
		final ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode json = mapper.readTree(response.getEntity(String.class));
			currentRevision = json.get("currentRevision").asInt();

			Iterator<JsonNode> it = json.get("orderedUpdates").getElements();
			while (it.hasNext()) {
				final JsonNode mapUpdateJson = mapper.readTree(it.next().asText());

				final MapUpdate.Type type = MapUpdate.Type.valueOf(mapUpdateJson.get("type").asText());
				switch (type) {
				case AddNode:
					updates.add(mapper.readValue(mapUpdateJson, AddNodeUpdate.class));
					break;
				case ChangeNodeAttribute:
					updates.add(mapper.readValue(mapUpdateJson, ChangeNodeAttributeUpdate.class));
					break;
				case DeleteNode:
					updates.add(mapper.readValue(mapUpdateJson, DeleteNodeUpdate.class));
					break;
				case MoveNode:
					updates.add(mapper.readValue(mapUpdateJson, MoveNodeUpdate.class));
					break;

				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new GetUpdatesResponse(currentRevision, updates);
	}

	@Override
	public String createNode(String mapId, String parentNodeId) {
		final WebResource resource = restClient.resource(serviceUrl).path("map/" + mapId + "/node/create");
		final MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("parentNodeId", parentNodeId);

		final ClientResponse response = resource.post(ClientResponse.class, formData);
		try {
			final AddNodeUpdate update = new ObjectMapper().readValue(response.getEntity(String.class), AddNodeUpdate.class);
			return update.getNewNodeId();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	@Override
	public boolean moveNodeTo(String mapId, String newParentId, String nodeToMoveId, int newIndex) {
		final WebResource resource = restClient.resource(serviceUrl).path("map/" + mapId + "/node/move");
		final MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("newParentNodeId", newParentId);
		formData.add("nodetoMoveId", nodeToMoveId);
		formData.add("newIndex", newIndex + "");

		final ClientResponse response = resource.post(ClientResponse.class, formData);
		LogUtils.info("Status: " + response.getStatus());
		return response.getStatus() == 200;
	}

	@Override
	public boolean removeNode(String mapId, String nodeId) {
		final WebResource resource = restClient.resource(serviceUrl).path("map/" + mapId + "/node/delete");
		final MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("nodeId", nodeId);

		ClientResponse response = resource.delete(ClientResponse.class, formData);

		LogUtils.info("Status: " + response.getStatus());
		return response.getStatus() == 200;

	}

	@Override
	public boolean changeNode(String mapId, String nodeId, String attribute, Object value) {
		try {
			final WebResource resource = restClient.resource(serviceUrl).path("map/" + mapId + "/node/change");
			final MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
			formData.add("nodeId", nodeId);
			Map<String, Object> attributeValueMap = new HashMap<String, Object>();
			attributeValueMap.put(attribute, value);
			formData.add("AttributeValueMapJson", new ObjectMapper().writeValueAsString(attributeValueMap));

			LogUtils.info("locking node");
			boolean isLocked = lockNode(mapId, nodeId);
			if (!isLocked)
				return false;
			LogUtils.info("changing");
			ClientResponse response = resource.post(ClientResponse.class, formData);
			LogUtils.info("releasing node");
			releaseNode(mapId, nodeId);

			LogUtils.info("Status: " + response.getStatus());
			return response.getStatus() == 200;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	private boolean lockNode(String mapId, String nodeId) {
		final AsyncWebResource resource = restClient.asyncResource(serviceUrl).path("map/" + mapId + "/node/requestLock");
		final MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("nodeId", nodeId);

		// ClientResponse response =
		resource.post(ClientResponse.class, formData);
		return true;
		// LogUtils.info("Status: " + response.getStatus());
		// return response.getStatus() == 200;
	}

	private boolean releaseNode(String mapId, String nodeId) {
		final AsyncWebResource resource = restClient.asyncResource(serviceUrl).path("map/" + mapId + "/node/releaseLock");
		final MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("nodeId", nodeId);

		// ClientResponse response =
		resource.post(ClientResponse.class, formData);
		return true;
		// LogUtils.info("Status: " + response.getStatus());
		// return response.getStatus() == 200;
	}
}
