/*
 * Copyright 2006-2012 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.desktop.preferences;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.util.Collection;

import javax.swing.JFrame;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Proxy server settings
 */
public class WindowStateParameter implements Parameter<Object> {

	// Flag to indicate that MZmine is starting, after loading the parameter
	// first time, this flag is changed to false
	private static boolean firstMZmineStartup = true;

	@Override
	public String getName() {
		return "MZmine window state";
	}

	@Override
	public WindowStateParameter cloneParameter() {
		return this;
	}

	@Override
	public void loadValueFromXML(Element xmlElement) {

		// Set window position
		NodeList posElement = xmlElement.getElementsByTagName("position");
		if (posElement.getLength() == 1) {
			String posString = posElement.item(0).getTextContent();
			String posArray[] = posString.split(":");
			int posX = Integer.valueOf(posArray[0]);
			int posY = Integer.valueOf(posArray[1]);

			// Only update the window state on startup
			if (firstMZmineStartup
					&& (MZmineCore.getDesktop().getMainFrame() != null)) {
				JFrame mainFrame = MZmineCore.getDesktop().getMainFrame();
				mainFrame.setLocation(posX, posY);
			}
		}

		// Set window size
		NodeList sizeElement = xmlElement.getElementsByTagName("size");
		if (sizeElement.getLength() == 1) {
			String sizeString = sizeElement.item(0).getTextContent();
			String sizeArray[] = sizeString.split(":");

			int newState = Frame.NORMAL;

			int width = 800, height = 600;
			if (sizeArray[0].equals("maximized"))
				newState |= Frame.MAXIMIZED_HORIZ;
			else
				width = Integer.parseInt(sizeArray[0]);

			if (sizeArray[1].equals("maximized"))
				newState |= Frame.MAXIMIZED_VERT;
			else
				height = Integer.parseInt(sizeArray[1]);

			// Only update the window state on startup
			if (firstMZmineStartup
					&& (MZmineCore.getDesktop().getMainFrame() != null)) {
				JFrame mainFrame = MZmineCore.getDesktop().getMainFrame();
				mainFrame.setSize(width, height);
				mainFrame.setExtendedState(newState);
			}
		}

		// Change to flag to indicate that the next load is not a startup
		// anymore
		firstMZmineStartup = false;

	}

	@Override
	public void saveValueToXML(Element xmlElement) {

		// Get window properties
		JFrame mainFrame = MZmineCore.getDesktop().getMainFrame();
		if (mainFrame == null)
			return;

		Point position = mainFrame.getLocation();
		int state = mainFrame.getExtendedState();
		Dimension size = mainFrame.getSize();
		String mainWindowWidth, mainWindowHeight;
		if ((state & Frame.MAXIMIZED_HORIZ) != 0)
			mainWindowWidth = "maximized";
		else
			mainWindowWidth = String.valueOf(size.width);
		if ((state & Frame.MAXIMIZED_VERT) != 0)
			mainWindowHeight = "maximized";
		else
			mainWindowHeight = String.valueOf(size.height);

		// Add elements
		Document doc = xmlElement.getOwnerDocument();
		Element positionElement = doc.createElement("position");
		xmlElement.appendChild(positionElement);
		positionElement.setTextContent(position.x + ":" + position.y);
		Element sizeElement = doc.createElement("size");
		xmlElement.appendChild(sizeElement);
		sizeElement.setTextContent(mainWindowWidth + ":" + mainWindowHeight);

	}

	@Override
	public boolean checkValue(Collection<String> errorMessages) {
		return true;
	}

	@Override
	public Object getValue() {
		return null;
	}

	@Override
	public void setValue(Object newValue) {
	}

}
