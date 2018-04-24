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
    private int maxlinks = 500;
    private List<String> exclude;
    
    /**
     *  Runs this program.
     *  @param args the command line arguments
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws IOException
    {
        Wiki enWiki = Wiki.createInstance("en.wikipedia.org");
        ExternalLinkPopularity elp = new ExternalLinkPopularity(enWiki);
        // meta-domains (edwardbetts.com = {{orphan}}
        elp.getExcludeList().addAll(Arrays.asList("wmflabs.org", "edwardbetts.com", "archive.org"));
        
        Map<String, String> parsedargs = new CommandLineParser()
            .synopsis("org.wikipedia.tools.ExternalLinkPopularity", "[options]")
            .addSingleArgumentFlag("--title", "wikipage", "The wiki page to get links from")
            .addSingleArgumentFlag("--limit", "n", "Fetch no more than n links (default: 500)")
            .parse(args);
        elp.setMaxLinks(Integer.parseInt(parsedargs.getOrDefault("--limit", "500")));
        String article = parsedargs.get("--title");
        if (article == null)
        {
            System.out.println("No article specified!");
            System.exit(1);
        }
        Map<String, Map<String, List<String>>> results = elp.fetchExternalLinks(Arrays.asList(article));
        Map<String, Map<String, Integer>> popresults = elp.determineLinkPopularity(results);
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
     *  default is 500.
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
     *  Determine a list of sites' popularity as external links. Each popularity
     *  score is capped at {@link #getMaxLinks()} because some domains are used 
     *  very frequently and we don't want to be here forever. <var>data</var>
     *  is of the form unique String &#8594; domain &#8594; links and can be
     *  the results from {@link #fetchExternalLinks(List)}.
     * 
     *  @param data a Map with unique String &#8594; domain &#8594; links
     *  @return a Map with page &#8594; domain &#8594; popularity
     *  @throws IOException if a network error occurs
     */
    public Map<String, Map<String, Integer>> determineLinkPopularity(Map<String, Map<String, List<String>>> data) throws IOException
    {
        // deduplicate domains
        Set<String> domains = new LinkedHashSet<>();
        data.forEach((page, pagedomaintourls) ->
        {
            domains.addAll(pagedomaintourls.keySet());
        });
        domains.removeIf(domain -> exclude.stream().anyMatch(exc -> domain.contains(exc)));

        // linksearch the domains to determine popularity
        // discard the linksearch data for now, but bear in mind that it could
        // be useful for some reason
        wiki.setQueryLimit(maxlinks);
        Map<String, Integer> lsresults = new HashMap<>();
        for (String domain : domains)
        {
            int count = wiki.linksearch("*." + domain, "http").size();
            // can't set namespace here due to $wgMiserMode and domains with
            // lots of links
            if (count < maxlinks)
                count += wiki.linksearch("*." + domain, "https").size();
            
            lsresults.put(domain, Math.min(count, maxlinks));
        }
        wiki.setQueryLimit(Integer.MAX_VALUE);
        
        Map<String, Map<String, Integer>> ret = new HashMap<>();
        data.forEach((page, pagedomaintourls) ->
        {
            Map<String, Integer> temp = new HashMap<>();
            pagedomaintourls.keySet().forEach(domain ->
            {
                temp.put(domain, lsresults.get(domain));
            });
            ret.put(page, temp);
        });
        return ret;
    }
    
    public String exportResultsAsWikitext(Map<String, Map<String, List<String>>> urldata, Map<String, Map<String, Integer>> popularity)
    {
        StringBuilder sb = new StringBuilder();
        urldata.forEach((page, pagedomaintourls) ->
        {
            if (pagedomaintourls.isEmpty())
                return;
            sb.append("== [[:");
            sb.append(page);
            sb.append("]]==\n");
            DoubleStream.Builder scores = DoubleStream.builder();
            DoubleSummaryStatistics dss = new DoubleSummaryStatistics();
            pagedomaintourls.forEach((domain, listoflinks) ->
            {
                Integer numlinks = popularity.get(page).get(domain);
                sb.append("*");
                sb.append(domain);
                if (numlinks >= maxlinks)
                    sb.append(" (at least ");
                else
                    sb.append(" (");
                sb.append(numlinks);
                if (numlinks == 1)
                    sb.append(" link)\n");
                else
                    sb.append(" links)\n");
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
                sb.append(";Summary statistics\n");
                sb.append("*COUNT: ");
                sb.append(temp.length);
                sb.append(String.format("\n*MEAN: %.1f\n", dss.getAverage()));
                Arrays.sort(temp);
                double[] quartiles = quartiles(temp);
                sb.append(String.format("*Q1: %.1f\n", quartiles[0]));
                sb.append(String.format("*MEDIAN: %.1f\n", median(temp)));
                sb.append(String.format("*Q3: %.1f\n\n", quartiles[1]));
            }
        });
        return sb.toString();
    }
    
    public String exportResultsAsHTML(Map<String, Map<String, List<String>>> urldata, Map<String, Map<String, Integer>> popularity)
    {
        StringBuilder sb = new StringBuilder();
        urldata.forEach((page, pagedomaintourls) ->
        {
            if (pagedomaintourls.isEmpty())
                return;
            sb.append("<h2><a href=\"");
            sb.append(wiki.getPageURL(page));
            sb.append("\">");
            sb.append(page);
            sb.append("</a></h2>\n<ul>\n");
            DoubleStream.Builder scores = DoubleStream.builder();
            DoubleSummaryStatistics dss = new DoubleSummaryStatistics();
            pagedomaintourls.forEach((domain, listoflinks) ->
            {
                Integer numlinks = popularity.get(page).get(domain);
                sb.append("<li>");
                sb.append(domain);
                if (numlinks >= maxlinks)
                    sb.append(" (at least ");
                else
                    sb.append(" (");
                sb.append(numlinks);
                if (numlinks == 1)
                    sb.append(" link; Linksearch: ");
                else
                    sb.append(" links; Linksearch: ");
                sb.append("<a href=\"");
                sb.append(wiki.getPageURL("Special:Linksearch/*." + domain));
                sb.append("\">http</a> <a href=\"");
                sb.append(wiki.getPageURL("Special:Linksearch/https://*." + domain));
                sb.append("\">https</a>)\n");
                scores.accept(numlinks);
                dss.accept(numlinks);
                sb.append("<ul>");
                for (String url : listoflinks)
                {
                    sb.append("<li><a href=\"");
                    sb.append(url);
                    sb.append("\">");
                    sb.append(url);
                    sb.append("</a>\n");
                }
                sb.append("</ul>\n");
            });
            sb.append("</ul>\n");
            // compute summary statistics
            if (pagedomaintourls.size() > 1)
            {
                double[] temp = scores.build().toArray();
                sb.append("<b>Summary statistics</b>\n<ul>\n");
                sb.append("<li>COUNT: ");
                sb.append(temp.length);
                sb.append(String.format("\n<li>MEAN: %.1f\n", dss.getAverage()));
                Arrays.sort(temp);
                double[] quartiles = quartiles(temp);
                sb.append(String.format("<li>Q1: %.1f\n", quartiles[0]));
                sb.append(String.format("<li>MEDIAN: %.1f\n", median(temp)));
                sb.append(String.format("<li>Q3: %.1f\n</ul>\n", quartiles[1]));
            }
        });
        return sb.toString();
    }
        
    // see https://en.wikipedia.org/wiki/Quartile (method 3)
    public static double[] quartiles(double[] values)
    {
        // Shit that should be in the JDK, part 2.
        if (values.length < 4)
            return new double[] { Double.NaN, Double.NaN };
        
        int middle = values.length / 2;
        int n = values.length / 4;
        double[] ret = new double[2];
        
        switch (values.length % 4)
        {
            case 0:
            case 2:
                double[] temp = Arrays.copyOfRange(values, 0, middle - 1);
                ret[0] = median(temp);
                temp = Arrays.copyOfRange(values, middle, values.length - 1);
                ret[1] = median(temp);
                return ret;
            case 1:
                ret[0] = values[n - 1]/4 + values[n] * 3/4;
                ret[1] = values[3*n] * 3/4 + values[3*n + 1]/4;
                return ret;
            case 3:
                ret[0] = values[n] * 3/4 + values[n+1]/4;
                ret[1] = values[3*n + 1]/4 + values[3*n+2] * 3/4;
                return ret;
        }
        throw new AssertionError("Unreachable.");
    }
    
    public static double median(double[] values)
    {
        // Shit that should be in the JDK, part 1.
        if (values.length < 1)
            return Double.NaN;
        
        int middle = values.length / 2;
        if (values.length % 2 == 1)
            return values[middle];
        else
            return (values[middle - 1] + values[middle])/2;
    }
}
