package org.wikibase.data;

public class GlobeCoordinate extends WikibaseDataType {
    private double longitude;
    private double latitude;
    private double precision;
    private Item globe;

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public Item getGlobe() {
        return globe;
    }

    public void setGlobe(Item globe) {
        this.globe = globe;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    @Override
    public String toString() {
        StringBuilder sbuild = new StringBuilder();
        sbuild.append(Math.abs(latitude));
        sbuild.append("°");
        sbuild.append(0 < latitude ? "S" : "N");
        sbuild.append(" ");
        sbuild.append(Math.abs(longitude));
        sbuild.append("°");
        sbuild.append(0 < latitude ? "W" : "E");
        return sbuild.toString();
    }
    
    
}
