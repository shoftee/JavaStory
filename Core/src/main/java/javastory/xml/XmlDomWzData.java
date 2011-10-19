/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package javastory.xml;

import javastory.wz.WzDataType;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javastory.wz.WzData;
import javastory.wz.WzDataEntity;

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
	    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
	    Document document = documentBuilder.parse(fis);
	    this.node = document.getFirstChild();

	} catch (ParserConfigurationException e) {
	    throw new RuntimeException(e);
	} catch (SAXException e) {
	    throw new RuntimeException(e);
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}
	this.imageDataDir = imageDataDir;
    }

    @Override
    public WzData getChildByPath(final String path) {
	final String segments[] = path.split("/");
	if (segments[0].equals("..")) {
	    return ((WzData) getParent()).getChildByPath(path.substring(path.indexOf("/") + 1));
	}

	Node myNode = node;
	for (int x = 0; x < segments.length; x++) {
	    NodeList childNodes = myNode.getChildNodes();
	    boolean foundChild = false;
	    for (int i = 0; i < childNodes.getLength(); i++) {
		final Node childNode = childNodes.item(i);
		if (childNode.getNodeType() == Node.ELEMENT_NODE && childNode.getAttributes().getNamedItem("name").getNodeValue().equals(segments[x])) {
		    myNode = childNode;
		    foundChild = true;
		    break;
		}
	    }
	    if (!foundChild) {
		return null;
	    }
	}
	final XmlDomWzData ret = new XmlDomWzData(myNode);
	ret.imageDataDir = new File(imageDataDir, getName() + "/" + path).getParentFile();
	return ret;
    }

    @Override
    public List<WzData> getChildren() {
	final List<WzData> ret = new ArrayList<WzData>();
	final NodeList childNodes = node.getChildNodes();
	for (int i = 0; i < childNodes.getLength(); i++) {
	    final Node childNode = childNodes.item(i);
	    if (childNode.getNodeType() == Node.ELEMENT_NODE) {
		final XmlDomWzData child = new XmlDomWzData(childNode);
		child.imageDataDir = new File(imageDataDir, getName());
		ret.add(child);
	    }
	}
	return ret;
    }

    @Override
    public Object getData() {
	final NamedNodeMap attributes = node.getAttributes();
	final WzDataType type = getType();
	switch (type) {
	    case DOUBLE: {
		return Double.valueOf(Double.parseDouble(attributes.getNamedItem("value").getNodeValue()));
	    }
	    case FLOAT: {
		return Float.valueOf(Float.parseFloat(attributes.getNamedItem("value").getNodeValue()));
	    }
	    case INT: {
		return Integer.valueOf(Integer.parseInt(attributes.getNamedItem("value").getNodeValue()));
	    }
	    case SHORT: {
		return Short.valueOf(Short.parseShort(attributes.getNamedItem("value").getNodeValue()));
	    }
	    case STRING:
	    case UOL: {
		return attributes.getNamedItem("value").getNodeValue();
	    }
	    case VECTOR: {
		return new Point(Integer.parseInt(attributes.getNamedItem("x").getNodeValue()), Integer.parseInt(attributes.getNamedItem("y").getNodeValue()));
	    }
	    case CANVAS: {
		return new FileStoredPngWzCanvas(Integer.parseInt(attributes.getNamedItem("width").getNodeValue()), Integer.parseInt(attributes.getNamedItem("height").getNodeValue()), new File(imageDataDir, getName() + ".png"));
	    }
	}
	return null;
    }

    @Override
	public final WzDataType getType() {
	final String nodeName = node.getNodeName();
	if (nodeName.equals("imgdir")) {
	    return WzDataType.PROPERTY;
	} else if (nodeName.equals("canvas")) {
	    return WzDataType.CANVAS;
	} else if (nodeName.equals("convex")) {
	    return WzDataType.CONVEX;
	} else if (nodeName.equals("sound")) {
	    return WzDataType.SOUND;
	} else if (nodeName.equals("uol")) {
	    return WzDataType.UOL;
	} else if (nodeName.equals("double")) {
	    return WzDataType.DOUBLE;
	} else if (nodeName.equals("float")) {
	    return WzDataType.FLOAT;
	} else if (nodeName.equals("int")) {
	    return WzDataType.INT;
	} else if (nodeName.equals("short")) {
	    return WzDataType.SHORT;
	} else if (nodeName.equals("string")) {
	    return WzDataType.STRING;
	} else if (nodeName.equals("vector")) {
	    return WzDataType.VECTOR;
	} else if (nodeName.equals("null")) {
	    return WzDataType.IMG_0x00;
	}
	return null;
    }

    @Override
    public WzDataEntity getParent() {
	final Node parentNode = node.getParentNode();
	if (parentNode.getNodeType() == Node.DOCUMENT_NODE) {
	    return null; // can't traverse outside the img file - TODO is this a problem?
	}
	final XmlDomWzData parentData = new XmlDomWzData(parentNode);
	parentData.imageDataDir = imageDataDir.getParentFile();
	return parentData;
    }

    @Override
    public String getName() {
	return node.getAttributes().getNamedItem("name").getNodeValue();
    }

    @Override
    public Iterator<WzData> iterator() {
	return getChildren().iterator();
    }
}
