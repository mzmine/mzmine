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

package net.sf.mzmine.parameters.parametertypes.ranges;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

import com.google.common.collect.Range;

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.tools.mzrangecalculator.MzRangeFormulaCalculatorModule;
import net.sf.mzmine.modules.tools.mzrangecalculator.MzRangeMassCalculatorModule;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesComponent;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelectionComponent;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;

public class MZRangeComponent extends DoubleRangeComponent
        implements ActionListener {

    private static final long serialVersionUID = 1L;
    private final JButton setAutoButton, fromMassButton, fromFormulaButton;

    public MZRangeComponent() {

        super(MZmineCore.getConfiguration().getMZFormat());

        setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 0));

        setAutoButton = new JButton("Auto range");
        setAutoButton.addActionListener(this);
        RawDataFile currentFiles[] = MZmineCore.getProjectManager()
                .getCurrentProject().getDataFiles();
        setAutoButton.setEnabled(currentFiles.length > 0);
        add(setAutoButton, 3, 0, 1, 1, 1, 0, GridBagConstraints.NONE);

        fromMassButton = new JButton("From mass");
        fromMassButton.addActionListener(this);
        add(fromMassButton, 4, 0, 1, 1, 1, 0, GridBagConstraints.NONE);
        
        fromFormulaButton = new JButton("From formula");
        fromFormulaButton.addActionListener(this);
        add(fromFormulaButton, 5, 0, 1, 1, 1, 0, GridBagConstraints.NONE);

    }

    @Override
    public void actionPerformed(ActionEvent event) {

        Object src = event.getSource();

        if (src == setAutoButton) {
            RawDataFile currentFiles[] = MZmineCore.getProjectManager()
                    .getCurrentProject().getDataFiles();
            ScanSelection scanSelection = new ScanSelection();

            try {
                ParameterSetupDialog setupDialog = (ParameterSetupDialog) SwingUtilities
                        .getWindowAncestor(this);
                RawDataFilesComponent rdc = (RawDataFilesComponent) setupDialog
                        .getComponentForParameter(new RawDataFilesParameter());
                if (rdc != null)
                    currentFiles = rdc.getValue().getMatchingRawDataFiles();
                ScanSelectionComponent ssc = (ScanSelectionComponent) setupDialog
                        .getComponentForParameter(new ScanSelectionParameter());
                if (ssc != null)
                    scanSelection = ssc.getValue();
            } catch (Exception e) {
                e.printStackTrace();
            }

            Range<Double> mzRange = null;
            for (RawDataFile file : currentFiles) {
                Scan scans[] = scanSelection.getMatchingScans(file);
                for (Scan s : scans) {
                    Range<Double> scanRange = s.getDataPointMZRange();
                    if (scanRange == null)
                        continue;
                    if (mzRange == null)
                        mzRange = scanRange;
                    else
                        mzRange = mzRange.span(scanRange);
                }
            }
            if (mzRange != null)
                setValue(mzRange);
        }

        if (src == fromMassButton) {
            Range<Double> mzRange = MzRangeMassCalculatorModule
                    .showRangeCalculationDialog();
            if (mzRange != null)
                setValue(mzRange);
        }
        
        if (src == fromFormulaButton) {
            Range<Double> mzRange = MzRangeFormulaCalculatorModule
                    .showRangeCalculationDialog();
            if (mzRange != null)
                setValue(mzRange);
        }

    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setAutoButton.setEnabled(enabled);
        fromMassButton.setEnabled(enabled);
        fromFormulaButton.setEnabled(enabled);
    }

}
