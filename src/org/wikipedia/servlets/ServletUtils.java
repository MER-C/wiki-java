/**
 *  @(#)ServletUtils.java 0.02 13/04/2025
 *  Copyright (C) 2011 - 2025 MER-C
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.

 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.wikipedia.servlets;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.security.*;
import java.time.OffsetDateTime;
import java.util.*;

import jakarta.servlet.http.*;

/**
 *  Common servlet code so that I can maintain it easier.
 *  @author MER-C
 *  @since 0.02
 */
public class ServletUtils
{
    /**
     *  Sanitizes untrusted input for XSS destined for inclusion in boring
     *  HTML attributes.
     *  @param input the input to be sanitized
     *  @see <a href="https://www.owasp.org/index.php/XSS_Prevention"> OWASP XSS
     *  Prevention Cheat Sheet Rule 2</a>
     *  @return the sanitized input or the empty string if input is null
     */
    public static String sanitizeForAttribute(String input)
    {        
        return sanitizeForAttributeOrDefault(input, "");
    }
    
    /**
     *  Sanitizes untrusted input for XSS destined for inclusion in boring
     *  HTML attributes.
     *  @param input the input to be sanitized
     *  @param def a default value for the input
     *  @see <a href="https://www.owasp.org/index.php/XSS_Prevention"> OWASP XSS
     *  Prevention Cheat Sheet Rule 2</a>
     *  @return the sanitized input or the the default string if input is null
     */
    public static String sanitizeForAttributeOrDefault(String input, String def)
    {
        if (input == null)
            return Objects.requireNonNull(def);
        return input.replaceAll("\"", "&quot;");
    }
    
    /**
     *  Sanitizes untrusted input for XSS destined for inclusion in URLs. (Note
     *  that most Wiki.java methods should handle this.)
     *  @param input the input to be sanitized
     *  @see <a href="https://www.owasp.org/index.php/XSS_Prevention"> OWASP XSS
     *  Prevention Cheat Sheet Rule 5</a>
     *  @return the sanitized input
     */
    public static String sanitizeForURL(String input)
    {
        return URLEncoder.encode(input, StandardCharsets.UTF_8);
    }
    
    /**
     *  Denotes the start of a collapsible section. Requires the separate 
     *  JavaScript file collapsible.js to function (otherwise the sections will
     *  be expanded).
     * 
     *  @param title the title of the collapsed section sanitized for XSS
     *  @param collapsed whether to start off in the collapsed state
     *  @return HTML for a collapsible section
     *  @see #endCollapsibleSection
     */
    public static String beginCollapsibleSection(String title, boolean collapsed)
    {
        return """
            <div class="collapsecontainer">
            <span class="collapseboxto">
            <span class="collapseheader">%s</span>
            <span class="showhidespan">[<a href="#fasfd" class="showhidelink">%s</a>]</span>
            </span>
            <div class="%s">
            """.formatted(title, collapsed ? "show" : "hide", collapsed ? "tocollapse" : "notcollapsed");
    }
    
    /**
     *  Denotes the end of a collapsible section.
     *  @return HTML denoting the end of a collapsible section
     *  @see #beginCollapsibleSection
     */
    public static String endCollapsibleSection()
    {
        // the only reason this exists is readibility 
        return "</div>\n</div>\n\n";
    }

    /**
     *  Generates pagination links.
     *  @param urlbase the URL to the servlet that is invariant of position in 
     *  the paginated list
     *  @param current the current offset
     *  @param amount the number of items per page
     *  @param max the maximum number of items
     *  @throws IllegalArgumentException if current &lt; 0, amount &lt; 1 or
     *  max &lt; 1
     *  @return HTML that contains pagination links
     */
    public static String generatePagination(String urlbase, int current, int amount, int max)
    {
        if (current < 0 || amount < 1 || max < 1)
            throw new IllegalArgumentException("Invalid pagination - current(" + 
                current + ") amount(" + amount + ") max(" + max + ")");
        
        StringBuilder sb = new StringBuilder("<p>");
        if (current > 0)
        {
            sb.append("""
                <a href="%s&offset=%d">""".formatted(urlbase, Math.max(0, current - amount)));
        }
        sb.append("Previous ");
        sb.append(amount);
        if (current > 0)
            sb.append("</a>");
        sb.append(" | ");

        if (max - current > amount)
        {
            sb.append("""
                <a href="%s&offset=%d">""".formatted(urlbase, current + amount));
        }
        sb.append("Next ");
        sb.append(amount);
        if (max - current > amount)
            sb.append("</a>");
        return sb.toString();
    }
    
    /**
     *  Presents a SHA-256 proof of work CAPTCHA. To pass the CAPTCHA, the  
     *  client needs to compute a nonce such that 
     *  sha256(nonce + timestamp + selected concatenated 
     *  HTTP parameters) begins with some quantity of zeros (difficulty is 
     *  customisable, default 4 for a runtime of about 1.5 s). A CAPTCHA page is
     *  shown when the user submits a request. When complete, the CAPTCHA page 
     *  redirects to the expected results. A solved CAPTCHA adds URL parameters
     *  <code>powans</code> (the solution), <code>powts</code>, <code>nonce</code>
     *  and <code>powdif</code> (the difficulty). The CAPTCHA expires after five 
     *  minutes.
     * 
     *  @param req a servlet request with scriptnonce attribute
     *  @param response the corresponding response
     *  @param params the request parameters to concatenate to form the challenge
     *  string. The challenge string cannot contain new lines.
     *  @param difficulty the difficulty of the CAPTCHA
     *  @return whether to continue servlet execution
     *  @throws IOException if a network error occurs
     *  @see captcha.js
     *  @since 0.02
     */
    public static boolean showCaptcha(HttpServletRequest req, HttpServletResponse response, List<String> params, 
        int difficulty) throws IOException
    {
        // no captcha for the initial input
        if (req.getParameterMap().isEmpty())
            return true;
        
        // TODO: 
        // *captcha.js does not propagate POST parameters
        // *inject CSP nonce header only when required
        // *server side nonce
        
        PrintWriter out = response.getWriter();
        String answer = req.getParameter("powans");
        String timestamp = req.getParameter("powts");
        String nonce = req.getParameter("nonce");
        String reqdifficulty = req.getParameter("powdif");
        
        StringBuilder paramstr = new StringBuilder();
        for (String param : params)
            paramstr.append(req.getParameter(param));
        String challenge = sanitizeForAttribute(paramstr.toString());
        // String snonce = (String)req.getAttribute("servernonce");
        String tohash = nonce + timestamp + challenge;

        // captcha not attempted, show CAPTCHA screen
        if (answer == null && timestamp == null && nonce == null && reqdifficulty == null)
        {
            out.println("""
                    <!doctype html>
                    <html>
                    <head>
                    <title>CAPTCHA</title>""");
            out.println("<script nonce=\"" + req.getAttribute("scriptnonce") + "\">");
            out.println("    window.chl = \"" + challenge + "\";");
            out.println("    window.difficulty = " + difficulty + ";");
            out.println("""
                    </script>
                    <script src="captcha.js" defer></script>
                    </head>
                    <body>
                    <h1>Verifying you are not a bot</h1>
                    <p>You should be redirected to your results shortly. Unfortunately JavaScript is required for this to work.
                    </body>
                    </html>
                    """);
            return false;
        }
        // incomplete parameters = fail
        else if (answer == null || timestamp == null || nonce == null || reqdifficulty == null)
        {
            response.setStatus(403);
            out.println("Incomplete CAPTCHA parameters");
            return false;
        }
        
        // not recent = show another CAPTCHA
        OffsetDateTime odt = OffsetDateTime.parse(timestamp);
        if (OffsetDateTime.now().minusMinutes(5).isAfter(odt))
        {
            response.setStatus(302);
            response.setHeader("Location", ServletUtils.getRequestURL(req));
            response.getWriter().close();
            return false;
        }
                        
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(tohash.getBytes(StandardCharsets.UTF_8));
            String expected = "%064x".formatted(new BigInteger(1, hash));
            int zeroes = Integer.parseInt(reqdifficulty);
            String prefix = "0".repeat(zeroes);
            if (answer.startsWith(prefix) && expected.equals(answer) && zeroes == difficulty)
                return true;
            
            response.setStatus(403);
            out.println("CAPTCHA failed");
            return false;
            
        }
        catch (NoSuchAlgorithmException ex)
        {
            response.setStatus(302);
            response.setHeader("Location", "/timeout.html");
            response.getWriter().close();
            return false;
        }
    }
    
    /**
     *  Reconstructs the request URL without transient parameters (e.g. CAPTCHA,
     *  pagination).
     *  @param req the request to construct the URL for
     *  @return the constructed URL
     *  @since 0.02
     */
    public static String getRequestURL(HttpServletRequest req)
    {
        Map<String, String[]> params = new LinkedHashMap(req.getParameterMap());
        if (params.isEmpty())
            return req.getRequestURL().toString();
        StringBuilder sb = new StringBuilder(req.getRequestURL());
        sb.append("?");
        // CAPTCHA parameters
        params.remove("powans");
        params.remove("nonce");
        params.remove("powts");
        params.remove("powdif");
        // pagination
        params.remove("offset");
        int i = 0;
        int last = params.size() - 1;
        for (var entry : params.entrySet())
        {
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue()[0]);
            if (i != last)
                sb.append("&");
            i++;
        }
        return sb.toString();
    }
}
