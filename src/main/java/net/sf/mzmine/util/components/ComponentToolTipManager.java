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

package net.sf.mzmine.util.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JWindow;
import javax.swing.ToolTipManager;

public class ComponentToolTipManager extends MouseAdapter implements
	MouseMotionListener {

    String toolTipText;
    Point preferredLocation;
    JComponent insideComponent;
    MouseEvent mouseEvent;
    private JWindow tipWindow;
    private JComponent newToolTipComponent;
    boolean enabled = true;
    boolean tipShowing = false;
    static boolean ignore = false;
    private MouseMotionListener moveBeforeEnterListener = null;

    public static String CUSTOM = "Custom - ";
    public static final Color bg = new Color(255, 250, 205);

    /**
     * 
     */
    public ComponentToolTipManager() {

	moveBeforeEnterListener = new MoveBeforeEnterListener();
    }

    /**
     * Enables or disables the tooltip.
     * 
     * @param flag
     *            true to enable the tip, false otherwise
     */
    public void setEnabled(boolean flag) {
	enabled = flag;
	if (!flag) {
	    hideTipWindow();
	}
    }

    /**
     * Returns true if this object is enabled.
     * 
     * @return true if this object is enabled, false otherwise
     */
    public boolean isEnabled() {
	return enabled;
    }

    /**
     * 
     */
    synchronized void showTipWindow() {
	if (tipWindow != null)
	    return;

	Component component = mouseEvent.getComponent();

	for (Container p = component.getParent(); p != null; p = p.getParent()) {
	    if (p instanceof JPopupMenu)
		break;
	    if (p instanceof Window) {
		if (!((Window) p).isFocused()) {
		    return;
		}
		break;
	    }
	}
	if (enabled) {
	    Point screenLocation = insideComponent.getLocationOnScreen();
	    Point location = new Point();

	    // Just to be paranoid
	    // hideTipWindow();

	    if (preferredLocation != null) {
		location.x = screenLocation.x + preferredLocation.x;
		location.y = screenLocation.y + preferredLocation.y;
	    } else {
		location.x = screenLocation.x + mouseEvent.getX();
		location.y = screenLocation.y + mouseEvent.getY() + 20;
	    }

	    // int index = Integer.parseInt(toolTipText);

	    Frame parentFrame = frameForComponent(insideComponent);

	    tipWindow = new JWindow(parentFrame);
	    tipWindow.setFocusableWindowState(false);
	    tipWindow.setLayout(new BorderLayout());
	    tipWindow.add(newToolTipComponent);
	    tipWindow.setLocation(location);
	    tipWindow.setBackground(bg);
	    tipWindow.pack();
	    tipWindow.setVisible(true);

	    tipShowing = true;
	}

    }

    /**
     * 
     */
    void hideTipWindow() {
	if (tipWindow != null) {
	    tipWindow.setVisible(false);
	    tipWindow.dispose();
	    tipWindow = null;
	    tipShowing = false;
	}
    }

    /**
     * Registers a component for tooltip management.
     * <p>
     * This will register key bindings to show and hide the tooltip text only if
     * <code>component</code> has focus bindings. This is done so that
     * components that are not normally focus traversable, such as
     * <code>JLabel</code>, are not made focus traversable as a result of
     * invoking this method.
     * 
     * @param component
     *            a <code>JComponent</code> object to add
     * @see JComponent#isFocusTraversable
     */
    public void registerComponent(JComponent component) {

	if (!(component instanceof ComponentToolTipProvider))
	    return;

	component.addMouseListener(this);
	component.addMouseMotionListener(moveBeforeEnterListener);

	ToolTipManager.sharedInstance().unregisterComponent(component);

    }

    /**
     * Removes a component from tooltip control.
     * 
     * @param component
     *            a <code>JComponent</code> object to remove
     */
    public void unregisterComponent(JComponent component) {
	component.removeMouseListener(this);
	component.removeMouseMotionListener(moveBeforeEnterListener);

    }

    /**
     * Called when the mouse enters the region of a component. This determines
     * whether the tool tip should be shown.
     * 
     * @param event
     *            the event in question
     */
    public void mouseEntered(MouseEvent event) {
	initiateToolTip(event);
    }

    /**
     * 
     * @param event
     */
    private void initiateToolTip(MouseEvent event) {

	JComponent component = (JComponent) event.getSource();
	String newToolTipText = component.getToolTipText(event);
	newToolTipComponent = ((ComponentToolTipProvider) component)
		.getCustomToolTipComponent(event);

	if (newToolTipComponent == null)
	    return;

	component.removeMouseMotionListener(moveBeforeEnterListener);

	Point location = event.getPoint();
	// ensure tooltip shows only in proper place
	if (location.x < 0 || location.x >= component.getWidth()
		|| location.y < 0 || location.y >= component.getHeight()) {
	    return;
	}

	component.removeMouseMotionListener(this);
	component.addMouseMotionListener(this);

	mouseEvent = event;
	toolTipText = newToolTipText;
	preferredLocation = component.getToolTipLocation(event);
	insideComponent = component;

	showTipWindow();
    }

    /**
     * Called when the mouse exits the region of a component. Any tool tip
     * showing should be hidden.
     * 
     * @param event
     *            the event in question
     */
    public void mouseExited(MouseEvent event) {
	reset();
	hideTipWindow();
    }

    /**
     * Called when the mouse is pressed. Any tool tip showing should be hidden.
     * 
     * @param event
     *            the event in question
     */
    public void mousePressed(MouseEvent event) {
	reset();
	hideTipWindow();
    }

    // implements java.awt.event.MouseMotionListener
    /**
     * Called when the mouse is pressed and dragged. Does nothing.
     * 
     * @param event
     *            the event in question
     */
    public void mouseDragged(MouseEvent event) {
    }

    // implements java.awt.event.MouseMotionListener
    /**
     * Called when the mouse is moved. Determines whether the tool tip should be
     * displayed.
     * 
     * @param event
     *            the event in question
     */
    public void mouseMoved(MouseEvent event) {

	if (ignore)
	    return;
	ignore = true;
	if (tipShowing) {
	    checkForTipChange(event);
	} else {
	    initiateToolTip(event);
	}
	ignore = false;
    }

    /**
     * Checks to see if the tooltip needs to be changed in response to the
     * MouseMoved event <code>event</code>.
     */
    private void checkForTipChange(MouseEvent event) {
	JComponent component = (JComponent) event.getSource();
	String newText = component.getToolTipText(event);
	Point newPreferredLocation = component.getToolTipLocation(event);

	if (newText != null && newPreferredLocation != null) {

	    if ((newText.equals(toolTipText))) {
		return;
	    } else {

		newToolTipComponent = ((ComponentToolTipProvider) component)
			.getCustomToolTipComponent(event);

		if (newToolTipComponent == null)
		    return;

		toolTipText = newText;
		mouseEvent = event;
		preferredLocation = newPreferredLocation;
		hideTipWindow();
		showTipWindow();
	    }
	} else {
	    reset();
	    hideTipWindow();
	}
    }

    private void reset() {
	toolTipText = null;
	preferredLocation = null;
	mouseEvent = null;
	newToolTipComponent = null;
    }

    /**
     * This listener is registered when the tooltip is first registered on a
     * component in order to catch the situation where the tooltip was turned on
     * while the mouse was already within the bounds of the component. This way,
     * the tooltip will be initiated on a mouse-entered or mouse-moved,
     * whichever occurs first. Once the tooltip has been initiated, we can
     * remove this listener and rely solely on mouse-entered to initiate the
     * tooltip.
     */
    private class MoveBeforeEnterListener extends MouseMotionAdapter {
	public void mouseMoved(MouseEvent e) {
	    if (tipShowing) {
		checkForTipChange(e);
	    } else {
		initiateToolTip(e);
	    }
	}
    }

    /**
     * 
     * @param component
     * @return
     */
    static Frame frameForComponent(Component component) {
	while (!(component instanceof Frame)) {
	    component = component.getParent();
	}
	return (Frame) component;
    }

}
