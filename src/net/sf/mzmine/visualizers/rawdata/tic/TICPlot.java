/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.tic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import net.sf.mzmine.userinterface.mainwindow.MainWindow;

/**
 * 
 */
public class TICPlot extends JPanel implements ActionListener, MouseListener,
        MouseMotionListener {

    private int minX, maxX;


    private int selectionFirstClick;
    private int selectionLastClick;

    private JPopupMenu popupMenu;
    private JMenuItem zoomToSelectionMenuItem;
    private JMenuItem zoomOutMenuItem;
    private JMenuItem zoomOutLittleMenuItem;
    private JMenuItem zoomSameToOthersMenuItem;
    private JMenuItem changeTicXicModeMenuItem;

    private TICVisualizer masterFrame;

    // Mouse selection
    private int mouseAreaStart;
    private int mouseAreaEnd;

    /**
     * Constructor: initializes the plot panel
     * 
     */
    public TICPlot(TICVisualizer masterFrame) {

        this.masterFrame = masterFrame;

        // Create popup-menu
        popupMenu = new JPopupMenu();
        zoomToSelectionMenuItem = new JMenuItem("Zoom to selection");
        zoomToSelectionMenuItem.addActionListener(this);
        zoomToSelectionMenuItem.setEnabled(false);
        popupMenu.add(zoomToSelectionMenuItem);

        zoomOutMenuItem = new JMenuItem("Zoom out full");
        zoomOutMenuItem.addActionListener(this);
        popupMenu.add(zoomOutMenuItem);

        zoomOutLittleMenuItem = new JMenuItem("Zoom out little");
        zoomOutLittleMenuItem.addActionListener(this);
        popupMenu.add(zoomOutLittleMenuItem);

        zoomSameToOthersMenuItem = new JMenuItem(
                "Set same zoom to other raw data viewers");
        zoomSameToOthersMenuItem.addActionListener(this);
        popupMenu.add(zoomSameToOthersMenuItem);

        popupMenu.addSeparator();

        changeTicXicModeMenuItem = new JMenuItem("Switch to XIC");
        changeTicXicModeMenuItem.addActionListener(this);
        popupMenu.add(changeTicXicModeMenuItem);

        selectionFirstClick = -1;
        selectionLastClick = -1;

        addMouseListener(this);
        addMouseMotionListener(this);
        
        setMinimumSize(new Dimension(300,100));
        setPreferredSize(new Dimension(500,250));
    }

    /**
     * This method paints the plot to this panel
     */
    public void paint(Graphics g) {

        super.paint(g);

        int width = getWidth();
        int height = getHeight();

        double retentionTimes[] = masterFrame.getRetentionTimes();
        double intensities[] = masterFrame.getIntensities();
        assert retentionTimes != null && intensities != null;

        double retValueMin = masterFrame.getZoomRTMin();
        double retValueMax = masterFrame.getZoomRTMax();
        double intValueMin = masterFrame.getZoomIntensityMin();
        double intValueMax = masterFrame.getZoomIntensityMax();

        int startIndex = 0, endIndex = retentionTimes.length - 1;
        while(retentionTimes[startIndex] < retValueMin) {
            if (startIndex == retentionTimes.length - 1) break;
            startIndex++;
        }
        while(retentionTimes[endIndex] > retValueMax) {
            if (endIndex == 0) break;
            endIndex--;
        }
        
        /* check if there is anything to draw */
        if (startIndex >= endIndex) return;     
        
        // Draw selection
        /*
         * x1 = (int) Math.round((double) diff_x_scr ((double) (mouseAreaStart -
         * minX) / (double) diff_x_dat)); x2 = (int) Math.round((double)
         * diff_x_scr ((double) (mouseAreaEnd - minX) / (double) diff_x_dat));
         * y1 = (int) (h - diff_y_scr * ((minY - minY) / diff_y_dat)); y2 =
         * (int) (h - diff_y_scr * ((maxY - minY) / diff_y_dat));
         * g.setColor(Color.lightGray); g.fillRect(x1, y2, x2 - x1, y1 - y2); //
         * x2-x1,y1-y2);
         */
        
        // Draw linegraph
        g.setColor(Color.blue);
        int x, y, prevx = 0, prevy = 0;
        double xAxisStep = (retValueMax - retValueMin) / width;
        double yAxisStep = (intValueMax - intValueMin) / height;
                
        for (int ind = startIndex; ind <= endIndex; ind++) {
            
            x = (int) Math.round((retentionTimes[ind] - retValueMin) / xAxisStep);
            y = height - (int) Math.round((intensities[ind] - intValueMin) / yAxisStep);
            
            if ((ind > startIndex) && (x > 0) && (y > 0)) { 
                g.drawLine(prevx, prevy, x, y);
                //Logger.put("line " + prevx + ":" + prevy + " -> " + x + ":" + y + " " + (retentionTimes[ind] - retValueMin));
            }

            prevx = x;
            prevy = y;
        }




        // Draw cursor position
        /*x = getCursorPositionScan();
        prevy = minY;
        y = maxY;
        x2 = (int) java.lang.Math.round((double) diff_x_scr
                * ((double) (x - minX) / (double) diff_x_dat));
        y1 = (int) (h - diff_y_scr * ((prevy - minY) / diff_y_dat));
        y2 = (int) (h - diff_y_scr * ((y - minY) / diff_y_dat));
        g.setColor(Color.red);
        g.drawLine(x2, y1, x2, y2);*/

    }

    /**
     * Implementation of ActionListener interface for this panel
     */
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();

        // Zoom into selected scan region
        if (src == zoomToSelectionMenuItem) {

            // Set run's scan range to current selected area in this plot
            // rawData.setSelectionScan(mouseAreaStart, mouseAreaEnd);

            // Clear selected area in the plot
            mouseAreaStart = getCursorPositionScan();
            mouseAreaEnd = getCursorPositionScan();
            zoomToSelectionMenuItem.setEnabled(false);

            // Refresh all visualizers
            // RawDataFile[] tmpRawDatas = new RawDataAtClient[1];
            // tmpRawDatas[0] = rawData;
            // MainWindow.getInstance().startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_SCAN,
            // rawData.getRawDataID());
            /*
             * BackgroundThread bt = new
             * BackgroundThread(MainWindow.getInstance(), msRun,
             * Visualizer.CHANGETYPE_SELECTION_SCAN,
             * BackgroundThread.TASK_REFRESHVISUALIZERS); bt.start();
             */
        }

        // Show whole scan region
        if (src == zoomOutMenuItem) {

            // Clear run's scan range: reset to full scan range
            // rawData.clearSelectionScan();

            // Clear selected area in the plot
            mouseAreaStart = getCursorPositionScan();
            mouseAreaEnd = getCursorPositionScan();

            // Refresh all visualizers
            // MainWindow.getInstance().startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_SCAN,
            // rawData.getRawDataID());

            /*
             * BackgroundThread bt = new
             * BackgroundThread(MainWindow.getInstance(), msRun,
             * Visualizer.CHANGETYPE_SELECTION_SCAN,
             * BackgroundThread.TASK_REFRESHVISUALIZERS); bt.start();
             */
        }

        // Zoom out to little wider scan region
        if (src == zoomOutLittleMenuItem) {

            // Calculate boundaries of the wider scan region based on the
            // current region
            int midX = (int) (java.lang.Math.round((double) (minX + maxX)
                    / (double) 2));
            int tmpMinX, tmpMaxX;

            if (((midX - minX) > 0) && ((maxX - midX) > 0)) {
                tmpMinX = (int) (java.lang.Math.round(midX - (midX - minX)
                        * 1.5));
                tmpMaxX = (int) (java.lang.Math.round(midX + (maxX - midX)
                        * 1.5));
            } else {
                tmpMinX = minX - 1;
                tmpMaxX = maxX + 1;
            }

            if (tmpMinX < 0) {
                tmpMinX = 0;
            }
     /*       if (tmpMaxX > (rawData.getNumOfScans() - 1)) {
                tmpMaxX = rawData.getNumOfScans() - 1;
            }*/

            // Set run's scan range to selected values
            // rawData.setSelectionScan(tmpMinX, tmpMaxX);

            // Clear selected area in the plot
            mouseAreaStart = getCursorPositionScan();
            mouseAreaEnd = getCursorPositionScan();
            zoomToSelectionMenuItem.setEnabled(false);

            // Refresh all visualizers
            // MainWindow.getInstance().startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_SCAN,
            // rawData.getRawDataID());

            /*
             * BackgroundThread bt = new
             * BackgroundThread(MainWindow.getInstance(), msRun,
             * Visualizer.CHANGETYPE_SELECTION_SCAN,
             * BackgroundThread.TASK_REFRESHVISUALIZERS); bt.start();
             */
        }

        // Copy same scan range settings to all other open runs
        if (src == zoomSameToOthersMenuItem) {

            // MainWindow.getInstance().setSameZoomToOtherRawDatas(rawData,
            // masterFrame.MainWindow.getInstance().SET_SAME_ZOOM_SCAN);

        }

        // Show a dialog where user can select range for XIC and switch to
        // that XIC
        if (src == changeTicXicModeMenuItem) {

        //    if (masterFrame.getXicMode()) {
        //        masterFrame.setTicMode();
        //        changeTicXicModeMenuItem.setText("Switch to XIC");
        //    }
            // Default range is cursor location +- 0.25
            double ricMZ = 0; //getCursorPositionMZ();
            double ricMZDelta = (double) 0.25;

            // Show dialog
            XICSetupDialog psd = new XICSetupDialog(
                    "Please give centroid and delta MZ values for XIC", ricMZ,
                    ricMZDelta);
            psd.setVisible(true);
            // if cancel was clicked
            if (psd.getExitCode() == -1) {
                MainWindow.getInstance().getStatusBar().setStatusText(
                        "Switch to XIC cancelled.");
                return;
            }

            // Validate given parameter values
            double d;
            int i;
            d = psd.getXicMZ();
            if (d < 0) {
                MainWindow.getInstance().getStatusBar().setStatusText(
                        "Error: incorrect parameter values.");
                return;
            }
            ricMZ = d;

            d = psd.getXicMZDelta();
            if (d < 0) {
                MainWindow.getInstance().getStatusBar().setStatusText(
                        "Error: incorrect parameter values.");
                return;
            }
            ricMZDelta = d;

        //    masterFrame.setXicMode(ricMZ - ricMZDelta, ricMZ + ricMZDelta);
            // Set run's mz range

            changeTicXicModeMenuItem.setText("Switch to TIC");

            // Refresh all visualizers
            // MainWindow.getInstance().startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_MZ,
            // rawData.getRawDataID());

            /*
             * BackgroundThread bt = new
             * BackgroundThread(MainWindow.getInstance(), msRun,
             * Visualizer.CHANGETYPE_SELECTION_MZ,
             * BackgroundThread.TASK_REFRESHVISUALIZERS); bt.start();
             */
        }
    }

    /**
     * Implementation of MouseListener interface methods
     */
    public void mouseClicked(MouseEvent e) {
        MainWindow.getInstance().getStatusBar().setStatusText("");
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {

        // If it wasn't normal click with first mouse button
        if (e.getButton() != MouseEvent.BUTTON1) {
            // then show pop up menu
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        } else {
            // else clear the selection
            selectionFirstClick = -1;
            selectionLastClick = -1;
        }
    }

    private int lastButtonPressedWas;

    public void mousePressed(MouseEvent e) {

        lastButtonPressedWas = e.getButton();

        // Only interested about normal first mouse button clicks here
        if (e.getButton() == MouseEvent.BUTTON1) {
            // Calculate scan number corresponding to the x coordinate of
            // mouse cursor when mouse was clicked
            int w = getWidth();
            double diff_x_dat = maxX - minX;
            double diff_x_scr = w;
            int xpos = (int) java.lang.Math.round((minX + diff_x_dat * e.getX()
                    / diff_x_scr));
            selectionFirstClick = xpos;

            // Clear selected area in the plot
            mouseAreaStart = xpos;
            mouseAreaEnd = xpos;
            zoomToSelectionMenuItem.setEnabled(false);

            // Set run's cursor location over the clicked scan
            setCursorPositionScan(xpos);
            // /
            // MainWindow.getInstance().getStatusBar().setCursorPosition(rawData);

            // Refresh visualizers
            // MainWindow.getInstance().startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_CURSORPOSITION_SCAN,
            // rawData.getRawDataID());
            /*
             * BackgroundThread bt = new
             * BackgroundThread(MainWindow.getInstance(), msRun,
             * Visualizer.CHANGETYPE_CURSORPOSITION_SCAN,
             * BackgroundThread.TASK_REFRESHVISUALIZERS); bt.start();
             */

        }
        MainWindow.getInstance().getStatusBar().setStatusText("");

    }

    /**
     * Implementation of methods for MouseMotionListener interface
     */
    public void mouseDragged(MouseEvent e) {

        if (lastButtonPressedWas != MouseEvent.BUTTON1) {
            return;
        }

        // Calculate scan number corresponding to the x coordinate of mouse
        // cursor
        int w = getWidth();
        double diff_x_dat = maxX - minX;
        double diff_x_scr = w;
        int xpos = (int) java.lang.Math.round(minX + diff_x_dat * e.getX()
                / diff_x_scr);

        // If area selection process is not underway, then start it and set
        // cursor to the starting point of area selection
        if (selectionFirstClick == -1) {
            selectionFirstClick = xpos;
            setCursorPositionScan(xpos);
        } else {

            // Otherwise, set the end point of selected area to current
            // cursor location
            selectionLastClick = xpos;

            // Make sure that the area doesn't extend over the full scan
            // range of this run
            if (selectionLastClick < 0) {
                selectionLastClick = 0;
            }
            /*if (selectionLastClick >= rawData.getNumOfScans()) {
                selectionLastClick = rawData.getNumOfScans() - 1;
            }*/

            // Sort selection first and last click point in acceding order
            // for drawing the selection
            if (selectionLastClick > selectionFirstClick) {
                mouseAreaStart = selectionFirstClick;
                mouseAreaEnd = selectionLastClick;
            } else {
                mouseAreaStart = selectionLastClick;
                mouseAreaEnd = selectionFirstClick;
            }

            // There is now some selection made and "zoom to selection" item
            // should be enabled in the pop-up menu
            zoomToSelectionMenuItem.setEnabled(true);
        }

        MainWindow.getInstance().getStatusBar().setStatusText("");
        repaint();

    }

    public void mouseMoved(MouseEvent e) {
    }
    
    int getCursorPositionScan() {
        return 0;
    }
    
    void setCursorPositionScan(int xpos) {
        
    }

}
