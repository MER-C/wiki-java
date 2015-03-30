/**
 *  @(#)WikiTests.java
 *  Copyright (C) 2011 - 2014 MER-C
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
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

import java.io.*;
import java.util.*;
import org.wikipedia.*;

/**
 *  Tests for Wiki.java that do not require being logged in. This file is for
 *  tests that cannot work as unit tests.
 *  @author MER-C
 */
public class WikiTests
{
    public static void main(String[] args) throws Exception
    {
        Wiki enWiki = new Wiki("en.wikipedia.org");
        enWiki.setMaxLag(-1);
        Wiki deWiki = new Wiki("de.wikipedia.org");
        deWiki.setMaxLag(-1);
        Wiki testWiki = new Wiki("test.wikipedia.org");
        testWiki.setMaxLag(-1);

        /*
        // imageUsage()
        for (String page : enWiki.imageUsage("Wiki.png", Wiki.PROJECT_NAMESPACE, Wiki.TEMPLATE_NAMESPACE))
            System.out.println(page);

        // getCategoryMembers()
        for (String page : enWiki.getCategoryMembers("Place of death missing"))
            System.out.println(page);
        for (String page : enWiki.getCategoryMembers("Miscellaneous pages for deletion", Wiki.USER_NAMESPACE, Wiki.TEMPLATE_NAMESPACE))
            System.out.println(page);
        for (String page : deWiki.getCategoryMembers("Kategorie:Wikipedia:Wartungsseite (MerlBot)", true, Wiki.PROJECT_NAMESPACE))
            System.out.println(page);

        // getPageHistory()
        for(Wiki.Revision rev : enWiki.getPageHistory("A. K. Fazlul Huq", null, null, true))
            System.out.println(rev);

        // getPageInfo(): protected, cascade protected page
        Map<String, Object> blah = enWiki.getPageInfo(" Main  Page ");
        for(Map.Entry<String, Object> entry : blah.entrySet())
        {
            System.out.print(entry.getKey());
            System.out.print(" => ");
            System.out.println(entry.getValue());
        }

        System.out.println();
        // getPageInfo(): protected, deleted page
        blah = enWiki.getPageInfo("Create a new page");
        for(Map.Entry<String, Object> entry : blah.entrySet())
        {
            System.out.print(entry.getKey());
            System.out.print(" => ");
            System.out.println(entry.getValue());
        }
        // enwiki now has a custom protection level
        // see also issue 39
        Map<String, Object> blah5[] = enWiki.getPageInfo(new String[] { "Template:La", "Template:La", "Zombie", "Ksdhfsjk", "Crème brûlée" });
        for (int i = 0; i < blah5.length; i++)
        {
            for (Map.Entry<String, Object> entry : blah5[i].entrySet())
            {
                System.out.print(entry.getKey());
                System.out.print(" => ");
                System.out.println(entry.getValue());
            }
        }

        // getInterWikiBacklinks()
        String[][] blah2 = enWiki.getInterWikiBacklinks("wikitravel");
        for (String[] entry : blah2)
        {
            if (enWiki.namespace(entry[0]) == Wiki.TEMPLATE_NAMESPACE)
                System.out.println("*[[" + entry[0] + "]] => " + entry[1]);
        }
            
        // getLinksOnPage
        for (String link : enWiki.getLinksOnPage("List of craters on Venus"))
            System.out.println(link);

        // getImagesOnPage
        for (String image : enWiki.getImagesOnPage("Main Page"))
            System.out.println(image);
        // This site runs an obsolete MW where "File:" doesn't exist
        for (String image : new Wiki("wiki.eclipse.org", "").getImagesOnPage("Main Page"))
            System.out.println(image);

        // getImage()
        byte[] image = enWiki.getImage("File:HellenicArmySeal.svg");
        FileOutputStream out = new FileOutputStream("File:HellenicArmySeal.svg");
        out.write(image);
        out.close();

        // User.getUserInfo()
        Wiki.User user = enWiki.getUser("Jimbo Wales");
        for(Map.Entry<String, Object> entry : user.getUserInfo().entrySet())
        {
            System.out.print(entry.getKey());
            System.out.print(" => ");
            Object temp = entry.getValue();
            System.out.println(temp instanceof Object[] ? Arrays.toString((Object[])temp) : temp);
        }
        System.out.println();

        // getLogEntries()
        for(Wiki.LogEntry entry : enWiki.getLogEntries(null, null, 5, Wiki.USER_RIGHTS_LOG, "", null, "User:Jimbo Wales", Wiki.ALL_NAMESPACES))
            System.out.println(entry);
        for (Wiki.LogEntry entry : enWiki.getLogEntries(501, Wiki.DELETION_LOG, "restore"))
            System.out.println(entry);

        // getUploads
        Wiki.User user4 = enWiki.getUser("Wizardman");
        for (Wiki.LogEntry entry : enWiki.getUploads(user4))
            System.out.println(entry);

        // ipblocklist
        for (Wiki.LogEntry entry : enWiki.getIPBlockList("Alice"))
            System.out.println(entry);

        // search
        for(String[] result : enWiki.search("WikiProject Spam zola enterprises", Wiki.PROJECT_TALK_NAMESPACE))
            System.out.println(Arrays.toString(result));
 
        System.out.println(enWiki.getSectionText("Wikipedia:Copyright_problems", 2));

        System.out.println(enWiki.parse("{{Main Page}}"));

        System.out.println(enWiki.getTopRevision("Wikipedia:Sandbox"));

        // contribs
        for (Wiki.Revision revision : enWiki.contribs("Qalnor"))
            System.out.println(revision);

        // contribs
        Calendar c = new GregorianCalendar(2008, 0, 1);
        for (Wiki.Revision revision : enWiki.contribs("", "127.0.", c, null))
            System.out.println(revision);

        // getImageHistory
        for (Wiki.LogEntry entry : enWiki.getImageHistory("Davis Motor Car Company logo.jpg"))
            System.out.println(entry);

        // Revision.diff
        System.out.println(enWiki.getRevision(473467375L).diff(Wiki.PREVIOUS_REVISION));

        System.out.println(ParserUtils.revisionsToWikitext(enWiki, enWiki.recentChanges(51)));
        System.out.println(ParserUtils.revisionsToHTML(enWiki, enWiki.recentChanges(51)));

        // query page
        for (String page : enWiki.queryPage("Uncategorizedpages"))
            System.out.println(page);

        // purge
        enWiki.purge(false, new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11" });

        // getPageInfo
        // String[] pages = enWiki.getCategoryMembers("Wikipedia featured article review candidates");
        // enWiki.getPageInfo(pages); // use a debugger, please
     
        // list users
        for (String user2 : enWiki.allUsers("AB", 51))
            System.out.println(user2);
        for (String user2 : enWiki.allUsersWithPrefix("AB "))
            System.out.println(user2);
        
        // interwikis
        for (Map.Entry entry : enWiki.getInterWikiLinks("Main Page").entrySet())
            System.out.println("[[:" + entry.getKey() + ":" + entry.getValue() + "]]");
            
        // linksearch
        List[] blah4 = enWiki.linksearch("bom.gov.au");
        for (int i = 0; i < blah4[0].size(); i++)
            System.out.println(blah4[0].get(i) + " --- " + blah4[1].get(i));


        // getOldImage/image history
        Wiki.LogEntry[] entries = enWiki.getImageHistory("File:Bohemian Rhapsody.png");
        for (Wiki.LogEntry entry : entries)
            System.out.println(entry);
        FileOutputStream out2 = new FileOutputStream("hello.png");
        out2.write(enWiki.getOldImage(entries[0]));
        out2.close();

        // allpages
        HashMap<String, Object> protect = new HashMap<>();
        protect.put("cascade", true);
        protect.put("edit", Wiki.FULL_PROTECTION);
        for (String page : enWiki.listPages("", protect, Wiki.MAIN_NAMESPACE, -1, -1, null))
            System.out.println(page);
        protect = new HashMap<>();
        protect.put("upload", Wiki.FULL_PROTECTION);
        protect.put("edit", Wiki.FULL_PROTECTION);
        for (String page : enWiki.listPages("", protect, Wiki.FILE_NAMESPACE, -1, -1, null))
            System.out.println(page);

        // getRevisions
        Wiki.Revision[] temp = enWiki.getRevisions(new long[] { 500000000L, 579126972L, 500000000L, 500000003L, 40000000L } );
        for (Wiki.Revision blah3 : temp)
            System.out.println(blah3);

        // getExternalLinksOnPage
        for (String url : enWiki.getExternalLinksOnPage("Albert Einstein"))
            System.out.println(url);

        // getCategories()
        for (String cat : enWiki.getCategories("Albert Einstein"))
            System.out.println(cat);
        for (String cat : enWiki.getCategories("Albert Einstein", true, true))
            System.out.println(cat);
        for (String s : testWiki.getCategoryMembers("A", true))
            System.out.println(s);
        */
    }
}
