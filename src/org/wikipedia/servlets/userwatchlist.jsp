<!--
    @(#)userwatchlist.jsp 0.01 24/01/2017
    Copyright (C) 2015 - 2017 MER-C
  
    This is free software: you are free to change and redistribute it under the 
    Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html> 
    for details. There is NO WARRANTY, to the extent permitted by law.
-->

<%@ include file="header.jsp" %>
<%@ page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>

<%
    request.setAttribute("toolname", "User watchlist");

    String inputpage = request.getParameter("page");
    String inputpage_url = "";
    String inputpage_attribute = "";
    if (inputpage != null)
    {
        inputpage_url = ServletUtils.sanitizeForURL(inputpage);
        inputpage_attribute = ServletUtils.sanitizeForAttribute(inputpage);
    }

    String temp = request.getParameter("skip");
    int skip = (temp == null) ? 0 : Integer.parseInt(temp);
    skip = Math.max(skip, 0);
%>

<!doctype html>
<html>
<head>
<link rel=stylesheet href="styles.css">
<title><%= request.getAttribute("toolname") %></title>
</head>

<body>
<p>
This tool retrieves recent (<5 days) contributions of a list of users. There is 
a limit of 30 users per request, though the list may be of indefinite length.

<p>
Syntax: one user per line, reason after # . Example:
        
<pre>
Example user # Copyright violations
// This is a comment
Someone # Spam
</pre>

<form action="./userwatchlist.jsp" method=GET>
<table>
<tr>
    <td>Input page:
    <td>
        <input type=text size=30 name=page required value="<%= inputpage_attribute %>">
        <%
        if (inputpage != null)
        {
        %>
        <a href="<%= wiki.getPageURL(inputpage) %>">visit</a> |
        <a href="<%= wiki.getIndexPHPURL() + "?action=edit&title=" + inputpage_url %>">edit</a>
        <%
        }
        %>
        
<tr><td>Skip:
    <td><input type=number size=30 name=skip value="<%= skip %>">
</table>
<input type=submit value="Submit">
</form>

<%
    if (inputpage == null)
    {
        %>
<%@ include file="footer.jsp" %>
        <%
        return;
    }
%>
<hr>
<p>
<%
    if (!inputpage.matches("^User:.+/.+\\.(cs|j)s$"))
    {
        %>
<span class="error">TESTING WOOP WOOP WOOP!</span>
<%@ include file="footer.jsp" %>
        <%
        return;
    }
    Wiki enWiki = Wiki.createInstance("en.wikipedia.org");
    enWiki.setMaxLag(-1);
    String us = inputpage.substring(5, inputpage.indexOf('/'));
    Wiki.User us2 = enWiki.getUser(us);
    if (us2 == null || !us2.isA("sysop"))
    {
        %>
<span class="error">TESTING TESTING WOOP WOOP WOOP!</span>
<%@ include file="footer.jsp" %>
        <%
        return;
    }
    String text;
    try
    {
        text = enWiki.getPageText(inputpage);
    }
    catch (FileNotFoundException ex) 
    {
        %>
<span class="error">ERROR: page &quot;<%= ServletUtils.sanitizeForHTML(inputpage) %>&quot; does not exist!</span>
<%@ include file="footer.jsp" %>
        <%
        return;
    }

    // parse input
    String[] lines = text.split("\n");
    Map<String, String> input = new LinkedHashMap<>();
    for (String user : lines)
    {
        // remove comments, parse reasons
        user = user.trim();
        if (user.contains("//"))
            user = user.substring(0, user.indexOf("//")).trim();
        int boundary = user.indexOf("#");
        String reason = "";
        if (boundary >= 0)
        {
            reason = user.substring(boundary + 1).trim();
            user = user.substring(0, boundary).trim();
        }
        if (user.isEmpty())
            continue;
        input.put(user, reason);
    }

    // top pagination
    if (skip > 0)
    {
    %>
<a href="./userwatchlist.jsp?page=<%= inputpage_url %>&skip=<%= Math.max(0, skip - 30) %>">Previous 30</a> | ");
    <%
    }
    else
        out.print("Previous 30 | ");

    if (input.size() - skip > 30)
    {
    %>
<a href="./userwatchlist.jsp?page=<%= inputpage_url %>&skip=<%= skip + 30 %>">Next 30</a>
    <%
    }
    else
        out.println("Next 30");

    for (Map.Entry<String, String> entry : input.entrySet())
    {
        String user = entry.getKey();
        String reason = ServletUtils.sanitizeForHTML(entry.getValue());
        // user links
        %>
<h3><%= user %></h3>
<p>
<ul>
    <li>
        <%
        out.println(ParserUtils.generateUserLinks(enWiki, user));
        if (!reason.isEmpty())
            out.println("<li><i>" + reason + "</i>");
        out.println("</ul>");

        // fetch and output contribs
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(5);
        Wiki.Revision[] contribs = enWiki.contribs(user, cutoff, null);
        if (contribs.length == 0)
            out.println("<p>No recent contributions or user does not exist.");
        else
            out.println(ParserUtils.revisionsToHTML(enWiki, contribs));
    }

    // end pagination
    if (skip > 0)
    {
    %>
<a href="./userwatchlist.jsp?page=<%= inputpage_url %>&skip=<%= Math.max(0, skip - 30) %>">Previous 30</a> | ");
    <%
    }
    else
        out.print("Previous 30 | ");

    if (input.size() - skip > 30)
    {
    %>
<a href="./userwatchlist.jsp?page=<%= inputpage_url %>&skip=<%= skip + 30 %>">Next 30</a>
    <%
    }
    else
        out.println("Next 30");

%>
<%@ include file="footer.jsp" %>
