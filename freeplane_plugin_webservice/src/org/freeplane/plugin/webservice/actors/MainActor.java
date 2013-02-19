package org.freeplane.plugin.webservice.actors;

import org.freeplane.plugin.webservice.Messages.AddNodeRequest;
import org.freeplane.plugin.webservice.Messages.ChangeNodeRequest;
import org.freeplane.plugin.webservice.Messages.CloseAllOpenMapsRequest;
import org.freeplane.plugin.webservice.Messages.CloseMapRequest;
import org.freeplane.plugin.webservice.Messages.CloseServerRequest;
import org.freeplane.plugin.webservice.Messages.ErrorMessage;
import org.freeplane.plugin.webservice.Messages.GetNodeRequest;
import org.freeplane.plugin.webservice.Messages.MindmapAsJsonRequest;
import org.freeplane.plugin.webservice.Messages.MindmapAsXmlRequest;
import org.freeplane.plugin.webservice.Messages.OpenMindMapRequest;
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
		System.out.println(message.getClass().getName()+" received.");
		ActorRef sender = getSender();
		Object response = null;
		try {
			//get map as json
			if(message instanceof MindmapAsJsonRequest) {
				response = Webservice.getMapModelJson((MindmapAsJsonRequest) message);
			}
			
			//get map as xml
			if(message instanceof MindmapAsXmlRequest) {
				response = Webservice.getMapModelXml((MindmapAsXmlRequest)message);
			}

			//add node to map
			if(message instanceof AddNodeRequest) {
				response = Webservice.addNode((AddNodeRequest) message);			
			}
			
			//change node
			if(message instanceof ChangeNodeRequest) {
				Webservice.changeNode((ChangeNodeRequest)message);
			}
			
			//remove node from map
			if(message instanceof RemoveNodeRequest) {
				Webservice.removeNode((RemoveNodeRequest) message);			
			}
			
			//get node from map
			if(message instanceof GetNodeRequest) {
				response = Webservice.getNode((GetNodeRequest) message);			
			}
			
			if (message instanceof OpenMindMapRequest){
				Webservice.openMindmap((OpenMindMapRequest)message);
			}
			
			//close map
			if(message instanceof CloseMapRequest) {
				Webservice.closeMap((CloseMapRequest)message);
			}
			
			//close all maps
			if(message instanceof CloseAllOpenMapsRequest) {
				Webservice.closeAllOpenMaps((CloseAllOpenMapsRequest)message);
			}
			
			//close server
			if(message instanceof CloseServerRequest) {
				Webservice.closeServer();
			}

			if(response != null)
				sender.tell(response, getSelf());
			
			
		}
		catch(Exception e) {
			System.out.println(e.getMessage());
			sender.tell(new ErrorMessage(e),getSelf());

		}
	}

}
