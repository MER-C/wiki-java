/**
 *  @(#)Revisions.java 0.01 07/08/2018
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

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.*;

public class Revisions
{
    private final Wiki wiki;
    
    private Revisions(Wiki wiki)
    {
        this.wiki = wiki;
    }
    
    /**
     *  Creates an instance of this class bound to a particular wiki (required
     *  for methods that make network requests to a wiki or access wiki state).
     * 
     *  @param wiki the wiki to bind to
     *  @return an instance of this utility class that is bound to that wiki
     */
    public static Revisions of(Wiki wiki)
    {
        return new Revisions(wiki);
    }
    
    /**
     *  Removes reverts from a list of revisions. A revert is defined as any 
     *  revision on a page that has the same SHA-1 as any previous (as in time) 
     *  revision on that page. As a side effect, the returned list is sorted
     *  by timestamp with the earliest revision first and with duplicates 
     *  removed.
     * 
     *  @param revisions the revisions to remove reverts from
     *  @return a copy of the list of revisions with reverts removed
     */
    public static List<Wiki.Revision> removeReverts(List<Wiki.Revision> revisions)
    {
        // Group revisions by page, then sort so that the oldest edits are first.
        Map<String, Set<Wiki.Revision>> stuff = revisions.stream()
            .collect(Collectors.groupingBy(Wiki.Revision::getTitle, Collectors.toCollection(TreeSet::new)));
        Set<Wiki.Revision> ret = new LinkedHashSet<>();
        stuff.forEach((page, listofrevisions) ->
        {
            // Therefore, if a sha1 matches any previous revisions it is a revert.
            Set<String> hashes = new HashSet<>();
            Iterator<Wiki.Revision> iter = listofrevisions.iterator();
            while (iter.hasNext())
            {
                String sha1 = iter.next().getSha1();
                if (sha1 == null)
                    continue;
                if (hashes.contains(sha1))
                    iter.remove();
                hashes.add(sha1);
            }
            ret.addAll(listofrevisions);
        });
        return new ArrayList<>(ret);
    }
    
    /**
     *  Turns a list of revisions into human-readable wikitext. Be careful, as
     *  slowness may result when copying large amounts of wikitext produced by
     *  this method, or by the wiki trying to parse it. Takes the form of:
     *
     *  <p>*(diff link) 2009-01-01 00:00 User (talk | contribs) (edit summary)
     *  @param revisions a list of revisions
     *  @return those revisions as wikitext
     *  @since Wiki.java 0.20
     */
    public static String toWikitext(Iterable<Wiki.Revision> revisions)
    {
        StringBuilder buffer = new StringBuilder(100000);
        buffer.append("<div style=\"font-family: monospace; font-size: 120%\">\n");
        for (Wiki.Revision rev : revisions)
        {
            // timestamp, link to oldid
            buffer.append("*");
            buffer.append("[[Special:Permanentlink/");
            buffer.append(rev.getID());
            buffer.append("|");
            buffer.append(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(rev.getTimestamp()));
            buffer.append("]] ");
            
            // diff link
            buffer.append("([[Special:Diff/");
            buffer.append(rev.getID());
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
            buffer.append(rev.getTitle());
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
                buffer.append(Events.DELETED_EVENT_HTML);
            
            // size
            buffer.append(" .. (");
            buffer.append(rev.getSize());
            buffer.append(" bytes) (");
            buffer.append(rev.getSizeDiff());
            
            // edit summary
            buffer.append(") .. (");
            String summary = rev.getComment();
            if (summary == null)
                buffer.append(Events.DELETED_EVENT_HTML);
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
     *  returned by {@link #toWikitext(Iterable)}.
     *  @param revisions the revisions to convert
     *  @return (see above)
     *  @see #toWikitext(Iterable)
     */
    public String toHTML(Iterable<Wiki.Revision> revisions)
    {
        StringBuilder buffer = new StringBuilder(100000);
        Users users = Users.of(wiki);
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
            String revurl = "<a href=\"" + rev.permanentUrl();
            buffer.append(revurl);
            buffer.append("&diff=prev\">prev</a>) ");
            
            // date
            buffer.append(revurl);
            buffer.append("\">");
            buffer.append(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(rev.getTimestamp()));
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
            
            // page name
            String page = rev.getTitle();
            buffer.append("<a href=\"");
            buffer.append(wiki.getPageUrl(page));
            buffer.append("\" class=\"pagename\">");
            buffer.append(WikitextUtils.recode(page));
            buffer.append("</a> .. ");
            
            // user links
            String temp = rev.getUser();
            if (temp != null)
                buffer.append(users.generateHTMLSummaryLinksShort(temp));
            else
                buffer.append(Events.DELETED_EVENT_HTML);
            
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
            if (rev.getParsedComment() != null)
                buffer.append(rev.getParsedComment());
            else
                buffer.append(Events.DELETED_EVENT_HTML);
            buffer.append(")\n");
        }
        buffer.append("</ul>\n");
        return buffer.toString();
    }
}
