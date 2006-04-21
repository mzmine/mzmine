/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.tic;

import java.awt.Color;
import java.awt.Cursor;
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
import net.sf.mzmine.util.FormatCoordinates;
import net.sf.mzmine.visualizers.rawdata.spectra.SpectrumVisualizer;

/**
 * 
 */
public class TICPlot extends JPanel implements ActionListener, MouseListener,
        MouseMotionListener {

    private static final int SELECTION_TOLERANCE = 10;

    private JPopupMenu popupMenu;
    private JMenuItem zoomOutMenuItem;
    private JMenuItem showSpectrumMenuItem;
    private JMenuItem zoomSameToOthersMenuItem;
    private JMenuItem changeTicXicModeMenuItem;

    private TICVisualizer masterFrame;

    private boolean mousePresent = false;
    private int mousePositionX, mousePositionY;
    private int lastClickX, lastClickY;
    private boolean mouseSelection = false;

    /**
     * Constructor: initializes the plot panel
     * 
     */
    public TICPlot(TICVisualizer masterFrame) {

        this.masterFrame = masterFrame;

        setBackground(Color.white);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        // Create popup-menu
        popupMenu = new JPopupMenu();
        popupMenu.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        zoomOutMenuItem = new JMenuItem("Zoom out");
        zoomOutMenuItem.addActionListener(this);
        zoomOutMenuItem.setEnabled(false);
        popupMenu.add(zoomOutMenuItem);

        zoomSameToOthersMenuItem = new JMenuItem(
                "Set same zoom to other raw data viewers");
        zoomSameToOthersMenuItem.addActionListener(this);
        popupMenu.add(zoomSameToOthersMenuItem);

        popupMenu.addSeparator();

        showSpectrumMenuItem = new JMenuItem("Show spectrum");
        showSpectrumMenuItem.addActionListener(this);
        popupMenu.add(showSpectrumMenuItem);

        popupMenu.addSeparator();

        changeTicXicModeMenuItem = new JMenuItem("Switch to XIC");
        changeTicXicModeMenuItem.addActionListener(this);
        popupMenu.add(changeTicXicModeMenuItem);

        addMouseListener(this);
        addMouseMotionListener(this);

        setMinimumSize(new Dimension(300, 100));
        setPreferredSize(new Dimension(500, 250));
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
        int scanNumbers[] = masterFrame.getScanNumbers();
        assert retentionTimes != null && intensities != null
                & scanNumbers != null;

        final double retValueMin = masterFrame.getZoomRTMin();
        final double retValueMax = masterFrame.getZoomRTMax();
        final double intValueMin = masterFrame.getZoomIntensityMin();
        final double intValueMax = masterFrame.getZoomIntensityMax();
        final double xAxisStep = (retValueMax - retValueMin) / width;
        final double yAxisStep = (intValueMax - intValueMin) / height;

        int startIndex = 1, endIndex = retentionTimes.length - 1;
        while (retentionTimes[startIndex] < retValueMin - 1) {
            if (startIndex == retentionTimes.length)
                break;
            startIndex++;
        }
        startIndex--;
        while (retentionTimes[endIndex] > retValueMax) {
            if (endIndex == 0)
                break;
            endIndex--;
        }

        // Draw selection
        if (mouseSelection) {
            g.setColor(Color.lightGray);
            int selX = Math.min(lastClickX, mousePositionX);
            int selY = Math.min(lastClickY, mousePositionY);
            int selWidth = Math.abs(mousePositionX - lastClickX);
            int selHeight = Math.abs(mousePositionY - lastClickY);
            if ((selWidth > SELECTION_TOLERANCE)
                    && (selHeight > SELECTION_TOLERANCE)) {
                g.drawRect(selX, selY, selWidth, selHeight);
            } else if (selWidth > SELECTION_TOLERANCE) {
                g.drawLine(lastClickX, lastClickY, mousePositionX, lastClickY);
            } else if (selHeight > SELECTION_TOLERANCE) {
                g.drawLine(lastClickX, lastClickY, lastClickX, mousePositionY);
            }
        }

        // Draw linegraph
        if (startIndex < endIndex) {

            g.setColor(new Color(0, 0, 224));
            int x, y, prevx = 0, prevy = 0;

            for (int ind = startIndex; ind <= endIndex; ind++) {

                x = (int) Math.round((retentionTimes[ind] - retValueMin)
                        / xAxisStep);
                y = height
                        - (int) Math.round((intensities[ind] - intValueMin)
                                / yAxisStep);

                if ((ind > startIndex) && (x > 0) && (y > 0)) {
                    g.drawLine(prevx, prevy, x, y);
                    // Logger.put("line " + prevx + ":" + prevy + " -> " + x +
                    // ":" +
                    // y + " " + (retentionTimes[ind] - retValueMin));
                }

                prevx = x;
                prevy = y;
            }
        }

        // draw cursor
        int cursorPosition = masterFrame.getCursorPosition();
        if (cursorPosition >= 0) {
            int cursorX = (int) Math
                    .round((retentionTimes[cursorPosition] - retValueMin)
                            / xAxisStep);
            g.setColor(Color.red);
            g.drawLine(cursorX, 0, cursorX, height);
            g.setColor(Color.black);
            g.setFont(g.getFont().deriveFont(10.0f));
            int textX;
            if (cursorX > width / 2)
                textX = cursorX - 70;
            else
                textX = cursorX + 10;
            g.drawString(
                    "Scan #" + String.valueOf(scanNumbers[cursorPosition]),
                    textX, 10);
            g.drawString("RT: "
                    + FormatCoordinates
                            .formatRTValue(retentionTimes[cursorPosition]),
                    textX, 22);
            g.drawString("IC: "
                    + FormatCoordinates
                            .formatIntensityValue(intensities[cursorPosition]),
                    textX, 34);
        }

        // draw mouse cursor
        if (mousePresent) {
            /*
             * g.drawLine(mousePositionX - 15, mousePositionY, mousePositionX +
             * 15, mousePositionY); g.drawLine(mousePositionX, 0,
             * mousePositionX, height);
             */
            double rt = retValueMin + xAxisStep * mousePositionX;
            double intensity = intValueMin + (intValueMax - intValueMin)
                    / height * (height - mousePositionY);
            String positionRT = "RT: " + FormatCoordinates.formatRTValue(rt);
            String positionInt = "IC: "
                    + FormatCoordinates.formatIntensityValue(intensity);
            int drawX = mousePositionX + 8;
            int drawY = mousePositionY - 20;

            if (drawX > width
                    - Math.max(positionRT.length(), positionInt.length()) * 5)
                drawX = mousePositionX
                        - Math.max(positionRT.length(), positionInt.length())
                        * 5 - 5;
            if (drawY < 5)
                drawY = mousePositionY + 15;
            g.setColor(Color.black);
            g.setFont(g.getFont().deriveFont(10.0f));
            g.drawString(positionRT, drawX, drawY);
            g.drawString(positionInt, drawX, drawY + 12);
        }
    }

    /**
     * Implementation of ActionListener interface for this panel
     */
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();

        // Show whole scan region
        if (src == zoomOutMenuItem) {
            zoomOutMenuItem.setEnabled(false);
            masterFrame.resetRTRange();
            masterFrame.resetIntensityRange();
        }

        // Copy same scan range settings to all other open runs
        if (src == zoomSameToOthersMenuItem) {
            // TODO:
        }
        
        if (src == showSpectrumMenuItem) {
            SpectrumVisualizer specVis = new SpectrumVisualizer(masterFrame
                    .getRawDataFile(), masterFrame.getScanNumbers()[masterFrame.getCursorPosition()]);
            MainWindow.getInstance().addInternalFrame(specVis);
        }

        // Show a dialog where user can select range for XIC and switch to
        // that XIC
        if (src == changeTicXicModeMenuItem) {

            if (masterFrame.getXicMode()) {
                masterFrame.resetMZRange();
                changeTicXicModeMenuItem.setText("Switch to XIC");
                return;
            }

            // Default range is cursor location +- 0.25
            double ricMZ = 0; // getCursorPositionMZ();
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

            ricMZ = psd.getXicMZ();
            if (ricMZ < 0) {
                MainWindow.getInstance().getStatusBar().setStatusText(
                        "Error: incorrect parameter values.");
                return;
            }

            ricMZDelta = psd.getXicMZDelta();
            if (ricMZDelta < 0) {
                MainWindow.getInstance().getStatusBar().setStatusText(
                        "Error: incorrect parameter values.");
                return;
            }

            changeTicXicModeMenuItem.setText("Switch to TIC");
            masterFrame.setMZRange(ricMZ - ricMZDelta, ricMZ + ricMZDelta);

        }

    }

    /**
     * Implementation of MouseListener interface methods
     */
    public void mouseClicked(MouseEvent e) {

        if (e.getButton() != MouseEvent.BUTTON1)
            return;

        int width = getWidth();
        double retValueMin = masterFrame.getZoomRTMin();
        double retValueMax = masterFrame.getZoomRTMax();
        final double xAxisStep = (retValueMax - retValueMin) / width;
        double clickedRT = retValueMin + (xAxisStep * e.getX());

        if (e.getClickCount() == 1) {
            masterFrame.setRTPosition(clickedRT);
        }

        if (e.getClickCount() == 2) {

            double[] retentionTimes = masterFrame.getRetentionTimes();
            int scanNumbers[] = masterFrame.getScanNumbers();

            // find the first scan number with RT higher than given rt
            int index;
            for (index = 1; index < retentionTimes.length; index++) {
                if (retentionTimes[index] > clickedRT)
                    break;
            }
            if (index == retentionTimes.length)
                return;

            if (clickedRT - retentionTimes[index - 1] < retentionTimes[index]
                    - clickedRT)
                index = index - 1;

            SpectrumVisualizer specVis = new SpectrumVisualizer(masterFrame
                    .getRawDataFile(), scanNumbers[index]);
            MainWindow.getInstance().addInternalFrame(specVis);

        }

    }

    public void mouseEntered(MouseEvent e) {
        mousePresent = true;
        repaint();
    }

    public void mouseExited(MouseEvent e) {
        mousePresent = false;
        repaint();
    }

    public void mouseReleased(MouseEvent e) {

        if (mouseSelection) {

            mouseSelection = false;
            int width = getWidth();
            int height = getHeight();
            double retValueMin = masterFrame.getZoomRTMin();
            double retValueMax = masterFrame.getZoomRTMax();
            double intValueMin = masterFrame.getZoomIntensityMin();
            double intValueMax = masterFrame.getZoomIntensityMax();
            int selX = Math.min(lastClickX, mousePositionX);
            int selY = height - Math.max(lastClickY, mousePositionY);
            int selWidth = Math.abs(mousePositionX - lastClickX);
            int selHeight = Math.abs(mousePositionY - lastClickY);
            double xAxisStep = (retValueMax - retValueMin) / width;
            double yAxisStep = (intValueMax - intValueMin) / height;

            if (selWidth > SELECTION_TOLERANCE) {
                double newRtMin = retValueMin + (selX * xAxisStep);
                double newRtMax = retValueMin + ((selX + selWidth) * xAxisStep);
                if (newRtMin < retValueMin)
                    newRtMin = retValueMin;
                if (newRtMax > retValueMax)
                    newRtMax = retValueMax;
                masterFrame.setRTRange(newRtMin, newRtMax);
                zoomOutMenuItem.setEnabled(true);
            }
            if (selHeight > SELECTION_TOLERANCE) {
                double newIntMin = intValueMin + (selY * yAxisStep);
                double newIntMax = intValueMin
                        + ((selY + selHeight) * yAxisStep);
                if (newIntMin < intValueMin)
                    newIntMin = intValueMin;
                if (newIntMax > intValueMax)
                    newIntMax = intValueMax;
                masterFrame.setIntensityRange(newIntMin, newIntMax);
                zoomOutMenuItem.setEnabled(true);
            }
            // no need to call repaint(), master frame will repaint
            // automatically
        } else if (e.isPopupTrigger()) {
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }

    }

    public void mousePressed(MouseEvent e) {

        if (e.isPopupTrigger()) {
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }

        if (e.getButton() == MouseEvent.BUTTON1) {
            mouseSelection = true;
            lastClickX = e.getX();
            lastClickY = e.getY();
        }
    }

    /**
     * Implementation of methods for MouseMotionListener interface
     */
    public void mouseDragged(MouseEvent e) {
        mousePositionX = e.getX();
        mousePositionY = e.getY();
        repaint();
    }

    public void mouseMoved(MouseEvent e) {
        mousePositionX = e.getX();
        mousePositionY = e.getY();
        repaint();
    }

}
