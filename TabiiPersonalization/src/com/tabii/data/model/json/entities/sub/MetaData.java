package com.tabii.data.model.json.entities.sub;

import jakarta.validation.constraints.NotBlank;

public class MetaData {
	
    @NotBlank
    private String viewType;
    
    // opsiyonel ?
	private boolean shuffle;

    // Getter & Setter
    public String getViewType() { return viewType; }
    public void setViewType(String viewType) { this.viewType = viewType; }
	public boolean isShuffle() {
		return shuffle;
	}
	public void setShuffle(boolean shuffle) {
		this.shuffle = shuffle;
	}
}