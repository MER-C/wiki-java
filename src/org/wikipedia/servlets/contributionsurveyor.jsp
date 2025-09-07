<%--
    @(#)contributionsurveyor.jsp 0.02 05/07/2021
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
    if (!ServletUtils.showCaptcha(request, response, List.of("user"), difficulty))
        throw new SkipPageException();
        
    request.setAttribute("toolname", "Contribution surveyor");
    request.setAttribute("scripts", new String[] { "common.js", "ContributionSurveyor.js" });

    String user = request.getParameter("user");
    String category = request.getParameter("category");
    boolean nominor = (request.getParameter("nominor") != null);
    boolean noreverts = (request.getParameter("noreverts") != null);
    boolean nodrafts = (request.getParameter("nodrafts") != null);
    boolean newonly = (request.getParameter("newonly") != null);
    boolean comingle = (request.getParameter("comingle") != null);

    String homewiki = ServletUtils.sanitizeForAttributeOrDefault(request.getParameter("wiki"), "en.wikipedia.org");
    String bytefloor = ServletUtils.sanitizeForAttributeOrDefault(request.getParameter("bytefloor"), "150");
    
    Wiki wiki = sessions.sharedSession(homewiki);
    wiki.setQueryLimit(10000); // 20 network requests, GAE only allows run time of 15s

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

    // get results
    List<String> survey = Collections.emptyList();
    if (request.getAttribute("error") == null && !users.isEmpty())
    {
        ContributionSurveyor surveyor = new ContributionSurveyor(wiki);
        surveyor.setIgnoringMinorEdits(nominor);
        surveyor.setIgnoringReverts(noreverts);
        surveyor.setNewOnly(newonly);
        surveyor.setComingled(comingle);
        surveyor.setDateRange(earliest_odt, latest_odt);
        surveyor.setMinimumSizeDiff(Integer.parseInt(bytefloor));
        surveyor.setFooter("Survey URL: " + ServletUtils.getRequestURL(request));
        
        // ns 118 = draft namespace on en.wikipedia
        int[] ns = nodrafts ? new int[] { Wiki.MAIN_NAMESPACE } : new int[] { Wiki.MAIN_NAMESPACE, Wiki.USER_NAMESPACE, 118 };
        survey = surveyor.outputContributionSurvey(users, true, false, false, ns);
        if (survey.isEmpty())
            request.setAttribute("error", "No edits found!");
        else
            request.setAttribute("contenttype", "text");
    }
%>
<%@ include file="header.jspf" %>
<%  
    if (!survey.isEmpty())
    {
        String fname = user == null ? category : user;
        response.setHeader("Content-Disposition", "attachment; filename="
            + URLEncoder.encode(fname, StandardCharsets.UTF_8) + ".txt");
        // TODO: output as ZIP (not straightforward: requires rewrite as Java Servlet)
        out.print(String.join("\n", survey));
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
    <td><label for=radio_user>User to survey:</label>
    <td><input type=text name=user id=user value="<%= ServletUtils.sanitizeForAttribute(user) %>" required>
<tr>
    <td><input type=radio name=mode id="radio_category">
    <td><label for=radio_category>Fetch users from category:</label>
    <td><input type=text name=category id=category value="<%= ServletUtils.sanitizeForAttribute(category) %>" disabled>
<tr>
    <td colspan=2>Home wiki:
    <td><input type=text name="wiki" value="<%= homewiki %>" required>
<tr>
    <td colspan=2>Exclude:
    <td><input type=checkbox name=nominor id=nominor value=1<%= (user == null || nominor) ? " checked" : "" %>>
        <label for=nominor>minor edits</label>
        <input type=checkbox name=noreverts id=noreverts value=1<%= (user == null || noreverts) ? " checked" : "" %>>
        <label for=noreverts>reverts</label>
        <input type=checkbox name=nodrafts id=nodrafts value=1<%= (user == null || nodrafts) ? " checked" : "" %>>
        <label for=nodrafts>userspace and draft (ns 118) edits</label>
        <input type=checkbox name=newonly id=newonly value=1<%= newonly ? " checked" : "" %>>
        <label for=newonly>all except new pages</label>
<tr>
    <td colspan=2>Show changes from:
    <td><input type=date name=earliest value="<%= earliest %>"> to 
        <input type=date name=latest value="<%= latest %>"> (inclusive)
<tr>
    <td colspan=2>Show changes that added at least:
    <td><input type=number name=bytefloor value="<%= bytefloor %>"> bytes
<tr>
    <td colspan=2>Output:
    <td><input type=checkbox name=comingle id=comingle value=1<%= comingle ? " checked" : "" %>>
    <label for=comingle title="for sockfarms where each user has few edits">comingled</label>
</table>
<input type=submit value="Survey user">
</form>
<%@ include file="footer.jspf" %>
