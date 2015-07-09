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

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ranges.IntRangeParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.GUIUtils;

import com.google.common.collect.Range;

public class ScanSelectionComponent extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private final JButton setButton, clearButton;

    private final JLabel restrictionsList;

    private Range<Integer> scanNumberRange;
    private Range<Double> scanRetentionTimeRange;
    private PolarityType polarity;
    private Integer msLevel;

    public ScanSelectionComponent() {

        BoxLayout layout = new BoxLayout(this, BoxLayout.X_AXIS);
        setLayout(layout);

        restrictionsList = new JLabel();        
        add(restrictionsList);
        updateRestrictionList();
        
        add(Box.createHorizontalStrut(10));
        
        setButton = GUIUtils.addButton(this, "Set filters", null, this);
        clearButton = GUIUtils.addButton(this, "Clear filters", null, this);

    }

    void setValue(ScanSelection newValue) {
        scanNumberRange = newValue.getScanNumberRange();
        scanRetentionTimeRange = newValue.getScanRetentionTimeRange();
        polarity = newValue.getPolarity();
        msLevel = newValue.getMsLevel();

        updateRestrictionList();
    }

    ScanSelection getValue() {
        return new ScanSelection(scanNumberRange, scanRetentionTimeRange,
                polarity, msLevel);
    }

    public void actionPerformed(ActionEvent event) {

        Object src = event.getSource();

        if (src == setButton) {

            SimpleParameterSet paramSet;
            ExitCode exitCode;
            Window parent = (Window) SwingUtilities.getAncestorOfClass(
                    Window.class, this);

            final String polarityTypes[] = { "Any", "+", "-" };
            final IntRangeParameter scanNumParameter = new IntRangeParameter(
                    "Scan number", "Range of included scan numbers", false,
                    scanNumberRange);
            final RTRangeParameter rtParameter = new RTRangeParameter(false);
            if (scanRetentionTimeRange != null)
                rtParameter.setValue(scanRetentionTimeRange);
            final ComboParameter<String> polarityParameter = new ComboParameter<>(
                    "Polarity", "Polarity", polarityTypes);
            if ((polarity == PolarityType.POSITIVE)
                    || (polarity == PolarityType.NEGATIVE))
                polarityParameter.setValue(polarity.toString());
            final IntegerParameter msLevelParameter = new IntegerParameter(
                    "MS level", "MS level", msLevel, false);

            paramSet = new SimpleParameterSet(new Parameter[] {
                    scanNumParameter, rtParameter, polarityParameter,
                    msLevelParameter });
            exitCode = paramSet.showSetupDialog(parent, true);
            if (exitCode == ExitCode.OK) {
                scanNumberRange = paramSet.getParameter(scanNumParameter)
                        .getValue();
                scanRetentionTimeRange = paramSet.getParameter(rtParameter)
                        .getValue();
                switch (paramSet.getParameter(polarityParameter).getValue()) {
                case "+":
                    polarity = PolarityType.POSITIVE;
                    break;
                case "-":
                    polarity = PolarityType.NEGATIVE;
                    break;
                default:
                    polarity = null;
                    break;
                }
                msLevel = paramSet.getParameter(msLevelParameter).getValue();
            }
        }

        if (src == clearButton) {
            scanNumberRange = null;
            scanRetentionTimeRange = null;
            polarity = null;
            msLevel = null;
        }

        updateRestrictionList();

    }

    @Override
    public void setToolTipText(String toolTip) {
        restrictionsList.setToolTipText(toolTip);
    }

    private void updateRestrictionList() {

        if ((scanNumberRange == null) && (scanRetentionTimeRange == null)
                && (polarity == null) && (msLevel == null)) {
            restrictionsList.setText("All");
            return;
        }

        StringBuilder newText = new StringBuilder("<html>");
        if (scanNumberRange != null) {
            newText.append("Scan number: " + scanNumberRange.lowerEndpoint()
                    + " - " + scanNumberRange.upperEndpoint() + "<br>");
        }
        if (scanRetentionTimeRange != null) {
            NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
            newText.append("Retention time: "
                    + rtFormat.format(scanRetentionTimeRange.lowerEndpoint())
                    + " - "
                    + rtFormat.format(scanRetentionTimeRange.upperEndpoint())
                    + " min.<br>");
        }
        if (polarity != null) {
            newText.append("Polarity: " + polarity + "<br>");
        }
        if (msLevel != null) {
            newText.append("MS level: " + msLevel);
        }

        restrictionsList.setText(newText.toString());
    }
}
