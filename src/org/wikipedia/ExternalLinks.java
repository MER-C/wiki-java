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

import java.io.IOException;
import java.net.*;
import java.util.*;

/**
 *  Utility methods to deal with external links and lists of external links.
 *  @author MER-C
 *  @version 0.01
 */
public class ExternalLinks
{
    private final Wiki wiki;
    private final Pages pageutils;
    private static final List<String> globalblacklist = new ArrayList<>();
    private final List<String> localblacklist = new ArrayList<>();
    private final List<String> blocked_domains = new ArrayList<>();
    private static final WMFWikiFarm sessions = WMFWikiFarm.instance();

    private ExternalLinks(Wiki wiki)
    {
        this.wiki = wiki;
        pageutils = Pages.of(wiki);
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
     *  "www" subdomain.
     * 
     *  <ul>
     *  <li>{@code extractDomain("http://example.com")} returns <samp>example.com</samp>
     *  <li>{@code extractDomain("https://www.example.com/index.jsp")} returns
     *      <samp>example.com</samp>
     *  <li>{@code extractDomain("https://test.example.com/index.jsp")} returns
     *      <samp>test.example.com</samp>.
     *  </ul>
     *  
     *  <p>
     *  The primary use case deals with URLs that come from live, working
     *  external links on wiki. Erroneous markup that MediaWiki parses as valid
     *  external links can give unpredictable results:
     *  
     *  <ul>
     *  <li>{@code extractDomain("http://www.example.com,")} returns {@code null}.
     *  <li>{@code extractDomain("http://http://example.com"}} returns <samp>http</samp>.
     *  </ul>
     * 
     *  @param url a valid URL
     *  @return the domain name or {@code null} if the URL cannot be parsed
     */
    public static String extractDomain(String url)
    {
        try
        {
            // https://stackoverflow.com/questions/9607903/get-domain-name-from-given-url
            URI uri = new URI(url);
            String domain = uri.getHost();
            if (domain == null)
                return null;
            return domain.contains("www.") ? domain.substring(4) : domain;
        }
        catch (URISyntaxException ex)
        {
            return null;
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
            builder.append("# [[:");
            builder.append(result[0]);
            builder.append("]] ([[Special:Edit/");
            builder.append(result[0]);
            builder.append("|edit]] | [[Special:PageHistory/");
            builder.append(result[0]);
            builder.append("|history]]) uses link [");
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
        for (String[] result : results)
        {
            buffer.append("\t<li>");
            buffer.append(pageutils.generatePageLink(result[0]));
            buffer.append(" (");
            buffer.append(pageutils.generatePageLink("Special:Edit/" + result[0], "edit"));
            buffer.append(" | ");
            buffer.append(pageutils.generatePageLink("Special:PageHistory/" + result[0], "history"));
            buffer.append(") uses link <a href=\"");
            buffer.append(result[1]);
            buffer.append("\">");
            buffer.append(result[1]);
            buffer.append("</a>\n");
        }
        buffer.append("</ol>");
        return buffer.toString();
    }
    
    /**
     *  Determines whether a site is on the global spam blacklist, modulo 
     *  Java/PHP regex differences.
     *  @param site the site to check
     *  @return whether a site is on the spam blacklist
     *  @throws IOException if a network error occurs
     *  @throws UnsupportedOperationException if the SpamBlacklist extension is
     *  not installed
     *  @see <a href="https://mediawiki.org/wiki/Extension:SpamBlacklist">Extension:SpamBlacklist</a>
     */
    public static boolean isGloballyBlacklisted(String site) throws IOException
    {
        if (globalblacklist.isEmpty())
            loadGlobalSpamBlacklist();
        
        // yes, I know about the spam whitelist, but I primarily intend to use
        // this to check entire domains whereas the spam whitelist tends to 
        // contain individual pages on websites
        for (String entry : globalblacklist)
            if (site.matches(entry))
                return true;
        return false;
    }
    
    /**
     *  Determines whether a site is on the global or local spam blacklist or 
     *  Blocked External Domains, modulo Java/PHP regex differences.
     *  @param site the site to check
     *  @return whether a site is on the spam blacklist
     *  @throws IOException if a network error occurs
     *  @throws UnsupportedOperationException if the SpamBlacklist and 
     *  AbuseFilter extensions are not installed
     *  @see <a href="https://mediawiki.org/wiki/Extension:SpamBlacklist">Extension:SpamBlacklist</a>
     */
    public boolean isLocallyBlacklisted(String site) throws IOException
    {
        // yes, I know about the spam whitelist, but I primarily intend to use
        // this to check entire domains whereas the spam whitelist tends to 
        // contain individual pages on websites
        if (isGloballyBlacklisted(site))
            return true;
        if (localblacklist.isEmpty())
            loadLocalSpamBlacklists();
        for (String entry : localblacklist)
            if (site.matches(entry))
                return true;
        return blocked_domains.contains(site);
    }
    
    /**
     *  (Re)loads the global spam blacklist cache.
     *  @throws IOException if a network error occurs
     *  @throws UnsupportedOperationException if the SpamBlacklist extension is
     *  not installed
     */
    public static void loadGlobalSpamBlacklist() throws IOException
    {
        WMFWiki meta = sessions.sharedSession("meta.wikimedia.org");
        meta.requiresExtension("SpamBlacklist");
        globalblacklist.clear();
        String[] gbl = meta.getPageText(List.of("Spam blacklist")).get(0).split("\\n");
        for (String entry : gbl)
        {
            if (entry.contains("#"))
                entry = entry.substring(0, entry.indexOf('#'));
            entry = entry.trim();
            if (!entry.isEmpty())
                globalblacklist.add(entry);
        }
    }
    
    /**
     *  (Re)loads the local spam blacklist cache (including Blocked External Domains).
     *  @throws IOException if a network error occurs
     *  @throws UnsupportedOperationException if the SpamBlacklist and 
     *  AbuseFilter extensions are not installed
     */
    public void loadLocalSpamBlacklists() throws IOException
    {
        List<String> pages = wiki.getPageText(List.of("MediaWiki:Spam-blacklist", "MediaWiki:BlockedExternalDomains.json"));
        localblacklist.clear();
        for (String entry : pages.get(0).split("\\n"))
        {
            if (entry.contains("#"))
                entry = entry.substring(0, entry.indexOf('#'));
            entry = entry.trim();
            if (!entry.isEmpty())
                localblacklist.add(entry);
        }
        blocked_domains.clear();
        for (String entry : pages.get(1).split("\\n"))
        {
            if (entry.contains("\"domain\":"))
            {
                int x1 = entry.indexOf("\": \"") + 4;
                int x2 = entry.length() - 2;
                blocked_domains.add(entry.substring(x1, x2));
            }
        }
    }
}
