<!--
    @(#)spamarchivesearch.jsp 0.01 24/01/2017
    Copyright (C) 2011 - 2017 MER-C
  
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.
  
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
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