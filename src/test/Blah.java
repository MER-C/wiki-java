package test;

import java.io.*;
import java.util.*;
import javax.security.auth.login.*;
import org.wikipedia.Wiki;

public class Blah
{
    private static final String CP = "Wikipedia:Copyright_problems";
    private static final String SCV = "Wikipedia:Suspected_copyright_violations";

    public static void main(String[] args) throws IOException, LoginException
    {
        Wiki enWiki = new Wiki("en.wikipedia.org");
        enWiki.login("username", "password".toCharArray());

        GregorianCalendar cal = new GregorianCalendar();
        // create daily pages
        String date1 = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH);
        String scppage = SCV + "/" + date1;
        enWiki.edit(scppage, "===== [[" + scppage + "|" + date1 + " (Suspected copyright violations)]] =====", "edit summary", false);
        String scvText = enWiki.getPageText(SCV);
        enWiki.edit(SCV, scvText + "{{/" + date1 + "}}\n", "edit summary", false);
        enWiki.edit(createCPPageName(cal), "{{subst:cppage}}", "edit summary", false);

        // check for close paraphrase, etc.
        String cpText = enWiki.getRenderedText(CP);
        StringBuilder cpAddition = new StringBuilder(1000);
        for (String article : enWiki.getCategoryMembers("Articles tagged for copyright problems"))
        {
            if (!cpText.contains(article))
            {
                cpAddition.append("*{{subst:article-cv|");
                cpAddition.append(article);
                cpAddition.append("}}: unlisted copyvio. ~~~~\n");
            }
        }
        for (String article : enWiki.getCategoryMembers("All copied and pasted articles and sections"))
        {
            if (!cpText.contains(article))
            {
                cpAddition.append("*{{subst:article-cv|");
                cpAddition.append(article);
                cpAddition.append("}}: copied and pasted. ~~~~\n");
            }
        }
        String cpTextToday = enWiki.getPageText(createCPPageName(cal));
        enWiki.edit(createCPPageName(cal), cpTextToday + cpAddition.toString(), "edit summary", false);

        // move expired copyright problems around for closing
        cal.add(Calendar.DAY_OF_MONTH, -7);
        String cpOldSection = enWiki.getSectionText(CP, 2);
        enWiki.edit(CP, cpOldSection + "{{" + createCPPageName(cal) + "}}\n", "edit summary", false, 2);
    }

    public static String createCPPageName(GregorianCalendar date)
    {
        return CP + "/" + date.get(Calendar.YEAR) + "_" +
            date.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH) + "_" +
            date.get(Calendar.DAY_OF_MONTH);
    }

}
