package org.freeplane.plugin.remote.actors;

import org.docear.messages.Messages.AddNodeRequest;
import org.docear.messages.Messages.ChangeNodeRequest;
import org.docear.messages.Messages.CloseAllOpenMapsRequest;
import org.docear.messages.Messages.CloseMapRequest;
import org.docear.messages.Messages.CloseServerRequest;
import org.docear.messages.Messages.CloseUnusedMaps;
import org.docear.messages.Messages.GetExpiredLocksRequest;
import org.docear.messages.Messages.GetNodeRequest;
import org.docear.messages.Messages.MindmapAsJsonRequest;
import org.docear.messages.Messages.MindmapAsXmlRequest;
import org.docear.messages.Messages.OpenMindMapRequest;
import org.docear.messages.Messages.RefreshLockRequest;
import org.docear.messages.Messages.ReleaseLockRequest;
import org.docear.messages.Messages.RemoveNodeRequest;
import org.docear.messages.Messages.RequestLockRequest;
import org.docear.messages.exceptions.LockNotFoundException;
import org.docear.messages.exceptions.MapNotFoundException;
import org.docear.messages.exceptions.NodeAlreadyLockedException;
import org.docear.messages.exceptions.NodeNotFoundException;
import org.freeplane.plugin.remote.RemoteController;
import org.freeplane.plugin.remote.v10.Actions;
import org.slf4j.Logger;

import akka.actor.ActorRef;
import akka.actor.Status;
import akka.actor.UntypedActor;

public class MainActor extends UntypedActor {

	public MainActor() {
	}

	@Override
	public void onReceive(Object message) throws Exception {
		final Logger logger  = RemoteController.getLogger();  
		logger.info("MainActor.onReceive => '{}' received.",message.getClass().getName());
		final ActorRef sender = getSender();
		logger.info("MainActor.onReceive => Sender: '{}'",sender.path());
		
		Object response = null;
		try {
			//get map as json
			if(message instanceof MindmapAsJsonRequest) {
				response = Actions.getMapModelJson((MindmapAsJsonRequest) message);
			}
			
			//get map as xml
			else if(message instanceof MindmapAsXmlRequest) {
				response = Actions.getMapModelXml((MindmapAsXmlRequest)message);
			}

			//add node to map
			else if(message instanceof AddNodeRequest) {
				response = Actions.addNode((AddNodeRequest) message);			
			}
			
			//change node
			else if(message instanceof ChangeNodeRequest) {
				Actions.changeNode((ChangeNodeRequest)message);
			}
			
			//remove node from map
			else if(message instanceof RemoveNodeRequest) {
				Actions.removeNode((RemoveNodeRequest) message);			
			}
			
			//get node from map
			else if(message instanceof GetNodeRequest) {
				response = Actions.getNode((GetNodeRequest) message);			
			}
			
			// Open mindmap
			else if (message instanceof OpenMindMapRequest){
				Actions.openMindmap((OpenMindMapRequest)message);
			}
			
			//close map
			else if(message instanceof CloseMapRequest) {
				Actions.closeMap((CloseMapRequest)message);
			}
			
			//close all maps
			else if(message instanceof CloseAllOpenMapsRequest) {
				Actions.closeAllOpenMaps((CloseAllOpenMapsRequest)message);
			}
			
			//close server
			else if(message instanceof CloseServerRequest) {
				Actions.closeServer((CloseServerRequest)message);
			}
			
			//refresh lock
			else if(message instanceof RefreshLockRequest) {
				Actions.refreshLock((RefreshLockRequest)message);
			}
			
			//release lock
			else if(message instanceof ReleaseLockRequest) {
				Actions.releaseLock((ReleaseLockRequest)message);
			}
			
			//request lock
			else if(message instanceof RequestLockRequest) {
				Actions.requestLock((RequestLockRequest)message);
			}
			
			//get expired locks
			else if(message instanceof GetExpiredLocksRequest) {
				response = Actions.getExpiredLocks((GetExpiredLocksRequest)message);
			}
			
			//close unused maps
			else if(message instanceof CloseUnusedMaps) {
				Actions.closeUnusedMaps((CloseUnusedMaps)message);
			}
		}
		catch(MapNotFoundException e) {
			logger.warn("MainActor.onReceive => Map not found exception catched. ",e);
			response = new Status.Failure(e);
		}
		catch(NodeNotFoundException e) {
			logger.warn("MainActor.onReceive => Node not found exception catched. ",e);
			response = new Status.Failure(e);
		}
		catch(NodeAlreadyLockedException e) {
			logger.warn("MainActor.onReceive => Node already locked exception catched. ",e);
			response = new Status.Failure(e);
		}
		catch(LockNotFoundException e) {
			logger.warn("MainActor.onReceive => Lock not found exception catched. ",e);
			response = new Status.Failure(e);
		}
		catch(Exception e) {
			logger.error("MainActor.onReceive => Unrecognized Exception! ",e);
			response = new Status.Failure(e);
		}
		
		if(response != null) {
			logger.info("MainActor.onReceive => sending '{}' as response.",response.getClass().getName());
			sender.tell(response, getSelf());
		} else {
			logger.info("MainActor.onReceive => No response available");
		}
	}

}
