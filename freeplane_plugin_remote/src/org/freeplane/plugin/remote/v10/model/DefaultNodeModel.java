package org.freeplane.plugin.remote.v10.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.io.JsonStringEncoder;
import org.freeplane.features.attribute.Attribute;
import org.freeplane.features.attribute.NodeAttributeTableModel;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.nodelocation.LocationModel;
import org.freeplane.plugin.remote.RemoteController;

@XmlRootElement(name="node")
@XmlAccessorType(XmlAccessType.FIELD)
public class DefaultNodeModel extends NodeModelBase implements Serializable{
	@XmlElement(name="children")
	public List<DefaultNodeModel> children;

	public Integer hGap;
	public Integer shiftY;
	public Map<String, String> attributes;

	/**
	 * necessary for JAX-B
	 */
	public DefaultNodeModel() {
		super();
	}
	
	public DefaultNodeModel(org.freeplane.features.map.NodeModel freeplaneNode, boolean autoloadChildren) {
		super(freeplaneNode,autoloadChildren);
		
		loadLocation(freeplaneNode);
		loadAttributes(freeplaneNode);
	}
	
	private void loadLocation(org.freeplane.features.map.NodeModel freeplaneNode) {
		LocationModel l = freeplaneNode.getExtension(LocationModel.class);
		if(l != null) {
			hGap = l.getHGap();
			shiftY = l.getShiftY();
		} else {
			hGap = 0;
			shiftY = 0;
		}
	}
	
	private void loadAttributes(org.freeplane.features.map.NodeModel freeplaneNode){
		NodeAttributeTableModel attributeModel = freeplaneNode.getExtension(NodeAttributeTableModel.class);
		if(attributeModel != null) {
			this.attributes = new HashMap<String, String>();
			for (Attribute attribute : attributeModel.getAttributes()){
				this.attributes.put(attribute.getName(), String.valueOf(attribute.getValue()));
			}
		} else {
			attributes = null;
		}
	}

	@Override
	public int loadChildren(boolean autoloadChildren) {
		children = new ArrayList<DefaultNodeModel>();
		
		MapController mapController = RemoteController.getModeController().getMapController();
		
		int totalCount = childrenIds.size();
		for(String nodeId : childrenIds) {
			NodeModel freeplaneChild = mapController.getNodeFromID(nodeId);
			children.add(new DefaultNodeModel(freeplaneChild,false));
		}
		
		if(autoloadChildren) {
			for(NodeModelBase child : this.children) {
				totalCount += child.loadChildren(true);
			}
		}
		
		childrenIds = null;
		return totalCount;
	}
	

	@Override
	public List<DefaultNodeModel> getAllChildren() {
		return children;
	}
	
	public String toJsonString() {
		String childrenList = "";
		if(children != null && children.size() > 0) {
			for(DefaultNodeModel node : children) {
				childrenList += ","+node.toJsonString();
			}
			childrenList = childrenList.substring(1);
		}
		
		
		StringBuilder builder = new StringBuilder();
		builder.append("{"+getJsonStringParts()+",");
		if(children != null) {
			builder.append("\"children\":["+childrenList+"],");
		}
		builder.append("\"attributes\":\"NOT IMPLEMENTED\",");
		builder.append("\"hGap\":\""+hGap+"\",");
		builder.append("\"shiftY\":\""+shiftY+"\"");
		builder.append("}");
		
		//return "{}"
		return builder.toString();
	}

}
