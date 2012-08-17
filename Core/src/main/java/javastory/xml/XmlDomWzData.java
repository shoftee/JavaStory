/*
 * This file is part of the OdinMS Maple Story Server Copyright (C) 2008 ~ 2010
 * Patrick Huy <patrick.huy@frz.cc> Matthias Butz <matze@odinms.de> Jan
 * Christian Meyer <vimes@odinms.de>
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation. You may not use, modify or distribute this
 * program under any other version of the GNU Affero General Public License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package javastory.xml;

import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javastory.wz.WzData;
import javastory.wz.WzDataEntity;
import javastory.wz.WzDataType;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlDomWzData implements WzData, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 523068040190693685L;
	private Node node;
	private File imageDataDir;

	private XmlDomWzData(final Node node) {
		this.node = node;
	}

	public XmlDomWzData(final FileInputStream fis, final File imageDataDir) {
		try {
			final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			final Document document = documentBuilder.parse(fis);
			this.node = document.getFirstChild();

		} catch (final ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (final SAXException e) {
			throw new RuntimeException(e);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		this.imageDataDir = imageDataDir;
	}

	@Override
	public WzData getChildByPath(final String path) {
		final String pathSegments[] = path.split("/");
		if (pathSegments[0].equals("..")) {
			return ((WzData) this.getParent()).getChildByPath(path.substring(path.indexOf("/") + 1));
		}

		Node current = this.node;
		for (final String pathSegment : pathSegments) {
			final NodeList children = current.getChildNodes();
			boolean foundChild = false;
			for (int i = 0; i < children.getLength(); i++) {
				final Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE 
					&& child.getAttributes().getNamedItem("name").getNodeValue().equals(pathSegment)) {
					
					current = child;
					foundChild = true;
					break;
				}
			}
			if (!foundChild) {
				return null;
			}
		}
		final XmlDomWzData ret = new XmlDomWzData(current);
		ret.imageDataDir = new File(this.imageDataDir, this.getName() + "/" + path).getParentFile();
		return ret;
	}
	
	public boolean hasChildAtPath(final String path) {
		final String pathSegments[] = path.split("/");
		if (pathSegments[0].equals("..")) {
			return ((WzData) this.getParent()).hasChildAtPath(path.substring(path.indexOf("/") + 1));
		}
		
		Node current = this.node;
		for (final String pathSegment : pathSegments) {
			final NodeList children = current.getChildNodes();
			boolean foundChild = false;
			for (int i = 0; i < children.getLength(); i ++) {
				final Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE 
					&& child.getAttributes().getNamedItem("name").getNodeValue().equals(pathSegment)) {
					
					current = child;
					foundChild = true;
					break;
				}
			}
			if (!foundChild) {
				return false;
			}
		}
		return true;
	}

	@Override
	public List<WzData> getChildren() {
		final List<WzData> ret = new ArrayList<WzData>();
		final NodeList childNodes = this.node.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			final Node childNode = childNodes.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				final XmlDomWzData child = new XmlDomWzData(childNode);
				child.imageDataDir = new File(this.imageDataDir, this.getName());
				ret.add(child);
			}
		}
		return ret;
	}

	@Override
	public Object getData() {
		final NamedNodeMap attributes = this.node.getAttributes();
		final WzDataType type = this.getType();
		switch (type) {
		case DOUBLE:
			final double doubleValue = Double.parseDouble(attributes.getNamedItem("value").getNodeValue());
			return Double.valueOf(doubleValue);
		case FLOAT:
			final float floatValue = Float.parseFloat(attributes.getNamedItem("value").getNodeValue());
			return Float.valueOf(floatValue);
		case INT:
			final int intValue = Integer.parseInt(attributes.getNamedItem("value").getNodeValue());
			return Integer.valueOf(intValue);
		case SHORT:
			final short shortValue = Short.parseShort(attributes.getNamedItem("value").getNodeValue());
			return Short.valueOf(shortValue);
		case STRING:
		case UOL:
			return attributes.getNamedItem("value").getNodeValue();
		case VECTOR:
			final String xNode = attributes.getNamedItem("x").getNodeValue();
			final String yNode = attributes.getNamedItem("y").getNodeValue();

			final int x = Integer.parseInt(xNode);
			final int y = Integer.parseInt(yNode);

			return new Point(x, y);
		case CANVAS:
			final String widthNode = attributes.getNamedItem("width").getNodeValue();
			final String heightNode = attributes.getNamedItem("height").getNodeValue();

			final int width = Integer.parseInt(widthNode);
			final int height = Integer.parseInt(heightNode);

			final File file = new File(this.imageDataDir, this.getName() + ".png");
			return new FileStoredPngWzCanvas(width, height, file);
		}
		return null;
	}

	@Override
	public final WzDataType getType() {
		final String nodeName = this.node.getNodeName();
		switch (nodeName) {
		case "imgdir":
			return WzDataType.PROPERTY;
		case "canvas":
			return WzDataType.CANVAS;
		case "convex":
			return WzDataType.CONVEX;
		case "sound":
			return WzDataType.SOUND;
		case "uol":
			return WzDataType.UOL;
		case "double":
			return WzDataType.DOUBLE;
		case "float":
			return WzDataType.FLOAT;
		case "int":
			return WzDataType.INT;
		case "short":
			return WzDataType.SHORT;
		case "string":
			return WzDataType.STRING;
		case "vector":
			return WzDataType.VECTOR;
		case "null":
			return WzDataType.IMG_0x00;
		default:
			return null;
		}
	}

	@Override
	public WzDataEntity getParent() {
		final Node parentNode = this.node.getParentNode();
		if (parentNode.getNodeType() == Node.DOCUMENT_NODE) {
			return null; 
			// can't traverse outside the img file
			// TODO is this a problem?
		}
		final XmlDomWzData parentData = new XmlDomWzData(parentNode);
		parentData.imageDataDir = this.imageDataDir.getParentFile();
		return parentData;
	}

	@Override
	public String getName() {
		return this.node.getAttributes().getNamedItem("name").getNodeValue();
	}

	@Override
	public Iterator<WzData> iterator() {
		return this.getChildren().iterator();
	}
}
