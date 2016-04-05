/**
*  This program is free software; you can redistribute it and/or
*  modify it under the terms of the GNU General Public License
*  as published by the Free Software Foundation; either version 3
*  of the License, or (at your option) any later version. Additionally
*  this file is subject to the "Classpath" exception.
*
*  This program is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License
*  along with this program; if not, write to the Free Software Foundation,
*  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/
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
