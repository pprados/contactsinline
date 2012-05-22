/*******************************************************************************
 * Copyright 2012 Philippe PRADOS 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package fr.prados.contacts.providers.ldap;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLUtils
{
	public static String elementToString(Node n)
	{

		String name = n.getNodeName();

		short type = n.getNodeType();

		if (Node.CDATA_SECTION_NODE == type)
		{
			return "<![CDATA[" + n.getNodeValue() + "]]&gt;";
		}

		if (name.length()>0 && name.charAt(0)=='#')
		{
			return "";
		}

		StringBuilder sb = new StringBuilder();
		sb.append('<').append(name);

		NamedNodeMap attrs = n.getAttributes();
		if (attrs != null)
		{
			for (int i = 0; i < attrs.getLength(); i++)
			{
				Node attr = attrs.item(i);
				sb.append(' ').append(attr.getNodeName()).append("=\"").append(encodeXML(attr.getNodeValue()))
						.append('\"');
			}
		}

		String textContent = null;
		NodeList children = n.getChildNodes();

		if (children.getLength() == 0)
		{
			if ((textContent = getTextContent(n)) != null && !"".equals(textContent))
			{
				sb.append(encodeXML(textContent)).append("</").append(name).append('>');
			}
			else
			{
				sb.append("/>").append('\n');
			}
		}
		else
		{
			sb.append('>').append('\n');
			boolean hasValidChildren = false;
			for (int i = 0; i < children.getLength(); i++)
			{
				String childToString = elementToString(children.item(i));
				if (!"".equals(childToString))
				{
					sb.append(childToString);
					hasValidChildren = true;
				}
			}

			if (!hasValidChildren && ((textContent = getTextContent(n)) != null))
			{
				sb.append(encodeXML(textContent));
			}

			sb.append("</").append(name).append('>');
		}

		return sb.toString();
	}

	private static String encodeXML(final String str)
	{
		final int max = str.length();
		final StringBuffer buf = new StringBuffer(max + 10);
		for (int i = 0; i < max; ++i)// NOPMD
		{
			final char c = str.charAt(i);
			switch (c)
			{
			case '&':
				buf.append("&amp;");
				break;
			case '<':
				buf.append("&lt;");
				break;
			case '>':
				buf.append("&gt;");
				break;
			case '"':
				buf.append("&quot;");
				break;
			case '\'':
				buf.append("&apos;");
				break;
			default:
				buf.append(c);
			}
		}
		return buf.toString();
	}
	/**
	 * return the text content of an element
	 */
	private static String getTextContent(org.w3c.dom.Node element)
	{
		StringBuffer childtext = new StringBuffer();
		NodeList childlist = element.getChildNodes();
		int ct = childlist.getLength();

		for (int j = 0; j < ct; j++)
		{
			org.w3c.dom.Node childNode = childlist.item(j);

			if ((childNode.getNodeType() == Node.TEXT_NODE)
					|| (childNode.getNodeType() == Node.CDATA_SECTION_NODE))
			{
				childtext.append(childNode.getNodeValue().trim());
			}
		}

		return childtext.toString();
	}

}
