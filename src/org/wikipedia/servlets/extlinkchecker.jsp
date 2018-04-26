<!--
    @(#)extlinkchecker.jsp 0.01 24/04/2018
    Copyright (C) 2018-20xx MER-C
  
    This is free software: you are free to change and redistribute it under the 
    Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html> 
    for details. There is NO WARRANTY, to the extent permitted by law.
-->

<%@ include file="header.jsp" %>
<%@ page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>

<%
    request.setAttribute("toolname", "External link checker (beta)");

    String wiki = request.getParameter("wiki");
    wiki = (wiki == null) ? "en.wikipedia.org" : ServletUtils.sanitizeForAttribute(wiki);

    String title = request.getParameter("title");
    title = (title == null) ? "" : ServletUtils.sanitizeForAttribute(title);
%>

<!doctype html>
<html>
<head>
<link rel=stylesheet href="styles.css">
<title><%= request.getAttribute("toolname") %></title>
</head>

<body>
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
        out.println("<hr>");

        Wiki enWiki = Wiki.createInstance(wiki);
        ExternalLinkPopularity elp = new ExternalLinkPopularity(enWiki);
        elp.getExcludeList().addAll(Arrays.asList("wmflabs.org", "edwardbetts.com", "archive.org"));
        Map<String, Map<String, List<String>>> results = elp.fetchExternalLinks(Arrays.asList(title));
        
        if (results.get(title).isEmpty())
            out.println("<span class=\"error\">No results found!</span>");
        else
        {
            Map<String, Map<String, Integer>> popresults = elp.determineLinkPopularity(results);
            out.println(elp.exportResultsAsHTML(results, popresults));
        }
    }
%>
<%@ include file="footer.jsp" %>