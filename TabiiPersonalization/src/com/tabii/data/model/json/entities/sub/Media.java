package com.tabii.data.model.json.entities.sub;

//Class for Image
public class Media {
	
	
	private String adServerType;
	private String codecType;
	private String drmSchema;
	private int priority;
	private String resourceId;
	private String type;
	private String url;

	public String getAdServerType() {
		return adServerType;
	}

	public void setAdServerType(String adServerType) {
		this.adServerType = adServerType;
	}

	public String getCodecType() {
		return codecType;
	}

	public void setCodecType(String codecType) {
		this.codecType = codecType;
	}

	public String getDrmSchema() {
		return drmSchema;
	}

	public void setDrmSchema(String drmSchema) {
		this.drmSchema = drmSchema;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public String toString() {
		return "Media [adServerType=" + adServerType + ", codecType=" + codecType + ", drmSchema=" + drmSchema
				+ ", priority=" + priority + ", resourceId=" + resourceId + ", type=" + type + ", url=" + url + "]";
	}



}
