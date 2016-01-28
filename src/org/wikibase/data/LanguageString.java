package org.wikibase.data;

public class LanguageString extends WikibaseDataType {
    private String language;
    private String text;

    public LanguageString(String language, String text) {
        super();
        this.language = language;
        this.text = text;
    }

    public String getLanguage() {
        return language;
    }

    public String getText() {
        return text;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(text);
        sb.append(" (");
        sb.append(language);
        sb.append(")");
        return sb.toString();
    }
}
