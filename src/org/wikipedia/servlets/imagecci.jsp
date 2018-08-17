<%--
    @(#)imagecci.jsp 0.03 07/02/2018
    Copyright (C) 2011 - 2018 MER-C

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
--%>

<%@ include file="header.jsp" %>
<%@ page pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@ page import="java.net.URLEncoder" %>

<%
    request.setAttribute("toolname", "Image contribution surveyor");

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

    String user = request.getParameter("user");
    String homewiki = request.getParameter("wiki");
    homewiki = (homewiki == null) ? "en.wikipedia.org" : ServletUtils.sanitizeForAttribute(homewiki);
    Wiki.User wpuser = null;
    
    if (user != null)
    {
        Wiki wiki = Wiki.createInstance(homewiki);
        wpuser = wiki.getUser(user);
        if (wpuser != null)
        {
            // create download prompt
            response.setContentType("text/plain;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(user, "UTF-8") + ".txt");

            // get results
            ContributionSurveyor surveyor = new ContributionSurveyor(wiki);
            if (!earliest.isEmpty())
                surveyor.setEarliestDateTime(earliestdate);
            if (!latest.isEmpty())
                surveyor.setLatestDateTime(latestdate);
            String[][] survey = surveyor.imageContributionSurvey(wpuser);

            // write results
            out.print(Users.generateWikitextSummaryLinks(user));
            out.println("* Survey URL: " + request.getRequestURL() + "?" + request.getQueryString());
            out.print(surveyor.formatImageSurveyAsWikitext(null, survey));
            out.println(surveyor.generateWikitextFooter());
            out.flush();
            out.close();
            return;
        }
    }
%>

<!doctype html>
<html>
<head>
<link rel=stylesheet href="styles.css">
<title><%= request.getAttribute("toolname") %></title>
</head>

<body>
<p>
This tool generates a listing of a user's image uploads for use at <a
href="//en.wikipedia.org/wiki/WP:CCI">Contributor copyright investigations.</a>

<p>
<form action="./imagecci.jsp" method=GET>
<table>
<tr>
    <td>User to survey:
    <td><input type=text name=user value="<%= user == null ? "" : ServletUtils.sanitizeForAttribute(user) %>" required>
<tr>
    <td>Home wiki:
    <td><input type=text name="wiki" value="<%= homewiki %>" required>
<tr>
    <td>Include uploads from:
    <td><input type=date name=earliest value="<%= earliest %>"></input> to 
        <input type=date name=latest value="<%= latest %>"></input> (inclusive)
</table>
<input type=submit value="Survey user">
</form>

<%
    if (user != null && wpuser == null)
        request.setAttribute("error", "ERROR: User " + ServletUtils.sanitizeForHTML(user) + " does not exist!");
%>
<%@ include file="footer.jsp" %>
