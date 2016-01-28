package org.wikibase.data;

public class CommonsMedia extends WikibaseDataType {
    private String fileName;

    public CommonsMedia(String fileName) {
        super();
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "CommonsMedia [fileName=" + fileName + "]";
    }
}
