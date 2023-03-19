/**
 *  @(#)WMFWikiFarm.java 0.01 28/08/2021
 *  Copyright (C) 2021-20XX MER-C and contributors
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
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.*;

/**
 *  Manages shared WMFWiki sessions and contains methods for dealing with WMF
 *  wikis in general.
 *  @author MER-C
 *  @since 0.01
 */
public class WMFWikiFarm
{
    private final HashMap<String, WMFWiki> sessions = new HashMap<>();
    private static final WMFWikiFarm SHARED_INSTANCE = new WMFWikiFarm();
    private Consumer<WMFWiki> setupfn;
    
    /**
     *  Computes the domain name (to use in {@link WMFWiki#newSession}) the 
     *  WMF wiki that has the given database name (e.g. "enwiki" for the English 
     *  Wikipedia, "nlwikisource"  for the Dutch Wikisource and "wikidatawiki" 
     *  for Wikidata).
     * 
     *  @param dbname a WMF wiki DB name
     *  @return the domain name for that wiki
     *  @see WMFWiki#newSession
     *  @throws IllegalArgumentException if the DB name is not recognized
     */
    public static String dbNameToDomainName(String dbname)
    {
        // special cases
        switch (dbname)
        {
            case "commonswiki":  return "commons.wikimedia.org";
            case "metawiki":     return "meta.wikimedia.org";
            case "wikidatawiki": return "www.wikidata.org";
        }
        
        // wiktionary
        int testindex = dbname.indexOf("wiktionary");
        if (testindex > 0)
            return dbname.substring(0, testindex) + ".wiktionary.org";
        
        testindex = dbname.indexOf("wiki");
        if (testindex > 0)
        {
            String langcode = dbname.substring(0, testindex);
            // most of these are Wikipedia
            if (dbname.endsWith("wiki"))
                return langcode + ".wikipedia.org";
            // Wikibooks, Wikinews, Wikiquote, Wikisource, Wikiversity, Wikivoyage
            return langcode + "." + dbname.substring(testindex) + ".org";
        }
        // Fishbowl/special wikis not implemented yet
        throw new IllegalArgumentException("Unrecognized wiki: " + dbname);
    }
    
    /**
     *  Returns a shared session manager. Note that multiple instances - i.e.
     *  multiple groups of sessions - are still permitted.
     *  @return a shared session manager
     */        
    public static WMFWikiFarm instance()
    {
        return SHARED_INSTANCE;
    }
    
    /**
     *  Returns the shared session for a given domain. If a session doesn't 
     *  exist, create it.
     *  @param domain a wiki domain
     *  @return the shared wiki session for that domain
     */
    public WMFWiki sharedSession(String domain)
    {
        if (sessions.containsKey(domain))
            return sessions.get(domain);
        WMFWiki wiki = WMFWiki.newSession(domain);
        if (setupfn != null)
            setupfn.accept(wiki);
        // if wikidata, wikidata.requiresExtension("WikibaseRepository");
        sessions.put(domain, wiki);
        return wiki;
    }
    
    /**
     *  Returns all shared sessions stored in this session manager.
     *  @return (see above)
     */
    public Collection<WMFWiki> getAllSharedSessions()
    {
        Set<WMFWiki> wikis = new HashSet<>();
        sessions.keySet().forEach(domain -> wikis.add(sessions.get(domain)));
        return wikis;
    }
    
    /**
     *  Sets a function that is called every time a WMFWiki session is created
     *  with this manager. The sole parameter is the new session. Use for a
     *  common setup routine.
     *  @param fn a function that is to be called on all new WMFWiki objects
     */
    public void setInitializer(Consumer<WMFWiki> fn)
    {
        setupfn = fn;
    }
    
    /**
     *  Fetches global user info. Returns:
     *  <ul>
     *  <li>home - (String) a String identifying the home wiki (e.g. "enwiki" 
     *      for the English Wikipedia)
     *  <li>registration - (OffsetDateTime) when the single account login was created
     *  <li>groups - (List&lt;String&gt;) this user is a member of these global 
     *      groups
     *  <li>rights - (List&lt;String&gt;) this user is explicitly granted the 
     *      ability to perform these actions globally
     *  <li>locked - (Boolean) whether this user account has been locked
     *  <li>editcount - (int) total global edit count
     *  <li>wikicount - (int) total number of wikis edited
     *  <li>DBNAME (e.g. "enwikisource" == "en.wikisource.org") - see below
     *  </ul>
     * 
     *  <p>
     *  For each wiki, a map is returned:
     *  <ul>
     *  <li>url - (String) the full URL of the wiki
     *  <li>groups - (List&lt;String&gt;) the local groups the user is in
     *  <li>editcount - (Integer) the local edit count
     *  <li>blocked - (Boolean) whether the user is blocked. Adds keys "blockexpiry"
     *      (OffsetDateTime) and "blockreason" (String) with obvious values. If the
     *      block is infinite, expiry is null.
     *  <li>registration - (OffsetDateTime) the local registration date
     *  </ul>
     * 
     *  @see #dbNameToDomainName
     *  @param username the username of the global user or null if the user does
     *  not exist. IPs are not allowed.
     *  @return user info as described above
     *  @throws IOException if a network error occurs
     *  @since WMFWiki 0.01
     */
    public Map<String, Object> getGlobalUserInfo(String username) throws IOException
    {
        // FIXME: throws UnknownError ("invaliduser" if user is an IP or is otherwise invalid
        // note: lock reason is not available, see https://phabricator.wikimedia.org/T331237
        WMFWiki wiki = sharedSession("meta.wikimedia.org");
        wiki.requiresExtension("CentralAuth");
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("meta", "globaluserinfo");
        getparams.put("guiprop", "groups|merged|unattached|rights");
        getparams.put("guiuser", wiki.normalize(username));
        String line = wiki.makeApiCall(getparams, null, "WMFWiki.getGlobalUserInfo");
        wiki.detectUncheckedErrors(line, null, null);
        if (line.contains("missing=\"\""))
            return null;
        
        // misc properties
        Map<String, Object> ret = new HashMap<>();
        ret.put("home", wiki.parseAttribute(line, "home", 0));
        String registrationdate = wiki.parseAttribute(line, "registration", 0);
        ret.put("registration", OffsetDateTime.parse(registrationdate));
        ret.put("locked", line.contains("locked=\"\""));
        int globaledits = 0;
        int wikicount = 0;
        
        // global groups/rights
        int mergedindex = line.indexOf("<merged>");
        List<String> globalgroups = new ArrayList<>();        
        int groupindex = line.indexOf("<groups");
        if (groupindex > 0 && groupindex < mergedindex)
        {
            for (int x = line.indexOf("<g>"); x > 0; x = line.indexOf("<g>", ++x))
            {
                int y = line.indexOf("</g>", x);
                globalgroups.add(line.substring(x + 3, y));
            }        
        }
        ret.put("groups", globalgroups);
        List<String> globalrights = new ArrayList<>();
        int rightsindex = line.indexOf("<rights");
        if (rightsindex > 0 && rightsindex < mergedindex)
        {
            for (int x = line.indexOf("<r>"); x > 0; x = line.indexOf("<r>", ++x))
            {
                int y = line.indexOf("</r>", x);
                globalrights.add(line.substring(x + 3, y));
            }        
        }
        ret.put("rights", globalrights);
        
        // individual wikis
        int mergedend = line.indexOf("</merged>");
        String[] accounts = line.substring(mergedindex, mergedend).split("<account ");
        for (int i = 1; i < accounts.length; i++)
        {
            Map<String, Object> userinfo = new HashMap<>();
            userinfo.put("url", wiki.parseAttribute(accounts[i], "url", 0));
            int editcount = Integer.parseInt(wiki.parseAttribute(accounts[i], "editcount", 0));
            globaledits += editcount;
            wikicount++;
            userinfo.put("editcount", editcount);
            
            registrationdate = wiki.parseAttribute(accounts[i], "registration", 0);
            OffsetDateTime registration = null;
            // TODO remove check when https://phabricator.wikimedia.org/T24097 is resolved
            if (registrationdate != null && !registrationdate.isEmpty())
                registration = OffsetDateTime.parse(registrationdate);
            userinfo.put("registration", registration);
            
            // blocked flag
            boolean blocked = accounts[i].contains("<blocked ");
            userinfo.put("blocked", blocked);
            if (blocked)
            {
                String expiry = wiki.parseAttribute(accounts[i], "expiry", 0);
                userinfo.put("blockexpiry", expiry.equals("infinity") ? null : OffsetDateTime.parse(expiry));
                userinfo.put("blockreason", wiki.parseAttribute(accounts[i], "reason", 0));
            }
            
            // local groups
            List<String> groups = new ArrayList<>();
            for (int x = accounts[i].indexOf("<group>"); x > 0; x = accounts[i].indexOf("<group>", ++x))
            {
                int y = accounts[i].indexOf("</group>", x);
                groups.add(accounts[i].substring(x + 7, y));
            }
            userinfo.put("groups", groups);
            ret.put(wiki.parseAttribute(accounts[i], "wiki", 0), userinfo);
        }
        ret.put("editcount", globaledits);
        ret.put("wikicount", wikicount);
        return ret;
    }
    
    /**
     *  Returns the list of publicly readable and editable wikis operated by the
     *  Wikimedia Foundation.
     *  @return (see above)
     *  @throws IOException if a network error occurs
     *  @since WMFWiki 0.01
     */
    public List<WMFWiki> getSiteMatrix() throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "sitematrix");
        WMFWiki meta = sharedSession("meta.wikimedia.org");
        String line = meta.makeApiCall(getparams, null, "WMFWikiFarm.getSiteMatrix");
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
            wikis.add(sharedSession(line.substring(a, b)));
        }
        int size = wikis.size();
        Logger temp = Logger.getLogger("wiki");
        temp.log(Level.INFO, "WMFWikiFarm.getSiteMatrix", "Successfully retrieved site matrix (" + size + " + wikis).");
        return wikis;
    }
    
    /**
     *  Returns the Wikidata items corresponding to the given titles.
     *  @param wiki the wiki where the titles are hosted
     *  @param titles a list of page names
     *  @return the corresponding Wikidata items, or null if either the Wikidata
     *  item or the local article doesn't exist
     *  @throws IOException if a network error occurs
     */
    public List<String> getWikidataItems(WMFWiki wiki, List<String> titles) throws IOException
    {
        String dbname = (String)wiki.getSiteInfo().get("dbname");
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "wbgetentities");
        getparams.put("sites", dbname);
                
        Map<String, String> results = new HashMap<>();
        WMFWiki wikidata_l = sharedSession("www.wikidata.org");
        for (String chunk : wikidata_l.constructTitleString(titles))
        {
            String line = wikidata_l.makeApiCall(getparams, Map.of("titles", chunk), "getWikidataItem");
            wikidata_l.detectUncheckedErrors(line, null, null);
            String[] entities = line.split("<entity ");
            for (int i = 1; i < entities.length; i++)
            {
                if (entities[i].contains("missing=\"\""))
                    continue;
                String wdtitle = wikidata_l.parseAttribute(entities[i], " id", 0);
                int index = entities[i].indexOf("\"" + dbname + "\"");
                String localtitle = wikidata_l.parseAttribute(entities[i], "title", index);
                results.put(localtitle, wdtitle);
            }
        }
        List<String> ret = wikidata_l.reorder(titles, results);
        wikidata_l.log(Level.INFO, "WMFWikiFarm.getWikidataItems", 
            "Successfully retrieved Wikidata items for " + titles.size() + " pages.");
        return ret;
    }
}
