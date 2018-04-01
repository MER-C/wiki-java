<!--
    @(#)spamarchivesearch.jsp 0.01 24/01/2017
    Copyright (C) 2011 - 2017 MER-C
  
    This is free software: you are free to change and redistribute it under the 
    Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html> 
    for details. There is NO WARRANTY, to the extent permitted by law.
-->

<%@ include file="header.jsp" %>
<%@ page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>

<%
    request.setAttribute("toolname", "Spam archive search");
    String query = request.getParameter("query");
%>

<!doctype html>
<html>
<head>
<link rel=stylesheet href="styles.css">
<title><%= request.getAttribute("toolname") %></title>
</head>

<body>
<p>
This tool searches various spam related noticeboards for a given query string. 
If you want to search a domain name, please enclose it in quotation marks.

<form action="./spamarchivesearch.jsp" method=GET>
<p>Search string: 
    <input type=text name=query required<%
    if (query != null)
    {
    %> value="<%= ServletUtils.sanitizeForAttribute(query) %>"<%
    }
    %>>
    <input type=submit value="Search">
</form>

<%
    if (query != null)
    {
%>
<hr>
<ul>
<%
        ArrayList<String[]> results = SpamArchiveSearch.archiveSearch(query);
        for (String[] result : results)
        {
            String blahwiki = result[0].contains("Talk:Spam blacklist") ? "meta.wikimedia" : "en.wikipedia";
%>
    <li><a href="//<%= blahwiki %>.org/wiki/<%= result[0] %>"><%= result[0] %></a>
<%
        }
%>
</ul>
<p><%= results.size() %> results found.
<%
    }
%>
<%@ include file="footer.jsp" %>