/**
 *  @(#)ExternalLinks.java 0.01 03/04/2018
 *  Copyright (C) 2018 - 20xx MER-C
 *
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
package org.wikipedia;

import java.net.*;
import java.util.*;

/**
 *  Utility methods to deal with external links and lists of external links.
 *  @author MER-C
 *  @version 0.01
 */
public class ExternalLinks
{
    private Wiki wiki;

    private ExternalLinks(Wiki wiki)
    {
        this.wiki = wiki;
    }
    
    /**
     *  Creates an instance of this class bound to a particular wiki (required
     *  for methods that make network requests to a wiki or for HTML output).
     * 
     *  @param wiki the wiki to bind to
     *  @return an instance of this utility class that is bound to that wiki
     */
    public static ExternalLinks of(Wiki wiki)
    {
        return new ExternalLinks(wiki);
    }
        
    /**
     *  Extracts the domain name from the given URL. This method strips only the
     *  "www" subdomain, that is {@code extractDomain("https://www.example.com/index.jsp")}
     *  returns <samp>example.com</samp> but {@code extractDomain("https://test.example.com/index.jsp")}
     *  returns <samp>test.example.com</samp>.
     *  @param url a valid URL
     *  @return the domain name
     */
    public static String extractDomain(String url)
    {
        try
        {
            // https://stackoverflow.com/questions/9607903/get-domain-name-from-given-url
            URI uri = new URI(url);
            String domain = uri.getHost();
            return domain.contains("www.") ? domain.substring(4) : domain;
        }
        catch (URISyntaxException ex)
        {
            // The primary use case deals with URLs that come from live, working
            // external links on wiki
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     *  Renders output of {@link Wiki#linksearch} in wikitext.
     *  @param results the results to render
     *  @param domain the domain that was searched
     *  @return the rendered wikitext
     */
    public static String linksearchResultsToWikitext(List<String[]> results, String domain)
    {
        StringBuilder builder = new StringBuilder(100);
        for (String[] result : results)
        {
            builder.append("# [[");
            builder.append(result[0]);
            builder.append("]] uses link [");
            builder.append(result[1]);
            builder.append("]\n");
        }
        return builder.toString();
    }

    /**
     *  Renders output of {@link Wiki#linksearch} in HTML.
     *  @param results the results to render
     *  @param domain the domain that was searched
     *  @return the rendered HTML
     */
    public String linksearchResultsToHTML(List<String[]> results, String domain)
    {
        StringBuilder buffer = new StringBuilder(1000);
        buffer.append("<p>\n<ol>\n");
        results.forEach((String[] result) ->
        {
            buffer.append("\t<li><a href=\"");
            buffer.append(wiki.getPageURL(result[0]));
            buffer.append("\">");
            buffer.append(result[0]);
            buffer.append("</a> uses link <a href=\"");
            buffer.append(result[1]);
            buffer.append("\">");
            buffer.append(result[1]);
            buffer.append("</a>\n");
        });
        buffer.append("</ol>");
        return buffer.toString();
    }
}
