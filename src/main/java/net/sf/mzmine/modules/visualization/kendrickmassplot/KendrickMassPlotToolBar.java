/*
 * Copyright 2006-2019 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.kendrickmassplot;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import net.sf.mzmine.util.GUIUtils;

/**
 * Kendrick mass plot toolbar class
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class KendrickMassPlotToolBar extends JToolBar {

  private static final long serialVersionUID = 1L;
  static final Icon blockSizeIcon = new ImageIcon("icons/blocksizeicon.png");
  static final Icon backColorIcon = new ImageIcon("icons/bgicon.png");
  static final Icon gridIcon = new ImageIcon("icons/gridicon.png");
  static final Icon annotationsIcon = new ImageIcon("icons/annotationsicon.png");
  static final Icon arrowUpIcon = new ImageIcon("icons/arrowupicon.png");
  static final Icon arrowDownIcon = new ImageIcon("icons/arrowdownicon.png");
  static final Icon kmdIcon = new ImageIcon("icons/KMDIcon.png");
  static final Icon rkmIcon = new ImageIcon("icons/RKMIcon.png");
  // To Do add icon for RKM and KMD
  DecimalFormat shiftFormat = new DecimalFormat("0.##;-0.##");

  public KendrickMassPlotToolBar(ActionListener masterFrame, // listener
      double xAxisShift, double yAxisShift, double zAxisShift, // shifts
      int xAxisCharge, int yAxisCharge, int zAxisCharge, // charge
      int xAxisDivisor, int yAxisDivisor, int zAxisDivisor, // divisor
      boolean useCustomXAxis, boolean useCustomZAxis, // custom axis
      boolean useXAxisRKM, boolean useYAxisRKM, boolean useZAxisRKM // RKM or KMD icon
  ) {

    super(JToolBar.VERTICAL);

    shiftFormat.format(xAxisShift);
    shiftFormat.format(yAxisShift);
    shiftFormat.format(zAxisShift);

    setFloatable(false);
    setFocusable(false);
    setMargin(new Insets(5, 5, 5, 5));
    setBackground(Color.white);

    // list for all components
    ArrayList<JComponent> componentsList = new ArrayList<JComponent>();

    // toggle buttons
    componentsList.add(GUIUtils.addButton(this, null, blockSizeIcon, masterFrame,
        "TOGGLE_BLOCK_SIZE", "Toggle block size"));

    componentsList.add(GUIUtils.addLabel(this, ""));

    componentsList.add(GUIUtils.addButton(this, null, backColorIcon, masterFrame,
        "TOGGLE_BACK_COLOR", "Toggle background color white/black"));

    componentsList
        .add(GUIUtils.addButton(this, null, gridIcon, masterFrame, "TOGGLE_GRID", "Toggle grid"));

    componentsList.add(GUIUtils.addLabel(this, ""));

    componentsList.add(GUIUtils.addButton(this, null, annotationsIcon, masterFrame,
        "TOGGLE_ANNOTATIONS", "Toggle annotations"));

    // add empty row
    addEmptyRow(componentsList);

    // yAxis components

    // header
    componentsList.add(GUIUtils.addLabel(this, "Y-Axis:"));
    componentsList.add(GUIUtils.addLabel(this, null));
    componentsList.add(GUIUtils.addLabel(this, null));

    // labels
    componentsList.add(GUIUtils.addLabel(this, "Shift", null, JLabel.CENTER, null));
    componentsList.add(GUIUtils.addLabel(this, "Charge", null, JLabel.CENTER, null));
    componentsList.add(GUIUtils.addLabel(this, "Divisor", null, JLabel.CENTER, null));

    // arrow up
    componentsList.add(
        GUIUtils.addButton(this, null, arrowUpIcon, masterFrame, "SHIFT_KMD_UP_Y", "Shift KMD up"));
    componentsList.add(GUIUtils.addButton(this, null, arrowUpIcon, masterFrame,
        "CHANGE_CHARGE_UP_Y", "Increase charge"));
    componentsList.add(GUIUtils.addButton(this, null, arrowUpIcon, masterFrame,
        "CHANGE_DIVISOR_UP_Y", "Increase divisor"));

    // arrow down
    componentsList.add(GUIUtils.addButton(this, null, arrowDownIcon, masterFrame,
        "SHIFT_KMD_DOWN_Y", "Shift KMD down"));
    componentsList.add(GUIUtils.addButton(this, null, arrowDownIcon, masterFrame,
        "CHANGE_CHARGE_DOWN_Y", "Decrease charge"));
    componentsList.add(GUIUtils.addButton(this, null, arrowDownIcon, masterFrame,
        "CHANGE_DIVISOR_DOWN_Y", "Decrease divisor"));

    // current
    componentsList.add(GUIUtils.addLabel(this, //
        String.valueOf(shiftFormat.format(yAxisShift)), null, JLabel.CENTER, null));
    componentsList.add(GUIUtils.addLabel(this, //
        String.valueOf(shiftFormat.format(yAxisCharge)), null, JLabel.CENTER, null));
    componentsList.add(GUIUtils.addLabel(this, //
        String.valueOf(shiftFormat.format(yAxisDivisor)), null, JLabel.CENTER, null));

    // use remainders instead of defects check box
    if (useYAxisRKM == false) {
      componentsList.add(GUIUtils.addButton(this, null, kmdIcon, masterFrame, "TOGGLE_RKM_KMD_Y",
          "Toggle RKM (remainders of Kendrick masses) and KMD (Kendrick mass defect)"));
    } else {
      componentsList.add(GUIUtils.addButton(this, null, rkmIcon, masterFrame, "TOGGLE_RKM_KMD_Y",
          "Toggle RKM (remainders of Kendrick masses) and KMD (Kendrick mass defect)"));
    }
    componentsList.add(GUIUtils.addLabel(this, null));
    componentsList.add(GUIUtils.addLabel(this, null));
    // xAxis
    if (useCustomXAxis) {

      // add empty row
      addEmptyRow(componentsList);

      // header
      componentsList.add(GUIUtils.addLabel(this, "X-Axis:"));
      componentsList.add(GUIUtils.addLabel(this, null));
      componentsList.add(GUIUtils.addLabel(this, null));

      // labels
      componentsList.add(GUIUtils.addLabel(this, "Shift", null, JLabel.CENTER, null));
      componentsList.add(GUIUtils.addLabel(this, "Charge", null, JLabel.CENTER, null));
      componentsList.add(GUIUtils.addLabel(this, "Divisor", null, JLabel.CENTER, null));

      // arrow up
      componentsList.add(GUIUtils.addButton(this, null, arrowUpIcon, masterFrame, "SHIFT_KMD_UP_X",
          "Shift KMD up"));
      componentsList.add(GUIUtils.addButton(this, null, arrowUpIcon, masterFrame,
          "CHANGE_CHARGE_UP_X", "Increase charge"));
      componentsList.add(GUIUtils.addButton(this, null, arrowUpIcon, masterFrame,
          "CHANGE_DIVISOR_UP_X", "Increase divisor"));

      // arrow down
      componentsList.add(GUIUtils.addButton(this, null, arrowDownIcon, masterFrame,
          "SHIFT_KMD_DOWN_X", "Shift KMD down"));
      componentsList.add(GUIUtils.addButton(this, null, arrowDownIcon, masterFrame,
          "CHANGE_CHARGE_DOWN_X", "Decrease charge"));
      componentsList.add(GUIUtils.addButton(this, null, arrowDownIcon, masterFrame,
          "CHANGE_DIVISOR_DOWN_X", "Decrease divisor"));

      // current
      componentsList.add(GUIUtils.addLabel(this, //
          String.valueOf(shiftFormat.format(xAxisShift)), null, JLabel.CENTER, null));
      componentsList.add(GUIUtils.addLabel(this, //
          String.valueOf(shiftFormat.format(xAxisCharge)), null, JLabel.CENTER, null));
      componentsList.add(GUIUtils.addLabel(this, //
          String.valueOf(shiftFormat.format(xAxisDivisor)), null, JLabel.CENTER, null));

      // use remainders instead of defects check box
      if (useXAxisRKM == false) {
        componentsList.add(GUIUtils.addButton(this, null, kmdIcon, masterFrame, "TOGGLE_RKM_KMD_X",
            "Toggle RKM (remainders of Kendrick masses) and KMD (Kendrick mass defect)"));
      } else {
        componentsList.add(GUIUtils.addButton(this, null, rkmIcon, masterFrame, "TOGGLE_RKM_KMD_X",
            "Toggle RKM (remainders of Kendrick masses) and KMD (Kendrick mass defect)"));
      }
      componentsList.add(GUIUtils.addLabel(this, null));
      componentsList.add(GUIUtils.addLabel(this, null));
    }

    // zAxis
    if (useCustomZAxis) {

      // add empty row
      addEmptyRow(componentsList);

      // header
      componentsList.add(GUIUtils.addLabel(this, "Z-Axis:"));
      componentsList.add(GUIUtils.addLabel(this, null));
      componentsList.add(GUIUtils.addLabel(this, null));

      // labels
      componentsList.add(GUIUtils.addLabel(this, "Shift", null, JLabel.CENTER, null));
      componentsList.add(GUIUtils.addLabel(this, "Charge", null, JLabel.CENTER, null));
      componentsList.add(GUIUtils.addLabel(this, "Divisor", null, JLabel.CENTER, null));

      // arrow up
      componentsList.add(GUIUtils.addButton(this, null, arrowUpIcon, masterFrame, "SHIFT_KMD_UP_Z",
          "Shift KMD up"));
      componentsList.add(GUIUtils.addButton(this, null, arrowUpIcon, masterFrame,
          "CHANGE_CHARGE_UP_Z", "Increase charge"));
      componentsList.add(GUIUtils.addButton(this, null, arrowUpIcon, masterFrame,
          "CHANGE_DIVISOR_UP_Z", "Increase divisor"));

      // arrow down
      componentsList.add(GUIUtils.addButton(this, null, arrowDownIcon, masterFrame,
          "SHIFT_KMD_DOWN_Z", "Shift KMD down"));
      componentsList.add(GUIUtils.addButton(this, null, arrowDownIcon, masterFrame,
          "CHANGE_CHARGE_DOWN_Z", "Decrease charge"));
      componentsList.add(GUIUtils.addButton(this, null, arrowDownIcon, masterFrame,
          "CHANGE_DIVISOR_DOWN_Z", "Decrease divisor"));

      // current
      componentsList.add(GUIUtils.addLabel(this, //
          String.valueOf(shiftFormat.format(zAxisShift)), null, JLabel.CENTER, null));
      componentsList.add(GUIUtils.addLabel(this, //
          String.valueOf(shiftFormat.format(zAxisCharge)), null, JLabel.CENTER, null));
      componentsList.add(GUIUtils.addLabel(this, //
          String.valueOf(shiftFormat.format(zAxisDivisor)), null, JLabel.CENTER, null));

      // use remainders instead of defects check box
      if (useZAxisRKM == false) {
        componentsList.add(GUIUtils.addButton(this, null, kmdIcon, masterFrame, "TOGGLE_RKM_KMD_Z",
            "Toggle RKM (remainders of Kendrick masses) and KMD (Kendrick mass defect)"));
      } else {
        componentsList.add(GUIUtils.addButton(this, null, rkmIcon, masterFrame, "TOGGLE_RKM_KMD_Z",
            "Toggle RKM (remainders of Kendrick masses) and KMD (Kendrick mass defect)"));
      }
      componentsList.add(GUIUtils.addLabel(this, null));
      componentsList.add(GUIUtils.addLabel(this, null));
    }
    JComponent[] components = new JComponent[componentsList.size()];

    JPanel componentsPanel =
        GUIUtils.makeTablePanel(components.length / 3, 3, componentsList.toArray(components));
    componentsPanel.setBackground(Color.WHITE);
    this.add(componentsPanel);
  }

  private void addEmptyRow(ArrayList<JComponent> componentsList) {
    componentsList.add(GUIUtils.addLabel(this, ""));
    componentsList.add(GUIUtils.addLabel(this, ""));
    componentsList.add(GUIUtils.addLabel(this, ""));
  }
}
