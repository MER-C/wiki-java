/**
 *  @(#)ExternalLinkPopularity.java 0.01 29/03/2018
 *  Copyright (C) 2018 MER-C
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
import java.util.stream.*;
import org.wikipedia.*;

/**
 *  This tool takes a list of articles, fetches the external links used within
 *  and checks their popularity. Use cases include looking for spam references
 *  and spam articles and providing a proxy for the quality of sourcing.
 *
 *  @author MER-C
 *  @version 0.01
 *  @see <a href="https://wikipediatools.appspot.com/extlinkchecker.jsp">
 *  External link checker (online version)</a> 
 */
public class ExternalLinkPopularity
{
    private final Wiki wiki;
    private int maxlinks = 1000;
    private List<String> exclude;
    
    /**
     *  Runs this program.
     *  @param args the command line arguments
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws IOException
    {
        Map<String, String> parsedargs = new CommandLineParser("org.wikipedia.tools.ExternalLinkPopularity")
            .synopsis("[options]")
            .addSingleArgumentFlag("--wiki", "example.org", "The wiki to fetch data from (default: en.wikipedia.org)")
            .addSingleArgumentFlag("--category", "Example category", "Analyze all articles in [[Category:Example category]] (not recursive)")
            .addSingleArgumentFlag("--title", "wikipage", "The wiki page to get links from")
            .addSingleArgumentFlag("--limit", "n", "Fetch no more than n links (default: 500)")
            .parse(args);
        
        String wikistring = parsedargs.getOrDefault("--wiki", "en.wikipedia.org");
        Wiki wiki = Wiki.newSession(wikistring);
        ExternalLinkPopularity elp = new ExternalLinkPopularity(wiki);
        // meta-domains (edwardbetts.com = {{orphan}}
        elp.getExcludeList().addAll(List.of("wmflabs.org", "edwardbetts.com", "archive.org"));
        elp.setMaxLinks(Integer.parseInt(parsedargs.getOrDefault("--limit", "500")));
        
        List<String> pages = new ArrayList<>();
        String category = parsedargs.get("--category");
        if (category != null)
            pages.addAll(wiki.getCategoryMembers(category, Wiki.MAIN_NAMESPACE));
        String article = parsedargs.get("--title");
        if (article != null)
            pages.add(article);
        if (pages.isEmpty())
        {
            System.out.println("No articles specified!");
            System.exit(1);
        }
        Map<String, Map<String, List<String>>> results = elp.fetchExternalLinks(pages);
        Map<String, Integer> popresults = elp.determineLinkPopularity(flatten(results));
        System.out.println(elp.exportResultsAsWikitext(results, popresults));
        
        // String[] spampages = enWiki.getCategoryMembers("Category:Wikipedia articles with undisclosed paid content from March 2018", Wiki.MAIN_NAMESPACE);
        // filter down the spam to recently created pages
        // List<String> recentspam = new ArrayList<>();
        // for (String page : spampages)
        //     if (enWiki.getFirstRevision(page).getTimestamp().isAfter(OffsetDateTime.parse("2018-02-01T00:00:00Z")))
        //       recentspam.add(page);
        //Map<String, Map<String, List<String>>> results = elp.fetchExternalLinks(recentspam);
        //Map<String, Map<String, Integer>> popresults = elp.determineLinkPopularity(results);
        //elp.exportResultsAsWikitext(results, popresults);
    }
    
    /**
     *  Creates a new instance of this tool.
     *  @param wiki the wiki to fetch data from
     */
    public ExternalLinkPopularity(Wiki wiki)
    {
        this.wiki = wiki;
        exclude = new ArrayList<>();
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
     *  Sets the maximum number of links fetched to determine popularity. It is 
     *  recommended to set a limit of not more than a few thousand to avoid 
     *  getting bogged down with large queries. Some domains are used very 
     *  frequently (10000+ links), often because they are reliable sources. This 
     *  quantity is passed directly to {@link Wiki#setQueryLimit(int)}. The 
     *  default is 1000.
     * 
     *  @param limit the query limit used
     *  @throws IllegalArgumentException if {@code limit < 1}
     *  @see #getMaxLinks() 
     */
    public void setMaxLinks(int limit)
    {
        if (limit < 1)
            throw new IllegalArgumentException("Limit must be greater than 1.");
        maxlinks = limit;
    }
    
    /**
     *  Returns the maximum number of links fetched to determine popularity.
     *  @return (see above)
     *  @see #setMaxLinks(int) 
     */
    public int getMaxLinks()
    {
        return maxlinks;
    }
    
    /**
     *  Gets the list of domains excluded from the analysis. This list is
     *  modifiable -- changes to it will affect subsequent analyses.
     *  @return the list of domains excluded from the analysis
     */
    public List<String> getExcludeList()
    {
        return exclude;
    }
    
    /**
     *  For each of a supplied list of <var>articles</var>, fetch the external
     *  links used within and group by domain.
     * 
     *  @param articles the list of articles to analyze
     *  @return a Map with page &#8594; domain &#8594; URL
     *  @throws IOException if a network error occurs
     */
    public Map<String, Map<String, List<String>>> fetchExternalLinks(List<String> articles) throws IOException
    {
        List<List<String>> links = wiki.getExternalLinksOnPage(articles);
        Map<String, Map<String, List<String>>> domaintourls = new HashMap<>();
        
        // group links used on each page by domain
        for (int i = 0; i < links.size(); i++)
        {
            Map<String, List<String>> pagedomaintourls = links.get(i).stream()
                .filter(link -> ExternalLinks.extractDomain(link) != null)
                .filter(link -> exclude.stream().noneMatch(exc -> link.contains(exc)))
                .collect(Collectors.groupingBy(domain ->
                {
                    String domain2 = ExternalLinks.extractDomain(domain);
                    // crude hack to remove subdomains
                    int a = domain2.indexOf('.') + 1;
                    if (domain2.indexOf('.', a) > 0)
                    {
                        String blah = domain2.substring(a);
                        if (blah.length() > 10)
                            return blah;
                    }
                    return domain2;
                }));
            domaintourls.put(articles.get(i), pagedomaintourls);
        }
        return domaintourls;
    }

    /**
     *  Flattens the output of {@link #fetchExternalLinks(List)} to a 
     *  single-level List.
     *  @param data the output to flatten
     *  @return the set of domains added
     */
    public static List<String> flatten(Map<String, Map<String, List<String>>> data)
    {
        List<String> domains = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<String>>> pagedomaintourls : data.entrySet())
            domains.addAll(pagedomaintourls.getValue().keySet());
        return domains;
    }
    
    /**
     *  Determine a list of sites' popularity as external links. Each popularity
     *  score is capped at {@link #getMaxLinks()} because some domains are used 
     *  very frequently and we don't want to be here forever. The result is 
     *  sorted by number of links found (least used domains first).
     * 
     *  @param data a list of domains to determine popularity for
     *  @return a Map with domain &#8594; popularity
     *  @throws IOException if a network error occurs
     */
    public Map<String, Integer> determineLinkPopularity(Collection<String> data) throws IOException
    {
        // deduplicate domains
        Set<String> domains = new LinkedHashSet<>();
        domains.addAll(data);
        domains.removeIf(domain -> exclude.stream().anyMatch(exc -> domain.contains(exc)));

        // linksearch the domains to determine popularity
        // discard the linksearch data for now, but bear in mind that it could
        // be useful for some reason
        wiki.setQueryLimit(maxlinks);
        Map<String, Integer> lsresults = new HashMap<>();
        for (String domain : domains)
        {
            int count = wiki.linksearch("*." + domain).size();
            // can't set namespace here due to $wgMiserMode and domains with
            // lots of links
            lsresults.put(domain, Math.min(count, maxlinks));
        }
        wiki.setQueryLimit(Integer.MAX_VALUE);
        return ArrayUtils.sortByValue(lsresults, Comparator.naturalOrder());
    }
    
    public String exportResultsAsWikitext(Map<String, Map<String, List<String>>> urldata, Map<String, Integer> popularity)
    {
        StringBuilder sb = new StringBuilder();
        urldata.forEach((page, pagedomaintourls) ->
        {
            if (pagedomaintourls.isEmpty())
                return;
            sb.append("""
                == [[:%s]]==
                """.formatted(page));
            DoubleStream.Builder scores = DoubleStream.builder();
            DoubleSummaryStatistics dss = new DoubleSummaryStatistics();
            pagedomaintourls.forEach((domain, listoflinks) ->
            {
                Integer numlinks = popularity.get(domain);
                sb.append("*");
                sb.append(domain);
                if (numlinks >= maxlinks)
                    sb.append(" (at least ");
                else
                    sb.append(" (");
                sb.append(numlinks);
                if (numlinks == 1)
                    sb.append(" link");
                else
                    sb.append(" links");
                sb.append("""
                    ; [[Special:Linksearch/*.%s|Linksearch]])
                    """.formatted(domain, domain));
                scores.accept(numlinks);
                dss.accept(numlinks);
                for (String url : listoflinks)
                {
                    sb.append("** ");
                    sb.append(url);
                    sb.append("\n");
                }
                sb.append("\n");
            });
            // compute summary statistics
            if (pagedomaintourls.size() > 1)
            {
                double[] temp = scores.build().toArray();
                Arrays.sort(temp);
                double[] quartiles = MathsAndStats.quartiles(temp);
                sb.append("""
                    ;Summary statistics
                    *COUNT: %d
                    *MEAN: %.1f
                    *Q1: %.1f
                    *MEDIAN: %.1f
                    *Q3: %.1f
                """.formatted(temp.length, dss.getAverage(), quartiles[0], MathsAndStats.median(temp), quartiles[1]));
            }
        });
        return sb.toString();
    }
    
    public String exportResultsAsHTML(Map<String, Map<String, List<String>>> urldata, Map<String, Integer> popularity)
    {
        Pages pageUtils = Pages.of(wiki);
        StringBuilder sb = new StringBuilder();
        urldata.forEach((page, pagedomaintourls) ->
        {
            if (pagedomaintourls.isEmpty())
                return;
            sb.append("""
                <h2>%s</h2>
                %s
                <ul>
                """.formatted(pageUtils.generatePageLink(page), pageUtils.generateSummaryLinks(page)));
            DoubleStream.Builder scores = DoubleStream.builder();
            DoubleSummaryStatistics dss = new DoubleSummaryStatistics();
            pagedomaintourls.forEach((domain, listoflinks) ->
            {
                Integer numlinks = popularity.get(domain);
                sb.append("<li>");
                sb.append(domain);
                if (numlinks >= maxlinks)
                    sb.append(" (at least ");
                else
                    sb.append(" (");
                sb.append(numlinks);
                if (numlinks == 1)
                    sb.append(" link; ");
                else
                    sb.append(" links; ");
                sb.append(pageUtils.generatePageLink("Special:Linksearch/*." + domain, "Linksearch"));
                sb.append(")\n");
                scores.accept(numlinks);
                dss.accept(numlinks);
                sb.append("<ul>");
                for (String url : listoflinks)
                {
                    sb.append("""
                        <li><a href="%s">%s</a>"
                        """.formatted(url, url));
                }
                sb.append("</ul>\n");
            });
            sb.append("</ul>\n");
            // compute summary statistics
            if (pagedomaintourls.size() > 1)
            {
                double[] temp = scores.build().toArray();
                Arrays.sort(temp);
                double[] quartiles = MathsAndStats.quartiles(temp);
                sb.append("""
                    <b>Summary statistics</b>
                    <ul>
                    <li>COUNT: %d
                    <li>MEAN: %.1f
                    <li>Q1: %.1f
                    <li>MEDIAN: %.1f
                    <li>Q3: %.1f
                    </ul>
                    """.formatted(temp.length, dss.getAverage(), quartiles[0], MathsAndStats.median(temp), quartiles[1]));
            }
        });
        return sb.toString();
    }
}
