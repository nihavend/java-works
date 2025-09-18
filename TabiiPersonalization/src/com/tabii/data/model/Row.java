package com.tabii.data.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tabii.data.model.entities.ShowCard;


@JsonTypeInfo(
	    use = JsonTypeInfo.Id.NAME,
	    include = JsonTypeInfo.As.EXISTING_PROPERTY,  // use rowType property
	    property = "rowType",
	    visible = true   // keep rowType in the subclass too
	)
	@JsonSubTypes({
	    @JsonSubTypes.Type(value = BannerRow.class, name = "banner"),
	    @JsonSubTypes.Type(value = CwRow.class, name = "continueWatching"),
	    @JsonSubTypes.Type(value = GenreRow.class, name = "genre"),
	    @JsonSubTypes.Type(value = LiveStreamRow.class, name = "livestream"),
	    @JsonSubTypes.Type(value = ShowRow.class, name = "show"),
	    @JsonSubTypes.Type(value = SpecialRow.class, name = "special")
	})

public abstract class Row {
	
	private List<ShowCard> contents;
	private int id;
	private String rowType;
	
	public List<ShowCard> getContents() {
		return contents;
	}
	public void setContents(List<ShowCard> contents) {
		this.contents = contents;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getRowType() {
		return rowType;
	}
	public void setRowType(String rowType) {
		this.rowType = rowType;
	}
	

}
