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
import org.freeplane.core.util.LogUtils;
import org.freeplane.plugin.remote.v10.Actions;

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
		LogUtils.info(message.getClass().getName()+" received.");
		
		final ActorRef sender = getSender();
		
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
				Actions.closeServer();
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
			LogUtils.warn("Map not found exception catched.",e);
			response = new Status.Failure(e);
		}
		catch(NodeNotFoundException e) {
			LogUtils.warn("Node not found exception catched.",e);
			response = new Status.Failure(e);
		}
		catch(NodeAlreadyLockedException e) {
			LogUtils.warn("Node already locked exception catched.",e);
			response = new Status.Failure(e);
		}
		catch(LockNotFoundException e) {
			LogUtils.warn("Lock not found exception catched.",e);
			response = new Status.Failure(e);
		}
		catch(Exception e) {
			LogUtils.severe("Unrecognized Exception:",e);
			response = new Status.Failure(e);
		}
		
		if(response != null)
			sender.tell(response, getSelf());
	}

}
