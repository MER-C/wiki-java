package org.wikibase.data;

import org.wikibase.WikibaseEntityFactory;

public class Quantity extends WikibaseDataType {
    private double amount;
    private Item unit;
    private double lowerBound;
    private double upperBound;

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Item getUnit() {
        return unit;
    }

    public void setUnit(Item unit) {
        this.unit = unit;
    }

    public double getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(double lowerBound) {
        this.lowerBound = lowerBound;
    }

    public double getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(double upperBound) {
        this.upperBound = upperBound;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(amount);
        if (null != unit) {
            sb.append(" ");
            sb.append(unit.getEnt());
        }
        return sb.toString();
    }

}
