<!--
    @(#)xwikilinksearch.jsp 0.03 06/07/2023
    Copyright (C) 2011 - 2023 MER-C
  
    This is free software: you are free to change and redistribute it under the 
    Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html> 
    for details. There is NO WARRANTY, to the extent permitted by law.
-->
<%@ include file="security.jspf" %>
<%
    request.setAttribute("toolname", "Cross-wiki linksearch");
    request.setAttribute("scripts", new String[] { "common.js", "XWikiLinksearch.js" });
    int limit = 500;

    String mode = Objects.requireNonNullElse(request.getParameter("mode"), "multi");
    String domain = ServletUtils.sanitizeForAttribute(request.getParameter("link"));
    String set = Objects.requireNonNullElse(request.getParameter("set"), "top25");
    String wikiinput = request.getParameter("wiki");
    if (wikiinput != null)
        wikiinput = ServletUtils.sanitizeForAttribute(wikiinput);
    boolean mailto = (request.getParameter("mailto") != null);

    String temp = request.getParameter("ns");
    boolean mainns = temp != null && temp.equals("0");
    int[] ns = mainns ? new int[] { Wiki.MAIN_NAMESPACE } : new int[0];
%>
<%@ include file="header.jspf" %>

<p>
This tool searches various Wikimedia projects for a specific link. Enter a 
domain name (example.com, not *.example.com or http://example.com) below. A 
timeout is more likely when searching for more wikis or protocols. For performance
reasons, results are limited to <%= limit %> links per wiki.

<form name="spamform" action="./linksearch.jsp" method=GET>
<table>
<tr>
    <td><input id="radio_multi" type=radio name=mode value=multi<%= mode.equals("multi") ?
         " checked" : "" %>>
    <td><label for="radio_multi">Wikis to search:</label>
    <td><select name=set id=set<%= mode.equals("multi") ? "" : " disabled" %>>
            <option value="top25"<%= set.equals("top25") ? " selected" : ""%>>Top 25 Wikipedias</option>
            <option value="top50"<%= set.equals("top50") ? " selected" : ""%>>Top 50 Wikipedias</option>
            <option value="major"<%= set.equals("major") ? " selected" : ""%>>Major Wikimedia projects</option>
        </select>
        
<tr>
    <td><input id="radio_single" type=radio name=mode value=single<%= mode.equals("single") ?
         " checked" : "" %>>
    <td><label for="radio_single">Single wiki:</label>
    <td><input type=text id=wiki name=wiki <%= mode.equals("single") ? "required value=" + 
        wikiinput : "disabled" %>>
        
<tr>
    <td colspan=2>Domain to search:
    <td><input type=text name=link required value="<%= domain %>">
        
<tr>
    <td colspan=2>Additional protocols:
    <td><input type=checkbox name=mailto id="mailto" value=1<%= mailto ? " checked" : "" %>>
        <label for="mailto">mailto</label>

<tr>
    <td><input type=checkbox name=ns id="main_ns" value=0<%= mainns ? " checked" : "" %>>
    <td colspan=3><label for="main_ns">Main namespace only? (May be unreliable.)</label>

</table>
<br>
<input type=submit value=Search>
</form>

<%
    // state with no input parameters
    if (domain.isEmpty())
    {
%>
<%@ include file="footer.jspf" %>
<%
    }
    Map<Wiki, List<String[]>> results = null;
    if (mode.equals("multi"))
    {
        results = switch (set)
        {
            case "top25" -> AllWikiLinksearch.crossWikiLinksearch(limit, 1, 
                domain, AllWikiLinksearch.TOP25, mailto, ns);
            case "top50" -> AllWikiLinksearch.crossWikiLinksearch(limit, 1, 
                domain, AllWikiLinksearch.TOP50, mailto, ns);
            case "major" -> AllWikiLinksearch.crossWikiLinksearch(limit, 1, 
                domain, AllWikiLinksearch.MAJOR_WIKIS, mailto, ns);
            default -> 
            {
                request.setAttribute("error", "Invalid wiki set selected!");
%>
<%@ include file="footer.jspf" %>
<%
            }
        };
    }
    else if (mode.equals("single"))
        results = AllWikiLinksearch.crossWikiLinksearch(limit, 1, domain, 
            List.of(sessions.sharedSession(wikiinput)), mailto, ns);

    out.println("<hr>");
    for (Map.Entry<Wiki, List<String[]>> entry : results.entrySet())
    {
        Wiki wiki = entry.getKey();
        Pages pageutils = Pages.of(wiki);   
        List<String[]> value = entry.getValue();
        out.println("<h3>" + wiki.getDomain() + "</h3>");
        out.println(ExternalLinks.of(wiki).linksearchResultsToHTML(value, domain));
        out.println("<p>");
        if (value.size() == limit)
            out.print("At least ");
        out.print(value.size());
        out.print(" links found (");
        out.print(pageutils.generatePageLink("Special:Linksearch/*." + domain, "linksearch") + ").");
    }
%>
<%@ include file="footer.jspf" %>
