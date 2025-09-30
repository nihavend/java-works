package com.tabii.data.model.entities.sub;
// Class for ExclusiveBadge
public class ExclusiveBadge {
    private String exclusiveBadgeType;

    // Getter and setter
    public String getExclusiveBadgeType() { return exclusiveBadgeType; }
    public void setExclusiveBadgeType(String exclusiveBadgeType) { this.exclusiveBadgeType = exclusiveBadgeType; }

    @Override
    public String toString() {
        return "ExclusiveBadge{" +
                "exclusiveBadgeType='" + exclusiveBadgeType + '\'' +
                '}';
    }
}