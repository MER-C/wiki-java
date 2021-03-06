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
<%@ include file="security.jspf" %>
<%@ include file="datevalidate.jspf" %>
<%
    request.setAttribute("toolname", "Image contribution surveyor");

    String user = request.getParameter("user");
    String homewiki = ServletUtils.sanitizeForAttributeOrDefault(request.getParameter("wiki"), "en.wikipedia.org");
    Wiki wiki = Wiki.newSession(homewiki);

    ContributionSurveyor surveyor = new ContributionSurveyor(wiki);
    Map<String, List<String>> survey = null;
    if (user != null)
    {
        Wiki.User wpuser = wiki.getUser(user);
        if (wpuser != null)
        {
            // get results
            request.setAttribute("contenttype", "text");
            surveyor.setDateRange(earliest_odt, latest_odt);
            survey = surveyor.imageContributionSurvey(wpuser);
        }
    }
%>
<%@ include file="header.jspf" %>
<%
    if (survey != null)
    {
        response.setHeader("Content-Disposition", "attachment; filename=" 
            + URLEncoder.encode(user, StandardCharsets.UTF_8) + ".txt");
        out.print(Users.generateWikitextSummaryLinks(user));
        out.println();
        List<String> sections = new ArrayList<>();
        sections.addAll(Pages.toWikitextPaginatedList(survey.get("local"), Pages.LIST_OF_LINKS, 
            (start, end) -> "===" + user + " Local files " + start + " to " + end + "===", 20, false));
        sections.addAll(Pages.toWikitextPaginatedList(survey.get("commons"), Pages.LIST_OF_LINKS, 
            (start, end) -> "===" + user + " Commons files " + start + " to " + end + "===", 20, false));
        sections.addAll(Pages.toWikitextPaginatedList(survey.get("transferred"), Pages.LIST_OF_LINKS, 
            (start, end) -> "===" + user + " Transferred files " + start + " to " + end + "===", 20, false));
        for (String section : sections)
            out.println(section);
        out.print(surveyor.generateWikitextFooter());
        out.println("Survey URL: " + request.getRequestURL() + "?" + request.getQueryString());
        return;
    }
%>

<p>
This tool generates a listing of a user's image uploads for use at <a
href="//en.wikipedia.org/wiki/WP:CCI">Contributor copyright investigations.</a>

<p>
<form action="./imagecci.jsp" method=GET>
<table>
<tr>
    <td>User to survey:
    <td><input type=text name=user value="<%= ServletUtils.sanitizeForAttribute(user) %>" required>
<tr>
    <td>Home wiki:
    <td><input type=text name="wiki" value="<%= homewiki %>" required>
<tr>
    <td>Include uploads from:
    <td><input type=date name=earliest value="<%= earliest %>"> to 
        <input type=date name=latest value="<%= latest %>"> (inclusive)
</table>
<input type=submit value="Survey user">
</form>

<%
    if (user != null && survey == null)
        request.setAttribute("error", "ERROR: User " + ServletUtils.sanitizeForHTML(user) + " does not exist!");
%>
<%@ include file="footer.jspf" %>
