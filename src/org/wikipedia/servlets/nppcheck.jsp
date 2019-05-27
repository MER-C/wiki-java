<!--
    @(#)nppcheck.jsp 0.01 26/05/2019
    Copyright (C) 2019 - MER-C
  
    This is free software: you are free to change and redistribute it under the 
    Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html> 
    for details. There is NO WARRANTY, to the extent permitted by law.
-->
<%@ include file="datevalidate.jspf" %>
<%
    request.setAttribute("toolname", "NPP/AFC checker");

    String username = ServletUtils.sanitizeForAttribute(request.getParameter("username"));
    NPPCheck.Mode mode = NPPCheck.Mode.fromString(request.getParameter("mode"));
    String offsetparam = request.getParameter("offset");
    if (offsetparam == null)
        offsetparam = "0";
%>
<%@ include file="header.jsp" %>

<p>
This tool retrieves recent new page patrols and moves from draft/user space to 
main space for a given user (or for all users) and page metadata. A query limit of 7500 log entries applies.

<form action="./nppcheck.jsp" method=GET>
<table>
<tr>
    <td>Username (leave blank for all):
    <td><input type=text name=username value="<%= username %>">
<tr>
    <td>Fetch:
    <td><input type=radio name=mode value="patrols" <%= mode == NPPCheck.Mode.PATROLS ? " checked" : "" %>>New page patrols
        <input type=radio name=mode value="drafts" <%= mode == NPPCheck.Mode.DRAFTS ? " checked" : "" %>>Moves from Draft to Main
        <input type=radio name=mode value="userspace" <%= mode == NPPCheck.Mode.USERSPACE ? " checked" : "" %>>Moves from User to Main
<tr>
    <td>Show patrols from:
    <td><input type=date name=earliest value="<%= earliest %>"> to 
        <input type=date name=latest value="<%= latest %>"> (inclusive)
</table>
<input type=hidden name=offset value="<%= offsetparam %>">
<input type=submit value="Search">
</form>

<%
    if (mode == null)
    {
%>
<%@ include file="footer.jsp" %>
<%
    }
    out.println("<hr>");

    Wiki enWiki = Wiki.newSession("en.wikipedia.org");
    Users users = Users.of(enWiki);
    enWiki.setMaxLag(-1);
    enWiki.setQueryLimit(7500);
    NPPCheck check = new NPPCheck(enWiki);
    List<Wiki.LogEntry> logs = check.fetchLogs(username, earliest_odt, latest_odt, mode);

    if (logs.isEmpty())
    {
%>
<p>No results found!
<%@ include file="footer.jsp" %>
<%
    }

    // fetch metadata
    // limit to 50 articles per page
    int offset = Integer.parseInt(offsetparam);
    List<Wiki.LogEntry> logsub = logs.subList(offset, Math.min(logs.size(), offset + 51));
    List<Duration> dt_patrol = Events.timeBetweenEvents(logsub);
    dt_patrol.add(Duration.ofSeconds(-1));
    if (logsub.size() == 51)
        logsub.remove(50);
    Map<String, Object>[] pageinfo = check.fetchMetadata(logsub, mode);

    String requesturl = "./nppcheck.jsp?username=" + username + "&earliest=" + earliest
        + "&latest=" + latest + "&mode=" + request.getParameter("mode") + "&offset=";
    out.println(ServletUtils.generatePagination(requesturl, offset, 50, logs.size()));

    // output table to HTML
%>
<table class="wikitable">
<tr>
<%
    if (mode != NPPCheck.Mode.PATROLS)
        out.println("  <th>Draft");
%>
  <th>Article
  <th>Create timestamp
  <th>Review timestamp
  <th>Article age at review (s)
  <th>Time between reviews (s)
  <th>Size
  <th>Author
  <th>Author registration timestamp
  <th>Author edit count
  <th>Author age at creation (days)
  <th>Author blocked?
<%
    if (username.isEmpty())
        out.println("  <th>Reviewer");

    for (int i = 0; i < pageinfo.length; i++)
    {
        Map<String, Object> info = pageinfo[i];
        Wiki.Revision first = (Wiki.Revision)info.get("firstrevision");
        Wiki.LogEntry entry = (Wiki.LogEntry)info.get("logentry");
        Wiki.User creator = (Wiki.User)info.get("creator");
        String title = (String)info.get("pagename");
        OffsetDateTime patroldate = entry.getTimestamp();
        int size = (Integer)info.get("size");

        OffsetDateTime createdate = null;
        OffsetDateTime registrationdate = null;
        int editcount = -1;
        String creatorname = "null";
        boolean blocked = false;

        Duration dt_article = Duration.ofSeconds(-1);
        Duration dt_user = Duration.ofSeconds(-86401);

        if (first != null)
        {
            creatorname = first.getUser();
            createdate = first.getTimestamp();
            dt_article = Duration.between(createdate, patroldate);
            if (creator != null)
            {
                editcount = creator.countEdits();
                registrationdate = creator.getRegistrationDate();
                blocked = creator.isBlocked();
                if (registrationdate != null)
                    dt_user = Duration.between(registrationdate, createdate);
            }
        }

        out.println("<tr>");
        if (mode != NPPCheck.Mode.PATROLS)
        {
            String draft = entry.getTitle();
            out.println("  <td><a href=\"" + enWiki.getPageUrl(draft) + "\">" + draft + "</a>");
        }
%>
  <td><a href="<%= enWiki.getPageUrl(title) %>"><%= title %></a>
  <td><%= createdate %>
  <td><%= patroldate %>
  <td><%= dt_article.getSeconds() %>
  <td><%= dt_patrol.get(i).getSeconds() %>
  <td><%= size %>
  <td><%= users.generateHTMLSummaryLinksShort(creatorname) %>
  <td><%= registrationdate %>
  <td><%= editcount %>
  <td><%= dt_user.getSeconds() / 86400 %>
  <td><%= blocked %>
<%
        if (username.isEmpty())
        {
            String reviewer = entry.getUser();
            out.println("  <td>" + users.generateHTMLSummaryLinksShort(reviewer));
        }
    }
    out.println("</table>");

    // output pagination
    out.println(ServletUtils.generatePagination(requesturl, offset, 50, logs.size()));
%>
<%@ include file="footer.jsp" %>

