package tests;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.docear.messages.Messages.AddNodeRequest;
import org.docear.messages.Messages.AddNodeResponse;
import org.docear.messages.Messages.ChangeNodeRequest;
import org.docear.messages.Messages.CloseAllOpenMapsRequest;
import org.docear.messages.Messages.CloseMapRequest;
import org.docear.messages.Messages.CloseUnusedMaps;
import org.docear.messages.Messages.GetNodeRequest;
import org.docear.messages.Messages.GetNodeResponse;
import org.docear.messages.Messages.MindmapAsJsonReponse;
import org.docear.messages.Messages.MindmapAsJsonRequest;
import org.docear.messages.Messages.MindmapAsXmlRequest;
import org.docear.messages.Messages.MindmapAsXmlResponse;
import org.docear.messages.Messages.OpenMindMapRequest;
import org.docear.messages.Messages.RemoveNodeRequest;
import org.docear.messages.exceptions.MapNotFoundException;
import org.docear.messages.exceptions.NodeNotFoundException;
import org.fest.assertions.Fail;
import org.freeplane.plugin.remote.v10.model.DefaultNodeModel;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Status.Failure;
import akka.actor.UntypedActor;
import akka.testkit.JavaTestKit;

import com.typesafe.config.ConfigFactory;

public class AkkaTests {

	private static ActorSystem system;
	private static ActorRef remoteActor;
	private static ActorRef localActor;
	private static ObjectMapper objectMapper;
	

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		system = ActorSystem.create("actoruser", ConfigFactory.load().getConfig("local"));
		remoteActor = system.actorFor("akka://freeplaneRemote@127.0.0.1:2553/user/main");
		//docear2
		//remoteActor = system.actorFor("akka://freeplaneRemote@141.45.146.224:2553/user/main");
		objectMapper = new ObjectMapper();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		system.shutdown();
	}

	@Before
	public  void setUp() throws Exception {
		localActor = system.actorOf(new Props(TheActor.class), "localActor_"+System.currentTimeMillis());
	}
	
	@After
	public void tearDown() throws Exception {
		remoteActor.tell(new CloseAllOpenMapsRequest(), localActor);
	}

	public void testSkeleton() {
		new JavaTestKit(system) {
			{
				//need to register to the localActor
				localActor.tell(getRef(), getRef());
			}
		};
	}
	
	/**
	 * testMindMapAsJson
	 * Open one of default test maps and receive json of map
	 */
	@Test
	public void testMindMapAsJson() {
		new JavaTestKit(system) {
			{
				localActor.tell(getRef(), getRef());
				new Within(duration("3 seconds")) {
					@Override
					protected void run() {
						remoteActor.tell(new MindmapAsJsonRequest("test_5"), getRef());

						MindmapAsJsonReponse response = expectMsgClass(MindmapAsJsonReponse.class);
						System.out.println(response.getJsonString());
						assertThat(response.getJsonString()).contains("\"root\":{\"id\":\"ID_0\",\"nodeText\":\"test_5 = MapID ; 5.mm = Title\"");
					}
				};
			}
		};
	}

	/**
	 * testMindMapAsJsonFail
	 * Try to open a not available map. Should throw MapNotFoundException.
	 */
	@Test
	public void testMindMapAsJsonFail() {
		new JavaTestKit(system) {
			{
				localActor.tell(getRef(), getRef());
				new Within(duration("3 seconds")) {
					protected void run() {
						remoteActor.tell(new MindmapAsJsonRequest("6"), getRef());
						Failure response = expectMsgClass(Failure.class);
						System.out.println(response.cause());
						assertThat(response.cause() instanceof MapNotFoundException).isTrue();
					}
				};
			}
		};
	}
	
	
	/**
	 * testMindMapAsXml
	 * Send MindMap to server. Request opened Mindmap as xml.
	 */
	@Test
	public void testMindMapAsXml() {
		new JavaTestKit(system) {
			{
				localActor.tell(getRef(), getRef());
				new Within(duration("3 seconds")) {
					protected void run() {
						sendMindMapToServer(5);
		
						remoteActor.tell(new MindmapAsXmlRequest("5"),localActor);
		
						MindmapAsXmlResponse response = expectMsgClass(MindmapAsXmlResponse.class);
						assertThat(response.getXmlString()).contains("<node TEXT=\"right_L1P0_Links\" COLOR=\"#000000\" STYLE=\"as_parent\" MAX_WIDTH=\"600\" MIN_WIDTH=\"1\" POSITION=\"right\" ID=\"ID_1\" CREATED=\"1354627639897\" MODIFIED=\"1355079961660\" HGAP=\"70\" VSHIFT=\"-160\">");
		
						closeMindMapOnServer(5);
					}
				};
			}
		};
	}

	
	/**
	 * testMindMapAsXmlFail
	 * Requesting not opened mindmap. should throw MapNotFoundException
	 */
	@Test
	public void testMindMapAsXmlFail() {
		new JavaTestKit(system) {
			{
				new Within(duration("3 seconds")) {
					protected void run() {
						localActor.tell(getRef(), getRef());
						remoteActor.tell(new MindmapAsXmlRequest("5"),localActor);
		
						Failure response = expectMsgClass(Failure.class);
						assertThat(response.cause() instanceof MapNotFoundException).isTrue();
			
					}
				};
			}
		};
	}
	
	
	/**
	 * testAddNodeRequest
	 * Open Map. Add new node to root node.
	 */
	@Test
	public void testAddNodeRequest() {
		new JavaTestKit(system) {
			{
				localActor.tell(getRef(), getRef());
				new Within(duration("3 seconds")) {
					protected void run() {
						try {
						sendMindMapToServer(5);
						remoteActor.tell(new AddNodeRequest("5", "ID_0"), localActor);

						AddNodeResponse response = expectMsgClass(AddNodeResponse.class);
						
						DefaultNodeModel node = objectMapper.readValue(response.getNode(), DefaultNodeModel.class);
						System.out.println(node.nodeText);
						Assert.assertEquals("",node.nodeText);
						
						} catch (JsonMappingException e) {
							Fail.fail("json mapping error", e);
						} catch (JsonParseException e) {
							Fail.fail("json parse error", e);
						} catch (IOException e) {
							Fail.fail("json IOException error", e);
						} finally {
							closeMindMapOnServer(5);
						}
					}
				};
			}
		};
	}

	/**
	 * testAddNodeRequestFailInvalidNode
	 * Open Map. Add new node to invalid node. Should throw NodeNotFoundException
	 */
	@Test
	public void testAddNodeRequestFailInvalidNode() {
		new JavaTestKit(system) {
			{
				localActor.tell(getRef(), getRef());
				new Within(duration("3 seconds")) {
					protected void run() {
						sendMindMapToServer(5);
						remoteActor.tell(new AddNodeRequest("5", "ID_FAIL"), localActor);

						Failure response = expectMsgClass(Failure.class);
						assertThat(response.cause() instanceof NodeNotFoundException).isTrue();
						closeMindMapOnServer(5);
					}
				};
			}
		};
	}
	
	/**
	 * testAddNodeRequestFailInvalidMap
	 * Open no Map. Try to add node. Should throw MapNotFoundException
	 */
	@Test
	public void testAddNodeRequestFailInvalidMap() {
		new JavaTestKit(system) {
			{
				localActor.tell(getRef(), getRef());
				new Within(duration("3 seconds")) {
					protected void run() {
						remoteActor.tell(new AddNodeRequest("16", "ID_FAIL"), localActor);

						Failure response = expectMsgClass(Failure.class);
						assertThat(response.cause() instanceof MapNotFoundException).isTrue();
					}
				};
			}
		};
	}

	/**
	 * testGetNodeRequest
	 * Get node from map 
	 */
	@Test
	public void testGetNodeRequest() {
		new JavaTestKit(system) {
			{
				localActor.tell(getRef(),getRef());
				new Within(duration("3 seconds")) {
					protected void run() {
						try {
						sendMindMapToServer(5);
						remoteActor.tell(new GetNodeRequest("5", "ID_1", 1), localActor);

						GetNodeResponse response = expectMsgClass(GetNodeResponse.class);
						DefaultNodeModel node = objectMapper.readValue(response.getNode(), DefaultNodeModel.class);
						System.out.println(node.nodeText);
						assertThat(node.nodeText).isEqualTo("right_L1P0_Links");
						assertThat(node.hGap).isEqualTo(70);

						
						} catch (JsonMappingException e) {
							Fail.fail("json mapping error", e);
						} catch (JsonParseException e) {
							Fail.fail("json parse error", e);
						} catch (IOException e) {
							Fail.fail("json IOException error", e);
						} finally {
							closeMindMapOnServer(5);
						}
					}
				};
			}
		};
	}
	
	/**
	 * testGetNodeRequest
	 * Get invalid node from map. Should throw NodeNotFoundException
	 */
	@Test
	public void testGetNodeRequestFailInvalidNode() {
		new JavaTestKit(system) {
			{
				localActor.tell(getRef(),getRef());
				new Within(duration("3 seconds")) {
					protected void run() {
						sendMindMapToServer(5);
						remoteActor.tell(new GetNodeRequest("5", "ID_FAIL", 1), localActor);

						Failure response = expectMsgClass(Failure.class);
						assertThat(response.cause() instanceof NodeNotFoundException).isTrue();
						closeMindMapOnServer(5);
					}
				};
			}
		};
	}

	/**
	 * testRemoveNodeRequest
	 * send map to server. remove valid node from Map. check if node with id isn't available any more.
	 */
	@Test
	public void testRemoveNodeRequest() {
		new JavaTestKit(system) {
			{
				localActor.tell(getRef(),getRef());
				new Within(duration("3 seconds")) {
					protected void run() {
						sendMindMapToServer(5);
						remoteActor.tell(new RemoveNodeRequest("5", "ID_1"), localActor);

						remoteActor.tell(new GetNodeRequest("5", "ID_1", 1), localActor);
						Failure response = expectMsgClass(Failure.class);
						assertThat(response.cause() instanceof NodeNotFoundException).isTrue();

						closeMindMapOnServer(5);
					}
				};
			}
		};
	}
	
	/**
	 * testRemoveNodeRequestFailInvalidNode
	 * send map to server. remove valid node from Map. check if 
	 */
	@Test
	public void testRemoveNodeRequestFailInvalidNode() {
		new JavaTestKit(system) {
			{
				localActor.tell(getRef(),getRef());
				new Within(duration("3 seconds")) {
					protected void run() {
						sendMindMapToServer(5);
						remoteActor.tell(new RemoveNodeRequest("5", "ID_FAIL"), localActor);
					
						Failure response = expectMsgClass(Failure.class);
						assertThat(response.cause() instanceof NodeNotFoundException).isTrue();

						closeMindMapOnServer(5);
					}
				};
			}
		};
	}

	/**
	 * testChangeNodeRequest
	 * change available node to defined attributes. check if node got attributes.
	 */
	@Test
	public void testChangeNodeRequest() {
		new JavaTestKit(system) {
			{
				localActor.tell(getRef(),getRef());
				new Within(duration("10 seconds")) {
					protected void run() {
						sendMindMapToServer(5);
						
						final String nodeId = "ID_1";
						final String newNodeText = "This is a new nodeText";
						final Boolean isHtml = false;
						final Boolean folded = true;
						final String[] icons = new String[] {"yes"};
						final String link = "http://www.google.de";
						final Integer hGap = 10;
						final Integer shiftY = 10;
						final Map<String,String> attr = new HashMap<String, String>();
						attr.put("key", "value");
						
						
						
						DefaultNodeModel node = new DefaultNodeModel();
						node.id = nodeId;
						node.nodeText = newNodeText;
						node.isHtml = isHtml;
						node.folded = folded;
						node.icons = icons;
						node.link = link;
						node.hGap = hGap;
						node.shiftY = shiftY;
						node.attributes = attr;
						
						ObjectMapper om = new ObjectMapper();
						String nodeAsJSON = null;
						try {
							nodeAsJSON = om.writeValueAsString(node);
						} catch (Exception e) {
							Fail.fail("error parsing DefaultNodeModel");
						}
						remoteActor.tell(new ChangeNodeRequest("5", nodeAsJSON), localActor);

						remoteActor.tell(new GetNodeRequest("5", "ID_1", 1), localActor);
						GetNodeResponse response = expectMsgClass(GetNodeResponse.class);
						
						try {
						final DefaultNodeModel receivedNode = objectMapper.readValue(response.getNode(), DefaultNodeModel.class);
						
						assertThat(receivedNode.nodeText).isEqualTo(newNodeText);
						assertThat(receivedNode.isHtml).isEqualTo(isHtml);
						assertThat(receivedNode.folded).isEqualTo(folded);
						assertThat(receivedNode.link).isEqualTo(link);
						assertThat(receivedNode.hGap).isEqualTo(hGap);
						assertThat(receivedNode.shiftY).isEqualTo(shiftY);

						} catch (JsonMappingException e) {
							Fail.fail("json mapping error", e);
						} catch (JsonParseException e) {
							Fail.fail("json parse error", e);
						} catch (IOException e) {
							Fail.fail("json IOException error", e);
						} finally {
							closeMindMapOnServer(5);
						}
					}
				};
			}
		};
	}

	
	/**
	 * testChangeNodeRequestFailInvalidNode
	 * change invalid node to defined attributes. Should throw NodeNotFoundException
	 */
	@Test
	public void testChangeNodeRequestFailInvalidNode() {
		new JavaTestKit(system) {
			{
				localActor.tell(getRef(),getRef());
				new Within(duration("10 seconds")) {
					protected void run() {
						sendMindMapToServer(5);
						
						DefaultNodeModel node = new DefaultNodeModel();
						node.id = "ID_FAIL";
						node.nodeText = "This is a new nodeText";
						
						String nodeAsJSON = null;
						try {
							nodeAsJSON = objectMapper.writeValueAsString(node);
						} catch (Exception e) {
							Fail.fail("error parsing DefaultNodeModel");
						}
						remoteActor.tell(new ChangeNodeRequest("5", nodeAsJSON), localActor);
						
						Failure response = expectMsgClass(Failure.class);
					
						assertThat(response.cause() instanceof NodeNotFoundException).isTrue();
					}
				};
			}
		};
	}


	/**
	 * sendMapGetAsJsonAndCloseOnServerTest
	 * send Map, get map as json, close map
	 */
	@Test
	public void sendMapGetAsJsonAndCloseOnServerTest() {
		new JavaTestKit(system) {
			{
				new Within(duration("4 seconds")) {
					@Override
					public void run() {
						sendMindMapToServer(5);

						remoteActor.tell(new MindmapAsJsonRequest("5"), getRef());

						MindmapAsJsonReponse response = expectMsgClass(MindmapAsJsonReponse.class);
						System.out.println(response.getJsonString());
						assertThat(response.getJsonString()).contains("\"root\":{\"id\":\"ID_0\",\"nodeText\":\"test_5 = MapID ; 5.mm = Title\"");

						closeMindMapOnServer(5);
					}
				};
			}
		};
	}
	
	/**
	 * sendMapGetAsJsonAndCloseOnServerTestFailDoubleClose
	 * send Map, get map as json, close map, close map again. should run.
	 */
	@Test
	public void sendMapGetAsJsonAndCloseOnServerTestDoubleClose() {
		new JavaTestKit(system) {
			{
				new Within(duration("3 seconds")) {
					@Override
					public void run() {
						sendMindMapToServer(5);

						remoteActor.tell(new MindmapAsJsonRequest("5"), getRef());

						MindmapAsJsonReponse response = expectMsgClass(MindmapAsJsonReponse.class);
						System.out.println(response.getJsonString());
						assertThat(response.getJsonString()).contains("\"root\":{\"id\":\"ID_0\",\"nodeText\":\"test_5 = MapID ; 5.mm = Title\"");
						
						closeMindMapOnServer(5);
						expectNoMsg();
					}
				};
			}
		};
	}
	
	@Test
	public void testCloseNotAccessedMaps() {
		new JavaTestKit(system) {
			{
				localActor.tell(getRef(),getRef());
				new Within(duration("3 seconds")) {
					@Override
					public void run() {
						sendMindMapToServer(5);

						//close maps that haven't been used for 1 ms
						remoteActor.tell(new CloseUnusedMaps(1), localActor);
						//expectNoMsg();

						remoteActor.tell(new MindmapAsJsonRequest("5"), localActor);
						Failure response = expectMsgClass(Failure.class);
						assertThat(response.cause()).isInstanceOf(MapNotFoundException.class);

						//no map closing needed, because it has been closed due to beeing unused
					}
				};
			}
		};
	}

	/**
	 * simulateMultipleUserAkka
	 * four user opening 4 different maps, each in one thread
	 */
	@Test
	@Ignore
	public void simulateMultipleUserAkka() {
		final Semaphore finishSemaphore = new Semaphore(-3);

		for (int i = 1; i <= 4; i++) {
			final int mapId = i == 4 ? 5 : i;
			final ActorRef local = system.actorOf(new Props(TheActor.class), "multiactor"+mapId);

			new Thread( new Runnable() {

				@Override
				public void run() {
					new JavaTestKit(system) {{
						local.tell(getRef(),getRef());

						new Within(duration("5 seconds")) {

							@Override
							public void run() {
								sendMindMapToServer(mapId);
								
								remoteActor.tell(new MindmapAsJsonRequest(mapId + "", 5),local);
								MindmapAsJsonReponse response = expectMsgClass(MindmapAsJsonReponse.class);

								if(mapId == 1) {
									assertThat(response.getJsonString()).contains("\"root\":{\"id\":\"ID_1723255651\",\"nodeText\":\"foo2\"");
								} else if(mapId == 2) {
									assertThat(response.getJsonString()).contains("\"isReadonly\":false,\"root\":{\"id\":\"ID_1723255651\",\"nodeText\":\"New Mindmap\"");
								} else if(mapId == 3) {
									assertThat(response.getJsonString()).contains("\"isReadonly\":false,\"root\":{\"id\":\"ID_1723255651\",\"nodeText\":\"Welcome\"");
								} else if(mapId == 5) {
									assertThat(response.getJsonString()).contains("\"isReadonly\":false,\"root\":{\"id\":\"ID_0\",\"nodeText\":\"test_5 = MapID ; 5.mm = Title\"");
								}

								closeMindMapOnServer(mapId);

								finishSemaphore.release();
							}
						};

					}};
				}
			}).start();
		}
		
		finishSemaphore.acquireUninterruptibly();
	}

	public void sendMindMapToServer(final int id) {
		final URL pathURL = AkkaTests.class.getResource("/files/mindmaps/" + id + ".mm");

		try {
			final File f = new File(pathURL.toURI());
			String mapContent = FileUtils.readFileToString(f);
			final OpenMindMapRequest request = new OpenMindMapRequest(mapContent);

			assertThat(f).isNotNull();

			new JavaTestKit(system) {
				{
					new Within(duration("5 seconds")) {
						public void run() {
							remoteActor.tell(request, localActor);
						}
					};
				}
			};

		} catch (URISyntaxException e) {
		} catch (IOException e) {
		}


	}

	public void closeMindMapOnServer(final int id) {
		new JavaTestKit(system) {
			{
				new Within(duration("2 seconds")) {
					public void run() {
						remoteActor.tell(new CloseMapRequest(id+""), localActor);
					}
				};
			}
		};


	}

	public static class TheActor extends UntypedActor {
		ActorRef target;
		@Override
		public void onReceive(Object message) throws Exception {
			System.out.println(message.getClass().getName() + " received");

			if (message instanceof Failure) {
				System.err.println("warning: Error occured.");
				//org.fest.assertions.Fail.fail("An error occured", ((Failure) message).cause());
			}

			if (message instanceof ActorRef) {
				target = (ActorRef)message;
			} else {
				if(target != null)
					target.tell(message, getSelf());
			}

		}

	}
}
