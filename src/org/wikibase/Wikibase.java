/**
*  This program is free software; you can redistribute it and/or
*  modify it under the terms of the GNU General Public License
*  as published by the Free Software Foundation; either version 3
*  of the License, or (at your option) any later version. Additionally
*  this file is subject to the "Classpath" exception.
*
*  This program is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License
*  along with this program; if not, write to the Free Software Foundation,
*  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/
package org.wikibase;

import java.io.IOException;

import java.net.URLEncoder;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.wikibase.data.Entity;
import org.wikipedia.Wiki;

public class Wikibase extends Wiki {

    public Wikibase(String url) {
        super(url);
    }
    
    public Wikibase() {
        this("www.wikidata.org");
    }

    /**
     * Returns an entity identified by the title of a wiki page.
     * 
     * @param site
     * @param pageName
     * @return
     * @throws IOException
     * @throws WikibaseException
     */
    public Entity getWikibaseItemBySiteAndTitle(final String site, final String pageName)
        throws IOException, WikibaseException {
        final StringBuilder url = new StringBuilder(query);
        url.append("action=wbgetentities");
        url.append("&sites=" + site);
        url.append("&titles=" + URLEncoder.encode(pageName, "UTF-8"));
        url.append("&format=xml");

        final String text = fetch(url.toString(), "getWikibaseItem");

        Entity entity = WikibaseEntityFactory.getWikibaseItem(text);
        return entity;
    }

    /**
     * Returns the entity taken as a parameter, populated with data from wikibase. Use this when you have the Entity object
     * that only contains the ID, which will happen if this entity is reached via another entity's property.
     * 
     * @param item
     * @return
     * @throws IOException
     * @throws WikibaseException
     */
    public Entity getWikibaseItemById(final Entity item) throws IOException, WikibaseException {
        return getWikibaseItemById(item.getId());
    }

    /**
     * Returns the entity associated with the specified wikibase id.
     * 
     * @param item
     * @return
     * @throws IOException
     * @throws WikibaseException
     */
    public Entity getWikibaseItemById(final String id) throws IOException, WikibaseException {
        if (null == id || 0 == id.trim().length())
            return null;
        StringBuilder actualId = new StringBuilder(id.trim());
        if ('q' != Character.toLowerCase(actualId.charAt(0))) {
            if (Pattern.matches("\\d+", actualId)) {
                actualId.insert(0, 'Q');
            } else {
                throw new WikibaseException(id + " is not a valid Wikibase id");
            }
        }
        final StringBuilder url = new StringBuilder(query);
        url.append("action=wbgetentities");
        url.append("&ids=").append(actualId);
        url.append("&format=xml");

        final String text = fetch(url.toString(), "getWikibaseItem");

        System.out.println(text);
        return WikibaseEntityFactory.getWikibaseItem(text);
    }

    /**
     * Retrieves the title of the corresponding page in another site.
     * 
     * @param site
     * @param pageName
     * @param language
     * @return
     * @throws WikibaseException
     * @throws IOException
     */
    public String getTitleInLanguage(final String site, final String pageName, final String language)
        throws WikibaseException, IOException {
        Entity ent = getWikibaseItemBySiteAndTitle(site, pageName);
        return ent.getSitelinks().get(language).getPageName();
    }

    /**
     * Links two pages from different sites via wikibase.
     * 
     * @param fromsite
     * @param fromtitle
     * @param tosite
     * @param totitle
     * @throws IOException
     */
    public void linkPages(final String fromsite, final String fromtitle, final String tosite, final String totitle)
        throws IOException {
        final StringBuilder url1 = new StringBuilder(query);
        url1.append("action=wbgetentities");
        url1.append("&sites=" + tosite);
        url1.append("&titles=" + URLEncoder.encode(totitle, "UTF-8"));
        url1.append("&format=xml");
        final String text = fetch(url1.toString(), "linkPages");

        final int startindex = text.indexOf("<entity");
        final int endindex = text.indexOf(">", startindex);
        final String entityTag = text.substring(startindex, endindex);
        final StringTokenizer entityTok = new StringTokenizer(entityTag, " ", false);
        String q = null;
        while (entityTok.hasMoreTokens()) {
            final String entityAttr = entityTok.nextToken();
            if (!entityAttr.contains("=")) {
                continue;
            }
            final String[] entityParts = entityAttr.split("\\=");
            if (entityParts[0].trim().startsWith("title")) {
                q = entityParts[1].trim().replace("\"", "");
            }
        }

        if (null == q) {
            return;
        }

        final StringBuilder getTokenURL = new StringBuilder(query);
        getTokenURL.append("prop=info");
        getTokenURL.append("&intoken=edit");
        getTokenURL.append("&titles=" + URLEncoder.encode(q, "UTF-8"));
        getTokenURL.append("&format=xml");
        String res = fetch(getTokenURL.toString(), "linkPages");

        final int pagestartindex = res.indexOf("<page ");
        final int pageendindex = res.indexOf(">", pagestartindex);
        final String pageTag = res.substring(pagestartindex, pageendindex);

        String edittoken = null;
        final StringTokenizer pageTok = new StringTokenizer(pageTag, " ", false);
        while (pageTok.hasMoreTokens()) {
            final String pageAttr = pageTok.nextToken();
            if (!pageAttr.contains("=")) {
                continue;
            }
            final String[] entityParts = pageAttr.split("=");
            if (entityParts[0].trim().startsWith("edittoken")) {
                edittoken = entityParts[1].trim().replace("\"", "");
            }
        }
        final StringBuilder postdata = new StringBuilder();
        postdata.append("&tosite=" + tosite);
        postdata.append("&totitle=" + URLEncoder.encode(totitle, "UTF-8"));
        postdata.append("&fromtitle=" + URLEncoder.encode(fromtitle, "UTF-8"));
        postdata.append("&fromsite=" + fromsite);
        postdata.append("&token=" + URLEncoder.encode(edittoken, "UTF-8"));
        postdata.append("&format=xmlfm");

        res = post(query + "action=wblinktitles", postdata.toString(), "linkPages");
    }
}
