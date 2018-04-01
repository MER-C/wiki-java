<!--
    @(#)editorintersection.jsp 0.01 05/10/2017
    Copyright (C) 2017 MER-C
  
    This is free software: you are free to change and redistribute it under the 
    Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html> 
    for details. There is NO WARRANTY, to the extent permitted by law.
-->

<%@ include file="header.jsp" %>
<%@ page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>

<%
    request.setAttribute("toolname", "Article/editor intersection (beta)");

    String wikiparam = request.getParameter("wiki");
    wikiparam = (wikiparam == null) ? "en.wikipedia.org" : ServletUtils.sanitizeForAttribute(wikiparam);

    String mode = request.getParameter("mode");
    if (mode == null)
        mode = "pages";

    String pages = request.getParameter("pages");
    pages = (pages == null) ? "" : ServletUtils.sanitizeForHTML(pages);

    String category = request.getParameter("category");
    if (category != null)
        category = ServletUtils.sanitizeForAttribute(category);

    String user = request.getParameter("user");
    if (user != null)
        user = ServletUtils.sanitizeForAttribute(user);

    String earliest = request.getParameter("earliest");
    OffsetDateTime earliestdate = null;
    earliest = (earliest == null) ? "" : ServletUtils.sanitizeForAttribute(earliest);
    if (!earliest.isEmpty())
        earliestdate = OffsetDateTime.parse(earliest + "T00:00:00Z");
    
    String latest = request.getParameter("latest");
    OffsetDateTime latestdate = null;
    latest = (latest == null) ? "" : ServletUtils.sanitizeForAttribute(latest);
    if (!latest.isEmpty())
        latestdate = OffsetDateTime.parse(latest + "T23:59:59Z");
    
    boolean noadmin = (request.getParameter("noadmin") != null);
    boolean nobot = (request.getParameter("nobot") != null);
    boolean noanon = (request.getParameter("noanon") != null);
    boolean nominor = (request.getParameter("nominor") != null);
    boolean noreverts = (request.getParameter("noreverts") != null);
%>

<!doctype html>
<html>
<head>
<link rel=stylesheet href="styles.css">
<script src="collapsible.js"></script>
<script src="EditorIntersection.js"></script>
<title><%= request.getAttribute("toolname") %></title>
</head>

<body>
<p>
This tool retrieves the common editors of a given set of pages. Query limits of
1500 edits/revisions and 25 articles (contributions: most recent, category members: 
first in the GUI) apply.

<form action="./editorintersection.jsp" method=POST>
<table>
<tr>
    <td colspan=2>Wiki:
    <td><input type=text name=wiki value="<%= wikiparam %>" required>
<tr>
    <td><input type=radio name=mode id="radio_cat" value="category"<%= mode.equals("category") ? " checked" : "" %>>
    <td>Category:
    <td><input type=text id=category name=category <%= mode.equals("category") ? "value=\"" + category + "\" required" : "disabled"%>>
<tr>
    <td><input type=radio name=mode id="radio_user" value="contribs"<%= mode.equals("contribs") ? " checked" : "" %>>
    <td>Pages edited by:
    <td><input type=text id=user name=user <%= mode.equals("contribs") ? "value=\"" + user + "\" required" : "disabled"%>>
<tr>
    <td valign=top><input type=radio name=mode id="radio_pages" value="pages"<%= mode.equals("pages") ? " checked" : "" %>>
    <td valign=top>Pages:<br>(one per line)
    <td>
        <textarea id=pages name=pages rows=10 <%= mode.equals("pages") ? "required" : "disabled" %>>
<%= pages %>
        </textarea>
<tr>
    <td colspan=2>Exclude: 
    <td><input type=checkbox name=noadmin value=1<%= (pages.isEmpty() || noadmin) ? " checked" : "" %>>admins</input>
        <input type=checkbox name=nobot value=1<%= (pages.isEmpty() || nobot) ? " checked" : "" %>>bots</input>
        <input type=checkbox name=noanon value=1<%= noanon ? " checked" : "" %>>IPs</input>
        <input type=checkbox name=nominor value=1<%= nominor ? " checked" : "" %>>minor edits</input>
        <input type=checkbox name=noreverts value=1<%= noreverts ? " checked" : "" %>>reverts</input>
<tr>
    <td colspan=2>Show changes from:
    <td><input type=date name=earliest value="<%= earliest %>"></input> to 
        <input type=date name=latest value="<%= latest %>"></input> (inclusive)
</table>
<br>
<input type=submit value=Search>
</form>

<%
    if (earliestdate != null && latestdate != null && earliestdate.isAfter(latestdate))
    {
%>
    <hr>
    <span class="error">Earliest date is after latest date!</span>
<%@ include file="footer.jsp" %>
<%
        return;
    }

    Wiki wiki = Wiki.createInstance(wikiparam);
    wiki.setMaxLag(-1);
    wiki.setQueryLimit(1500);

    Stream<String> pagestream = null;
    if (mode.equals("category"))
        pagestream = Arrays.stream(wiki.getCategoryMembers(category));
    else if (mode.equals("contribs"))
    {
        out.println("<hr>");
        pagestream = Arrays.stream(wiki.contribs(user))
            .map(Wiki.Revision::getPage);
    }
    else if (mode.equals("pages"))
    {
        // state with no input parameters
        if (pages.isEmpty())
        {
%>
<%@ include file="footer.jsp" %>
<%
            return;
        }
        pagestream = Arrays.stream(pages.split("\r\n")).map(String::trim);
    }
    out.println("<hr>");
    String[] pagesarray = pagestream
        .distinct()
        .filter(wiki.namespace(page) >= 0)
        .limit(25)
        .toArray(String[]::new);
    if (pagesarray.length < 2)
    {
%>
    <span class="error">Need at least two distinct pages to perform an intersection!</span>
<%@ include file="footer.jsp" %>
<%
        return;
    }
    ArticleEditorIntersector aei = new ArticleEditorIntersector(wiki);
    aei.setIgnoringMinorEdits(nominor);
    aei.setIgnoringReverts(noreverts);
    if (!earliest.isEmpty())
        aei.setEarliestDateTime(earliestdate);
    if (!latest.isEmpty())
        aei.setLatestDateTime(latestdate);
    Map<String, List<Wiki.Revision>> results = aei.intersectArticles(pagesarray, noadmin, nobot, noanon);
    if (results.isEmpty())
    {
%>
    <span class="error">No intersection after applying exclusions and removing non-existing pages!</span>
<%@ include file="footer.jsp" %>
<%
        return;
    }
    Map<String, Map<String, List<Wiki.Revision>>> bypage = new HashMap<>();
    results.forEach((key, value) ->
    {
        // group by article
        Map<String, List<Wiki.Revision>> grouppage = value.stream()
            .collect(Collectors.groupingBy(Wiki.Revision::getPage));
        bypage.put(key, grouppage);
    });
    String blah = bypage.entrySet().stream().sorted((entry1, entry2) -> 
    {
        // sort by number of articles hit
        return entry2.getValue().size() - entry1.getValue().size();

    }).map(entry ->
    {
        // generate HTML
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>");
        sb.append(entry.getKey());
        sb.append("</h2>\n");
        sb.append(ParserUtils.generateUserLinks(wiki, entry.getKey()));

        for (Map.Entry<String, List<Wiki.Revision>> entry2 : entry.getValue().entrySet())
        {
            List<Wiki.Revision> revs = entry2.getValue();
            StringBuilder title = new StringBuilder("<a href=\"");
            title.append(wiki.getPageURL(entry2.getKey()));
            title.append("\">");
            title.append(entry2.getKey());
            title.append("</a> (<a href=\"");
            title.append(wiki.getIndexPHPURL());
            title.append("?title=");
            title.append(entry2.getKey());
            title.append("&action=history\">history</a>) &ndash; ");
            title.append(revs.size());
            title.append(" edit");
            if (revs.size() > 1)
                title.append("s");
            sb.append("<p>\n");
            sb.append(ServletUtils.beginCollapsibleSection(title.toString(), true));
            sb.append(ParserUtils.revisionsToHTML(wiki, revs.toArray(new Wiki.Revision[revs.size()])));
            sb.append(ServletUtils.endCollapsibleSection());
        }
        return sb;
    }).collect(Collectors.joining());
    out.println(blah);
%>
<%@ include file="footer.jsp" %>