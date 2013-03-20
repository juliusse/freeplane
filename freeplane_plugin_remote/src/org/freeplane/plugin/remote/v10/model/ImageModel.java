package org.freeplane.plugin.remote.v10.model;

import java.io.Serializable;

public class ImageModel implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String URI;
	public float size;
	
	@SuppressWarnings("unused")
	private ImageModel() {
		
	}
	
	public ImageModel(String URI, float size) {
		this.URI = URI;
		this.size = size;
	}
}
