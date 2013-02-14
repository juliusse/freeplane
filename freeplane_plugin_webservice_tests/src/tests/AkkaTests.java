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
import org.freeplane.plugin.webservice.v10.Webservice;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;

import com.typesafe.config.ConfigFactory;

public class AkkaTests {

	private static ActorSystem system;
	private static ActorRef remoteActor;
	private static ActorRef localActor;
	
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
		remoteActor.tell(new MindmapAsJsonRequest("test_1"), localActor);
	}


	
	@Test
	public void simulateMultipleUser() {
		final Semaphore finishSemaphore = new Semaphore(-3);
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
				assertThat(((MindmapAsJsonReponse)message).getJsonString()).isNotNull();
			}
			
			if(message instanceof ErrorMessage) {
				org.fest.assertions.Fail.fail("An error occured", ((ErrorMessage)message).getException());
			}
			
		}
	}

}
