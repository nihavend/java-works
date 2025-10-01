package com.tabii.data.model.json.entities;

import java.util.ArrayList;

import com.tabii.data.model.json.entities.sub.Badge;
import com.tabii.data.model.json.entities.sub.Image;

public class Banner {
	
	public ArrayList<Badge> Badges;
	
	public String contentType;
	public String description;

	public ArrayList<Badge> ExclusiveBadges;
	
	public boolean favorite;
	
	public ArrayList<GenreCard> genres;
	
	public int id;
	
	public ArrayList<Image> images;
	
	public String madeYear;
	
	public String spot;
	
	public String title;

}
