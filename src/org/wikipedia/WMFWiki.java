/**
 *  @(#)WMFWiki.java 0.02 28/08/2021
 *  Copyright (C) 2011 - 20xx MER-C and contributors
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
import javax.security.auth.login.*;

/**
 *  Stuff specific to Wikimedia wikis.
 *  @author MER-C
 *  @version 0.02
 */
public class WMFWiki extends Wiki
{
    // caches
    private static String globalblacklist;
    private String localblacklist;
    
    // Shared sessions
    private static WMFWiki metawiki;
    
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
     *  Denotes the global auth log (contains (un)lock/(un)hide actions). 
     *  Requires extension CentralAuth.
     *  @see Wiki#getLogEntries
     */
    public static final String GLOBAL_AUTH_LOG = "globalauth";
    
    /**
     *  Denotes the global (IP) block log. Requires extension GlobalBlocking.
     *  @see Wiki#getLogEntries
     */
    public static final String GLOBAL_BLOCK_LOG = "gblblock";

    /**
     *  Creates a new MediaWiki API client for the WMF wiki that has the given 
     *  domain name.
     *  @param domain a WMF wiki domain name e.g. en.wikipedia.org
     */
    protected WMFWiki(String domain)
    {
        super(domain, "/w", "https://");
    }
    
    /**
     *  Creates a new MediaWiki API client for the WMF wiki that has the given 
     *  domain name.
     *  @param domain a WMF wiki domain name e.g. en.wikipedia.org
     *  @return the constructed API client object
     */
    public static WMFWiki newSession(String domain)
    {
        WMFWiki wiki = new WMFWiki(domain);
        wiki.initVars();
        return wiki;
    }
    
    /**
     *  Returns the shared reference to MetaWiki. This instance is modifiable.
     *  @see <a href="https://meta.wikimedia.org">Main Page</a>
     *  @return (see above)
     */
    public static WMFWiki sharedMetaWikiSession()
    {
        if (metawiki == null)
        {
            metawiki = newSession("meta.wikimedia.org");
            metawiki.requiresExtension("SiteMatrix"); // getSiteMatrix
        }
        return metawiki;
    }
    
    /**
     *  Returns the list of publicly readable and editable wikis operated by the
     *  Wikimedia Foundation.
     *  @return (see above)
     *  @throws IOException if a network error occurs
     */
    public static List<WMFWiki> getSiteMatrix() throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "sitematrix");
        WMFWiki meta = sharedMetaWikiSession();
        String line = meta.makeApiCall(getparams, null, "WMFWiki.getSiteMatrix");
        meta.detectUncheckedErrors(line, null, null);
        List<WMFWiki> wikis = new ArrayList<>(1000);

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
            wikis.add(newSession(line.substring(a, b)));
        }
        int size = wikis.size();
        Logger temp = Logger.getLogger("wiki");
        temp.log(Level.INFO, "WMFWiki.getSiteMatrix", "Successfully retrieved site matrix (" + size + " + wikis).");
        return wikis;
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
    public List<String[]> getGlobalUsage(String title) throws IOException
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

    	return usage;
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
            globalblacklist = sharedMetaWikiSession().getPageText(List.of("Spam blacklist")).get(0);
        if (localblacklist == null)
            localblacklist = getPageText(List.of("MediaWiki:Spam-blacklist")).get(0);
        
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
                Map<String, String> details = new HashMap<>();
                details.put("revid", parseAttribute(items[i], "revid", 0));
                details.put("filter_id", parseAttribute(items[i], "filter_id", 0));
                details.put("result", parseAttribute(items[i], "result", 0));
                results.add(new LogEntry(id, timestamp, loguser, null, null, ABUSE_LOG, action, target, details));
            }
        });
        log(Level.INFO, "WMFWiki.getAbuselogEntries", "Sucessfully returned abuse filter log entries (" + filterlog.size() + " entries).");
        return filterlog;
    }
    
    /**
     *  Renders the ledes of articles (everything in the first section) as plain 
     *  text. Requires extension CirrusSearch. Output order is the same as input
     *  order, and a missing page will correspond to {@code null}. The returned  
     *  plain text has the following characteristics:
     *  
     *  <ul>
     *  <li>There are no paragraph breaks or new lines.
     *  <li>HTML block elements (e.g. &lt;blockquote&gt;), image captions, 
     *      tables, reference markers, references and external links with no
     *      description are all removed.
     *  <li>Templates are evaluated. If a template yields inline text (as opposed
     *      to a table or image) the result is not removed. That means navboxes
     *      and hatnotes are removed, but inline warnings such as [citation 
     *      needed] are not.
     *  <li>The text of list items is not removed, but bullet points are.
     *  </ul>
     * 
     *  @param titles a list of pages to get plain text for
     *  @return the first section of those pages, as plain text
     *  @throws IOException if a network error occurs
     *  @see #getPlainText(List) 
     */
    public List<String> getLedeAsPlainText(List<String> titles) throws IOException
    {
        requiresExtension("CirrusSearch");
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("prop", "cirrusbuilddoc"); // slightly shorter output than cirrusdoc
        
        List<List<String>> temp = makeVectorizedQuery("cb", getparams, titles, 
            "getLedeAsPlainText", -1, (text, result) -> 
        {
            result.add(parseAttribute(text, "opening_text", 0));
        });
        // mapping is one to one, so should flatten each entry
        List<String> ret = new ArrayList<>();
        for (List<String> item : temp)
            ret.add(item.get(0));
        return ret;
    }
    
    /**
     *  Renders articles as plain text. Requires extension CirrusSearch. Output 
     *  order is the same as input order, and a missing page will correspond to 
     *  {@code null}.The returned plain text has the following characteristics
     *  additional to those mentioned in {@link #getLedeAsPlainText(List)}:
     *  
     *  <ul>
     *  <li>There are no sections or section headers.
     *  <li>References are present at the end of the text, regardless of
     *      how they were formatted in wikitext.
     *  <li>Some small &lt;div&gt; templates still remain.
     *  <li>See also and external link lists remain (but the external links
     *      themselves are removed).
     *  </ul>
     * 
     *  @param titles a list of pages to get plain text for
     *  @return those pages rendered as plain text
     *  @throws IOException if a network error occurs
     *  @see #getLedeAsPlainText(List)
     */
    public List<String> getPlainText(List<String> titles) throws IOException
    {
        requiresExtension("CirrusSearch");
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("prop", "cirrusbuilddoc"); // slightly shorter output than cirrusdoc
        
        List<List<String>> temp = makeVectorizedQuery("cb", getparams, titles, 
            "getPlainText", -1, (text, result) -> 
        {
            result.add(parseAttribute(text, " text", 0)); // not to be confused with "opening_text" etc.
        });
        // mapping is one to one, so should flatten each entry
        List<String> ret = new ArrayList<>();
        for (List<String> item : temp)
            ret.add(item.get(0));
        return ret;
    }
    
    /**
     *  Returns the Wikidata items corresponding to the given titles.
     *  @param titles a list of page names
     *  @return the corresponding Wikidata items, or null if either the Wikidata
     *  item or the local article doesn't exist
     *  @throws IOException if a network error occurs
     */
    public List<String> getWikidataItems(List<String> titles) throws IOException
    {
        String dbname = (String)getSiteInfo().get("dbname");
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "wbgetentities");
        getparams.put("sites", dbname);
        
        // WORKAROUND: this module doesn't accept mixed GET/POST requests
        // often need to slice up titles into smaller chunks than slowmax (here 25)
        // FIXME: replace with constructTitleString when Wikidata is behaving correctly
        TreeSet<String> ts = new TreeSet<>();
        for (String title : titles)
            ts.add(normalize(title));
        List<String> titles_enc = new ArrayList<>(ts);
        ArrayList<String> titles_chunked = new ArrayList<>();
        int count = 0;
        do
        {
            titles_chunked.add(String.join("|", 
                titles_enc.subList(count, Math.min(titles_enc.size(), count + 25))));
            count += 25;
        }
        while (count < titles_enc.size());
        
        Map<String, String> results = new HashMap<>();
        WMFWiki wikidata_l = WMFWiki.newSession("www.wikidata.org");
        for (String chunk : titles_chunked)
        {
            getparams.put("titles", chunk);
            String line = wikidata_l.makeApiCall(getparams, null, "getWikidataItem");
            wikidata_l.detectUncheckedErrors(line, null, null);
            String[] entities = line.split("<entity ");
            for (int i = 1; i < entities.length; i++)
            {
                if (entities[i].contains("missing=\"\""))
                    continue;
                String wdtitle = parseAttribute(entities[i], " id", 0);
                int index = entities[i].indexOf("\"" + dbname + "\"");
                String localtitle = parseAttribute(entities[i], "title", index);
                results.put(localtitle, wdtitle);
            }
        }
        // reorder
        List<String> ret = new ArrayList<>();
        for (String title : titles)
            ret.add(results.get(normalize(title)));
        return ret;
    }
    
    /**
     *  Patrols or unpatrols new pages using the PageTriage extension. If a page
     *  is not in the queue, then this method adds the page to the PageTriage
     *  queue, which also unpatrols it.
     * 
     *  @param pageid the page to (un)patrol
     *  @param reason the reason for (un)patrolling the page for the log
     *  @param patrol true to patrol, false to unpatrol
     *  @param skipnotif does not send a notification to the author when the page
     *  is (un)patrolled, ignored for old pages
     *  @throws IOException if a network error occurs
     *  @throws SecurityException if one does not have the rights to patrol pages
     *  @throws CredentialExpiredException if cookies have expired
     *  @throws AccountLockedException if user is blocked
     *  @throws IllegalArgumentException if the page is not in a namespace where
     *  PageTriage is enabled.
     *  @since 0.02
     *  @see <a href="https://en.wikipedia.org/wiki/Wikipedia:Page_Curation">Extension
     *  documentation</a>
     */
    public void triageNewPage(long pageid, String reason, boolean patrol, boolean skipnotif) throws IOException, LoginException
    {
        pageTriageAction(pageid, reason, patrol, skipnotif);
    }
    
    /**
     *  Internal method for handling PageTriage actions.
     *  @param pageid the page to (un)patrol or add to the queue
     *  @param reason the reason for (un)patrolling the page
     *  @param patrol whether to (un)patrol the page, or null to add it to the queue
     *  @param skipnotif do not notify the article author. Only applicable if 
     *  {@code patrol != null}.
     *  @throws IOException if a network error occurs
     *  @throws SecurityException if one does not have the rights to patrol pages
     *  @throws CredentialExpiredException if cookies have expired
     *  @throws AccountLockedException if user is blocked
     *  @throws IllegalArgumentException if the page is not in a namespace where
     *  PageTriage is enabled.
     *  @since 0.02
     *  @see <a href="https://en.wikipedia.org/wiki/Wikipedia:Page_Curation">Extension
     *  documentation</a>
     *  @see <a href="https://en.wikipedia.org/w/api.php?action=help&modules=pagetriageaction">API
     *  documentation</a>
     */
    protected synchronized void pageTriageAction(long pageid, String reason, Boolean patrol, boolean skipnotif) throws IOException, LoginException
    {
        requiresExtension("PageTriage");
        checkPermissions("patrol", "patrol");
        throttle();
        
        Map<String, Object> params = new HashMap<>();
        params.put("action", "pagetriageaction");
        params.put("pageid", pageid);
        params.put("token", getToken("csrf"));
        if (reason != null)
            params.put("note", reason);
        if (patrol != null)
        {
            params.put("reviewed", patrol ? "1" : "0");
            if (skipnotif)
                params.put("skipnotif", "1");
        }
        else
            params.put("enqueue", "1");
        
        String response = makeApiCall(new HashMap<>(), params, "pageTriageAction");
        // Unfortunately there is no way to tell whether a particular page is
        // in a PageTriage queue, so we are left with this.
        // Enqueue pages that aren't in the queue when unpatrol requested.
        if (response.contains("<error code=\"bad-pagetriage-page\"") && Boolean.FALSE.equals(patrol))
            pageTriageAction(pageid, reason, null, false);
        checkErrorsAndUpdateStatus(response, "pageTriageAction", Map.of(
            "bad-pagetriage-enqueue-invalidnamespace", desc -> 
                new IllegalArgumentException("Cannot (un)patrol page, PageTriage is not enabled for this namespace.")), null);
        log(Level.INFO, "pageTriageAction", "Successfully (un)patrolled page " + pageid);
    }
}
