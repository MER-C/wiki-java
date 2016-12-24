/**
 *  @(#)ParserUtils.java 0.02 23/12/2016
 *  Copyright (C) 2012-2017 MER-C
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

import java.text.SimpleDateFormat;
import java.util.*;

/**
 *  Various parsing methods that e.g. turning Wiki.java objects into wikitext 
 *  and HTML and vice versa.
 *  @author MER-C
 *  @version 0.01
 */
public class ParserUtils
{
    /**
     *  Standard markup for RevDeleted stuff.
     */
    private static final String DELETED = "<span style=\"color: #aaaaaa, text-decoration: strike\">deleted</span>";
    
    /**
     *   Parses a list of links into its individual elements. Such a list
     *   should be in the form:
     *
     *  <pre>
     *  * [[Main Page]]
     *  * [[Wikipedia:Featured picture candidates]]
     *  * [[:File:Example.png]]
     *  </pre>
     *
     *  in which case <tt>{ "Main Page", "Wikipedia:Featured picture
     *  candidates", "File:Example.png" }</tt> is the return value.
     *
     *  @param list a list of pages
     *  @see #formatList
     *  @return an array of the page titles
     *  @since Wiki.java 0.11
     */
    public static String[] parseList(String list)
    {
        StringTokenizer tokenizer = new StringTokenizer(list, "[]");
        ArrayList<String> titles = new ArrayList<>(667);
        tokenizer.nextToken(); // skip the first token
        while (tokenizer.hasMoreTokens())
        {
            String token = tokenizer.nextToken();
            // skip any containing new lines or double letters
            if (token.contains("\n") || token.isEmpty())
                continue;
            
            // trim the starting colon, if present
            if (token.startsWith(":"))
                token = token.substring(1);
            titles.add(token);
        }
        return titles.toArray(new String[titles.size()]);
    }

    /**
     *  Formats a list of pages, say, generated from one of the query methods
     *  into something that would be editor-friendly. Does the exact opposite
     *  of <tt>parseList()</tt>, i.e. { "Main Page", "Wikipedia:Featured
     *  picture candidates", "File:Example.png" } becomes the string:
     *
     *  <pre>
     *  *[[:Main Page]]
     *  *[[:Wikipedia:Featured picture candidates]]
     *  *[[:File:Example.png]]
     *  </pre>
     *
     *  @param pages an array of page titles
     *  @return see above
     *  @see #parseList
     *  @since Wiki.java 0.14
     */
    public static String formatList(String[] pages)
    {
        StringBuilder buffer = new StringBuilder(10000);
        for (String page : pages)
        {
            buffer.append("*[[:");
            buffer.append(page);
            buffer.append("]]\n");
        }
        return buffer.toString();
    }

    /**
     *  Turns a list of revisions into human-readable wikitext. Be careful, as
     *  slowness may result when copying large amounts of wikitext produced by
     *  this method, or by the wiki trying to parse it. Takes the form of:
     *
     *  <p>*(diff link) 2009-01-01 00:00 User (talk | contribs) (edit summary)
     *  @param wiki the parent wiki
     *  @param revisions a list of revisions
     *  @return those revisions as wikitext
     *  @since Wiki.java 0.20
     */
    public static String revisionsToWikitext(Wiki wiki, Wiki.Revision[] revisions)
    {
        StringBuilder buffer = new StringBuilder(revisions.length * 100);
        buffer.append("<div style=\"font-family: monospace; font-size: 120%\">\n");
        for (Wiki.Revision rev : revisions)
        {
            // timestamp, link to oldid
            buffer.append("*");
            buffer.append("[[Special:Permanentlink/");
            buffer.append(rev.getRevid());
            Calendar timestamp = rev.getTimestamp();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            buffer.append("|");
            buffer.append(format.format(timestamp.getTime()));
            buffer.append("]] ");
            
            // diff link
            buffer.append("([[Special:Diff/");
            buffer.append(rev.getRevid());
            buffer.append("|diff]]) ");
            
            if (rev.isNew())
                buffer.append("'''N''' ");
            else
                buffer.append(". ");
            if (rev.isMinor())
                buffer.append("'''m''' ");
            else
                buffer.append(". ");
            if (rev.isBot())
                buffer.append("'''b''' ");
            else
                buffer.append(". ");
            
            buffer.append("[[");
            buffer.append(rev.getPage());
            buffer.append("]] .. ");
            
            // user
            String user2 = rev.getUser();
            if (user2 != null)
            {
                buffer.append("[[User:");
                buffer.append(user2);
                buffer.append("|");
                buffer.append(user2);
                buffer.append("]] ([[User talk:");
                buffer.append(user2);
                buffer.append("|talk]] | [[Special:Contributions/");
                buffer.append(user2);
                buffer.append("|contribs]])");
            }
            else
                buffer.append(DELETED);
            
            // size
            buffer.append(" .. (");
            buffer.append(rev.getSize());
            buffer.append(" bytes) (");
            buffer.append(rev.getSizeDiff());
            
            // edit summary
            buffer.append(") .. (");
            String summary = rev.getSummary();
            if (summary == null)
                buffer.append(DELETED);
            // kill wikimarkup
            buffer.append("<nowiki>");
            buffer.append(summary);
            buffer.append("</nowiki>)\n");
        }
        buffer.append("</div>");
        return buffer.toString();
    }
    
    /**
     *  Generates HTML from Wiki.Revisions. Output is similar to parsed wikitext
     *  returned by <tt>revisionsToWikitext()</tt>.
     *  @param wiki the parent wiki of the revisions
     *  @param revisions the revisions to convert
     *  @return (see above)
     *  @see #revisionsToWikitext(Wiki wiki, Wiki.Revision[] revisions)
     */
    public static String revisionsToHTML(Wiki wiki, Wiki.Revision[] revisions)
    {
        StringBuilder buffer = new StringBuilder(100000);
        buffer.append("<ul class=\"htmlrevisions\">\n");
        boolean colored = true;
        for (Wiki.Revision rev : revisions)
        {         
            // alternate background color for readability
            if (colored)
                buffer.append("<li class=\"shaded\">(");
            else
                buffer.append("<li>(");
            colored = !colored;
            
            // diff link
            String page = recode(rev.getPage());
            StringBuilder temp2 = new StringBuilder("<a href=\"");
            temp2.append(wiki.base);
            temp2.append(page.replace(' ', '_'));
            temp2.append("&oldid=");
            temp2.append(rev.getRevid());
            buffer.append(temp2);
            buffer.append("&diff=prev\">prev</a>) ");
            
            // date
            buffer.append(temp2);
            buffer.append("\">");
            Calendar timestamp = rev.getTimestamp();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            buffer.append(format.format(timestamp.getTime()));
            buffer.append("</a> ");
            
            if (rev.isNew())
                buffer.append("<b>N</b>");
            else
                buffer.append(".");
            if (rev.isMinor())
                buffer.append("<b>m</b>");
            else
                buffer.append(".");
            if (rev.isBot())
                buffer.append("<b>b</b> ");
            else
                buffer.append(". ");
            
            // pages never contain XSS characters
            buffer.append("<a href=\"//");
            buffer.append(wiki.getDomain());
            buffer.append("/wiki/");
            buffer.append(page.replace(' ', '_'));
            buffer.append("\" class=\"pagename\">");
            buffer.append(page);
            buffer.append("</a> .. ");
            
            // usernames never contain XSS characters
            String temp = recode(rev.getUser());
            if (temp != null)
            {
                buffer.append("<a href=\"//");
                buffer.append(wiki.getDomain());
                buffer.append("/wiki/User:");
                buffer.append(temp);
                buffer.append("\">");
                buffer.append(temp);
                buffer.append("</a> (<a href=\"//");
                buffer.append(wiki.getDomain());
                buffer.append("/wiki/User talk:");
                buffer.append(temp);
                buffer.append("\">talk</a> | <a href=\"//");
                buffer.append(wiki.getDomain());
                buffer.append("/wiki/Special:Contributions/");
                buffer.append(temp);
                buffer.append("\">contribs</a>)");
            }
            else
                buffer.append(DELETED);
            
            // size
            buffer.append(" .. (");
            buffer.append(rev.getSize());
            buffer.append(" bytes) (");
            int sizediff = rev.getSizeDiff();
            if (sizediff > 0)
                buffer.append("<span class=\"sizeincreased\">");
            else
                buffer.append("<span class=\"sizedecreased\">");
            buffer.append(rev.getSizeDiff());
            buffer.append("</span>");
            
            // edit summary
            buffer.append(") .. (");
            if (rev.getSummary() != null)
                buffer.append(recode(rev.getSummary()));
            else
                buffer.append(DELETED);
            buffer.append(")\n");
        }
        buffer.append("</ul>\n");
        return buffer.toString();
    }
    
    /**
     *  Renders output of {@link Wiki#linksearch} in wikitext.
     *  @param results the results to render
     *  @param domain the domain that was searched
     *  @return 
     *  @since 0.02
     */
    public static String linksearchResultsToWikitext(List[] results, String domain)
    {
        StringBuilder builder = new StringBuilder(100);
        int linknumber = results[0].size();
        for (int i = 0; i < linknumber; i++)
        {
            builder.append("# [[");
            builder.append((String)results[0].get(i));
            builder.append("]] uses link [");
            builder.append(results[1].get(i));
            builder.append("]\n");
        }
        builder.append(linknumber);
        builder.append(" links found. ([[Special:Linksearch/*.");
        builder.append(domain);
        builder.append("|Linksearch]])");
        return builder.toString();
    }
    
    /**
     *  Renders output of {@link Wiki#linksearch} in HTML.
     *  @param results the results to render
     *  @param domain the domain that was searched (should already be sanitized
     *  for XSS)
     *  @param wiki the wiki that was searched
     *  @return the rendered HTML
     *  @since 0.02
     */
    public static String linksearchResultsToHTML(List[] results, Wiki wiki, String domain)
    {
        StringBuilder buffer = new StringBuilder(1000);
        buffer.append("<p>\n<ol>\n");
        for (int j = 0; j < results[0].size(); j++)
        {
            buffer.append("\t<li><a href=\"//");
            buffer.append(wiki.getDomain());
            buffer.append("/wiki/");
            buffer.append((String)results[0].get(j));
            buffer.append("\">");
            buffer.append((String)results[0].get(j));
            buffer.append("</a> uses link <a href=\"");
            buffer.append(results[1].get(j).toString());
            buffer.append("\">");
            buffer.append(results[1].get(j).toString());
            buffer.append("</a>\n");
        }
        buffer.append("</ol>\n<p>");
        buffer.append(results[0].size());
        buffer.append(" links found. (<a href=\"//");
        buffer.append(wiki.getDomain());
        buffer.append("/wiki/Special:Linksearch/*.");
        buffer.append(domain);
        buffer.append("\">Linksearch</a>)\n");
        return buffer.toString();
    }
    
    /**
     *  Reverse of Wiki.decode()
     *  @param in input string
     *  @return recoded input string
     */
    public static String recode(String in)
    {
        in = in.replace("&", "&amp;");
        in = in.replace("<", "&lt;").replace(">", "&gt;"); // html tags
        in = in.replace("\"", "&quot;");
        in = in.replace("'", "&#039;");
        return in;
    }
}
