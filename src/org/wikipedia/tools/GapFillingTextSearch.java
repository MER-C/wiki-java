/**
 *  @(#)GapFillingTextSearch.java 0.01 10/06/2018
 *  Copyright (C) 2018-20XX MER-C and contributors
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

package org.wikipedia.tools;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import org.wikipedia.*;

/**
 *  Fills in a couple of missing features in Wikimedia's search engine, in
 *  particular searching the list of articles created by a user, searching a 
 *  predefined list of articles or pages linked from a given page.
 * 
 *  @see <a href="https://www.mediawiki.org/wiki/Help:CirrusSearch">Documentation 
 *  of CirrusSearch, the search engine used by the WMF</a>
 *  @author MER-C
 *  @version 0.01
 */
public class GapFillingTextSearch
{
    // TODO:
    // 1) Servlet version
    // 2) Come to think about it, this class is a logical place to put string 
    // contains/regex find and replacement methods.
    
    private final Wiki wiki;
    
    /**
     *  Runs this program.
     *  @param args the command line arguments
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws Exception
    {
        Map<String, String> parsedargs = new CommandLineParser()
            .synopsis("org.wikipedia.tools.SearchArticlesCreated", "[options] query")
            .addSingleArgumentFlag("--wiki", "example.org", "The wiki to fetch data from (default: en.wikipedia.org)")
            .addSingleArgumentFlag("--user", "Username", "Search articles created by Username")
            .addSingleArgumentFlag("--linksfrom", "wikipage", "The wiki page to get links from")
            .addBooleanFlag("--regex", "Treat the query as a regular expression")
            .addBooleanFlag("--case-sensitive", "Treat the query as case sensitive")
            .parse(args);
        Wiki thiswiki = Wiki.createInstance(parsedargs.getOrDefault("--wiki", "en.wikipedia.org"));
        boolean regex = parsedargs.containsKey("--regex");
        boolean casesensitive = parsedargs.containsKey("--case-sensitive");
        GapFillingTextSearch gfs = new GapFillingTextSearch(thiswiki);
        
        String query = parsedargs.get("default");
        if (query == null)
        {
            System.out.println("No query specified!");
            System.exit(0);
        }
        
        String user = parsedargs.get("--user");
        String linksfrom = parsedargs.get("--linksfrom");
        Map<?, String> inputs = null;
        if (user != null)
            inputs = gfs.fetchArticlesCreatedBy(user);
        else if (linksfrom != null)
        {
            HashMap<String, String> temp = new HashMap<>();
            String[] links = thiswiki.getLinksOnPage(linksfrom);
            String[] content = thiswiki.getPageText(links);
            for (int i = 0; i < links.length; i++)
                if (content[i] != null)
                    temp.put(links[i], content[i]);
            inputs = temp;
        }
        if (inputs == null)
        {
            System.out.println("No list of pages to search specified!");
            System.exit(0);
        }
        
        Map<?, String> results;
        if (regex)
        {
            Pattern pattern = casesensitive ? Pattern.compile(query) : Pattern.compile(query, Pattern.CASE_INSENSITIVE);
            results = gfs.regexMatchAndExtractSnippets(inputs, pattern);
        }
        else
            results = gfs.searchAndExtractSnippets(inputs, query, casesensitive);
         
        results.forEach((revision, snippet) ->
        {
            System.out.println(revision);
            System.out.println("\t" + snippet + "\n");
        });
    }
    
    /**
     *  Creates a new instance of this tool.
     *  @param wiki the wiki to fetch data from
     */
    public GapFillingTextSearch(Wiki wiki)
    {
        this.wiki = wiki;
    }
    
    /**
     *  Returns the wiki that this tool fetches data from.
     *  @return (see above)
     */
    public Wiki getWiki()
    {
        return wiki;
    }
    
    /**
     *  Fetches the articles created by the given user and the text of the 
     *  current revision of those pages.
     *  @param user the user to fetch articles for
     *  @return a map containing revision the article was created -> 
     *  current text of that page
     *  @throws IOException if a network error occurs
     */
    public Map<Wiki.Revision, String> fetchArticlesCreatedBy(String user) throws IOException
    {
        Map<String, Boolean> options = new HashMap<>();
        options.put("new", Boolean.TRUE);
        Wiki.RequestHelper rh = wiki.new RequestHelper().inNamespaces(Wiki.MAIN_NAMESPACE);
        List<Wiki.Revision> contribs = wiki.contribs(Arrays.asList(user), null, rh, options).get(0);
        
        // get text of all articles
        List<String> articles = new ArrayList<>();
        for (Wiki.Revision revision : contribs)
            articles.add(revision.getTitle());
        String[] pagetexts = wiki.getPageText(articles.toArray(new String[0]));        
        Map<Wiki.Revision, String> ret = new HashMap<>();
        for (int i = 0; i < contribs.size(); i++)
        {
            Wiki.Revision revision = contribs.get(i);
            ret.putIfAbsent(revision, pagetexts[i]);
        }
        return ret;
    }
    
    /**
     *  Searches for the given <var>query</var> term by performing a string 
     *  contains operation on the text contained in each entry of the search 
     *  data and then extracts a snippet surrounding the first match.  
     *  <var>searchdata</var> is of the form uniqueID -> page text associated 
     *  with that uniqueID and can be the return value of {@link 
     *  #fetchArticlesCreatedBy(String)}.
     * 
     *  @param <IDType> the type of uniqueID
     *  @param searchdata a map of uniqueID -> article text associated with that
     *  uniqueID
     *  @param query the search term to look for
     *  @param casesensitive whether the search should be case sensitive
     *  @return a map: uniqueID -> snippet
     */
    public <IDType> Map<IDType, String> searchAndExtractSnippets(Map<IDType, String> searchdata, String query, 
        boolean casesensitive)
    {
        String matcher = casesensitive ? query : query.toLowerCase();
        Map<IDType, String> ret = new HashMap<>();
        
        for (Map.Entry<IDType, String> entry : searchdata.entrySet())
        {
            IDType uniqueID = entry.getKey();
            String pagetext = entry.getValue();
        
            if (casesensitive)
            {
                if (pagetext.contains(matcher))
                    ret.putIfAbsent(uniqueID, extractSnippet(pagetext, pagetext.indexOf(matcher)));
            }
            else
            {
                String pagetextlower = pagetext.toLowerCase();
                if (pagetextlower.contains(matcher))
                    ret.putIfAbsent(uniqueID, extractSnippet(pagetext, pagetextlower.indexOf(matcher)));
            }                
        }
        return ret;
    }
    
    /**
     *  Performs a regex match on the text contained in each entry of the 
     *  search data and then extracts a snippet surrounding the first match.  
     *  <var>searchdata</var> is of the form uniqueID -> page text associated 
     *  with that uniqueID and can be the return value of {@link 
     *  #fetchArticlesCreatedBy(String)}.
     * 
     *  @param <IDType> the type of uniqueID
     *  @param searchdata a map of uniqueID -> article text associated with that
     *  uniqueID
     *  @param pattern the regex pattern to match to
     *  @return a map: uniqueID -> snippet
     */
    public <IDType> Map<IDType, String> regexMatchAndExtractSnippets(Map<IDType, String> searchdata, Pattern pattern)
    {
        Map<IDType, String> ret = new HashMap<>();
        for (Map.Entry<IDType, String> entry : searchdata.entrySet())
        {
            IDType revision = entry.getKey();
            String pagetext = entry.getValue();
            
            Matcher matcher = pattern.matcher(pagetext);
            if (matcher.find())
               ret.putIfAbsent(revision, extractSnippet(pagetext, matcher.start()));
        }
        return ret;
    }
    
    /**
     *  Extracts a 30 word snippet from <var>text</var> surrounding
     *  <var>indexofmatch</var>.
     *  @param text the text to extract a snippet for
     *  @param indexofmatch where to extract the snippet from
     *  @return the 30 word snippet
     *  @throws StringIndexOutOfBoundsException 
     */
    public String extractSnippet(String text, int indexofmatch)
    {
        if (text.length() <= indexofmatch || indexofmatch < 0)
            throw new StringIndexOutOfBoundsException("indexofmatch (" + indexofmatch 
                + ") does not fit within the supplied string (length = " + text.length() + ")");
        String[] words = text.split("\\s");
        int size = 0;
        for (int i = 0; i < words.length; i++)
        {
            size += (words[i].length() + 1);
            if (indexofmatch < size)
            {
                // extract the snippet
                String[] subarray = Arrays.copyOfRange(words, Math.max(0, i - 15), Math.min(i + 15, words.length - 1));
                return String.join(" ", subarray);
            }
        }
        throw new AssertionError("Unreachable.");
    }
}
