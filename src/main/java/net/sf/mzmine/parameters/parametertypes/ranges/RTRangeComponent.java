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
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesComponent;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

import com.google.common.collect.Range;

public class RTRangeComponent extends DoubleRangeComponent
        implements ActionListener {

    private static final long serialVersionUID = 1L;
    private final JButton setAutoButton;

    public RTRangeComponent() {

        super(MZmineCore.getConfiguration().getRTFormat());

        setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 0));

        add(new JLabel("min."), 3, 0, 1, 1, 1, 0, GridBagConstraints.NONE);

        setAutoButton = new JButton("Auto range");
        setAutoButton.addActionListener(this);
        RawDataFile currentFiles[] = MZmineCore.getProjectManager()
                .getCurrentProject().getDataFiles();
        setAutoButton.setEnabled(currentFiles.length > 0);
        add(setAutoButton, 4, 0, 1, 1, 1, 0, GridBagConstraints.NONE);
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        Object src = event.getSource();

        if (src == setAutoButton) {
            RawDataFile currentFiles[] = MZmineCore.getProjectManager()
                    .getCurrentProject().getDataFiles();

            try {
                ParameterSetupDialog setupDialog = (ParameterSetupDialog) SwingUtilities
                        .getWindowAncestor(this);
                RawDataFilesComponent rdc = (RawDataFilesComponent) setupDialog
                        .getComponentForParameter(new RawDataFilesParameter());

                // If the current setup dialog has no raw data file selector, it
                // is probably in the parent dialog, so let's check it
                if (rdc == null) {
                    setupDialog = (ParameterSetupDialog) setupDialog
                            .getParent();
                    if (setupDialog != null) {
                        rdc = (RawDataFilesComponent) setupDialog
                                .getComponentForParameter(
                                        new RawDataFilesParameter());
                    }
                }
                if (rdc != null)
                    currentFiles = rdc.getValue().getMatchingRawDataFiles();
            } catch (Exception e) {
                e.printStackTrace();
            }

            Range<Double> rtRange = null;
            for (RawDataFile file : currentFiles) {
                Range<Double> fileRange = file.getDataRTRange();
                if (rtRange == null)
                    rtRange = fileRange;
                else
                    rtRange = rtRange.span(fileRange);
            }
            setValue(rtRange);
        }

    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setAutoButton.setEnabled(enabled);
    }
}
