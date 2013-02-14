package org.freeplane.plugin.webservice.actors;

import org.freeplane.plugin.webservice.Messages.AddNodeRequest;
import org.freeplane.plugin.webservice.Messages.ErrorMessage;
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
		ActorRef sender = getSender();
		Object response = null;
		try {
		//get map as json
		if(message instanceof MindmapAsJsonRequest) {
			MindmapAsJsonRequest request = (MindmapAsJsonRequest) message;
			response = Webservice.getMapModel(request);
		}
		
		//add node to map
		if(message instanceof AddNodeRequest) {
			AddNodeRequest request = (AddNodeRequest) message;
			response = Webservice.addNode(request);			
		}
		
		sender.tell(response, getSelf());
		}
		catch(Exception e) {
			sender.tell(new ErrorMessage(e),getSelf());
			
		}
	}

}
