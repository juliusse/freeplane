package org.freeplane.plugin.remote.v10.model;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.freeplane.features.icon.MindIcon;
import org.freeplane.features.link.NodeLinks;
import org.freeplane.features.map.NodeModel;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
abstract public class NodeModelBase implements Serializable {
	private static final long serialVersionUID = 1L;

	public String id;
	public String nodeText;
	public Boolean isHtml;
	public Boolean folded;
	public String[] icons;
	public ImageModel image;
	public String link;
	public String locked;

	
	public List<String> childrenIds;

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
		locked = null;
		//freeplaneNode = null;
	}

	public NodeModelBase(org.freeplane.features.map.NodeModel freeplaneNode, boolean autoloadChildren) {

		this.id = freeplaneNode.getID();
		this.nodeText = freeplaneNode.getText();
		this.isHtml = freeplaneNode.getXmlText() != null;
		this.folded = freeplaneNode.isFolded();
		this.icons = getIconArray(freeplaneNode);
		this.image = getImage(freeplaneNode);

		URI uri = NodeLinks.getValidLink(freeplaneNode);
		this.link = uri != null ? uri.toString() : null;

		LockModel lm = freeplaneNode.getExtension(LockModel.class);
		this.locked = lm != null ? lm.getUsername() : null;

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

	@JsonIgnore
	public abstract List<DefaultNodeModel> getAllChildren();

	
	public String toJsonString() {
		try {
		final ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(this);
		} catch (Exception e) {
			return "";
		}
	}
	
//	@JsonIgnore
//	protected String getJsonStringParts() {
//		String childrenList = "";
//		if(childrenIds != null) {
//			for(String cId : childrenIds) {
//				childrenList += ",\""+cId+"\"";
//			}
//			childrenList = childrenList.substring(1);
//		}
//
//		return  "\"id\":\""+id+"\"," +
//		"\"nodeText\":\""+new String(JsonStringEncoder.getInstance().quoteAsString(nodeText))+"\"," +
//		"\"isHtml\":"+isHtml.toString()+"," +
//		"\"link\":\""+(link != null ? new String(JsonStringEncoder.getInstance().quoteAsString(link)) : "")+"\"," +
//		"\"folded\":"+folded+"," +
//		"\"locked\":\""+(locked != null ? new String(JsonStringEncoder.getInstance().quoteAsString(locked)) : "")+"\"," +
//		(childrenIds != null && childrenIds.size() > 0 ? "\"childrenIds\":["+childrenList+"]," : "") +
//		"\"image\":\""+"NOT IMPLEMENTED"+"\"," +
//		"\"icons\":\""+"NOT IMPLEMENTED"+"\"";
//	}
}
