/**
 *  @(#)WMFWiki.java 0.01 29/03/2011
 *  Copyright (C) 2011 - 2018 MER-C and contributors
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

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.util.stream.*;
import java.time.*;

/**
 *  Stuff specific to Wikimedia wikis.
 *  @author MER-C
 *  @version 0.01
 */
public class WMFWiki extends Wiki
{
    // caches
    private static String globalblacklist;
    private String localblacklist;
    
    /**
     *  Denotes entries in the [[Special:Abuselog]]. These cannot be accessed
     *  through [[Special:Log]] or getLogEntries.
     *  @see #getAbuseLogEntries(int[], Wiki.RequestHelper) 
     */
    public static final String ABUSE_LOG = "abuselog";
    
    /**
     *  Denotes entries in the spam blacklist log. This is a privileged log type.
     *  Requires extension SpamBlacklist.
     *  @see Wiki#getLogEntries
     */
    public static final String SPAM_BLACKLIST_LOG = "spamblacklist";

    /**
     *  Creates a new WMF wiki that has the given domain name.
     *  @param domain a WMF wiki domain name e.g. en.wikipedia.org
     */
    protected WMFWiki(String domain)
    {
        super(domain, "/w", "https://");
    }
    
    /**
     *  Creates a new WMF wiki that has the given domain name.
     *  @param domain a WMF wiki domain name e.g. en.wikipedia.org
     *  @return the constructed Wiki object
     */
    public static WMFWiki createInstance(String domain)
    {
        WMFWiki wiki = new WMFWiki(domain);
        wiki.initVars();
        return wiki;
    }

    /**
     *  Returns the list of publicly readable and editable wikis operated by the
     *  Wikimedia Foundation.
     *  @return (see above)
     *  @throws IOException if a network error occurs
     */
    public static WMFWiki[] getSiteMatrix() throws IOException
    {
        WMFWiki wiki = createInstance("en.wikipedia.org");
        wiki.requiresExtension("SiteMatrix");
        wiki.setMaxLag(0);
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "sitematrix");
        String line = wiki.makeApiCall(getparams, null, "WMFWiki.getSiteMatrix");
        ArrayList<WMFWiki> wikis = new ArrayList<>(1000);

        // form: <special url="http://wikimania2007.wikimedia.org" code="wikimania2007" fishbowl="" />
        // <site url="http://ab.wiktionary.org" code="wiktionary" closed="" />
        for (int x = line.indexOf("url=\""); x >= 0; x = line.indexOf("url=\"", x))
        {
            int a = line.indexOf("https://", x) + 8;
            int b = line.indexOf('\"', a);
            int c = line.indexOf("/>", b);
            x = c;
            
            // check for closed/fishbowl/private wikis
            String temp = line.substring(b, c);
            if (temp.contains("closed=\"\"") || temp.contains("private=\"\"") || temp.contains("fishbowl=\"\""))
                continue;
            wikis.add(createInstance(line.substring(a, b)));
        }
        int size = wikis.size();
        Logger temp = Logger.getLogger("wiki");
        temp.log(Level.INFO, "WMFWiki.getSiteMatrix", "Successfully retrieved site matrix (" + size + " + wikis).");
        return wikis.toArray(new WMFWiki[size]);
    }

    /**
     *  Require the given extension be installed on this wiki, or throw an 
     *  UnsupportedOperationException if it isn't.
     *  @param extension the name of the extension to check
     *  @throws UnsupportedOperationException if that extension is not
     *  installed on this wiki
     *  @throws UncheckedIOException if the site info cache is not populated
     *  and a network error occurs when populating it
     *  @see Wiki#installedExtensions
     */
    public void requiresExtension(String extension)
    {
        if (!installedExtensions().contains(extension))
            throw new UnsupportedOperationException("Extension \"" + extension
                + "\" is not installed on " + getDomain() + ". "
                + "Please check the extension name and [[Special:Version]].");
    }
    
    /**
     *  Get the global usage for a file.
     * 
     *  @param title the title of the page (must contain "File:")
     *  @return the global usage of the file, including the wiki and page the file is used on
     *  @throws IOException if a network error occurs
     *  @throws IllegalArgumentException if <code>{@link Wiki#namespace(String) 
     *  namespace(title)} != {@link Wiki#FILE_NAMESPACE}</code>
     *  @throws UnsupportedOperationException if the GlobalUsage extension is 
     *  not installed
     *  @see <a href="https://mediawiki.org/wiki/Extension:GlobalUsage">Extension:GlobalUsage</a>
     */
    public String[][] getGlobalUsage(String title) throws IOException
    {
        requiresExtension("Global Usage");
    	if (namespace(title) != FILE_NAMESPACE)
            throw new IllegalArgumentException("Cannot retrieve Globalusage for pages other than File pages!");
        
    	Map<String, String> getparams = new HashMap<>();
        getparams.put("prop", "globalusage");
        getparams.put("titles", normalize(title));
    	
        List<String[]> usage = makeListQuery("gu", getparams, null, "getGlobalUsage", -1, (line, results) ->
        {
            for (int i = line.indexOf("<gu"); i > 0; i = line.indexOf("<gu", ++i))
                results.add(new String[] {
                    parseAttribute(line, "wiki", i),
                    parseAttribute(line, "title", i)
                });
        });

    	return usage.toArray(new String[0][0]);
    }
    
    /**
     *  Determines whether a site is on the spam blacklist, modulo Java/PHP 
     *  regex differences.
     *  @param site the site to check
     *  @return whether a site is on the spam blacklist
     *  @throws IOException if a network error occurs
     *  @throws UnsupportedOperationException if the SpamBlacklist extension
     *  is not installed
     *  @see <a href="https://mediawiki.org/wiki/Extension:SpamBlacklist">Extension:SpamBlacklist</a>
     */
    public boolean isSpamBlacklisted(String site) throws IOException
    {
        requiresExtension("SpamBlacklist");
        if (globalblacklist == null)
        {
            WMFWiki meta = createInstance("meta.wikimedia.org");
            globalblacklist = meta.getPageText("Spam blacklist");
        }
        if (localblacklist == null)
            localblacklist = getPageText("MediaWiki:Spam-blacklist");
        
        // yes, I know about the spam whitelist, but I primarily intend to use
        // this to check entire domains whereas the spam whitelist tends to 
        // contain individual pages on websites
        
        Stream<String> global = Arrays.stream(globalblacklist.split("\n"));
        Stream<String> local = Arrays.stream(localblacklist.split("\n"));
        
        return Stream.concat(global, local).map(str ->
        {
            if (str.contains("#"))
                return str.substring(0, str.indexOf('#'));
            else 
                return str;
        }).map(String::trim)
        .filter(str -> !str.isEmpty())
        .anyMatch(str -> site.matches(str));
    }
    
    /**
     *  Gets abuse log entries. Requires extension AbuseFilter. An abuse log 
     *  entry will have a set <var>id</var>, <var>target</var> set to the title
     *  of the page, <var>action</var> set to the action that was attempted (e.g.  
     *  "edit") and {@code null} (parsed)comment. <var>details</var> are a Map
     *  containing <var>filter_id</var>, <var>revid</var> if the edit was 
     *  successful and <var>result</var> (what happened). 
     * 
     *  <p>
     *  Accepted parameters from <var>helper</var> are:
     *  <ul>
     *  <li>{@link Wiki.RequestHelper#withinDateRange(OffsetDateTime, 
     *      OffsetDateTime) date range}
     *  <li>{@link Wiki.RequestHelper#byUser(String) user}
     *  <li>{@link Wiki.RequestHelper#byTitle(String) title}
     *  <li>{@link Wiki.RequestHelper#reverse(boolean) reverse}
     *  <li>{@link Wiki.RequestHelper#limitedTo(int) local query limit}
     *  </ul>
     *  
     *  @param filters fetch log entries triggered by these filters (optional, 
     *  use null or empty list to get all filters)
     *  @param helper a {@link Wiki.RequestHelper} (optional, use null to not
     *  provide any of the optional parameters noted above)
     *  @return the abuse filter log entries
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @throws UnsupportedOperationException if the AbuseFilter extension
     *  is not installed
     *  @see <a href="https://mediawiki.org/wiki/Extension:AbuseFilter">Extension:AbuseFilter</a>
     */
    public List<LogEntry> getAbuseLogEntries(int[] filters, Wiki.RequestHelper helper) throws IOException
    {
        requiresExtension("Abuse Filter");
        int limit = -1;
        Map<String, String> getparams = new HashMap<>();
        getparams.put("list", "abuselog");
        if (filters.length > 0)
            getparams.put("aflfilter", constructNamespaceString(filters));
        if (helper != null)
        {
            helper.setRequestType("afl");
            getparams.putAll(helper.addUserParameter());
            getparams.putAll(helper.addTitleParameter());
            getparams.putAll(helper.addReverseParameter());
            getparams.putAll(helper.addDateRangeParameters());
            limit = helper.limit();
        }
        
        List<LogEntry> filterlog = makeListQuery("afl", getparams, null, "WMFWiki.getAbuseLogEntries", limit, (line, results) ->
        {
            String[] items = line.split("<item ");
            for (int i = 1; i < items.length; i++)
            {
                long id = Long.parseLong(parseAttribute(items[i], "id", 0));
                OffsetDateTime timestamp = OffsetDateTime.parse(parseAttribute(items[i], "timestamp", 0));
                String loguser = parseAttribute(items[i], "user", 0);
                String action = parseAttribute(items[i], "action", 0);
                String target = parseAttribute(items[i], "title", 0);
                Map<String, Object> details = new HashMap<>();
                String revid = parseAttribute(items[i], "revid", 0); // may be null
                if (revid != null)
                    details.put("revid", Long.valueOf(revid));
                details.put("filter_id", Integer.valueOf(parseAttribute(items[i], "filter_id", 0)));
                details.put("result", parseAttribute(items[i], "result", 0));
                results.add(new LogEntry(id, timestamp, loguser, null, null, ABUSE_LOG, action, target, details));
            }
        });
        log(Level.INFO, "WMFWiki.getAbuselogEntries", "Sucessfully returned abuse filter log entries (" + filterlog.size() + " entries).");
        return filterlog;
    }
}
