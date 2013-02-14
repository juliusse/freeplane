package tests;

import java.util.concurrent.Semaphore;

import javax.print.attribute.standard.Finishings;

import org.freeplane.plugin.webservice.Messages.MindmapAsJsonReponse;
import org.freeplane.plugin.webservice.Messages.MindmapAsJsonRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;

import com.typesafe.config.ConfigFactory;

public class AkkaTests {

	private ActorSystem system;
	private ActorRef remoteActor;
	private ActorRef localActor;
	
	@Before
	public void setUp() throws Exception {
		system = ActorSystem.create("actoruser", ConfigFactory.load().getConfig("local"));
        remoteActor = system.actorFor("akka://freeplaneRemote@127.0.0.1:2553/user/main");
        localActor = system.actorOf(new Props(TheActor.class),"localActor");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		
		remoteActor.tell(new MindmapAsJsonRequest("test_1"), localActor);
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	
	public static class TheActor extends UntypedActor {
		@Override
		public void onReceive(Object message) throws Exception {
			if(message instanceof MindmapAsJsonReponse) {
				System.out.println(((MindmapAsJsonReponse)message).getJsonString());
			}
			
		}
	}

}
