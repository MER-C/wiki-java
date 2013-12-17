/**
 *  @(#)CCIAnalyzer.java 0.01 16/12/2013
 *  Copyright (C) 2013 - 2014 MER-C
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
import java.net.*;
import java.util.logging.*;
import java.util.zip.*;
import org.wikipedia.Wiki;

/**
 *
 */
public class CCIAnalyzer
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException
    {
        //if (args.length < 1)
        //{
        //    System.out.println("First argument must be the CCI page to analyse.");
        //    System.exit(1);
        //}
        Wiki enWiki = new Wiki("en.wikipedia.org");
        StringBuilder cci = new StringBuilder(enWiki.getPageText("User:MER-C/Sandbox"));
        
        // parse the list of diffs
        ArrayList<String> minoredits = new ArrayList<String>(500);
        for (int i = cci.indexOf("{{dif|"); i > 0; i = cci.indexOf("{{dif|", ++i))
        {
            int x = cci.indexOf("}}", i);
            String edit = cci.substring(i, x + 2);
            x = edit.indexOf("|");
            int y = edit.indexOf("|", x + 1);
            String oldid = edit.substring(x + 1, y);

            // Fetch diff. No plain text diffs for performance reasons, see
            // https://bugzilla.wikimedia.org/show_bug.cgi?id=13209
            // We don't use the Wiki.java method here, this avoids an extra query.
            String diff = fetch("https://en.wikipedia.org/w/api.php?format=xml&action=query&prop=revisions&rvdiffto=prev&revids=" + oldid);
            diff = diff.replace("&lt;", "<");
            diff = diff.replace("&gt;", ">");
            // However, it is easy to strip the HTML.
            boolean major = false;
            for (int j = diff.indexOf("diff-addedline"); j > 0; j = diff.indexOf("diff-addedline", ++j))
            {
                x = diff.indexOf("<div>", j) + 5;
                y = diff.indexOf("<", x);
                String addedline = diff.substring(x, y);
                if (diff.contains("diffchange diffchange-inline"))
                {
                    for (int k = addedline.indexOf("diffchange diffchange-inline", j); k > 0; k = addedline.indexOf("diffchange diffchange-inline", ++k))
                    {
                        x = diff.indexOf(">", j) + 1;
                        y = diff.indexOf("<", x);
                        String delta = addedline.substring(x, y);
                        major |= analyzeDelta(delta);
                        if (major)
                            break;
                    }
                }
                else
                    major |= analyzeDelta(addedline);
                if (major)
                    break;
            }
            if (!major)
                minoredits.add(edit);
        }
        
        // remove all minor edits from the CCI
        for (String minoredit : minoredits)
        {
            int x = cci.indexOf(minoredit);
            cci.delete(x, x + minoredit.length());
            System.out.println(minoredit);
        }
        System.out.println("----------------------");
        System.out.println(cci);
    }
    
    /**
     *  Determines whether a given delta is a major edit. A "major edit" is
     *  defined as something that adds more than 6 words.
     *  @param delta the delta to check
     *  @return whether this is a major edit
     */
    public static boolean analyzeDelta(String delta)
    {
        // This treats wikilinks as separate tokens.
        // From what I see, all articles still have 6 words between wikilinks.
        StringTokenizer tk = new StringTokenizer(delta, "<>[]{}|=");
        while (tk.hasMoreTokens())
        {
            String token =  tk.nextToken();
            if (token.split("\\s").length > 6)
                return true;
        }
        return false;
    }
    
    private static String fetch(String url) throws IOException
    {
        // connect
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(60000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("Accept-encoding", "gzip");
        connection.connect();

        // get the text
        BufferedReader in = new BufferedReader(new InputStreamReader(
            new GZIPInputStream(connection.getInputStream())));
        String line;
        StringBuilder text = new StringBuilder(100000);
        while ((line = in.readLine()) != null)
        {
            text.append(line);
            text.append("\n");
        }
        in.close();
        String temp = text.toString();
        if (temp.contains("<error code="))
            // Something *really* bad happened. Most of these are self-explanatory
            // and are indicative of bugs (not necessarily in this framework) or 
            // can be avoided entirely.
            throw new UnknownError("MW API error. Server response was: " + temp);
        return temp;
    }
}
