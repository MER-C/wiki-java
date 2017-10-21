<!--
    @(#)editorintersection.jsp 0.01 05/10/2017
    Copyright (C) 2017 MER-C
  
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
<%@ page contentType="text/html" pageEncoding="UTF-8" 
    trimDirectiveWhitespaces="true" %>

<%
    request.setAttribute("toolname", "Article/editor intersection (beta)");

    String wikiparam = request.getParameter("wiki");
    if (wikiparam == null)
        wikiparam = "en.wikipedia.org";
    else
        wikiparam = ServletUtils.sanitizeForAttribute(wikiparam);

    String mode = request.getParameter("mode");
    if (mode == null)
        mode = "pages";

    String pages = request.getParameter("pages");
    if (pages == null)
        pages = "";
    else
        pages = ServletUtils.sanitizeForHTML(pages);

    String category = request.getParameter("category");
    if (category != null)
        category = ServletUtils.sanitizeForAttribute(category);

    String user = request.getParameter("user");
    if (user != null)
        user = ServletUtils.sanitizeForAttribute(user);

    boolean noadmin = (request.getParameter("noadmin") != null);
    boolean nobot = (request.getParameter("nobot") != null);
    boolean noanon = (request.getParameter("noanon") != null);
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
</table>
<br>
<input type=submit value=Search>
</form>

<%
    Wiki wiki = Wiki.createInstance(wikiparam);
    wiki.setMaxLag(-1);
    wiki.setQueryLimit(1500);

    String[] pagesarray = null;
    if (mode.equals("category"))
    {
        out.println("<hr>");
        pagesarray = wiki.getCategoryMembers(category);
        if (pagesarray.length == 0)
        {
%>
    <span class="error">Category <%= ServletUtils.sanitizeForHTML(category) %> does not exist or is empty!</span>
    <%@ include file="footer.jsp" %>
<%
            return;
        }
    }
    else if (mode.equals("contribs"))
    {
        out.println("<hr>");
        pagesarray = Arrays.stream(wiki.contribs(user))
            .map(Wiki.Revision::getPage)
            .distinct()
            .toArray(String[]::new);
        if (pagesarray.length == 0)
        {
%>
    <span class="error">User <%= ServletUtils.sanitizeForHTML(user) %> does not exist or has no edits!</span>
    <%@ include file="footer.jsp" %>
<%
            return;
        }
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
        String[] temp = pages.split("\r\n");
        for (int i = 0; i < temp.length; i++)
            temp[i] = temp[i].trim();
        pagesarray = temp;
    }
    pagesarray = Arrays.copyOf(pagesarray, Math.min(pagesarray.length, 24));
        
    Map<String, List<Wiki.Revision>> results = ArticleEditorIntersection.articleEditorIntersection(wiki, pagesarray, noadmin, nobot, noanon);
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
            String title = entry2.getKey() + " &ndash; " + revs.size() + " edit";
            if (revs.size() > 1)
                title += "s";
            sb.append("<p>\n");
            sb.append(ServletUtils.beginCollapsibleSection(title, true));
            sb.append(ParserUtils.revisionsToHTML(wiki, revs.toArray(new Wiki.Revision[revs.size()])));
            sb.append(ServletUtils.endCollapsibleSection());
        }
        return sb;
    }).collect(Collectors.joining());
    out.println(blah);
%>
<%@ include file="footer.jsp" %>