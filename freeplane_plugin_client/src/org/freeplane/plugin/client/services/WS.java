package org.freeplane.plugin.client.services;

import java.util.concurrent.Executor;

import org.codehaus.jackson.JsonNode;

import com.google.common.util.concurrent.ListenableFuture;

public interface WS {
	Executor getExecutor();
	
	
	ListenableFuture<Boolean> login(String username, String password);
	
	ListenableFuture<Boolean> listenIfUpdatesOccur(String mapId);
	
	ListenableFuture<JsonNode> getMapAsXml(String mapId);
	
	ListenableFuture<GetUpdatesResponse> getUpdatesSinceRevision(String mapId, int sinceRevision);
	
	ListenableFuture<String> createNode(String mapId, String parentNodeId);
	
	ListenableFuture<Boolean> moveNodeTo(String mapId, String newParentId, String nodeToMoveId, int newIndex);
	ListenableFuture<Boolean> removeNode(String mapId, String nodeId);
	ListenableFuture<Boolean> changeNode(String mapId, String nodeId, String attribute, Object value);
	
	
}
