package org.freeplane.plugin.webservice.actors;

import org.freeplane.plugin.webservice.Messages.AddNodeRequest;
import org.freeplane.plugin.webservice.Messages.ChangeNodeRequest;
import org.freeplane.plugin.webservice.Messages.CloseMapRequest;
import org.freeplane.plugin.webservice.Messages.CloseServerRequest;
import org.freeplane.plugin.webservice.Messages.ErrorMessage;
import org.freeplane.plugin.webservice.Messages.MindmapAsJsonRequest;
import org.freeplane.plugin.webservice.Messages.MindmapAsXmlRequest;
import org.freeplane.plugin.webservice.Messages.RemoveNodeRequest;
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
				response = Webservice.getMapModelJson(request);
			}
			
			//get map as xml
			if(message instanceof MindmapAsXmlRequest) {
				response = Webservice.getMapModelXml((MindmapAsXmlRequest)message);
			}

			//add node to map
			if(message instanceof AddNodeRequest) {
				AddNodeRequest request = (AddNodeRequest) message;
				response = Webservice.addNode(request);			
			}
			
			//change node
			if(message instanceof ChangeNodeRequest) {
				Webservice.changeNode((ChangeNodeRequest)message);
			}
			
			//add node to map
			if(message instanceof RemoveNodeRequest) {
				RemoveNodeRequest request = (RemoveNodeRequest) message;
				Webservice.removeNode(request);			
			}
			
			//close map
			if(message instanceof CloseMapRequest) {
				Webservice.closeMap((CloseMapRequest)message);
			}
			
			//close server
			if(message instanceof CloseServerRequest) {
				Webservice.closeServer();
			}

			if(response != null)
				sender.tell(response, getSelf());
			
			
		}
		catch(Exception e) {
			sender.tell(new ErrorMessage(e),getSelf());

		}
	}

}
