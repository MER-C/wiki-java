<%--
    @(#)contributionsurveyor.jsp 0.01 27/01/2018
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
    request.setAttribute("toolname", "Contribution surveyor");
    request.setAttribute("scripts", new String[] { "common.js", "ContributionSurveyor.js" });

    String user = request.getParameter("user");
    String category = request.getParameter("category");
    boolean nominor = (request.getParameter("nominor") != null);
    boolean noreverts = (request.getParameter("noreverts") != null);
    boolean nodrafts = (request.getParameter("nodrafts") != null);

    String homewiki = ServletUtils.sanitizeForAttributeOrDefault(request.getParameter("wiki"), "en.wikipedia.org");
    String bytefloor = ServletUtils.sanitizeForAttributeOrDefault(request.getParameter("bytefloor"), "150");
    
    Wiki wiki = Wiki.newSession(homewiki);
    wiki.setQueryLimit(35000); // 70 network requests

    List<String> users = new ArrayList<>();
    if (user != null)
        users.add(user);
    else if (category != null)
    {
        List<String> catmembers = wiki.getCategoryMembers(category, Wiki.USER_NAMESPACE);
        if (catmembers.isEmpty())
            request.setAttribute("error", "Category \"" + ServletUtils.sanitizeForHTML(category) + "\" contains no users!");
        else
            for (String tempstring : catmembers)
                users.add(wiki.removeNamespace(tempstring));
    }

    ContributionSurveyor surveyor = new ContributionSurveyor(wiki);
    String survey = null;

    // get results
    if (request.getAttribute("error") == null && !users.isEmpty())
    {
        surveyor.setIgnoringMinorEdits(nominor);
        surveyor.setIgnoringReverts(noreverts);
        surveyor.setDateRange(earliest_odt, latest_odt);
        surveyor.setMinimumSizeDiff(Integer.parseInt(bytefloor));
        
        // ns 118 = draft namespace on en.wikipedia
        int[] ns = nodrafts ? new int[] { Wiki.MAIN_NAMESPACE } : new int[] { Wiki.MAIN_NAMESPACE, Wiki.USER_NAMESPACE, 118 };
        List<String> surveydata = surveyor.outputContributionSurvey(users, false, ns);
        if (surveydata.isEmpty())
        {
            request.setAttribute("error", "No edits found!");
            survey = null;
        }
        else
        {
            request.setAttribute("contenttype", "text");
            // TODO: output as ZIP
            String footer = "Survey URL: " + request.getRequestURL() + "?" + request.getQueryString();
            for (int i = 0; i < surveydata.size(); i++)
                surveydata.set(i, surveydata.get(i) + footer);
            survey = String.join("\n", surveydata);
        }
    }
%>
<%@ include file="header.jspf" %>
<%  
    if (survey != null)
    {
        if (user != null)
        {
            response.setHeader("Content-Disposition", "attachment; filename=" 
                + URLEncoder.encode(user, StandardCharsets.UTF_8) + ".txt");
        }
        else // category != null
            response.setHeader("Content-Disposition", "attachment; filename=" 
                + URLEncoder.encode(category, StandardCharsets.UTF_8) + ".txt");
        out.print(survey);
        
        return;
    }
 %>

<p>
This tool generates a listing of a user's edits for use at <a
href="//en.wikipedia.org/wiki/WP:CCI">Contributor copyright investigations</a>
and other venues. It isolates and ranks major edits by size. A query limit of
10000 edits (after namespace filter and minor edit exclusion) applies.

<p>
<form action="./contributionsurveyor.jsp" method=GET>
<table>
<tr>
    <td><input type=radio name=mode id="radio_user" checked>
    <td>User to survey:
    <td><input type=text name=user id=user value="<%= ServletUtils.sanitizeForAttribute(user) %>" required>
<tr>
    <td><input type=radio name=mode id="radio_category">
    <td>Fetch users from category:
    <td><input type=text name=category id=category value="<%= ServletUtils.sanitizeForAttribute(category) %>" disabled>
<tr>
    <td colspan=2>Home wiki:
    <td><input type=text name="wiki" value="<%= homewiki %>" required>
<tr>
    <td colspan=2>Exclude:
    <td><input type=checkbox name=nominor value=1<%= (user == null || nominor) ? " checked" : "" %>>minor edits
        <input type=checkbox name=noreverts value=1<%= (user == null || noreverts) ? " checked" : "" %>>reverts
        <input type=checkbox name=nodrafts value=1<%= (user == null || nodrafts) ? " checked" : "" %>>userspace and draft (ns 118) edits
<tr>
    <td colspan=2>Show changes from:
    <td><input type=date name=earliest value="<%= earliest %>"> to 
        <input type=date name=latest value="<%= latest %>"> (inclusive)
<tr>
    <td colspan=2>Show changes that added at least:
    <td><input type=number name=bytefloor value="<%= bytefloor %>"> bytes
</table>
<input type=submit value="Survey user">
</form>
<%@ include file="footer.jspf" %>
