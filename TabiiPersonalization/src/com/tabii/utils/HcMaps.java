package com.tabii.utils;

public enum HcMaps {
	// Enum constants
	QUEUES("queuesMap"), ROWS("rowsMap"), IMAGES("imagesMap"), EXCBADGES("exclusiveBadgesMap"), BADGES("badgesMap"), GENRE("genreMap"), SHOWS("showsMap");

	private final String mapName;

	HcMaps(final String mapName) {
        this.mapName = mapName;
    }

	
	public  String  getMapName() {
		return mapName;
	}
}