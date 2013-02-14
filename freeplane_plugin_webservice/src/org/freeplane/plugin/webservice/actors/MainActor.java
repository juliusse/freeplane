package org.freeplane.plugin.webservice.actors;

import org.freeplane.plugin.webservice.Messages.AddNodeRequest;
import org.freeplane.plugin.webservice.Messages.MindmapAsJsonReponse;
import org.freeplane.plugin.webservice.Messages.MindmapAsJsonRequest;
import org.freeplane.plugin.webservice.v10.Webservice;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

public class MainActor extends UntypedActor {
	
	public MainActor() {
		
	}
	
	public MainActor(ActorRef listener) {
	}
	
	@Override
	public void onReceive(Object message) throws Exception {
		//get map as json
		if(message instanceof MindmapAsJsonRequest) {
			MindmapAsJsonRequest request = (MindmapAsJsonRequest) message;
			String result = Webservice.getMapModel(request);
			
			getSender().tell(new MindmapAsJsonReponse(result), getSelf());
		}
		
		//add node to map
		if(message instanceof AddNodeRequest) {
			AddNodeRequest request = (AddNodeRequest) message;
			String result = Webservice.addNode(request);
			
			getSender().tell(new MindmapAsJsonReponse(result), getSelf());
		}
	}

}
