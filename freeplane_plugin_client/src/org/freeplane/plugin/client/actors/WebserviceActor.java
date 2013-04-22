//package org.freeplane.plugin.client.actors;
//
//import org.freeplane.plugin.client.ClientController;
//import org.freeplane.plugin.client.actors.WebserviceActor.Messages.LoginRequest;
//import org.freeplane.plugin.client.actors.WebserviceActor.Messages.LoginResponse;
//
//import akka.actor.UntypedActor;
//
//public class WebserviceActor extends UntypedActor {
//
//	@Override
//	public void onReceive(Object message) throws Exception {
//
//		if(message instanceof LoginRequest) {
//			final String username = ((LoginRequest) message).getUsername();
//			final String password = ((LoginRequest) message).getPassword();
//			final boolean success = ClientController.webservice().login(username, password).get();
//			getSender().tell(new LoginResponse(success), getSelf());
//		}
//		
//	}
//
//	public static final class Messages {
//		public static class LoginRequest {
//			private final String username;
//			private final String password;
//
//			public LoginRequest(String username, String password) {
//				super();
//				this.username = username;
//				this.password = password;
//			}
//
//			public String getUsername() {
//				return username;
//			}
//
//			public String getPassword() {
//				return password;
//			}
//
//		}
//
//		public static class LoginResponse {
//			private final boolean success;
//
//			public LoginResponse(boolean success) {
//				super();
//				this.success = success;
//			}
//
//			public boolean isSuccess() {
//				return success;
//			}
//
//		}
//	}
//}
