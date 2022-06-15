<%--
    @(#)imagecci.jsp 0.03 07/02/2018
    Copyright (C) 2011 - 2022 MER-C

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
    String homewiki = ServletUtils.sanitizeForAttributeOrDefault(request.getParameter("wiki"), "en.wikipedia.org");
    String user = request.getParameter("user");
    if (user != null)
        request.setAttribute("contenttype", "text");
%>
<%@ include file="header.jspf" %>
<%
    List<String> surveydata = null;
    if (user != null)
    {
        WMFWiki wiki = sessions.sharedSession(homewiki);
        ContributionSurveyor surveyor = new ContributionSurveyor(wiki);
        surveyor.setDateRange(earliest_odt, latest_odt);
        surveydata = surveyor.outputContributionSurvey(List.of(user), false, false, true);

        String footer = "Survey URL: " + request.getRequestURL() + "?" + request.getQueryString();
        // TODO: output as ZIP
        for (int i = 0; i < surveydata.size(); i++)
            surveydata.set(i, surveydata.get(i) + footer);
            
        response.setHeader("Content-Disposition", "attachment; filename=" 
            + URLEncoder.encode(user, StandardCharsets.UTF_8) + ".txt");
        out.print(String.join("\n", surveydata));
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
    if (user != null && surveydata.isEmpty())
        request.setAttribute("error", "ERROR: User " + ServletUtils.sanitizeForHTML(user) + " does not exist!");
%>
<%@ include file="footer.jspf" %>
