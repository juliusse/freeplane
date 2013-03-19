package org.freeplane.plugin.remote.v10.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.freeplane.features.map.NodeModel;

import com.sun.accessibility.internal.resources.accessibility;

@XmlRootElement(name = "mapModel")
@XmlAccessorType(XmlAccessType.FIELD)
public class MapModel implements Serializable {
	public String id;
	public Boolean isReadonly;
	public RootNodeModel root;
	public String name;
	public Long revision;

	public MapModel() {
	}
	
	public MapModel(org.freeplane.features.map.MapModel freeplaneMap,String name, Long revision, boolean autoloadChildren) {
		id = freeplaneMap.getTitle();
		isReadonly = freeplaneMap.isReadOnly();
		this.name = name;
		this.revision = revision;
		
		NodeModel rootNodeFreeplane = freeplaneMap.getRootNode();
		root = new RootNodeModel(rootNodeFreeplane, autoloadChildren);
	}
	
	public String toJsonString() {
		return "{\"id\":\""+id+"\",\"name\":\""+name+"\",\"revision\":\""+revision+"\",\"isReadonly\":\""+isReadonly+"\",\"root\":"+root.toJsonString()+"}";
	}
	
}
