<!--
    @(#)userwatchlist.jsp 0.02 31/08/2017
    Copyright (C) 2015 - 20xx MER-C

    This is free software: you are free to change and redistribute it under the
    Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html>
    for details. There is NO WARRANTY, to the extent permitted by law.
-->

<%
    request.setAttribute("toolname", "User watchlist");
    request.setAttribute("earliest_default", LocalDate.now(ZoneOffset.UTC).minusDays(30));

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
    boolean newonly = (request.getParameter("newonly") != null);

    Wiki enWiki = Wiki.newSession("en.wikipedia.org");
    enWiki.setMaxLag(-1);
    enWiki.setQueryLimit(30000); // 60 network requests
    Users userUtils = Users.of(enWiki);
    Revisions revisionUtils = Revisions.of(enWiki);
    Pages pageUtils = Pages.of(enWiki);
%>

<%@ include file="datevalidate.jspf" %>
<%@ include file="header.jspf" %>

<p>
This tool retrieves contributions of a list of users. There is a limit of 50
users per request, though the list may be of indefinite length.

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
    <td>Input page or category:
    <td>
        <input type=text size=50 name=page required value="<%= inputpage_attribute %>">
        <%
        if (inputpage != null)
        {
            out.print("(" + pageUtils.generatePageLink(inputpage, "visit") + " &middot; ");
        %>
        <a href="<%= enWiki.getIndexPhpUrl() + "?action=edit&title=" + inputpage_url %>">edit</a>)
        <%
        }
        %>

<tr><td>Show changes from:
    <td><input type=date name=earliest value="<%= earliest %>"> to
        <input type=date name=latest value="<%= latest %>"> (inclusive)
<tr><td>Show:
    <td><input type=checkbox name=newonly value=1<%= newonly ? " checked" : 
        "" %>>New pages only
<tr><td>Skip:
    <td><input type=number size=50 name=skip value="<%= skip %>">
</table>
<input type=submit value="Submit">
</form>

<%
    if (inputpage == null)
    {
        %>
<%@ include file="footer.jspf" %>
        <%
    }

    Map<String, String> input = new LinkedHashMap<>();
    if (enWiki.namespace(inputpage) == Wiki.CATEGORY_NAMESPACE)
    {
        List<String> catmembers = enWiki.getCategoryMembers(inputpage);
        for (String member : catmembers)
            input.put(enWiki.removeNamespace(member), "");
    }
    else if (inputpage.matches("^User:.+/.+\\.(cs|j)s$"))
    {
        String us = inputpage.substring(5, inputpage.indexOf('/'));
        Wiki.User us2 = enWiki.getUser(us);
        if (us2 == null || !us2.isA("sysop"))
        {
            request.setAttribute("error", "TESTING WOOP WOOP WOOP!");
%>
<%@ include file="footer.jspf" %>
<%
        }
        String text = enWiki.getPageText(inputpage);
        if (text == null)
        {
            request.setAttribute("error", "ERROR: page &quot;" + ServletUtils.sanitizeForHTML(inputpage) + "&quot; does not exist!");
%>
<%@ include file="footer.jspf" %>
<%
        }
        // parse input
        String[] lines = text.split("\n");

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
    }
    else
    {
        request.setAttribute("error", "TESTING WOOP WOOP WOOP!");
%>
<%@ include file="footer.jspf" %>
<%
    }

    if (input.isEmpty())
    {
        request.setAttribute("error", "ERROR: no users found!");
%>
<%@ include file="footer.jspf" %>
<%
    }

    // top pagination
    String requesturl = "./userwatchlist.jsp?page=" +  inputpage_url + "&earliest=" + earliest
        + "&latest=" + latest + "&skip=";
    out.println("<hr>");
    out.println(ServletUtils.generatePagination(requesturl, skip, 50, input.size()));

    // fetch contributions
    Wiki.RequestHelper rh = enWiki.new RequestHelper()
        .withinDateRange(earliest_odt, latest_odt);
    if (newonly)
        rh = rh.filterBy(Map.of("new", Boolean.TRUE));
    List<String> users = new ArrayList<>(input.keySet());
    List<String> userstofetch = users.subList(skip, Math.min(skip + 50, users.size()));
    List<List<Wiki.Revision>> contribs = enWiki.contribs(userstofetch, null, rh);

    for (int i = 0; i < userstofetch.size(); i++)
    {
        String user = userstofetch.get(i);
        String reason = ServletUtils.sanitizeForHTML(input.get(user));
        // user links
        %>
<h3><%= user %></h3>
<p>
<ul>
    <li>
        <%
        out.println(userUtils.generateHTMLSummaryLinks(user));
        if (!reason.isEmpty())
            out.println("<li><i>" + reason + "</i>");
        out.println("</ul>");

        // write contribs
        List<Wiki.Revision> usercontribs = contribs.get(i);
        if (usercontribs.isEmpty())
            out.println("<p>No contributions within date range or user does not exist.");
        else
            out.println(revisionUtils.toHTML(usercontribs));
    }

    // end pagination
    out.println(ServletUtils.generatePagination(requesturl, skip, 50, input.size()));
%>
<%@ include file="footer.jspf" %>
