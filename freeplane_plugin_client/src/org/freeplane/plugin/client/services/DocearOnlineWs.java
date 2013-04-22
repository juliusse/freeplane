package org.freeplane.plugin.client.services;

import java.io.PrintStream;
import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.output.NullOutputStream;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.freeplane.core.util.LogUtils;
import org.freeplane.plugin.client.ClientController;
import org.freeplane.plugin.remote.v10.model.updates.AddNodeUpdate;
import org.freeplane.plugin.remote.v10.model.updates.ChangeNodeAttributeUpdate;
import org.freeplane.plugin.remote.v10.model.updates.DeleteNodeUpdate;
import org.freeplane.plugin.remote.v10.model.updates.MapUpdate;
import org.freeplane.plugin.remote.v10.model.updates.MoveNodeUpdate;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.dispatch.Futures;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class DocearOnlineWs implements WS {
	private final ClientController clientController;
	private final String serviceUrl = "http://localhost:9000";
	// private final String serviceUrl = "https://staging.my.docear.org";
	private final Client restClient;

	public DocearOnlineWs(ClientController clientController) {
		this.clientController = clientController;
		// com.google.common.util.concurrent.
		PrintStream stream = new PrintStream(new NullOutputStream());
		// disableCertificateValidation();
		restClient = ApacheHttpClient.create();
		restClient.addFilter(new LoggingFilter(stream));
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

		final String source = clientController.source();
		restClient.addFilter(new ClientFilter() {

			@Override
			public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
				String uriString = request.getURI().toASCIIString();
				uriString = uriString.contains("?") ? uriString + "&" : uriString + "?";

				final URI newUri = URI.create(uriString + "source=" + source);
				request.setURI(newUri);

				return getNext().handle(request);
			}
		});
	}

	public static void disableCertificateValidation() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} };

		// Ignore differences between given hostname and certificate hostname
		HostnameVerifier hv = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(hv);
		} catch (Exception e) {
		}
	}

	@Override
	public Future<Boolean> login(final String username, final String password) {
		final WebResource loginResource = restClient.resource(serviceUrl).path("rest/login");
		MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("username", username);
		formData.add("password", password);
		final ClientResponse loginResponse = loginResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);

		return Futures.successful(loginResponse.getStatus() == 200);
	}

	@Override
	public Future<Boolean> listenIfUpdatesOccur(final String mapId) {
		return Futures.future(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				final WebResource resource = restClient.resource(serviceUrl).path("map/" + mapId + "/listen");

				final ClientResponse loginResponse = resource.get(ClientResponse.class);
				return loginResponse.getStatus() == 200;
			}
		}, clientController.system().dispatcher());

	}

	@Override
	public Future<JsonNode> getMapAsXml(final String mapId) {

		try {
			final WebResource mapAsXmlResource = restClient.resource(serviceUrl).path("map/" + mapId + "/xml");
			final JsonNode response = new ObjectMapper().readTree(mapAsXmlResource.get(String.class));
			return Futures.successful(response);
		} catch (Exception e) {
			e.printStackTrace();
			return Futures.failed(e);
		}

	}

	@Override
	public Future<GetUpdatesResponse> getUpdatesSinceRevision(final String mapId, final int sinceRevision) {

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
			return Futures.failed(e);
		}
		return Futures.successful(new GetUpdatesResponse(currentRevision, updates));

	}

	@Override
	public Future<String> createNode(final String mapId, final String parentNodeId) {

		final WebResource resource = restClient.resource(serviceUrl).path("map/" + mapId + "/node/create");
		final MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("parentNodeId", parentNodeId);

		final ClientResponse response = resource.post(ClientResponse.class, formData);
		try {
			final AddNodeUpdate update = new ObjectMapper().readValue(response.getEntity(String.class), AddNodeUpdate.class);
			return Futures.successful(update.getNewNodeId());
		} catch (Exception e) {
			e.printStackTrace();
			return Futures.failed(e);
		}
	}

	@Override
	public Future<Boolean> moveNodeTo(final String mapId, final String newParentId, final String nodeToMoveId, final int newIndex) {
		final WebResource resource = restClient.resource(serviceUrl).path("map/" + mapId + "/node/move");
		final MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("newParentNodeId", newParentId);
		formData.add("nodetoMoveId", nodeToMoveId);
		formData.add("newIndex", newIndex + "");

		final ClientResponse response = resource.post(ClientResponse.class, formData);
		LogUtils.info("Status: " + response.getStatus());
		return Futures.successful(response.getStatus() == 200);

	}

	@Override
	public Future<Boolean> removeNode(final String mapId, final String nodeId) {

		final WebResource resource = restClient.resource(serviceUrl).path("map/" + mapId + "/node/delete");
		final MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("nodeId", nodeId);

		ClientResponse response = resource.delete(ClientResponse.class, formData);

		LogUtils.info("Status: " + response.getStatus());
		return Futures.successful(response.getStatus() == 200);

	}

	@Override
	public Future<Boolean> changeNode(final String mapId, final String nodeId, final String attribute, final Object value) {
		try {
			final WebResource resource = restClient.resource(serviceUrl).path("map/" + mapId + "/node/change");
			final MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
			formData.add("nodeId", nodeId);
			Map<String, Object> attributeValueMap = new HashMap<String, Object>();
			attributeValueMap.put(attribute, value);
			formData.add("AttributeValueMapJson", new ObjectMapper().writeValueAsString(attributeValueMap));

			LogUtils.info("locking node");
			// boolean isLocked =
			boolean isLocked = Await.result(lockNode(mapId, nodeId), Duration.create("5 seconds"));
			if (!isLocked)
				return Futures.successful(false);
			LogUtils.info("changing");
			ClientResponse response = resource.post(ClientResponse.class, formData);
			LogUtils.info("releasing node");
			releaseNode(mapId, nodeId);

			LogUtils.info("Status: " + response.getStatus());
			return Futures.successful(response.getStatus() == 200);

		} catch (Exception e) {
			e.printStackTrace();
			return Futures.failed(e);
		}
	}

	private Future<Boolean> lockNode(final String mapId, final String nodeId) {
		final WebResource resource = restClient.resource(serviceUrl).path("map/" + mapId + "/node/requestLock");
		final MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("nodeId", nodeId);

		ClientResponse response = resource.post(ClientResponse.class, formData);
		LogUtils.info("Status: " + response.getStatus());
		return Futures.successful(response.getStatus() == 200);
	}

	private Future<Boolean> releaseNode(final String mapId, final String nodeId) {
		final WebResource resource = restClient.resource(serviceUrl).path("map/" + mapId + "/node/releaseLock");
		final MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("nodeId", nodeId);

		ClientResponse response = resource.post(ClientResponse.class, formData);

		LogUtils.info("Status: " + response.getStatus());
		return Futures.successful(response.getStatus() == 200);
	}
}
