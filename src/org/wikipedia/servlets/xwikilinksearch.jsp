<!--
    @(#)xwikilinksearch.jsp 0.02 27/01/2017
    Copyright (C) 2011 - 2017 MER-C
  
    This is free software: you are free to change and redistribute it under the 
    Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html> 
    for details. There is NO WARRANTY, to the extent permitted by law.
-->

<%@ include file="header.jsp" %>
<%@ page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>

<%
    request.setAttribute("toolname", "Cross-wiki linksearch");

    String mode = request.getParameter("mode");
    if (mode == null)
        mode = "multi";

    String domain = request.getParameter("link");
    domain = (domain == null) ? "" : ServletUtils.sanitizeForAttribute(domain);
    
    String set = request.getParameter("set");
    if (set == null)
        set = "top20";

    String wikiinput = request.getParameter("wiki");
    if (wikiinput != null)
        wikiinput = ServletUtils.sanitizeForAttribute(wikiinput);

    boolean https = (request.getParameter("https") != null);
    boolean mailto = (request.getParameter("mailto") != null);

    String temp = request.getParameter("ns");
    boolean mainns = temp != null && temp.equals("0");
    int[] ns = mainns ? new int[] { Wiki.MAIN_NAMESPACE } : new int[0];
%>

<!doctype html>
<html>
<head>
<link rel=stylesheet href="styles.css">
<title><%= request.getAttribute("toolname") %></title>
<script type="text/javascript" src="XWikiLinksearch.js"></script>
</head>

<body>
<p>
This tool searches various Wikimedia projects for a specific link. Enter a 
domain name (example.com, not *.example.com or http://example.com) below. A 
timeout is more likely when searching for more wikis or protocols. For performance
reasons, results are limited to between 500 and 1000 links per wiki.

<form name="spamform" action="./linksearch.jsp" method=GET>
<table>
<tr>
    <td><input id="radio_multi" type=radio name=mode value=multi<%= mode.equals("multi") ?
         " checked" : "" %>>
    <td>Wikis to search:
    <td><select name=set id=set<%= mode.equals("multi") ? "" : " disabled" %>>
            <option value="top20"<%= set == "top20" ? " selected" : ""%>>Top 20 Wikipedias</option>
            <option value="top40"<%= set == "top40" ? " selected" : ""%>>Top 40 Wikipedias</option>
            <option value="major"<%= set == "major" ? " selected" : ""%>>Major Wikimedia projects</option>
        </select>
        
<tr>
    <td><input id="radio_single" type=radio name=mode value=single<%= mode.equals("single") ?
         " checked" : "" %>>
    <td>Single wiki:
    <td><input type=text id=wiki name=wiki <%= mode.equals("single") ? "required value=" + 
        wikiinput : "disabled" %>>
        
<tr>
    <td colspan=2>Domain to search:
    <td><input type=text name=link required value="<%= domain %>">
        
<tr>
    <td colspan=2>Additional protocols:
    <td><input type=checkbox name=https value=1<%= (https || domain.isEmpty()) ?
        " checked" : "" %>>HTTPS
        <input type=checkbox name=mailto value=1<%= mailto ? " checked" : "" %>>mailto

<tr>
    <td><input type=checkbox name=ns value=0<%= mainns ? " checked" : "" %>>
    <td colspan=3>Main namespace only?

</table>
<br>
<input type=submit value=Search>
</form>

<%
    // state with no input parameters
    if (domain.isEmpty())
    {
%>
<%@ include file="footer.jsp" %>
<%
        return;
    }
    out.println("<hr>");
    Map<Wiki, List<String[]>> results = null;
    if (mode.equals("multi"))
    {
        switch (set)
        {
            case "top20":
                results = AllWikiLinksearch.crossWikiLinksearch(true, 1, 
                    domain, AllWikiLinksearch.TOP20, https, mailto, ns);
                break;
            case "top40":
                results = AllWikiLinksearch.crossWikiLinksearch(true, 1, 
                    domain, AllWikiLinksearch.TOP40, https, mailto, ns);
                break;
            case "major":
                results = AllWikiLinksearch.crossWikiLinksearch(true, 1, 
                    domain, AllWikiLinksearch.MAJOR_WIKIS, https, mailto, ns);
                break;
            default:
    %>
    <span class="error">Invalid wiki set selected!</span>
    <%@ include file="footer.jsp" %>
    <%
                return;
        }
    }
    else if (mode.equals("single"))
        results = AllWikiLinksearch.crossWikiLinksearch(true, 1, domain, 
            Arrays.asList(Wiki.createInstance(wikiinput), https, mailto, ns);

    for (Map.Entry<Wiki, List<String[]>> entry : results.entrySet())
    {
        Wiki wiki = entry.getKey();
        List<String[]> value = entry.getValue();
        out.println("<h3>" + wiki.getDomain() + "</h3>");
        out.println(ParserUtils.linksearchResultsToHTML(value, wiki, domain));
        out.println("<p>");
        if (value.size() > 500)
            out.print("At least ");
        out.print(value.size());
        out.print(" links found ");
        out.print("(<a href=\"" + wiki.getPageURL("Special:Linksearch/*." + domain)
            + "\">HTTP linksearch</a> | ");
        out.println("<a href=\"" + wiki.getPageURL("Special:Linksearch/https://*." + domain)
            + "\">HTTPS linksearch</a>).");
    }
%>
<%@ include file="footer.jsp" %>