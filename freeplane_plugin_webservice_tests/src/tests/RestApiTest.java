package tests;
import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.freeplane.plugin.webservice.v10.Webservice;
import org.freeplane.plugin.webservice.v10.WebserviceHelper;
import org.freeplane.plugin.webservice.v10.model.DefaultNodeModel;
import org.freeplane.plugin.webservice.v10.model.MapModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;


public class RestApiTest {

	private Client client;


	@Before
	public void setUp() throws Exception {
		ClientConfig clientConfig = new DefaultClientConfig();
		clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

		client = Client.create();
	}

	@After
	public void tearDown() throws Exception {
		client.destroy();
	}

	@Test
	public void addNodeToRootAndRemoveTest() {
//		WebResource wr = client.resource("http://localhost:8080/rest/v1");
//		String nodeId = wr.path("addNodeToRootNode").accept(MediaType.APPLICATION_JSON_TYPE).get(String.class);
//		assertThat(nodeId).startsWith("ID_");
//
//		Boolean deleted = Boolean.valueOf(wr.path("removeNode").path(nodeId).delete(String.class));
//		assertThat(deleted).isTrue();
	}

	@Test
	public void getMapAsJsonTest() {
		WebResource wr = client.resource("http://localhost:8080/rest/v1");
		
		WebResource getMapResource = wr.path("map").path("1").path("json").queryParam("nodeCount", "5");
		ClientResponse cr = getMapResource.accept(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

		
		
		// Jackson Way to get map
		//		String content = cr.getEntity(String.class);
		//		System.out.println(content);
		//		ObjectMapper mapper = new ObjectMapper();
		//		mapper.configure(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
		//      MapModel map = mapper.readValue(content, MapModel.class);// cr.getEntity(MapModel.class);
		
		assertThat(cr.getStatus()).isEqualTo(200);
		
		MapModel map = cr.getEntity(MapModel.class);
		
		assertThat(map).isInstanceOf(MapModel.class);
		assertThat(map.root.nodeText).isEqualTo("foo2");
	}
	
//	@Test
//	public void getAddAndRemoveNodeToFirstRightChild() {
//		WebResource wr = client.resource("http://localhost:8080/rest/v1");
//		
//		MapModel map = wr.path("map").path("json").path("1").queryParam("nodeCount", "5").get(MapModel.class);
//		DefaultNodeModel node =  map.root.rightChildren.get(0);
//		
//		WebResource addNodeResource = wr.path("node").path(node.id).queryParam("nodeText", "myNode");
//		ClientResponse cr = addNodeResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class);
//		
//		assertThat(cr.getStatus()).isEqualTo(200);
//		DefaultNodeModel newNode = cr.getEntity(DefaultNodeModel.class);
//		
//		assertThat(newNode).isInstanceOf(DefaultNodeModel.class);
//		assertThat(newNode.nodeText).isEqualTo("myNode");
//		
//		
//		
//		Boolean deleted = Boolean.valueOf(wr.path("removeNode").path(newNode.id).delete(String.class));
//		assertThat(deleted).isTrue();
//		
//	}
	
	@Test
	public void sendMindmapToApplicationAndGetMindmapAsJson() throws URISyntaxException {
		WebResource wr = client.resource("http://localhost:8080/rest/v1");
		WebResource sendMapResource = wr.path("map");
		InputStream in = Webservice.class.getResourceAsStream("/files/mindmaps/1.mm");
		URL pathURL = Webservice.class.getResource("/files/mindmaps/1.mm");
		File f = new File( pathURL.toURI());
		
		String contentDeposition = "attachement; filename=\"1.mm\"";
		assertThat(f).isNotNull();
		
//		MultivaluedMap formData = new MultivaluedMapImpl();
//		formData.add("file", in);
		ClientResponse response = sendMapResource.type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
				.header("Content-Deposition", contentDeposition)
				.put(ClientResponse.class, in);
		
		assertThat(response.getStatus()).isEqualTo(200);
		
//		String nodeId = wr.path("addNodeToRootNode").accept(MediaType.APPLICATION_JSON_TYPE).get(String.class);
//		assertThat(nodeId).startsWith("ID_");
//		
		MapModel model = wr.path("map/1/json").accept(MediaType.APPLICATION_JSON_TYPE).get(MapModel.class);
		assertThat(model).isNotNull();
		
		
	}
	
//	@Test
//	public void selectOpenedMapTest() throws URISyntaxException {
//		WebResource wr = client.resource("http://localhost:8080/rest/v1");
//		
//		// 1. send a map
//		WebResource sendMapResource = wr.path("openMindmap");
//		InputStream in = Webservice.class.getResourceAsStream("/files/mindmaps/2.mm");
//		URL pathURL = Webservice.class.getResource("/files/mindmaps/2.mm");
//		File f = new File( pathURL.toURI());
//		
//		String mapId = WebserviceHelper.getMapIdFromFile(f);
//		
//		String contentDeposition = "attachement; filename=\"2.mm\"";
//		assertThat(f).isNotNull();
//		
//		ClientResponse response = sendMapResource.type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
//				.header("Content-Deposition", contentDeposition)
//				.put(ClientResponse.class, in);
//		
//		assertThat(response.getStatus()).isEqualTo(200);		
//		
//		// 2. select another map
//		response = wr.path("map").path("json").path("1").get(ClientResponse.class);
//		assertThat(response.getStatus()).isEqualTo(200);
//		
//		// 3. reselect first map
//		response = wr.path("selectMindmap").path(mapId).put(ClientResponse.class);
//		assertThat(response.getStatus()).isEqualTo(200);
//		
//		DefaultNodeModel node = wr.path("node").path("ID_1247776391").get(DefaultNodeModel.class);
//		assertThat(node.nodeText).isEqualTo("test");
//		
//	}

}
