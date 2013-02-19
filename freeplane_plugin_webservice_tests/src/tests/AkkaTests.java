package tests;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.Semaphore;

import org.freeplane.plugin.webservice.Messages.AddNodeRequest;
import org.freeplane.plugin.webservice.Messages.AddNodeResponse;
import org.freeplane.plugin.webservice.Messages.CloseMapRequest;
import org.freeplane.plugin.webservice.Messages.ErrorMessage;
import org.freeplane.plugin.webservice.Messages.MindmapAsJsonReponse;
import org.freeplane.plugin.webservice.Messages.MindmapAsJsonRequest;
import org.freeplane.plugin.webservice.Messages.GetNodeRequest;
import org.freeplane.plugin.webservice.Messages.GetNodeResponse;
import org.freeplane.plugin.webservice.Messages.OpenMindMapRequest;
import org.freeplane.plugin.webservice.Messages.ChangeNodeRequest;
import org.freeplane.plugin.webservice.v10.Webservice;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
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
	private static Semaphore finishSemaphore;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		system = ActorSystem.create("actoruser", ConfigFactory.load().getConfig("local"));
		remoteActor = system.actorFor("akka://freeplaneRemote@127.0.0.1:2553/user/main");
		localActor = system.actorOf(new Props(TheActor.class), "localActor");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		system.shutdown();
	}

	@Test
	public void testMindMapAsJson() {
		new JavaTestKit(system) {
			{
				new Within(duration("3 seconds")) {
					protected void run() {
						remoteActor.tell(new MindmapAsJsonRequest("5"), localActor);

						MindmapAsJsonReponse response = expectMsgClass(MindmapAsJsonReponse.class);
						System.out.println(response.getJsonString());
						Assert.assertTrue(response.getJsonString().contains("\"root\":{\"id\":\"ID_1723255651\",\"nodeText\":\"foo2\""));
					}
				};
			}
		};
	}

	@Test
	public void testAddNodeRequest() {
		new JavaTestKit(system) {
			{
				new Within(duration("3 seconds")) {
					protected void run() {
						sendMindMapToServer(5);
						remoteActor.tell(new AddNodeRequest("5.mm", "ID_1"), localActor);

						AddNodeResponse response = expectMsgClass(AddNodeResponse.class);
						System.out.println(response.getNode().nodeText);
						Assert.assertEquals("",response.getNode().nodeText);
					}
				};
			}
		};
	}
	
	@Test
	public void testGetNodeRequest() {
		new JavaTestKit(system) {
			{
				new Within(duration("3 seconds")) {
					protected void run() {
						sendMindMapToServer(5);
						remoteActor.tell(new GetNodeRequest("5.mm", "ID_1", 1), getRef());

						GetNodeResponse response = expectMsgClass(GetNodeResponse.class);
						System.out.println(response.getNode().nodeText);
						Assert.assertEquals("right_L1P0_Links",response.getNode().nodeText);
						Assert.assertEquals("70",response.getNode().hGap);
					}
				};
			}
		};
	}

	@Test
	public void testChangeNodeRequest() {
		new JavaTestKit(system) {
			{
				new Within(duration("3 seconds")) {
					protected void run() {
						sendMindMapToServer(5);
						String newNodeText = "This is a new nodeText";
						String nodeAsJSON = "{\"id\":\"ID_1\",\"nodeText\":\"" + newNodeText + "\"}";
						remoteActor.tell(new ChangeNodeRequest("5.mm", nodeAsJSON), getRef());

						expectNoMsg();
						
						remoteActor.tell(new GetNodeRequest("5", "ID_1", 1), getRef());
						GetNodeResponse response = expectMsgClass(GetNodeResponse.class);
						Assert.assertEquals(newNodeText, response.getNode().nodeText);
						
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
					public void run() {
						sendMindMapToServer(1);

						remoteActor.tell(new MindmapAsJsonRequest("1"), getRef());

						MindmapAsJsonReponse response = expectMsgClass(MindmapAsJsonReponse.class);
						System.out.println(response.getJsonString());
						Assert.assertTrue(response.getJsonString().contains("\"root\":{\"id\":\"ID_1723255651\",\"nodeText\":\"foo2\""));


						closeMindMapOnServer(1);
					}
				};
			}
		};

	}

	@Test
	@Ignore
	public void simulateMultipleUser() {
		//new JavaTestKit(system) {{

		finishSemaphore = new Semaphore(-3);

		for (int i = 1; i <= 4; i++) {
			final int mapId = i == 4 ? 5 : i;
			new Thread(new Runnable() {


				@Override
				public void run() {
					//						new Within(duration("5 seconds")) {
					//							public void run() {
					sendMindMapToServer(mapId);

					localActor.tell(new MindmapAsJsonRequest(mapId + "", 5),
							localActor);

					closeMindMapOnServer(mapId);

					finishSemaphore.release();
					//							}
					//						}

				}
			}).start();
		}

		finishSemaphore.acquireUninterruptibly();
		//}};
	}

	@Test
	public void simulateMultipleUserAkka() {
		//final ActorRef remoteActor = system.actorFor("akka://freeplaneRemote@127.0.0.1:2553/user/main");
		new JavaTestKit(system) {{
			//final JavaTestKit probe = new JavaTestKit(system);
			localActor.tell(getRef(),getRef());
			
			new Within(duration("2 seconds")) {
				
				public void run() {
					sendMindMapToServer(1);
					sendMindMapToServer(2);
					sendMindMapToServer(3);
					sendMindMapToServer(5);
					
					
					MindmapAsJsonReponse response;
					
					remoteActor.tell(new MindmapAsJsonRequest(1 + "", 5),localActor);
					//probe.
					response = expectMsgClass(MindmapAsJsonReponse.class);
					System.out.println(response.getJsonString());
					Assert.assertTrue(response.getJsonString().contains("\"root\":{\"id\":\"ID_1723255651\",\"nodeText\":\"foo2\""));
					
					remoteActor.tell(new MindmapAsJsonRequest(2 + "", 5),localActor);
					response = expectMsgClass(duration("2 seconds"),MindmapAsJsonReponse.class);
					System.out.println(response.getJsonString());
					Assert.assertTrue(response.getJsonString().contains("\"id\":\"2.mm\",\"isReadonly\":false,\"root\":{\"id\":\"ID_1723255651\",\"nodeText\":\"New Mindmap\""));
					
					remoteActor.tell(new MindmapAsJsonRequest(3 + "", 5),localActor);
					response = expectMsgClass(duration("2 seconds"),MindmapAsJsonReponse.class);
					System.out.println(response.getJsonString());
					Assert.assertTrue(response.getJsonString().contains("\"id\":\"3.mm\",\"isReadonly\":false,\"root\":{\"id\":\"ID_1723255651\",\"nodeText\":\"Welcome\""));
					
					remoteActor.tell(new MindmapAsJsonRequest(5 + "", 5),localActor);
					response = expectMsgClass(duration("2 seconds"),MindmapAsJsonReponse.class);
					System.out.println(response.getJsonString());
					Assert.assertTrue(response.getJsonString().contains("\"id\":\"5.mm\",\"isReadonly\":false,\"root\":{\"id\":\"ID_1723255651\",\"nodeText\":\"test_5 = MapID ; 5.mm = Title\""));


					

					closeMindMapOnServer(1);
					closeMindMapOnServer(2);
					closeMindMapOnServer(3);
					closeMindMapOnServer(5);

				}
			};

		}};
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

			if (message instanceof ErrorMessage) {
				System.out.println(message);
				org.fest.assertions.Fail.fail("An error occured", ((ErrorMessage) message).getException());
			}
			
			else if (message instanceof ActorRef) {
				target = (ActorRef)message;
			} else {
				target.tell(message, getSelf());
			}

		}

	}
}
