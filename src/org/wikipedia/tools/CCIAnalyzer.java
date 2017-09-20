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
import java.util.zip.*;
import javax.swing.JFileChooser;
import org.wikipedia.Wiki;

/**
 *  Identifies trivial diffs in a contributor copyright investigation.
 *  @author MER-C
 *  @version 0.01
 */
public class CCIAnalyzer
{
    /**
     *  Runs this program.
     *  @param args the command line arguments
     *  args[0] = wiki page to read (optional)
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws IOException
    {
        Wiki enWiki = new Wiki("en.wikipedia.org");
        StringBuilder cci;
        if (args.length < 1)
        {
            // read in from file
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
                System.exit(0);      
            BufferedReader in = new BufferedReader(new FileReader(fc.getSelectedFile()));
            String line;
            cci = new StringBuilder();
            while ((line = in.readLine()) != null)
            {
                cci.append(line);
                cci.append("\n");
            }
        }
        else
            // or read in from supplied wiki page
            cci = new StringBuilder(enWiki.getPageText(args[0]));
        
        // some HTML strings we are looking for
        // see https://en.wikipedia.org/w/api.php?action=query&prop=revisions&revids=77350972&rvdiffto=prev
        String diffaddedbegin = "&lt;td class=&quot;diff-addedline&quot;&gt;"; // <td class="diff-addedline">
        String diffaddedend = "&lt;/td&gt;"; // </td>
        String deltabegin = "&lt;ins class=&quot;diffchange diffchange-inline&quot;&gt;"; // <ins class="diffchange diffchange-inline">
        String deltaend = "&lt;/ins&gt;"; // </ins>
        
        // parse the list of diffs
        ArrayList<String> minoredits = new ArrayList<>(500);
        for (int i = cci.indexOf("{{dif|"); i >= 0; i = cci.indexOf("{{dif|", ++i))
        {
            int x = cci.indexOf("}}", i);
            String edit = cci.substring(i, x + 2);
            x = edit.indexOf("|");
            int y = edit.indexOf("|", x + 1);
            String oldid = edit.substring(x + 1, y);

            // Fetch diff. No plain text diffs for performance reasons, see
            // https://phabricator.wikimedia.org/T15209
            // We don't use the Wiki.java method here, this avoids an extra query.
            String diff = fetch("https://en.wikipedia.org/w/api.php?format=xml&action=query&prop=revisions&rvdiffto=prev&revids=" + oldid);
            // Condense deltas to avoid problems like https://en.wikipedia.org/w/index.php?title=&diff=prev&oldid=486611734
            diff = diff.toLowerCase();
            diff = diff.replace(deltaend + " " + deltabegin, " ");
            // If the diff is empty (see https://en.wikipedia.org/w/index.php?diff=343490272)
            // it will not contain diffaddedbegin -> default major to true.
            boolean major = true;
            // It is easy to strip the HTML.
            for (int j = diff.indexOf(diffaddedbegin); j >= 0; j = diff.indexOf(diffaddedbegin, j))
            {
                int y2 = diff.indexOf(diffaddedend, j);
                String addedline = diff.substring(j + diffaddedbegin.length(), y2);
                addedline = addedline.replaceFirst("^&lt;div&gt;", "");
                addedline = addedline.replace("&lt;/div&gt;", "");
                if (addedline.contains(deltabegin))
                {
                    for (int k = addedline.indexOf(deltabegin); k >= 0; k = addedline.indexOf(deltabegin, k))
                    {
                        int y3 = addedline.indexOf(deltaend, k);
                        String delta = addedline.substring(k + deltabegin.length(), y3);
                        major = analyzeDelta(delta);
                        if (major)
                            break;
                        k = y3;
                    }
                }
                else
                    major = analyzeDelta(addedline);
                if (major)
                    break;
                j = y2;
            }
            if (!major)
                minoredits.add(edit);
        }
        
        // remove all minor edits from the CCI
        for (String minoredit : minoredits)
        {
            int x = cci.indexOf(minoredit);
            cci.delete(x, x + minoredit.length());
            
            // we don't care about minor edits that add less than 500 chars
            int y = minoredit.indexOf("|");
            y = minoredit.indexOf("|", y + 1);
            int size = Integer.parseInt(minoredit.substring(y + 2, minoredit.length() - 3));
            if (size > 499)
                System.out.println(minoredit);
        }
        System.out.println("----------------------");
        
        // clean up output CCI listing
        String[] articles = cci.toString().split("\\n");
        StringBuilder cleaned = new StringBuilder();
        for (String article : articles)
        {
            // remove articles where all diffs are trivial
            if (article.contains("''''''") && !article.contains("{{dif|"))
                continue;
            // strip any left-over bold/unbold markers
            cleaned.append(article.replaceAll("''''''", ""));
            cleaned.append("\n");
        }
        System.out.println(cleaned);
    }
    
    /**
     *  Determines whether a given delta is a major edit. A "major edit" is
     *  defined as something that adds more than 9 words.
     *  @param delta the delta to check
     *  @return whether this is a major edit
     */
    public static boolean analyzeDelta(String delta)
    {
        // remove some common strings
        // {{subst:afd}}
        if (delta.contains("please do not remove or change this afd message until the issue is settled"))
            return false;
        if (delta.contains("end of afd message, feel free to edit beyond this point"))
            return false;
        if (delta.contains("{{afdm|"))
            return false;
        // {{subst:prod}}
        if (delta.contains("{{proposed deletion/dated|"))
            return false;
        // {{subst:prod blp}}
        if (delta.contains("{{prod blp/dated|"))
            return false;
        if (delta.contains("{{infobox "))
            return false;
        
        // remove wikilinks and files
        StringBuilder temp = new StringBuilder(delta);
        for (int i = temp.indexOf("[["); i > 0; i = temp.indexOf("[["))
        {
            int j = temp.indexOf("]]", i);
            if (j < 0) // unbalanced brackets
                break;
            int k = temp.indexOf("|", i);
            temp.delete(j, j + 2); // ]] => empty string
            if (k < j && k > 0)
                temp.delete(i, k + 1); // [[Blah de blah| => empty string
            else
                temp.delete(i, i + 2); // [[ => empty string
        }
        
        // decode() the delta
        String delta2 = temp.toString().replace("&lt;", "<");
        delta2 = delta2.replace("&gt;", ">");
        
        // From what I see, all articles still have 9 words between other markup.
        StringTokenizer tk = new StringTokenizer(delta2, "<>{}|=");
        while (tk.hasMoreTokens())
        {
            String token =  tk.nextToken();
            if (token.split("\\s").length > 9)
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
        StringBuilder text = new StringBuilder(100000);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
            new GZIPInputStream(connection.getInputStream()))))
        {
            String line;
            while ((line = in.readLine()) != null)
            {
                text.append(line);
                text.append("\n");
            }
        }
        String temp = text.toString();
        if (temp.contains("<error code="))
            // Something *really* bad happened. Most of these are self-explanatory
            // and are indicative of bugs (not necessarily in this framework) or 
            // can be avoided entirely.
            throw new UnknownError("MW API error. Server response was: " + temp);
        return temp;
    }
}
