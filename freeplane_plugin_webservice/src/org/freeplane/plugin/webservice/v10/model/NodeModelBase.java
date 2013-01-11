package org.freeplane.plugin.webservice.v10.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

import org.freeplane.features.icon.MindIcon;
import org.freeplane.features.link.NodeLinks;
import org.freeplane.features.map.NodeModel;

@XmlTransient
@XmlSeeAlso(value={DefaultNodeModel.class,RootNodeModel.class})
abstract public class NodeModelBase {

	public final String id;
	public final String nodeText;
	public final Boolean isHtml;
	public final Boolean folded;
	public final String[] icons;
	public final ImageModel image;
	public final String link;

	//@XmlTransient
	//protected final NodeModel freeplaneNode;
	@XmlElement
	protected List<String> childrenIds;

	/**
	 * necessary for JAX-B
	 */
	protected NodeModelBase() {
		id = null;
		nodeText = null;
		isHtml = false;
		folded = false;
		icons = null;
		image = null;
		link = null;
		//freeplaneNode = null;
	}

	public NodeModelBase(org.freeplane.features.map.NodeModel freeplaneNode, boolean autoloadChildren) {

		this.id = freeplaneNode.getID();
		this.nodeText = freeplaneNode.getText();
		this.isHtml = freeplaneNode.getXmlText() != null;
		this.folded = freeplaneNode.isFolded();
		this.icons = getIconArray(freeplaneNode);
		this.image = getImage(freeplaneNode);
		//this.freeplaneNode = freeplaneNode;


		//NodeLinks nl = freeplaneNode.getExtension(NodeLinks.class);
		
		//get link
		URI uri = NodeLinks.getValidLink(freeplaneNode);
		this.link = uri != null ? uri.toString() : null;

		saveChildrenIds(freeplaneNode);

		if(autoloadChildren) { //load children models
			loadChildren(true);
		}
	}

	private String[] getIconArray(org.freeplane.features.map.NodeModel freeplaneNode) {
		String[] iconNames = new String[freeplaneNode.getIcons().size()];
		int count = 0;
		for(MindIcon mi : freeplaneNode.getIcons()) {
			iconNames[count++] = mi.getName();
		}
		return iconNames;
	}

	private ImageModel getImage(org.freeplane.features.map.NodeModel freeplaneNode) {
		// TODO: implement; Where is the Image hidden? (JS) 
		return null;
	}

	/**
	 * stores nodeIds
	 * @param freeplaneNode
	 */
	protected void saveChildrenIds(NodeModel freeplaneNode) {
		childrenIds = new ArrayList<String>();

		for(NodeModel node : freeplaneNode.getChildren()) {
			childrenIds.add(node.getID());
		}
	}

	/**
	 * loads the children into the model
	 * @return number of children that have been added
	 */
	public abstract int loadChildren(boolean autoloadChildren);

	public abstract List<DefaultNodeModel> getAllChildren();
}
