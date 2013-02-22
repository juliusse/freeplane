package org.freeplane.plugin.webservice.actors;

import org.docear.messages.Messages.AddNodeRequest;
import org.docear.messages.Messages.ChangeNodeRequest;
import org.docear.messages.Messages.CloseAllOpenMapsRequest;
import org.docear.messages.Messages.CloseMapRequest;
import org.docear.messages.Messages.CloseServerRequest;
import org.docear.messages.Messages.GetNodeRequest;
import org.docear.messages.Messages.MindmapAsJsonRequest;
import org.docear.messages.Messages.MindmapAsXmlRequest;
import org.docear.messages.Messages.OpenMindMapRequest;
import org.docear.messages.Messages.RemoveNodeRequest;
import org.freeplane.plugin.webservice.v10.Webservice;

import akka.actor.ActorRef;
import akka.actor.Status;
import akka.actor.UntypedActor;

public class MainActor extends UntypedActor {

	public MainActor() {

	}

	public MainActor(ActorRef listener) {
	}

	@Override
	public void onReceive(Object message) throws Exception {
		System.out.println(message.getClass().getName()+" received.");
		ActorRef sender = getSender();
		Object response = null;
		try {
			//get map as json
			if(message instanceof MindmapAsJsonRequest) {
				response = Webservice.getMapModelJson((MindmapAsJsonRequest) message);
			}
			
			//get map as xml
			else if(message instanceof MindmapAsXmlRequest) {
				response = Webservice.getMapModelXml((MindmapAsXmlRequest)message);
			}

			//add node to map
			else if(message instanceof AddNodeRequest) {
				response = Webservice.addNode((AddNodeRequest) message);			
			}
			
			//change node
			else if(message instanceof ChangeNodeRequest) {
				Webservice.changeNode((ChangeNodeRequest)message);
			}
			
			//remove node from map
			else if(message instanceof RemoveNodeRequest) {
				Webservice.removeNode((RemoveNodeRequest) message);			
			}
			
			//get node from map
			else if(message instanceof GetNodeRequest) {
				response = Webservice.getNode((GetNodeRequest) message);			
			}
			
			else if (message instanceof OpenMindMapRequest){
				Webservice.openMindmap((OpenMindMapRequest)message);
			}
			
			//close map
			else if(message instanceof CloseMapRequest) {
				Webservice.closeMap((CloseMapRequest)message);
			}
			
			//close all maps
			else if(message instanceof CloseAllOpenMapsRequest) {
				Webservice.closeAllOpenMaps((CloseAllOpenMapsRequest)message);
			}
			
			//close server
			else if(message instanceof CloseServerRequest) {
				Webservice.closeServer();
			}

			if(response != null)
				sender.tell(response, getSelf());
			
			
		}
		catch(Exception e) {
			System.out.println(e.getMessage());
			//sender.tell(new ErrorMessage(e),getSelf());
			sender.tell(new Status.Failure(e),getSelf());

		}
	}

}
