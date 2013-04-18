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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
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
	private final ListeningExecutorService threadPool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

	private final String serviceUrl = "http://localhost:9000";
	// private final String serviceUrl = "https://staging.my.docear.org";
	private final Client restClient;

	public DocearOnlineWs() {
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

		restClient.addFilter(new ClientFilter() {

			@Override
			public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
				String uriString = request.getURI().toASCIIString();
				uriString = uriString.contains("?") ? uriString + "&" : uriString + "?";

				final URI newUri = URI.create(uriString + "source=" + ClientController.source());
				request.setURI(newUri);

				return getNext().handle(request);
			}
		});
	}
	
	@Override
	public Executor getExecutor() {
		return threadPool;
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
	public ListenableFuture<Boolean> login(final String username, final String password) {

		return threadPool.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				final WebResource loginResource = restClient.resource(serviceUrl).path("rest/login");
				MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
				formData.add("username", username);
				formData.add("password", password);
				final ClientResponse loginResponse = loginResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);

				return loginResponse.getStatus() == 200;
			}
		});

	}

	@Override
	public ListenableFuture<Boolean> listenIfUpdatesOccur(final String mapId) {
		return threadPool.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				final WebResource resource = restClient.resource(serviceUrl).path("map/" + mapId + "/listen");

				final ClientResponse loginResponse = resource.get(ClientResponse.class);
				return loginResponse.getStatus() == 200;
			}
		});
	}

	@Override
	public ListenableFuture<JsonNode> getMapAsXml(final String mapId) {
		return threadPool.submit(new Callable<JsonNode>() {

			@Override
			public JsonNode call() throws Exception {
				try {
					final WebResource mapAsXmlResource = restClient.resource(serviceUrl).path("map/" + mapId + "/xml");
					final JsonNode response = new ObjectMapper().readTree(mapAsXmlResource.get(String.class));
					return response;
				} catch (Exception e) {
					e.printStackTrace();
				}

				return null;
			}
		});
	}

	@Override
	public ListenableFuture<GetUpdatesResponse> getUpdatesSinceRevision(final String mapId, final int sinceRevision) {
		return threadPool.submit(new Callable<GetUpdatesResponse>() {

			@Override
			public GetUpdatesResponse call() throws Exception {
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
		});
	}

	@Override
	public ListenableFuture<String> createNode(final String mapId, final String parentNodeId) {
		return threadPool.submit(new Callable<String>() {

			@Override
			public String call() throws Exception {
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
		});
	}

	@Override
	public ListenableFuture<Boolean> moveNodeTo(final String mapId, final String newParentId, final String nodeToMoveId, final int newIndex) {
		return threadPool.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				final WebResource resource = restClient.resource(serviceUrl).path("map/" + mapId + "/node/move");
				final MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
				formData.add("newParentNodeId", newParentId);
				formData.add("nodetoMoveId", nodeToMoveId);
				formData.add("newIndex", newIndex + "");

				final ClientResponse response = resource.post(ClientResponse.class, formData);
				LogUtils.info("Status: " + response.getStatus());
				return response.getStatus() == 200;
			}
		});
	}

	@Override
	public ListenableFuture<Boolean> removeNode(final String mapId, final String nodeId) {
		return threadPool.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				final WebResource resource = restClient.resource(serviceUrl).path("map/" + mapId + "/node/delete");
				final MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
				formData.add("nodeId", nodeId);

				ClientResponse response = resource.delete(ClientResponse.class, formData);

				LogUtils.info("Status: " + response.getStatus());
				return response.getStatus() == 200;
			}
		});

	}

	@Override
	public ListenableFuture<Boolean> changeNode(final String mapId, final String nodeId, final String attribute, final Object value) {
		return threadPool.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				try {
					final WebResource resource = restClient.resource(serviceUrl).path("map/" + mapId + "/node/change");
					final MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
					formData.add("nodeId", nodeId);
					Map<String, Object> attributeValueMap = new HashMap<String, Object>();
					attributeValueMap.put(attribute, value);
					formData.add("AttributeValueMapJson", new ObjectMapper().writeValueAsString(attributeValueMap));

					LogUtils.info("locking node");
					// boolean isLocked =
					boolean isLocked = Futures.getUnchecked(lockNode(mapId, nodeId));
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
		});
	}

	private ListenableFuture<Boolean> lockNode(final String mapId, final String nodeId) {
		return threadPool.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				final WebResource resource = restClient.resource(serviceUrl).path("map/" + mapId + "/node/requestLock");
				final MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
				formData.add("nodeId", nodeId);

				ClientResponse response = resource.post(ClientResponse.class, formData);
				LogUtils.info("Status: " + response.getStatus());
				return response.getStatus() == 200;
			}
		});
	}

	private ListenableFuture<Boolean> releaseNode(final String mapId, final String nodeId) {
		return threadPool.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				final WebResource resource = restClient.resource(serviceUrl).path("map/" + mapId + "/node/releaseLock");
				final MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
				formData.add("nodeId", nodeId);

				ClientResponse response = resource.post(ClientResponse.class, formData);

				LogUtils.info("Status: " + response.getStatus());
				return response.getStatus() == 200;
			}
		});
	}
}
