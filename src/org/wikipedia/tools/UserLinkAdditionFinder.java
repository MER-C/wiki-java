/**
 *  @(#)UserLinkAdditionFinder.java 0.01 01/09/2015
 *  Copyright (C) 2015 MER-C
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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.stream.Stream;
import javax.swing.JFileChooser;
import org.wikipedia.Wiki;

/**
 *  Finds links added by a user in the main namespace.
 *  @author MER-C
 *  @version 0.01
 */
public class UserLinkAdditionFinder
{
    /**
     *  Runs this program.
     *  @param args the command line arguments (not used)
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws IOException
    {
        Wiki enWiki = new Wiki("en.wikipedia.org");
        // read in from file
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
            System.exit(0);      
        
        Map<String, Set<String>> domains = new HashMap<>();
        System.out.println("{| class=\"wikitable\"\n");
        
        Files.lines(fc.getSelectedFile().toPath(), Charset.forName("UTF-8"))
            // fetch contributions for each user
            .map(user -> {
                // goddammit Oracle!
                try 
                { 
                    return Optional.of(enWiki.contribs(user, Wiki.MAIN_NAMESPACE));
                }
                catch (IOException ex)
                {
                    System.out.println("IOException for fetching contribs of user " + user);
                    Optional<Wiki.Revision[]> temp = Optional.empty();
                    return temp;
                }
            })
            // goddammit Oracle (again)! TODO: replace this when Java 1.9 is a thing.
            .flatMap(opt -> opt.isPresent() ? Stream.of(opt.get()) : Stream.empty())
            // fetch and parse diffs
            .map(revision -> {
                try
                {
                    return parseDiff(revision);
                }
                catch (IOException ex)
                {
                    return new String[] {
                        "" + revision.getRevid(),
                        "IOException when fetching revision"
                    };
                }
            })
            .filter(links -> links.length > 1)
            // parse diffs
            .forEach(links -> {
                StringBuilder temp = new StringBuilder("|-\n|| [[Special:Diff/");
                temp.append(links[0]);
                temp.append("]]\n|| ");
                for (int i = 2; i < links.length; i++)
                {
                    temp.append(links[i]);
                    temp.append("\n");
                    // get domain name
                    String[] temp2 = links[i].split("/");
                    String domain = temp2[2].replace("www.", "");
                    if (domains.containsKey(domain))
                        domains.get(domain).add(links[1]);
                    else
                    {
                        HashSet<String> blah = new HashSet<>();
                        blah.add(links[1]);
                        domains.put(domain, blah);
                    }
                }
                System.out.println(temp.toString());
            });
        System.out.println("|}");
        
        System.out.println("== Domain list ==");
        for (String domain : domains.keySet())
            System.out.println("*{{spamlink|" + domain + "}}");
        System.out.println();
        
        System.out.println("== Blacklist log ==");
        for (Map.Entry<String, Set<String>> entry : domains.entrySet())
        {
            String domain = entry.getKey().replace(".", "\\.");
            System.out.print(" \\b" + domain + "\\b");
            for (int i = domain.length(); i < 35; i++)
                System.out.print(' ');
            System.out.print(" # ");
            for (String spammer : entry.getValue())
                System.out.print("{{user|" + spammer + "}} ");
            System.out.println();
        }
        System.out.flush();
    }
    
    /**
     *  Returns a list of external links added by a particular revision.
     *  @param revision the revision to check of added external links.
     *  @return an array: [0] = the revid, [1] = the user, [2+] = added URLs.
     *  @throws IOException if a network error occurs
     */
    public static String[] parseDiff(Wiki.Revision revision) throws IOException
    {
        // fetch the diff
        String diff;
        if (revision.isNew())
            diff = revision.getText();
        else
            diff = revision.diff(Wiki.PREVIOUS_REVISION);

        // some HTML strings we are looking for
        // see https://en.wikipedia.org/w/api.php?action=query&prop=revisions&revids=77350972&rvdiffto=prev
        String diffaddedbegin = "<td class=\"diff-addedline\">";
        String diffaddedend = "</td>";
        String deltabegin = "<ins class=\"diffchange diffchange-inline\">";
        String deltaend = "</ins>";
        // link regex
        Pattern pattern = Pattern.compile("https?://.+?\\..{2,}?(?:\\s|]|<|$)");
        
        ArrayList<String> links = new ArrayList<>();
        links.add("" + revision.getRevid());
        links.add(revision.getUser());
        
        // Condense deltas to avoid problems like https://en.wikipedia.org/w/index.php?title=&diff=prev&oldid=486611734
        diff = diff.toLowerCase();
        diff = diff.replace(deltaend + " " + deltabegin, " ");
        diff = diff.replace("&lt;", "<");
        for (int j = diff.indexOf(diffaddedbegin); j >= 0; j = diff.indexOf(diffaddedbegin, j))
        {
            int y2 = diff.indexOf(diffaddedend, j);
            String addedline = diff.substring(j + diffaddedbegin.length(), y2);
            addedline = addedline.replaceFirst("^<div>", "");
            addedline = addedline.replace("</div>", "");
            if (addedline.contains(deltabegin))
            {
                for (int k = addedline.indexOf(deltabegin); k >= 0; k = addedline.indexOf(deltabegin, k))
                {
                    int y3 = addedline.indexOf(deltaend, k);
                    String delta = addedline.substring(k + deltabegin.length(), y3);
                    // extract links
                    Matcher matcher = pattern.matcher(delta);
                    while (matcher.find())
                        links.add(matcher.group().split("[\\|<\\]\\s]")[0]);
                        
                    k = y3;
                }
            }
            else
            {
                Matcher matcher = pattern.matcher(addedline);
                while (matcher.find())
                    links.add(matcher.group().split("[\\|<\\]\\s]")[0]);
            }
            j = y2;
        }
        
        return links.toArray(new String[links.size()]);
    }
}