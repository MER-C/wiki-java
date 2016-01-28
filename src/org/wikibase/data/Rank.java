package org.wikibase.data;

public enum Rank {
    DEPRECATED, NORMAL, PREFERRED;
    
    public static Rank fromString(String rankName) {
        if ("deprecated".equalsIgnoreCase(rankName)) {
            return DEPRECATED;
        }
        if ("preferred".equalsIgnoreCase(rankName)) {
            return PREFERRED;
        }
        return Rank.NORMAL;
    }
}
