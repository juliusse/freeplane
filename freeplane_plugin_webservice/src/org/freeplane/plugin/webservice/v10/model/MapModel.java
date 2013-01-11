package org.freeplane.plugin.webservice.v10.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.freeplane.features.map.NodeModel;

@XmlRootElement(name = "mapModel")
@XmlAccessorType(XmlAccessType.FIELD)
public class MapModel {
	public String id;
	public Boolean isReadonly;
	public RootNodeModel root;

	public MapModel() {
	}
	
	public MapModel(org.freeplane.features.map.MapModel freeplaneMap, boolean autoloadChildren) {
		id = freeplaneMap.getTitle();
		isReadonly = freeplaneMap.isReadOnly();
		
		NodeModel rootNodeFreeplane = freeplaneMap.getRootNode();
		root = new RootNodeModel(rootNodeFreeplane, autoloadChildren);
	}
	
	
}
