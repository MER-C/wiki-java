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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.wikibase.data.Entity;
import org.wikibase.data.Property;
import org.wikibase.data.Sitelink;
import org.xml.sax.SAXException;

public class WikibaseEntityFactory {
    private static Map<String, Entity> items = new HashMap<String, Entity>();
    private static Pattern ENTITY_ID_PATTERN = Pattern.compile("\\s*(q|Q)?(\\d+)\\s*");

    public static Entity getWikibaseItem(String text) throws WikibaseException {
        Node entityNode = null;
        try {
            DocumentBuilderFactory domBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = domBuilderFactory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(text.getBytes()));

            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xPath = xpathFactory.newXPath();
            XPathExpression apiExpression = xPath.compile("/api[1]");

            Node apiNode = (Node) apiExpression.evaluate(document, XPathConstants.NODE);

            if (null == apiNode) {
                throw new WikibaseException("API root node not found in text.");
            }

            NamedNodeMap apiAttrs = apiNode.getAttributes();
            Node successAttr = apiAttrs.getNamedItem("success");
            String successStatus = successAttr.getTextContent();
            if (!"1".equals(successStatus)) {
                throw new WikibaseException("API call unsuccessful.");
            }

            Node entitiesNode = null;
            Node eachApiChild = apiNode.getFirstChild();
            while (null != eachApiChild) {
                if ("entities".equals(eachApiChild.getNodeName())) {
                    entitiesNode = eachApiChild;
                    break;
                }
                eachApiChild = eachApiChild.getNextSibling();
            }

            if (null == entitiesNode) {
                throw new WikibaseException("Wikibase entities node not found.");
            }
            Node presumptiveEntityNode = entitiesNode.getFirstChild();
            while (null != presumptiveEntityNode) {
                if ("entity".equals(presumptiveEntityNode.getNodeName())) {
                    entityNode = presumptiveEntityNode;
                    break;
                }
                presumptiveEntityNode = presumptiveEntityNode.getNextSibling();
            }
            if (null == entityNode) {
                throw new WikibaseException("No entity found.");
            }

            // parse entity attrs
            NamedNodeMap entityAttrs = entityNode.getAttributes();
            String itemId = entityAttrs.getNamedItem("title").getNodeValue();
            Matcher idMatcher = ENTITY_ID_PATTERN.matcher(itemId);
            if (!idMatcher.matches()) {
                throw new WikibaseException("String " + itemId + " is not a valid entity ID");
            }
            Entity entity = new Entity("Q" + idMatcher.group(2));
            loadItem(entity, entityNode);
            return entity;
        } catch (XPathExpressionException e) {
            throw new WikibaseException(e);
        } catch (DOMException e) {
            throw new WikibaseException(e);
        } catch (ParserConfigurationException e) {
            throw new WikibaseException(e);
        } catch (SAXException e) {
            throw new WikibaseException(e);
        } catch (IOException e) {
            throw new WikibaseException(e);
        }

    }

    public static void loadItem(Entity entity, Node entityNode) throws WikibaseException {

        // parse entity children
        Node entityChild = entityNode.getFirstChild();
        while (null != entityChild) {
            if ("labels".equalsIgnoreCase(entityChild.getNodeName())) {
                Node labelNode = entityChild.getFirstChild();
                while (null != labelNode) {
                    if ("label".equalsIgnoreCase(labelNode.getNodeName())) {
                        String language = labelNode.getAttributes().getNamedItem("language").getNodeValue();
                        String value = labelNode.getAttributes().getNamedItem("value").getNodeValue();
                        entity.addLabel(language, value);
                    }
                    labelNode = labelNode.getNextSibling();
                }
            } else if ("descriptions".equalsIgnoreCase(entityChild.getNodeName())) {
                Node descrNode = entityChild.getFirstChild();
                while (null != descrNode) {
                    if ("description".equalsIgnoreCase(descrNode.getNodeName())) {
                        String language = descrNode.getAttributes().getNamedItem("language").getNodeValue();
                        String value = descrNode.getAttributes().getNamedItem("value").getNodeValue();
                        entity.addDescription(language, value);
                    }
                    descrNode = descrNode.getNextSibling();
                }

            } else if ("claims".equalsIgnoreCase(entityChild.getNodeName())) {
                Node propNode = entityChild.getFirstChild();
                while (null != propNode) {
                    if ("property".equalsIgnoreCase(propNode.getNodeName())) {
                        String propId = propNode.getAttributes().getNamedItem("id").getNodeValue();
                        Property prop = WikibasePropertyFactory.getWikibaseProperty(propId);
                        Node claimNode = propNode.getFirstChild();
                        while (null != claimNode) {
                            if ("claim".equalsIgnoreCase(claimNode.getNodeName())) {
                                entity.addClaim(prop, WikibaseClaimFactory.fromNode(claimNode));
                            }
                            claimNode = claimNode.getNextSibling();
                        }
                    }
                    propNode = propNode.getNextSibling();
                }
            } else if ("sitelinks".equalsIgnoreCase(entityChild.getNodeName())) {
                Node sitelinkNode = entityChild.getFirstChild();
                while (null != sitelinkNode) {
                    if ("sitelink".equalsIgnoreCase(sitelinkNode.getNodeName())) {
                        String siteName = sitelinkNode.getAttributes().getNamedItem("site").getNodeValue();
                        String pageName = sitelinkNode.getAttributes().getNamedItem("title").getNodeValue();
                        Sitelink sitelink = new Sitelink(siteName, pageName);
                        Node badgesNode = sitelinkNode.getFirstChild();
                        while (null != badgesNode) {
                            if ("badges".equals(badgesNode.getNodeName())) {
                                Node badgeNode = badgesNode.getFirstChild();
                                while (null != badgeNode) {
                                    sitelink.addBadge(new Entity(badgeNode.getNodeValue()));
                                    badgeNode = badgeNode.getNextSibling();
                                }
                            }
                            badgesNode = badgesNode.getNextSibling();
                        }
                        entity.addSitelink(sitelink);
                    }
                    sitelinkNode = sitelinkNode.getNextSibling();
                }
            }
            entityChild = entityChild.getNextSibling();
        }
        entity.setLoaded(true);
    }

}
