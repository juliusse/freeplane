package org.freeplane.plugin.webservice.actors;

import org.freeplane.plugin.webservice.Messages.MindmapAsJsonReponse;
import org.freeplane.plugin.webservice.Messages.MindmapAsJsonRequest;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

public class MainActor extends UntypedActor {
	
	public MainActor() {
		
	}
	
	public MainActor(ActorRef listener) {
	}
	
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof MindmapAsJsonRequest) {
			MindmapAsJsonRequest request = (MindmapAsJsonRequest) message;
			
			getSender().tell(new MindmapAsJsonReponse("blub"), getSelf());
		}
	}

}
