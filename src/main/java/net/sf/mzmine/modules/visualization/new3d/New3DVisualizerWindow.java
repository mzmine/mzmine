/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.visualization.new3d;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.desktop.impl.WindowsMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;
import net.sf.mzmine.taskcontrol.TaskPriority;
import visad.ProjectionControl;
import visad.VisADException;
import visad.java3d.MouseBehaviorJ3D;

import com.google.common.collect.Range;

/**
 * 3D visualizer frame.
 */
public class New3DVisualizerWindow extends JFrame{

  private static final long serialVersionUID = 1L;

  // Logger.
  private static final Logger LOG = Logger.getLogger(New3DVisualizerWindow.class.getName());

  // Title font.
  private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 12);

  // Plot size preference.
  private static final Dimension PREFERRED_PLOT_SIZE = new Dimension(700, 500);

 // private final ThreeDDisplay display;
  private final ThreeDBottomPanel bottomPanel;
  private JDialog propertiesDialog;

  // Axes bounds.
  private final Range<Double> rtRange;
  private final Range<Double> mzRange;

  // Raw data file.
  private final RawDataFile dataFile;

  /**
   * Create the visualization window.
   *
   * @param file the raw data file.
   * @param msLevel MS level
   * @param rt RT range.
   * @param rtRes RT resolution.
   * @param mz m/z range.
   * @param mzRes m/z resolution.
   * @throws RemoteException if there are VisAD problems.
   * @throws VisADException if there are VisAD problems.
   */
  public New3DVisualizerWindow(final RawDataFile file, final Scan scans[], final Range<Double> rt,
      final int rtRes, final Range<Double> mz, final int mzRes)
      throws VisADException, RemoteException {

    super("3D view: [" + file.getName() + "]");

    // Configure.
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setBackground(Color.white);

    // Initialize.
    dataFile = file;
    rtRange = rt;
    mzRange = mz;

    // Create 3D display and configure its component.
   // display = new ThreeDDisplay();
   // final Component plot3D = display.getComponent();
    //plot3D.setPreferredSize(PREFERRED_PLOT_SIZE);
//    plot3D.addMouseWheelListener(this);

    // Create bottom panel.
    bottomPanel = new ThreeDBottomPanel(this, file);

    // Layout panel.
    setLayout(new BorderLayout());
  //  add(plot3D, BorderLayout.CENTER);
    add(createTitleLabel(file.getName()), BorderLayout.NORTH);
    //add(new ThreeDToolBar(this), BorderLayout.EAST);
    add(bottomPanel, BorderLayout.SOUTH);

    // Add the Windows menu
    JMenuBar menuBar = new JMenuBar();
    menuBar.add(new WindowsMenu());
    setJMenuBar(menuBar);

    pack();

    // get the window settings parameter
    ParameterSet paramSet =
        MZmineCore.getConfiguration().getModuleParameters(New3DVisualizerModule.class);
    WindowSettingsParameter settings =
        paramSet.getParameter(ThreeDVisualizerParameters.windowSettings);

    // update the window and listen for changes
    settings.applySettingsToWindow(this);
    this.addComponentListener(settings);

    // Add sampling task.
    MZmineCore.getTaskController().addTask(
        new ThreeDSamplingTask(file, scans, rtRange, mzRange, rtRes, mzRes, bottomPanel),
        TaskPriority.HIGH);

    MZmineCore.getDesktop().addPeakListTreeListener(bottomPanel);
  }

 /**
   * Create title label.
   *
   * @param text title text.
   * @return the newly created label.
   */
  private static JLabel createTitleLabel(final String text) {
    final JLabel label = new JLabel(text);
    label.setFont(TITLE_FONT);
    label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    label.setHorizontalAlignment(SwingConstants.CENTER);
    return label;
  }
}
