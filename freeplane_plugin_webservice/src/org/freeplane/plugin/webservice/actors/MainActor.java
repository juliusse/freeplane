package org.freeplane.plugin.webservice.actors;

import org.freeplane.plugin.webservice.Messages.AddNodeRequest;
import org.freeplane.plugin.webservice.Messages.ChangeNodeRequest;
import org.freeplane.plugin.webservice.Messages.ErrorMessage;
import org.freeplane.plugin.webservice.Messages.GetNodeRequest;
import org.freeplane.plugin.webservice.Messages.GetNodeResponse;
import org.freeplane.plugin.webservice.Messages.MindmapAsJsonRequest;
import org.freeplane.plugin.webservice.Messages.OpenMindMapRequest;
import org.freeplane.plugin.webservice.Messages.RemoveNodeRequest;
import org.freeplane.plugin.webservice.v10.Webservice;

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

			if(response != null)
				sender.tell(response, getSelf());
			
			
		}
		catch(Exception e) {
			sender.tell(new ErrorMessage(e),getSelf());

		}
	}

}
