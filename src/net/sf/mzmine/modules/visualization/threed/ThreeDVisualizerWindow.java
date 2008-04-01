/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.threed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.util.Range;
import visad.ProjectionControl;
import visad.java3d.MouseBehaviorJ3D;

/**
 * 3D visualizer frame
 */
public class ThreeDVisualizerWindow extends JInternalFrame implements
        TaskListener, MouseWheelListener, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private static final Font titleFont = new Font("SansSerif", Font.BOLD, 12);

    private ThreeDToolBar toolBar;
    private JLabel titleLabel;
    private ThreeDBottomPanel bottomPanel;

    private RawDataFile dataFile;
    private int msLevel;

    private ThreeDDisplay display;

    // Axes bounds
    private Range rtRange, mzRange;

    private Desktop desktop;

    public ThreeDVisualizerWindow(RawDataFile dataFile, int msLevel,
            Range rtRange, int rtResolution, Range mzRange, int mzResolution) {

        super(dataFile.toString(), true, true, true, true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        this.desktop = MZmineCore.getDesktop();
        this.dataFile = dataFile;
        this.msLevel = msLevel;
        this.rtRange = rtRange;
        this.mzRange = mzRange;

        toolBar = new ThreeDToolBar(this);
        add(toolBar, BorderLayout.EAST);

        titleLabel = new JLabel();
        titleLabel.setFont(titleFont);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        add(titleLabel, BorderLayout.NORTH);

        bottomPanel = new ThreeDBottomPanel(this, dataFile);
        add(bottomPanel, BorderLayout.SOUTH);

        int scanNumbers[] = dataFile.getScanNumbers(msLevel, rtRange);
        if (scanNumbers.length == 0) {
            desktop.displayErrorMessage("No scans found at MS level " + msLevel
                    + " within given retention time range.");
            return;
        }

        // create 3D display
        try {
            display = new ThreeDDisplay();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // add the 3D component
        Component threeDPlot = display.getComponent();
        threeDPlot.setPreferredSize(new Dimension(700, 500));
        threeDPlot.addMouseWheelListener(this);
        add(threeDPlot, BorderLayout.CENTER);

        updateTitle();
        pack();

        Task updateTask = new ThreeDSamplingTask(dataFile, scanNumbers,
                rtRange, mzRange, rtResolution, mzResolution, display);

        MZmineCore.getTaskController().addTask(updateTask, TaskPriority.HIGH,
                this);

        // After we have constructed everything, load the peak lists into the
        // bottom panel
        bottomPanel.rebuildPeakListSelector(MZmineCore.getCurrentProject());

    }

    void updateTitle() {

        StringBuffer title = new StringBuffer();

        title.append("[" + dataFile + "]");
        title.append(": 3D view");

        setTitle(title.toString());

        title.append(", MS");
        title.append(msLevel);

        titleLabel.setText(title.toString());

    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {

        if (task.getStatus() == TaskStatus.ERROR) {
            desktop.displayErrorMessage("Error while updating 3D visualizer: "
                    + task.getErrorMessage());
            return;
        }

        if (task.getStatus() == TaskStatus.FINISHED) {
            // Add this window to desktop
            desktop.addInternalFrame(this);
        }

    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskStarted(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskStarted(Task task) {
        // do nothing
    }

    /**
     * @see java.awt.event.MouseWheelListener#mouseWheelMoved(java.awt.event.MouseWheelEvent)
     */
    public void mouseWheelMoved(MouseWheelEvent event) {

        int rot = event.getWheelRotation();
        try {

            ProjectionControl pControl = display.getProjectionControl();
            double[] pControlMatrix = pControl.getMatrix();

            // scale depending on wheel rotation direction
            double scale = (rot < 0 ? 1.03 : 0.97);

            double[] mult = MouseBehaviorJ3D.static_make_matrix(0.0, 0.0, 0.0,
                    scale, 0.0, 0.0, 0.0);

            double newMatrix[] = MouseBehaviorJ3D.static_multiply_matrix(mult,
                    pControlMatrix);

            pControl.setMatrix(newMatrix);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals("PROPERTIES")) {
            ThreeDPropertiesDialog dialog = new ThreeDPropertiesDialog(display);
            dialog.setVisible(true);
        }

        if (command.equals("PEAKLIST_CHANGE")) {

            PeakList selectedPeakList = bottomPanel.getSelectedPeakList();
            if (selectedPeakList == null)

                return;

            logger.finest("Loading a peak list " + selectedPeakList
                    + " to a 3D view of " + dataFile);

            Peak peaks[] = selectedPeakList.getPeaksInsideScanAndMZRange(
                    dataFile, rtRange, mzRange);

            display.setPeaks(selectedPeakList, peaks,
                    bottomPanel.showCompoundNameSelected());

        }

        if (command.equals("SHOW_ANNOTATIONS")) {
            display.toggleShowingPeaks();
        }

    }

}