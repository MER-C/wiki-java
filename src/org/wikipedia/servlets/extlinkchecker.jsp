<!--
    @(#)extlinkchecker.jsp 0.01 24/04/2018
    Copyright (C) 2018-20xx MER-C
  
    This is free software: you are free to change and redistribute it under the 
    Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html> 
    for details. There is NO WARRANTY, to the extent permitted by law.
-->
<%@ include file="security.jspf" %>
<%
    request.setAttribute("toolname", "External link checker");

    String wiki = ServletUtils.sanitizeForAttributeOrDefault(request.getParameter("wiki"), "en.wikipedia.org");
    String title = ServletUtils.sanitizeForAttribute(request.getParameter("title"));
%>
<%@ include file="header.jspf" %>

<p>
This tool performs linksearches to count how many live links exist to each 
unique domain used in external links by a given article. Useful for finding
long-standing reference spam.

<form action="./extlinkchecker.jsp" method=GET>
<table>
<tr>
    <td colspan=2>Wiki:
    <td><input type=text name=wiki value="<%= wiki %>" required>
<tr>
    <td colspan=2>Title:
    <td><input type=text name=title value="<%= title %>" required>
</table>
<br>
<input type=submit value=Search>
</form>

<%
    if (!title.isEmpty())
    {
        Wiki enWiki = sessions.sharedSession(wiki);
        ExternalLinkPopularity elp = new ExternalLinkPopularity(enWiki);
        elp.getExcludeList().addAll(List.of("wmflabs.org", "edwardbetts.com", "archive.org"));
        Map<String, Map<String, List<String>>> results = elp.fetchExternalLinks(List.of(title));
        
        if (results.get(title).isEmpty())
            request.setAttribute("error", "No results found!");
        else
        {
            Map<String, Integer> popresults = elp.determineLinkPopularity(ExternalLinkPopularity.flatten(results));
            out.println("<hr>");
            out.println(elp.exportResultsAsHTML(results, popresults));
        }
    }
%>
<%@ include file="footer.jspf" %>
