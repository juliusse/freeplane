package org.freeplane.plugin.webservice.v10.model;

public class ImageModel {
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
