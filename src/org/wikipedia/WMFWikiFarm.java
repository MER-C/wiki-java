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

/**
 *  Manages shared WMFWiki sessions and contains methods for dealing with WMF
 *  wikis in general.
 *  @author MER-C
 *  @since 0.01
 */
public class WMFWikiFarm
{
    private final HashMap<String, WMFWiki> sessions = new HashMap<>();
    
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
        // if wikidata, wikidata.requiresExtension("WikibaseRepository");
        sessions.put(domain, wiki);
        return wiki;
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
     *  @param username the username of the global user. IPs and non-existing users
     *  are not allowed.
     *  @return user info as described above
     *  @throws IOException if a network error occurs
     */
    public Map<String, Object> getGlobalUserInfo(String username) throws IOException
    {
        // fixme(?): throws UnknownError ("invaliduser" if user is an IP, doesn't exist
        // or otherwise is invalid
        WMFWiki wiki = sharedSession("meta.wikimedia.org");
        wiki.requiresExtension("CentralAuth");
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("meta", "globaluserinfo");
        getparams.put("guiprop", "groups|merged|unattached|rights");
        getparams.put("guiuser", wiki.normalize(username));
        String line = wiki.makeApiCall(getparams, null, "WMFWiki.getGlobalUserInfo");
        
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
}
