package org.freeplane.plugin.client.actors;

import org.freeplane.core.util.LogUtils;
import org.freeplane.plugin.client.ClientController;
import org.freeplane.plugin.client.services.GetUpdatesResponse;
import org.freeplane.plugin.client.services.WS;
import org.freeplane.plugin.remote.v10.model.updates.MapUpdate;

import scala.concurrent.Future;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;

public class ListenForUpdatesActor extends UntypedActor {

	private String currentMapId;
	private String mapIdForThisExecution;
	private int currentRevision;

	@Override
	public void onReceive(Object message) throws Exception {
		LogUtils.info(message.toString());
		if (message instanceof Messages.SetMapAndRevision) {
			currentMapId = ((Messages.SetMapAndRevision) message).getMapId();
			currentRevision = ((Messages.SetMapAndRevision) message).getRevision();
		} else if (message.equals("listen")) {
			LogUtils.info("listening");
			mapIdForThisExecution = currentMapId;
			
			final Future<Boolean> future = webservice().listenIfUpdatesOccur(mapIdForThisExecution);
			Patterns.pipe(future, getContext().system().dispatcher()).to(getSelf());
		} else if (message instanceof Boolean) {
			final Boolean updateOccured = (Boolean)message;
			if(updateOccured && mapIdForThisExecution.equals(currentMapId)) {
				LogUtils.info("updates occured");
				final Future<GetUpdatesResponse> future = webservice().getUpdatesSinceRevision(mapIdForThisExecution, currentRevision);
				Patterns.pipe(future, getContext().system().dispatcher()).to(getSelf());
			} else {
				getSelf().tell("listen", getSelf());
			}
		} else if (message instanceof GetUpdatesResponse) {
			final GetUpdatesResponse response = (GetUpdatesResponse) message;

			this.currentRevision = response.getCurrentRevision();

			for (MapUpdate mapUpdate : response.getOrderedUpdates()) {
				ClientController.applyChangesActor().tell(mapUpdate, getSelf());

			}
			getSelf().tell("listen", getSelf());
		}
	}

	public int getCurrentRevision() {
		return currentRevision;
	}

	public String getCurrentMapId() {
		return currentMapId;
	}

	public void changeMap(String mapId, int currentRevision) {
		this.currentMapId = mapId;
		this.currentRevision = currentRevision;
	}

	private WS webservice() {
		return ClientController.webservice();
	}

	public final static class Messages {
		private Messages() {
		}

		public static class SetMapAndRevision {
			private final String mapId;
			private final int revision;

			public SetMapAndRevision(String mapId, int revision) {
				super();
				this.mapId = mapId;
				this.revision = revision;
			}

			public String getMapId() {
				return mapId;
			}

			public int getRevision() {
				return revision;
			}

		}
	}
}
