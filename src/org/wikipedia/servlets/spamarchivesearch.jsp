<!--
    @(#)spamarchivesearch.jsp 0.01 24/01/2017
    Copyright (C) 2011 - 2017 MER-C
  
    This is free software: you are free to change and redistribute it under the 
    Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html> 
    for details. There is NO WARRANTY, to the extent permitted by law.
-->

<%
    request.setAttribute("toolname", "Spam archive search");
    String query = request.getParameter("query");
%>
<%@ include file="header.jsp" %>

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
        ArrayList<Map<String, Object>> results = SpamArchiveSearch.archiveSearch(query);
        for (Map<String, Object> result : results)
        {
            String title = (String)result.get("title");
            String blahwiki = title.contains("Talk:Spam blacklist") ? "meta.wikimedia" : "en.wikipedia";
%>
    <li><a href="//<%= blahwiki %>.org/wiki/<%= title %>"><%= title %></a>
<%
        }
%>
</ul>
<p><%= results.size() %> results found.
<%
    }
%>
<%@ include file="footer.jsp" %>