package org.freeplane.plugin.remote.v10.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.freeplane.features.map.MapController;
import org.freeplane.features.map.NodeModel;
import org.freeplane.plugin.remote.RemoteController;

@XmlRootElement(name="node")
@XmlAccessorType(XmlAccessType.FIELD)
public class RootNodeModel extends NodeModelBase implements Serializable {
	private static final long serialVersionUID = 1L;
	
	//@XmlElement(required=true,nillable=true)
	@XmlElement(name="leftChildren")
	public List<DefaultNodeModel> leftChildren;
	//@XmlElement(required=true)
	@XmlElement(name="rightChildren")
	public List<DefaultNodeModel> rightChildren;

	//public NodeModel preferredChild;
	
	
	/**
	 * necessary for JAX-B
	 */
	@SuppressWarnings("unused")
	private RootNodeModel() {
		super();
	}
	
	/**
	 * automatically converts the whole tree
	 * @param freeplaneNode
	 */
	public RootNodeModel(org.freeplane.features.map.NodeModel freeplaneNode, boolean autoloadChildren) {
		super(freeplaneNode,autoloadChildren);
	}
	
	
	
	@Override
	public int loadChildren(boolean autoloadChildren) {
		leftChildren = new ArrayList<DefaultNodeModel>();
		rightChildren = new ArrayList<DefaultNodeModel>();
		
		MapController mapController = RemoteController.getModeController().getMapController(); 
		int totalCount = childrenIds.size();
		for(String nodeId : childrenIds) {
			NodeModel child = mapController.getNodeFromID(nodeId);
			if(child.isLeft()) {
				this.leftChildren.add(new org.freeplane.plugin.remote.v10.model.DefaultNodeModel(child,false));
			} else {
				this.rightChildren.add(new org.freeplane.plugin.remote.v10.model.DefaultNodeModel(child,false));
			}
		}
		
		if(autoloadChildren) {
			for(DefaultNodeModel child : this.leftChildren) {
				totalCount += child.loadChildren(true);
			}
			for(DefaultNodeModel child : this.rightChildren) {
				totalCount += child.loadChildren(true);
			}
		}
			
		childrenIds = null;
		return totalCount;
	}

	@Override
	public List<DefaultNodeModel> getAllChildren() {
		List<DefaultNodeModel> list = new ArrayList<DefaultNodeModel>(leftChildren);
		list.addAll(rightChildren);
		return list;
	}

	public String toJsonString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{"+getJsonStringParts()+",\"leftChildren\":[");
		for(int i = 0; i < leftChildren.size(); i ++) {
			builder.append(leftChildren.get(i).toJsonString());
			if(i < leftChildren.size()-1)
				builder.append(",");
		}
		builder.append("],\"rightChildren\":[");
		for(int i = 0; i < rightChildren.size(); i ++) {
			builder.append(rightChildren.get(i).toJsonString());
			if(i < rightChildren.size()-1)
				builder.append(",");
		}
		builder.append("]}");
		return builder.toString();
	}
}
