package org.wikibase;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wikibase.data.Property;

public class WikibasePropertyFactory {
    private static Map<String, Property> props = new HashMap<String, Property>();
    private static Pattern PROP_ID_PATTERN = Pattern.compile("\\s*(p|P)?(\\d+)\\s*");
    public static Property getWikibaseProperty(String id) {
        Matcher idMatcher = PROP_ID_PATTERN.matcher(id);
        if (!idMatcher.matches()) {
            return null;
        }
        String actualId = "P" + idMatcher.group(2);
        
        Property prop = props.get(actualId);
        if (null == prop) {
            prop = new Property(actualId);
            props.put(actualId, prop);
        }
        return prop;
    }
}