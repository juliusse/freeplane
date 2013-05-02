package org.freeplane.plugin.client.services;

import org.codehaus.jackson.JsonNode;
import org.freeplane.plugin.client.User;

import scala.concurrent.Future;

public interface WS {
	/**
	 * 
	 * @param username
	 * @param password
	 * @return User or null on failure
	 */
	Future<User> login(String username, String password);
	
	Future<Boolean> listenIfUpdatesOccur(String username, String accessToken, String mapId);
	
	Future<JsonNode> getMapAsXml(String username, String accessToken, String mapId);
	
	Future<GetUpdatesResponse> getUpdatesSinceRevision(String username, String accessToken, String mapId, int sinceRevision);
	
	Future<String> createNode(String username, String accessToken, String mapId, String parentNodeId);
	
	Future<Boolean> moveNodeTo(String username, String accessToken, String mapId, String newParentId, String nodeToMoveId, int newIndex);
	Future<Boolean> removeNode(String username, String accessToken, String mapId, String nodeId);
	Future<Boolean> changeNode(String username, String accessToken, String mapId, String nodeId, String attribute, Object value);
	
	
}
