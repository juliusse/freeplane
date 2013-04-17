package org.freeplane.plugin.client.services;

import org.codehaus.jackson.JsonNode;

public interface WS {
	
	boolean login(String username, String password);
	
	boolean listenIfUpdatesOccur(String mapId);
	
	JsonNode getMapAsXml(String mapId);
	
	GetUpdatesResponse getUpdatesSinceRevision(String mapId, int sinceRevision);
	
	String createNode(String mapId, String parentNodeId);
	
	boolean moveNodeTo(String mapId, String newParentId, String nodeToMoveId, int newIndex);
	boolean removeNode(String mapId, String nodeId);
	boolean changeNode(String mapId, String nodeId, String attribute, Object value);
}
