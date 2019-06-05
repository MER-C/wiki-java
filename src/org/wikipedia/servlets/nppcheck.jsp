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

    WMFWiki enWiki = WMFWiki.newSession("en.wikipedia.org");
    Users users = Users.of(enWiki);
    Pages pageutils = Pages.of(enWiki);
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
    Map<String, Object>[] pageinfo = check.fetchMetadata(logsub);
    pageinfo = check.fetchCreatorMetadata(pageinfo);
    List<String> snippets = check.fetchSnippets(logsub);
    List<String> reviewers = new ArrayList<>();
    List<String> drafts = new ArrayList<>();
    for (Wiki.LogEntry log : logsub)
    {
        reviewers.add(log.getUser());
        if (mode != NPPCheck.Mode.PATROLS)
            drafts.add(log.getTitle());
    }
    List<Wiki.User> reviewerdata = new ArrayList<>();
    if (username.isEmpty())
        reviewerdata = enWiki.getUsers(reviewers);
    Map<String, Object>[] draftinfo = null;
    if (mode != NPPCheck.Mode.PATROLS)
        draftinfo = enWiki.getPageInfo(drafts.toArray(new String[0]));

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
  <th>Article age at review
<%
    if (!username.isEmpty())
    {
        out.println("<th>Time between reviews");
    }
%>
  <th>Size
  <th>Author
  <th>Author registration timestamp
  <th>Author edit count
  <th>Author age at creation
  <th>Author blocked
<%
    if (username.isEmpty())
    {
        out.println("  <th>Reviewer");
        out.println("  <th>Reviewer edit count");
    }
    out.println("<th>Snippet");

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
        
        out.println("<tr class=\"revision\">");
        if (mode != NPPCheck.Mode.PATROLS)
        {
            String draft = entry.getTitle();
            out.println("  <td class=\"title\">" + pageutils.generatePageLink(draft, (Boolean)draftinfo[i].get("exists")));
        }
%>
  <td class="title"><%= pageutils.generatePageLink(title, (Boolean)pageinfo[i].get("exists")) %>
  <td class="date"><%= createdate %>
  <td class="date"><%= patroldate %>
  <td class="revsize"><%= MathsAndStats.formatDuration(dt_article) %>
<%
    if (!username.isEmpty())
    {
        out.println("<td class=\"revsize\">" + MathsAndStats.formatDuration(dt_patrol.get(i)));
    }
%>
  <td class="revsize"><%= size %>
  <td class="user"><%= users.generateHTMLSummaryLinksShort(creatorname) %>
  <td class="date"><%= registrationdate %>
  <td class="revsize"><%= editcount %>
  <td class="revsize"><%= MathsAndStats.formatDuration(dt_user) %>
  <td class="boolean"><%= blocked %>
<%
        if (username.isEmpty())
        {
            String reviewer = entry.getUser();
            out.println("  <td class=\"user\">" + users.generateHTMLSummaryLinksShort(reviewer));
            out.println("  <td class=\"revsize\">" + reviewerdata.get(i).countEdits());
        }
        out.println("  <td>" + snippets.get(i));
    }
    out.println("</table>");

    // output pagination
    out.println(ServletUtils.generatePagination(requesturl, offset, 50, logs.size()));
%>
<%@ include file="footer.jsp" %>

