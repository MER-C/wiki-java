package org.wikibase.data;

public class StringData extends WikibaseDataType {
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public StringData(String value) {
        super();
        this.value = value;
    }

    @Override
    public String toString() {
        return "StringData [value=" + value + "]";
    }
}
