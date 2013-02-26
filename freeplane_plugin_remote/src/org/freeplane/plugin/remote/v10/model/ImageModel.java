package org.freeplane.plugin.remote.v10.model;

import java.io.Serializable;

public class ImageModel implements Serializable {
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
