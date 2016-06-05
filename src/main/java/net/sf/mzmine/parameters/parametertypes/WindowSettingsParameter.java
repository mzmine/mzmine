/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.parameters.parametertypes;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Collection;

import javax.swing.JFrame;

import net.sf.mzmine.parameters.Parameter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class WindowSettingsParameter implements Parameter<Object>,
        ComponentListener {

    private static final String SIZE_ELEMENT = "size";
    private static final String POSITION_ELEMENT = "position";
    private static final String MAXIMIZED_ELEMENT = "maximized";

    private static Point startPosition;

    private Point position;
    private Dimension dimension;
    private boolean isMaximized = false;

    private Point offset = new Point(20, 20);

    @Override
    public String getName() {
        return "Window state";
    }

    @Override
    public WindowSettingsParameter cloneParameter() {
        return this;
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
        return true;
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {

        // Window position
        NodeList posElement = xmlElement.getElementsByTagName(POSITION_ELEMENT);
        if (posElement.getLength() == 1) {
            String posString = posElement.item(0).getTextContent();
            String posArray[] = posString.split(":");
            if (posArray.length == 2) {
                int posX = Integer.valueOf(posArray[0]);

                int posY = Integer.valueOf(posArray[1]);
                position = new Point(posX, posY);
            }
        }

        // Window size
        NodeList sizeElement = xmlElement.getElementsByTagName(SIZE_ELEMENT);
        if (sizeElement.getLength() == 1) {
            String sizeString = sizeElement.item(0).getTextContent();
            String sizeArray[] = sizeString.split(":");
            if (sizeArray.length == 2) {
                int width = Integer.parseInt(sizeArray[0]);
                int height = Integer.parseInt(sizeArray[1]);
                dimension = new Dimension(width, height);
            }
        }

        // Window maximized
        NodeList maximizedElement = xmlElement
                .getElementsByTagName(MAXIMIZED_ELEMENT);
        if (maximizedElement.getLength() == 1) {
            String maximizedString = maximizedElement.item(0).getTextContent();
            isMaximized = Boolean.valueOf(maximizedString);
        }

    }

    @Override
    public void saveValueToXML(Element xmlElement) {

        // Add elements
        Document doc = xmlElement.getOwnerDocument();

        if (position != null) {
            Element positionElement = doc.createElement(POSITION_ELEMENT);
            xmlElement.appendChild(positionElement);
            positionElement.setTextContent(position.x + ":" + position.y);
        }

        if (dimension != null) {
            Element sizeElement = doc.createElement(SIZE_ELEMENT);
            xmlElement.appendChild(sizeElement);
            sizeElement
                    .setTextContent(dimension.width + ":" + dimension.height);
        }

        Element maximizedElement = doc.createElement(MAXIMIZED_ELEMENT);
        xmlElement.appendChild(maximizedElement);
        maximizedElement.setTextContent(String.valueOf(isMaximized));

    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public void setValue(Object newValue) {
        // ignore
    }

    /**
     * Set window size and position according to the values in this instance
     */
    public void applySettingsToWindow(JFrame frame) {
        if (position != null) {
            if (!isMaximized) {
                // Reset to default, if we go outside screen limits
                Dimension screenSize = Toolkit.getDefaultToolkit()
                        .getScreenSize();
                Point bottomRightPos = new Point(position.x + offset.x
                        + frame.getWidth(), position.y + offset.y
                        + frame.getHeight());
                if (startPosition != null
                        && !(new Rectangle(screenSize).contains(bottomRightPos))) {
                    position = new Point(startPosition);
                }
                // Keep translating otherwise
                position.translate(offset.x, offset.y);
            }
            frame.setLocation(position);

            if (startPosition == null)
                startPosition = new Point(position);
        }
        if (dimension != null) {
            frame.setSize(dimension);
        }
        if (isMaximized) {
            frame.setExtendedState(Frame.MAXIMIZED_HORIZ | Frame.MAXIMIZED_VERT);
        }
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        if (!(e.getComponent() instanceof JFrame))
            return;
        JFrame frame = (JFrame) e.getComponent();
        int state = frame.getExtendedState();
        isMaximized = ((state & Frame.MAXIMIZED_HORIZ) != 0)
                && ((state & Frame.MAXIMIZED_VERT) != 0);
        if (!isMaximized) {
            position = frame.getLocation();
        }
    }

    @Override
    public void componentResized(ComponentEvent e) {
        if (!(e.getComponent() instanceof JFrame))
            return;
        JFrame frame = (JFrame) e.getComponent();
        int state = frame.getExtendedState();
        isMaximized = ((state & Frame.MAXIMIZED_HORIZ) != 0)
                && ((state & Frame.MAXIMIZED_VERT) != 0);
        if (!isMaximized) {
            dimension = frame.getSize();
        }
    }

    @Override
    public void componentHidden(ComponentEvent e) {
        // ignore
    }

    @Override
    public void componentShown(ComponentEvent e) {
        // ignore
    }

}
