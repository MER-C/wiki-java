/**
 *  @(#)Pages.java 0.01 31/03/2018
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
package org.wikipedia;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;
import javax.security.auth.login.*;

/**
 *  Utility methods for lists of wiki pages.
 *  @author MER-C
 *  @version 0.01
 */
public class Pages 
{
    private final Wiki wiki;
    
    /**
     *  A function, when supplied to {@link #toWikitextList(Iterable, Function, 
     *  boolean)}, transforms a {@code List} of pages into a list of links in
     *  wikitext.
     *  @see #toWikitextList(Iterable, Function, boolean)
     */
    public static final Function<String, String> LIST_OF_LINKS = title -> "[[:" + title + "]]";
    
    private Pages(Wiki wiki)
    {
        this.wiki = wiki;
    }
    
    /**
     *  Creates an instance of this class bound to a particular wiki (required
     *  for methods that make network requests to a wiki).
     * 
     *  @param wiki the wiki to bind to
     *  @return an instance of this utility class that is bound to that wiki
     */
    public static Pages of(Wiki wiki)
    {
        return new Pages(wiki);
    }
    
    /**
     *  Parses a wikitext list of links into its individual elements. Such a 
     *  list should be in the form:
     *
     *  <pre>
     *  * [[Main Page]]
     *  * [[Wikipedia:Featured picture candidates]]
     *  * [[:File:Example.png]]
     *  * [[Cape Town#Economy]]
     *  * [[Link|with description]]
     *  </pre>
     *
     *  in which case <samp>{ "Main Page", "Wikipedia:Featured picture
     *  candidates", "File:Example.png", "Cape Town#Economy", "Link" }</samp> is
     *  the return value. Numbered lists are allowed. Nested lists are 
     *  flattened. Link descriptions are removed.
     *
     *  @param wikitext a wikitext list of pages as described above
     *  @see #toWikitextList(Iterable, Function, boolean)
     *  @return a list of parsed titles
     *  @since Wiki.java 0.11
     */
    public static List<String> parseWikitextList(String wikitext)
    {
        String[] lines = wikitext.split("\n");
        List<String> titles = new ArrayList<>();
        for (String line : lines)
        {
            int wikilinkstart = line.indexOf("[[");
            int wikilinkend = line.indexOf("]]");
            if (wikilinkstart < 0 || wikilinkend < 0)
                continue;
            titles.add(WikitextUtils.parseWikilink(line.substring(wikilinkstart, wikilinkend + 2)).get(0));
        }
        return titles;
    }

    /**
     *  Exports a list of pages, say, generated from one of the query methods to
     *  wikitext. When supplied with {@link #LIST_OF_LINKS}, this method does 
     *  the exact opposite of {@link #parseWikitextList(String)}, i.e. {@code
     *  { "Main Page", "Wikipedia:Featured picture candidates", "File:Example.png" }}
     *  becomes the string:
     *
     *  <pre>
     *  *[[:Main Page]]
     *  *[[:Wikipedia:Featured picture candidates]]
     *  *[[:File:Example.png]]
     *  </pre>
     * 
     *  <p>
     *  If a <var>numbered</var> list is desired, the output is:
     * 
     *  <pre>
     *  #[[:Main Page]]
     *  #[[:Wikipedia:Featured picture candidates]]
     *  #[[:File:Example.png]]
     *  </pre>
     * 
     *  <p>
     *  The generator function may be used, for instance, to <a  
     *  href="https://en.wikipedia.org/wiki/Category:Pagelinks_templates"> supply 
     *  a different template depending on namespace</a>, to insert other 
     *  template arguments or add custom wikilink descriptions.
     *
     *  @param pages a list of page titles
     *  @param generator a generator of wikitext given a particular title
     *  @param numbered whether this is a numbered list
     *  @return the list, exported as wikitext
     *  @see #parseWikitextList(String)
     *  @since Wiki.java 0.14
     */
    public static String toWikitextList(Iterable<String> pages, Function<String, String> generator, boolean numbered)
    {
        StringBuilder buffer = new StringBuilder(10000);
        for (String page : pages)
        {
            buffer.append(numbered ? "#" : "*");
            buffer.append(generator.apply(page));
            buffer.append("\n");
        }
        return buffer.toString();
    }
    
    /**
     *  Exports a list of pages, say, generated from one of the query methods to
     *  wikitext, where each page is the single argument of the given 
     *  <var>template</var>. For example: {@code { "Main Page",
     *  "Wikipedia:Featured picture candidates", "File:Example.png" }} becomes 
     *  the string:
     *
     *  <pre>
     *  *{{template|1=Main Page}}
     *  *{{template|1=Wikipedia:Featured picture candidates}}
     *  *{{template|File:Example.png}}
     *  </pre>
     * 
     *  If a <var>numbered</var> list is desired, the output is:
     * 
     *  <pre>
     *  #{{template|1=Main Page}}
     *  #{{template|1=Wikipedia:Featured picture candidates}}
     *  #{{template|1=File:Example.png}}
     *  </pre>
     *
     *  @param pages a list of page titles
     *  @param template the template of which the input titles are the first and 
     *  only argument
     *  @param numbered whether this is a numbered list
     *  @return the list, exported as wikitext
     */
    public static String toWikitextTemplateList(Iterable<String> pages, String template, boolean numbered)
    {
        return toWikitextList(pages, page -> "{{" + template + "|1=" + page + "}}", numbered);
    }
    
    /**
     *  Exports a list of pages, say, generated from one of the query methods to
     *  wikitext. This variant runs a pagination function that inserts text at 
     *  the start of each segment of <var>pagespersegment</var> pages in order 
     *  to, for instance, generates a section header every so many entries in the 
     *  list. Note this method returns a list of wikitext, one entry for each 
     *  segment.
     * 
     *  @param pages a list of page titles
     *  @param generator a generator of wikitext given a particular title
     *  @param paginator a function that takes the index of the first and last
     *  pages and returns a String to be inserted into the wikitext before the 
     *  first article
     *  @param pagespersegment how many pages between instances of the paginator
     *  text
     *  @param numbered whether this is a numbered list
     *  @throws IllegalArgumentException if there is less than one page per insertion
     *  @return the list of pages, broken up into chunks of pagespersection
     *  @since ContributionSurveyor 0.04
     */
    public static List<String> toWikitextPaginatedList(Collection<String> pages, Function<String, String> generator, 
        BiFunction<Integer, Integer, String> paginator, int pagespersegment, boolean numbered)
    {
        if (pagespersegment < 1)
            throw new IllegalArgumentException("There must be at least one page per section.");
        if (pages.isEmpty())
            return Collections.EMPTY_LIST;
        
        List<String> ret = new ArrayList<>();
        StringBuilder out = new StringBuilder(10000);
        int max = pages.size();
        int counter = 1;
        for (String page : pages)
        {
            // output page header
            if (counter % pagespersegment == 1)
            {
                if (counter != 1)
                {
                    out.append("\n");
                    ret.add(out.toString());
                    out.setLength(0);
                }
                int sectionmax = Math.min(max, counter + pagespersegment - 1);
                out.append(paginator.apply(counter, sectionmax));
                out.append("\n");
            }
            out.append(numbered ? "#" : "*");
            out.append(generator.apply(page));
            out.append("\n");
            counter++;
        }
        out.append("\n");
        ret.add(out.toString());
        return ret;
    }

    /**
     *  Given a list of templates, fetch the only argument of the given template.
     *  This is deliberately simple because we don't want to wade into the mess  
     *  that is parsing metatemplates for this common use case. For instance:
     *  <kbd>{{user|A}} {{user|B}} {{user|C}}</kbd> becomes the list {@code 
     *  {"A", "B", "C"}}. This can be used to reverse {@link 
     *  toWikitextTemplateList(Iterable, String, boolean)}.
     *
     *  @param wikitext the wikitext to parse
     *  @param template the template to look for
     *  @return the list of arguments, assuming each template has a single argument
     */
    public static List<String> parseWikitextTemplateList(String wikitext, String template)
    {
        Pattern pattern = Pattern.compile("\\{\\{\\s*" + template + "\\s*", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(wikitext);
        List<String> arguments = new ArrayList<>();
        while (matcher.find())
        {
            int index = matcher.start();
            int start = wikitext.indexOf("|", index) + 1;
            int end = wikitext.indexOf("}}", index);
            int arg = wikitext.indexOf("=", index) + 1;
            if (arg < end)
                start = Math.max(start, arg);
            if (start >= 1 && start < end)
                arguments.add(wikitext.substring(start, end).trim());
            else if (start == 0)
                arguments.add("");
        }
        return arguments;
    }

    
    /**
     *  For a given list of pages, determine whether the supplied external links
     *  are present in the page.
     *  @param data a Map of title &#8594; list of links to check
     *  @return a Map of title &#8594; link checked &#8594; whether it is in 
     *  that page
     *  @throws IOException if a network error occurs
     */
    public Map<String, Map<String, Boolean>> containExternalLinks(Map<String, List<String>> data) throws IOException
    {
        List<List<String>> pagelinks = wiki.getExternalLinksOnPage(new ArrayList<>(data.keySet()));
        int counter = 0;
        Map<String, Map<String, Boolean>> ret = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : data.entrySet())
        {        
            List<String> addedlinks = entry.getValue();
            List<String> currentlinks = pagelinks.get(counter);
            Map<String, Boolean> stillthere = new HashMap<>();
            for (int i = 0; i < addedlinks.size(); i++)
            {
                String url = addedlinks.get(i);
                stillthere.put(url, currentlinks.contains(url));
            }
            ret.put(entry.getKey(), stillthere);
            counter++;
        }
        return ret;
    }
    
    /**
     *  Outputs a hyperlink to the given page. It is assumed the page exists.
     *  @param page a page to make a link for
     *  @return HTML for that link
     */
    public String generatePageLink(String page)
    {
        return generatePageLink(page, WikitextUtils.recode(page), true);
    }
    
    /**
     *  Outputs a hyperlink to the given page. Tags on a CSS class for red links
     *  if applicable.
     *  @param page a page to make a link for
     *  @param exists whether that page exists
     *  @return HTML for that link
     */
    public String generatePageLink(String page, boolean exists)
    {
        return generatePageLink(page, WikitextUtils.recode(page), exists);
    }

    /**
     *  Outputs a hyperlink to the given page. It is assumed the page exists.
     *  @param page a page to make a link for
     *  @param text the text for that link
     *  @return HTML for that link
     */    
    public String generatePageLink(String page, String text)
    {
        return generatePageLink(page, text, true);
    }

    /**
     *  Outputs a hyperlink to the given page. Tags on a CSS class for red links
     *  if applicable.
     *  @param page a page to make a link for
     *  @param text the text for that link
     *  @param exists whether that page exists
     *  @return HTML for that link
     */    
    public String generatePageLink(String page, String text, boolean exists)
    {
        // Implementation note: class name chosen to equal MediaWiki
        StringBuilder sb = new StringBuilder("<a href=\"");
        sb.append(wiki.getPageUrl(page));
        sb.append("\"");
        sb.append(exists ? ">" : " class=\"new\">");
        sb.append(text);
        sb.append("</a>");
        return sb.toString();
    }
    
    /**
     *  Generates summary page links of the form Page (edit | talk | history | logs)
     *  as HTML. Doesn't support talk pages yet.
     *  @param page the page to generate links for
     *  @return generated HTML
     */
    public String generateSummaryLinks(String page)
    {
        if (wiki.namespace(page) % 2 == 1)
            return ""; // no talk pages yet

        String indexPHPURL = wiki.getIndexPhpUrl();
        String pageenc = URLEncoder.encode(page, StandardCharsets.UTF_8);

        return generatePageLink(page) + " ("
            + "<a href=\"" + indexPHPURL + "?title=" + pageenc + "&action=edit\">edit</a> | "
            + generatePageLink(wiki.getTalkPage(page), "talk") + " | "
            + "<a href=\"" + indexPHPURL + "?title=" + pageenc + "&action=history\">history</a> | "
            + "<a href=\"" + indexPHPURL + "?title=Special:Log&page=" + pageenc + "\">logs</a>)";
    }
    
    /**
     *  Deletes all supplied <var>pages</var> and their associated talk pages.
     *  Requires admin privileges.
     * 
     *  @param pages a list of pages to delete
     *  @param reason the reason for deletion
     *  @param talkReason the reason to use when deleting the relevant talk pages
     *  Does not delete talk pages if {@code null}.
     *  @throws LoginException if one does not possess credentials to delete
     *  @return an array containing pages we were unable to delete
     *  @author Fastily
     *  @deprecated Wiki.java delete can now delete both pages in a single 
     *  action.
     */
    @Deprecated(forRemoval=true)
    public List<String> massDelete(Iterable<String> pages, String reason, String talkReason) throws LoginException
    {
        ArrayList<String> cantdelete = new ArrayList<>();
        for (String page : pages)
        {
            try
            {
                wiki.delete(page, reason, false);
            }
            catch (IOException | UncheckedIOException ex)
            {
                cantdelete.add(page);
                continue;
            }

            if (talkReason != null)
            {
                try
                {
                    wiki.delete(wiki.getTalkPage(page), talkReason, false);
                }
                catch (IOException | UncheckedIOException ex)
                {
                    cantdelete.add(page);
                }
            }
        }
        return cantdelete;
    }
}
