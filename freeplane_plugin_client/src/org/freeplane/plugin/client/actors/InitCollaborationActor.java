package org.freeplane.plugin.client.actors;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.freeplane.features.mapio.mindmapmode.MMapIO;
import org.freeplane.n3.nanoxml.XMLException;
import org.freeplane.plugin.client.ClientController;
import org.freeplane.plugin.client.ClientController.CheckForChangesRunnable;
import org.freeplane.plugin.client.actors.InitCollaborationActor.Messages.InitCollaborationMode;
import org.freeplane.plugin.client.actors.ListenForUpdatesActor.Messages.SetMapAndRevision;
import org.freeplane.plugin.client.services.WS;

import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;

public class InitCollaborationActor extends UntypedActor {

	private String mapId = null;
	
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof InitCollaborationMode) {
			final InitCollaborationMode msg = (InitCollaborationMode)message;
			this.mapId = msg.getMapId();
			final WS ws = ClientController.webservice();
			final Future<Boolean> loginFuture = ws.login(msg.getUsername(), msg.getPassword()); 
			Patterns.pipe(loginFuture, getContext().system().dispatcher()).to(getSelf());
			
		} 
		//login response
		else if(message instanceof Boolean) { 
			final Boolean loginSuccess = (Boolean)message;
			if(loginSuccess) {
				final WS ws = ClientController.webservice();
				final Future<JsonNode> mindmapFuture = ws.getMapAsXml(mapId);
				Patterns.pipe(mindmapFuture, getContext().system().dispatcher()).to(getSelf());
			}
		} 
		//mindmap as json
		else if(message instanceof JsonNode) {
			final JsonNode responseNode = (JsonNode)message;
			final int currentRevision = responseNode.get("currentRevision").asInt();
			final String xmlString = responseNode.get("xmlString").asText();
			final Random ran = new Random();
			final String filename = "" + System.currentTimeMillis() + ran.nextInt(100);
			final String tempDirPath = System.getProperty("java.io.tmpdir");
			final File file = new File(tempDirPath + "/docear/" + filename + ".mm");

			try {
				FileUtils.writeStringToFile(file, xmlString);
				final URL pathURL = file.toURI().toURL();

				final MMapIO mio = (MMapIO) ClientController.getMapIO();
				mio.newMap(pathURL);
			} catch (IOException e) {
				throw new AssertionError(e);
			} catch (URISyntaxException e) {
				throw new AssertionError(e);
			} catch (XMLException e) {
				throw new AssertionError(e);
			} finally {
				file.delete();
			}

			final ActorRef listenForUpdatesActor = ClientController.listenForUpdatesActor();
			listenForUpdatesActor.tell(new SetMapAndRevision(mapId,currentRevision),getSelf());
			listenForUpdatesActor.tell("listen", getSelf());

			final ActorSystem system = getContext().system();
			system.scheduler().schedule(
					Duration.Zero(), 
					Duration.create(1, TimeUnit.SECONDS), 
					new CheckForChangesRunnable(), 
					system.dispatcher());
		}

	}


	public static final class Messages {
		public static class InitCollaborationMode {
			private final String mapId;
			private final String username;
			private final String password;

			public InitCollaborationMode(String mapId, String username, String password) {
				super();
				this.mapId = mapId;
				this.username = username;
				this.password = password;
			}

			public String getUsername() {
				return username;
			}

			public String getPassword() {
				return password;
			}

			public String getMapId() {
				return mapId;
			}

		}
	}
}
