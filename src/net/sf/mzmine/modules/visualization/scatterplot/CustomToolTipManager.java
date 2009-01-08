/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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


package net.sf.mzmine.modules.visualization.scatterplot;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

public class CustomToolTipManager extends MouseAdapter implements MouseMotionListener  {

	String toolTipText;
    Point  preferredLocation;
    JComponent insideComponent;
    MouseEvent mouseEvent;
    private ToolTipWindow tipWindow;
	
    //private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private ScatterPlotDataSet dataSet;
	private int fold;
	
    /** The Window tip is being displayed in. This will be non-null if
     * the Window tip is in differs from that of insideComponent's Window.
     */

    boolean enabled = true;
    private boolean tipShowing = false;
    private static boolean ignore = false;
   
    private FocusListener focusChangeListener = null;
    private MouseMotionListener moveBeforeEnterListener = null;


    public CustomToolTipManager() {

    	moveBeforeEnterListener = new MoveBeforeEnterListener();
    }

    /**
     * Enables or disables the tooltip.
     *
     * @param flag  true to enable the tip, false otherwise
     */
    public void setEnabled(boolean flag) {
        enabled = flag;
        if (!flag) {
            hideTipWindow();
        }
    }
    
	public void setDataFile(ScatterPlotDataSet newSet) {
		this.dataSet = newSet;
	}

	public void setSelectedFold(int fold) {
		this.fold = fold;
	}


    /**
     * Returns true if this object is enabled.
     *
     * @return true if this object is enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }


    synchronized void  showTipWindow() {
        if(tipWindow != null)
            return;
        
        Component component = mouseEvent.getComponent();
        
	for (Container p = component.getParent(); p != null; p = p.getParent()) {
            if (p instanceof JPopupMenu) break;
	    if (p instanceof Window) {
		if (!((Window)p).isFocused()) {
		    return;
		}
		break;
	    }
	}
        if (enabled) {
            Point screenLocation = insideComponent.getLocationOnScreen();
            Point location = new Point();

            // Just to be paranoid
            //hideTipWindow();

            if(preferredLocation != null) {
                location.x = screenLocation.x + preferredLocation.x;
                location.y = screenLocation.y + preferredLocation.y;
            } else {
                location.x = screenLocation.x + mouseEvent.getX();
                location.y = screenLocation.y + mouseEvent.getY() + 20;
            }
            int index = Integer.parseInt(toolTipText);
            Frame parentFrame = frameForComponent(insideComponent);
    	    tipWindow = new ToolTipWindow(index, dataSet, fold, parentFrame);;
    	    tipWindow.setVisible(true);
    	    tipWindow.setLocation(location);
    	    
    	    tipWindow.pack();
    	    tipWindow.repaint();
    	    
    	    //logger.finest("Pinta tipwindow");
    	    
    	    tipShowing = true;
       }
        


    }

    void hideTipWindow() {
        if (tipWindow != null) {
        tipWindow.setVisible(false);
        tipWindow.dispose();
	    tipWindow = null;
	    tipShowing = false;
        }
    }
    


    // add keylistener here to trigger tip for access
    /**
     * Registers a component for tooltip management.
     * <p>
     * This will register key bindings to show and hide the tooltip text
     * only if <code>component</code> has focus bindings. This is done
     * so that components that are not normally focus traversable, such
     * as <code>JLabel</code>, are not made focus traversable as a result
     * of invoking this method.
     *
     * @param component  a <code>JComponent</code> object to add
     * @see JComponent#isFocusTraversable
     */
    public void registerComponent(JComponent component) {
    	
        component.removeMouseListener(this);
        component.addMouseListener(this);
        //logger.finest("Adiciona mouseListener");
        component.removeMouseMotionListener(moveBeforeEnterListener);
        component.addMouseMotionListener(moveBeforeEnterListener);
    }

    /**
     * Removes a component from tooltip control.
     *
     * @param component  a <code>JComponent</code> object to remove
     */
    public void unregisterComponent(JComponent component) {
        component.removeMouseListener(this);
        component.removeMouseMotionListener(moveBeforeEnterListener);

    }


    // implements java.awt.event.MouseListener
    /**
     *  Called when the mouse enters the region of a component.
     *  This determines whether the tool tip should be shown.
     *
     *  @param event  the event in question
     */
    public void mouseEntered(MouseEvent event) {
        initiateToolTip(event);
        //logger.finest("Event" + event);
    }

    private void initiateToolTip(MouseEvent event) {

    	JComponent component = (JComponent)event.getSource();
        String newToolTipText = component.getToolTipText(event);

        if (newToolTipText == null)
        	return;

      //  logger.finest(" Tooltip text = " + newToolTipText);
        
        component.removeMouseMotionListener(moveBeforeEnterListener);

        Point location = event.getPoint();
        // ensure tooltip shows only in proper place
        if (location.x < 0 || 
        		location.x >=component.getWidth() ||
        		location.y < 0 ||
        		location.y >= component.getHeight()) {
        	return;
        }

       // logger.finest(" Location x = " + location.x + " y = " + location.y);

        // A component in an unactive internal frame is sent two
	// mouseEntered events, make sure we don't end up adding
	// ourselves an extra time.
        component.removeMouseMotionListener(this);
        component.addMouseMotionListener(this);

            mouseEvent = event;
            Point newPreferredLocation = component.getToolTipLocation(
                                                         event);
            toolTipText = newToolTipText;
            preferredLocation = newPreferredLocation;
            insideComponent = component;
            showTipWindow();
    }

    // implements java.awt.event.MouseListener
    /**
     *  Called when the mouse exits the region of a component.
     *  Any tool tip showing should be hidden.
     *
     *  @param event  the event in question
     */
    public void mouseExited(MouseEvent event) {
        insideComponent = null;
        toolTipText = null;
        mouseEvent = null;
        hideTipWindow();
       // logger.finest("Event" + event);

    }

    // implements java.awt.event.MouseListener
    /**
     *  Called when the mouse is pressed.
     *  Any tool tip showing should be hidden.
     *
     *  @param event  the event in question
     */
    public void mousePressed(MouseEvent event) {
        hideTipWindow();
        mouseEvent = null;
      //  logger.finest("Event" + event);

    }

    // implements java.awt.event.MouseMotionListener
    /**
     *  Called when the mouse is pressed and dragged.
     *  Does nothing.
     *
     *  @param event  the event in question
     */
    public void mouseDragged(MouseEvent event) {
    }

    // implements java.awt.event.MouseMotionListener
    /**
     *  Called when the mouse is moved.
     *  Determines whether the tool tip should be displayed.
     *
     *  @param event  the event in question
     */
    public void mouseMoved(MouseEvent event) {
    	
    	//logger.finest("mouse event " + " ignore " + ignore);
    	if (ignore) return;
    	ignore = true;
        if (tipShowing) {
        	//logger.finest("TipShowing " + tipShowing);
            checkForTipChange(event);
        }
        else{
        	//logger.finest("TipShowing " + tipShowing);
        	initiateToolTip(event);
        }
    	ignore = false;
    }

    /**
     * Checks to see if the tooltip needs to be changed in response to
     * the MouseMoved event <code>event</code>.
     */
    private void checkForTipChange(MouseEvent event) {
        JComponent component = (JComponent)event.getSource();
        String newText = component.getToolTipText(event);
        Point  newPreferredLocation = component.getToolTipLocation(event);

        if (newText != null || newPreferredLocation != null) {
            mouseEvent = event;
          //  logger.finest("Valor de textToolTip " + newText + " old " + toolTipText);
            if ((newText.equals(toolTipText))){// && (tipWindow != null)){
            		return;
            } else {
                toolTipText = newText;
                preferredLocation = newPreferredLocation;
                hideTipWindow();
                showTipWindow();
            }
        } else {
            toolTipText = null;
            preferredLocation = null;
            mouseEvent = null;
            hideTipWindow();
        }
    }


  /* This listener is registered when the tooltip is first registered
   * on a component in order to catch the situation where the tooltip
   * was turned on while the mouse was already within the bounds of
   * the component.  This way, the tooltip will be initiated on a
   * mouse-entered or mouse-moved, whichever occurs first.  Once the
   * tooltip has been initiated, we can remove this listener and rely
   * solely on mouse-entered to initiate the tooltip.
   */
    private class MoveBeforeEnterListener extends MouseMotionAdapter {
        public void mouseMoved(MouseEvent e) {
            if (tipShowing) {
                checkForTipChange(e);
            }
            else{
            	initiateToolTip(e);
            }
        }
    }

    static Frame frameForComponent(Component component) {
        while (!(component instanceof Frame)) {
            component = component.getParent();
        }
        return (Frame)component;
    }


}

