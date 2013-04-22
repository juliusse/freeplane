package org.freeplane.plugin.client.services;

import org.codehaus.jackson.JsonNode;

import scala.concurrent.Future;

public interface WS {	
	Future<Boolean> login(String username, String password);
	
	Future<Boolean> listenIfUpdatesOccur(String mapId);
	
	Future<JsonNode> getMapAsXml(String mapId);
	
	Future<GetUpdatesResponse> getUpdatesSinceRevision(String mapId, int sinceRevision);
	
	Future<String> createNode(String mapId, String parentNodeId);
	
	Future<Boolean> moveNodeTo(String mapId, String newParentId, String nodeToMoveId, int newIndex);
	Future<Boolean> removeNode(String mapId, String nodeId);
	Future<Boolean> changeNode(String mapId, String nodeId, String attribute, Object value);
	
	
}
