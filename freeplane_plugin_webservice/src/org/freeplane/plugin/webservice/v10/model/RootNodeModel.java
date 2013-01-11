package org.freeplane.plugin.webservice.v10.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.freeplane.features.map.MapController;
import org.freeplane.features.map.NodeModel;
import org.freeplane.plugin.webservice.WebserviceController;

@XmlRootElement(name="node")
@XmlAccessorType(XmlAccessType.FIELD)
public class RootNodeModel extends NodeModelBase {
	
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
		
		MapController mapController = WebserviceController.getInstance().getModeController().getMapController(); 
		int totalCount = childrenIds.size();
		for(String nodeId : childrenIds) {
			NodeModel child = mapController.getNodeFromID(nodeId);
			if(child.isLeft()) {
				this.leftChildren.add(new org.freeplane.plugin.webservice.v10.model.DefaultNodeModel(child,false));
			} else {
				this.rightChildren.add(new org.freeplane.plugin.webservice.v10.model.DefaultNodeModel(child,false));
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

	
}
