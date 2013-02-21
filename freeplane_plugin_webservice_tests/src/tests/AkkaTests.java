package tests;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.codehaus.jackson.map.ObjectMapper;
import org.fest.assertions.Fail;
import org.freeplane.plugin.webservice.Messages.AddNodeRequest;
import org.freeplane.plugin.webservice.Messages.AddNodeResponse;
import org.freeplane.plugin.webservice.Messages.ChangeNodeRequest;
import org.freeplane.plugin.webservice.Messages.CloseAllOpenMapsRequest;
import org.freeplane.plugin.webservice.Messages.CloseMapRequest;
import org.freeplane.plugin.webservice.Messages.ErrorMessage;
import org.freeplane.plugin.webservice.Messages.GetNodeRequest;
import org.freeplane.plugin.webservice.Messages.GetNodeResponse;
import org.freeplane.plugin.webservice.Messages.MindmapAsJsonReponse;
import org.freeplane.plugin.webservice.Messages.MindmapAsJsonRequest;
import org.freeplane.plugin.webservice.Messages.MindmapAsXmlRequest;
import org.freeplane.plugin.webservice.Messages.MindmapAsXmlResponse;
import org.freeplane.plugin.webservice.Messages.OpenMindMapRequest;
import org.freeplane.plugin.webservice.Messages.RemoveNodeRequest;
import org.freeplane.plugin.webservice.v10.Webservice;
import org.freeplane.plugin.webservice.v10.exceptions.MapNotFoundException;
import org.freeplane.plugin.webservice.v10.exceptions.NodeNotFoundException;
import org.freeplane.plugin.webservice.v10.model.DefaultNodeModel;
import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.testkit.JavaTestKit;

import com.typesafe.config.ConfigFactory;

public class AkkaTests {

	private static ActorSystem system;
	private static ActorRef remoteActor;
	private static ActorRef localActor;
	

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		system = ActorSystem.create("actoruser", ConfigFactory.load().getConfig("local"));
		remoteActor = system.actorFor("akka://freeplaneRemote@127.0.0.1:2553/user/main");
		
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
					@Override
					protected void run() {
						remoteActor.tell(new MindmapAsJsonRequest("test_6"), getRef());
						ErrorMessage response = expectMsgClass(ErrorMessage.class);
						assertThat(response.getException() instanceof MapNotFoundException).isTrue();
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
				sendMindMapToServer(5);

				remoteActor.tell(new MindmapAsXmlRequest("5"),localActor);

				MindmapAsXmlResponse response = expectMsgClass(MindmapAsXmlResponse.class);
				assertThat(response.getXmlString()).contains("<node TEXT=\"right_L1P0_Links\" COLOR=\"#000000\" STYLE=\"as_parent\" MAX_WIDTH=\"600\" MIN_WIDTH=\"1\" POSITION=\"right\" ID=\"ID_1\" CREATED=\"1354627639897\" MODIFIED=\"1355079961660\" HGAP=\"70\" VSHIFT=\"-160\">");

				closeMindMapOnServer(5);
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
				localActor.tell(getRef(), getRef());
				remoteActor.tell(new MindmapAsXmlRequest("5"),localActor);

				ErrorMessage response = expectMsgClass(ErrorMessage.class);
				assertThat(response.getException() instanceof MapNotFoundException).isTrue();
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
					@Override
					protected void run() {
						sendMindMapToServer(5);
						remoteActor.tell(new AddNodeRequest("5", "ID_0"), localActor);

						AddNodeResponse response = expectMsgClass(AddNodeResponse.class);
						System.out.println(response.getNode().nodeText);
						assertThat(response.getNode().nodeText).equals("");
						closeMindMapOnServer(5);
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
					@Override
					protected void run() {
						sendMindMapToServer(5);
						remoteActor.tell(new AddNodeRequest("5", "ID_FAIL"), localActor);

						ErrorMessage response = expectMsgClass(ErrorMessage.class);
						assertThat(response.getException() instanceof NodeNotFoundException).isTrue();
						closeMindMapOnServer(5);
					}
				};
			}
		};
	}
	
	@Test
	public void testGetNodeRequest() {
		new JavaTestKit(system) {
			{
				localActor.tell(getRef(),getRef());
				new Within(duration("3 seconds")) {
					@Override
					protected void run() {
						sendMindMapToServer(5);
						remoteActor.tell(new GetNodeRequest("5", "ID_1", 1), localActor);

						GetNodeResponse response = expectMsgClass(GetNodeResponse.class);
						System.out.println(response.getNode().nodeText);
						assertThat(response.getNode().nodeText).isEqualTo("right_L1P0_Links");
						assertThat(response.getNode().hGap).isEqualTo(70);

						closeMindMapOnServer(5);
					}
				};
			}
		};
	}

	@Test
	public void testRemoveNodeRequest() {
		new JavaTestKit(system) {
			{
				localActor.tell(getRef(),getRef());
				new Within(duration("3 seconds")) {
					@Override
					protected void run() {
						sendMindMapToServer(5);
						remoteActor.tell(new RemoveNodeRequest("5", "ID_5"), localActor);

						//expectNoMsg();

						remoteActor.tell(new GetNodeRequest("5", "ID_5", 1), localActor);
						ErrorMessage response = expectMsgClass(ErrorMessage.class);
						assertThat(response.getException().getMessage()).contains("Node with id 'ID_5' not found");

						closeMindMapOnServer(5);
					}
				};
			}
		};
	}

	@Test
	public void testChangeNodeRequest() {
		new JavaTestKit(system) {
			{
				localActor.tell(getRef(),getRef());
				new Within(duration("10 seconds")) {
					@Override
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
						//String nodeAsJSON = "{\"id\":\"ID_1\",\"nodeText\":\"" + newNodeText + "\"}";
						String nodeAsJSON = null;
						try {
							nodeAsJSON = om.writeValueAsString(node);
						} catch (Exception e) {
							Fail.fail("error parsing DefaultNodeModel");
						}
						remoteActor.tell(new ChangeNodeRequest("5", nodeAsJSON), localActor);

						//expectNoMsg();

						remoteActor.tell(new GetNodeRequest("5", "ID_1", 1), localActor);
						GetNodeResponse response = expectMsgClass(GetNodeResponse.class);
						final DefaultNodeModel receivedNode = response.getNode();
						
						assertThat(receivedNode.nodeText).isEqualTo(newNodeText);
						assertThat(receivedNode.isHtml).isEqualTo(isHtml);
						assertThat(receivedNode.folded).isEqualTo(folded);
						//assertThat(receivedNode.icons).isEqualTo(icons);
						assertThat(receivedNode.link).isEqualTo(link);
						assertThat(receivedNode.hGap).isEqualTo(hGap);
						assertThat(receivedNode.shiftY).isEqualTo(shiftY);
						//assertThat(receivedNode.attributes.get("key")).isEqualTo("value");

						closeMindMapOnServer(5);
					}
				};
			}
		};
	}


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
						Assert.assertTrue(response.getJsonString().contains("\"root\":{\"id\":\"ID_0\",\"nodeText\":\"test_5 = MapID ; 5.mm = Title\""));


						closeMindMapOnServer(5);
					}
				};
			}
		};

	}

	@Test
	public void simulateMultipleUserAkka() {
		//final ActorRef remoteActor = system.actorFor("akka://freeplaneRemote@127.0.0.1:2553/user/main");

		final Semaphore finishSemaphore = new Semaphore(-3);

		for (int i = 1; i <= 4; i++) {
			final int mapId = i == 4 ? 5 : i;
			final ActorRef local = system.actorOf(new Props(TheActor.class), "multiactor"+mapId);

			new Thread( new Runnable() {

				@Override
				public void run() {
					new JavaTestKit(system) {{
						//final JavaTestKit probe = new JavaTestKit(system);
						local.tell(getRef(),getRef());

						new Within(duration("5 seconds")) {

							@Override
							public void run() {
								sendMindMapToServer(mapId);



								remoteActor.tell(new MindmapAsJsonRequest(mapId + "", 5),local);
								MindmapAsJsonReponse response = expectMsgClass(MindmapAsJsonReponse.class);
								//System.out.println(response.getJsonString());

								if(mapId == 1) {
									assertThat(response.getJsonString()).contains("\"root\":{\"id\":\"ID_1723255651\",\"nodeText\":\"foo2\"");
								} else if(mapId == 2) {
									assertThat(response.getJsonString()).contains("\"id\":\"2.mm\",\"isReadonly\":false,\"root\":{\"id\":\"ID_1723255651\",\"nodeText\":\"New Mindmap\"");
								} else if(mapId == 3) {
									assertThat(response.getJsonString()).contains("\"id\":\"3.mm\",\"isReadonly\":false,\"root\":{\"id\":\"ID_1723255651\",\"nodeText\":\"Welcome\"");
								} else if(mapId == 4) {
									assertThat(response.getJsonString()).contains("\"id\":\"5.mm\",\"isReadonly\":false,\"root\":{\"id\":\"ID_0\",\"nodeText\":\"test_5 = MapID ; 5.mm = Title\"");
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
		final URL pathURL = Webservice.class.getResource("/files/mindmaps/" + id + ".mm");

		try {
			final File f = new File(pathURL.toURI());
			final OpenMindMapRequest request = new OpenMindMapRequest(f);

			assertThat(f).isNotNull();

			new JavaTestKit(system) {
				{
					new Within(duration("5 seconds")) {
						@Override
						public void run() {
							remoteActor.tell(request, localActor);
							//expectNoMsg();
						}
					};
				}
			};

		} catch (URISyntaxException e) {
		}


	}

	public void closeMindMapOnServer(final int id) {
		new JavaTestKit(system) {
			{
				new Within(duration("2 seconds")) {
					@Override
					public void run() {
						remoteActor.tell(new CloseMapRequest(id+""), localActor);
						//expectNoMsg();
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

			if (message instanceof ErrorMessage) {
				System.err.println("warning: Error occured.");
				//org.fest.assertions.Fail.fail("An error occured", ((ErrorMessage) message).getException());
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
