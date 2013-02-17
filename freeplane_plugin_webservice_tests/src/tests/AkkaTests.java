package tests;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.Semaphore;

import org.freeplane.plugin.webservice.Messages.CloseMapRequest;
import org.freeplane.plugin.webservice.Messages.ErrorMessage;
import org.freeplane.plugin.webservice.Messages.MindmapAsJsonReponse;
import org.freeplane.plugin.webservice.Messages.MindmapAsJsonRequest;
import org.freeplane.plugin.webservice.Messages.OpenMindMapRequest;
import org.freeplane.plugin.webservice.actors.MainActor;
import org.freeplane.plugin.webservice.v10.Webservice;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;

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
		localActor = system.actorOf(new Props(TheActor.class),"localActor");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		system.shutdown();
	}

	@Test
	public void test() {
		//		final Props props = new Props(MainActor.class);
		//		final TestActorRef<MainActor> ref = TestActorRef.create(system, props, "testA");
		//		final MainActor actor = ref.underlyingActor();

		new JavaTestKit(system) {{
			//final Props props = new Props(MainActor.class);
			final ActorRef remoteActor = system.actorFor("akka://freeplaneRemote@127.0.0.1:2553/user/main");
			
			//final JavaTestKit probe = new JavaTestKit(system);
			
			remoteActor.tell(new MindmapAsJsonRequest("test_1"), getRef());
			
			expectMsgAllOf(Object.class);
			//expectMsgEquals(duration("5 seconds"), arg1)
		}};
	}



	@Test
	public void simulateMultipleUser() {
		finishSemaphore = new Semaphore(-3);
		for(int i = 1; i <= 4; i++) {
			final int mapId = i == 4 ? 5 : i;
			new Thread(new Runnable() {

				@Override
				public void run() {
					sendMindMapToServer(mapId);

					localActor.tell(new MindmapAsJsonRequest(mapId+"",5),localActor);

					closeMindMapOnServer(mapId);

					finishSemaphore.release();
				}
			}).start();
		}

		finishSemaphore.acquireUninterruptibly();
	}



	public void sendMindMapToServer(int id) {
		//InputStream in = Webservice.class.getResourceAsStream("/files/mindmaps/"+id+".mm");
		URL pathURL = Webservice.class.getResource("/files/mindmaps/"+id+".mm");
		File f = null;
		try {
			f = new File(pathURL.toURI());
		} catch (URISyntaxException e) {}

		OpenMindMapRequest request = new OpenMindMapRequest(f);

		String contentDeposition = "attachement; filename=\""+id+".mm\"";
		assertThat(f).isNotNull();


		remoteActor.tell(request,localActor);
	}



	public void closeMindMapOnServer(int id) {
		remoteActor.tell(new CloseMapRequest(id+""), localActor);

	}

	public static class TheActor extends UntypedActor {
		@Override
		public void onReceive(Object message) throws Exception {
			if(message instanceof MindmapAsJsonReponse) {
				getSender().tell(new Object(),getSelf());
				//org.fest.assertions.Fail.fail();
				//assertThat(((MindmapAsJsonReponse)message).getJsonString()).isNotNull();
				//finishSemaphore.release();
			}

			if(message instanceof ErrorMessage) {
				org.fest.assertions.Fail.fail("An error occured", ((ErrorMessage)message).getException());
			}

		}

	}
}
