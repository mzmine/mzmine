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

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.util.ExitCode;

public class RawDataFilesComponent extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private final JComboBox<RawDataFileSelectionType> typeCombo;
    private final JButton detailsButton;
    private final JLabel numFilesLabel;
    private RawDataFilesSelection currentValue = new RawDataFilesSelection();

    public RawDataFilesComponent() {

        BoxLayout layout = new BoxLayout(this, BoxLayout.X_AXIS);
        setLayout(layout);

        typeCombo = new JComboBox<>(RawDataFileSelectionType.values());
        typeCombo.addActionListener(this);
        add(typeCombo);

        detailsButton = new JButton("...");
        detailsButton.setEnabled(false);
        detailsButton.addActionListener(this);
        add(detailsButton);

        numFilesLabel = new JLabel();
        add(numFilesLabel);

    }

    void setValue(RawDataFilesSelection newValue) {
        currentValue = newValue.clone();
        RawDataFileSelectionType type = newValue.getSelectionType();
        if (type != null)
            typeCombo.setSelectedItem(type);
        updateNumFiles();
    }

    RawDataFilesSelection getValue() {
        return currentValue;
    }

    public void actionPerformed(ActionEvent event) {

        Object src = event.getSource();

        if (src == detailsButton) {
            RawDataFileSelectionType type = (RawDataFileSelectionType) typeCombo
                    .getSelectedItem();

            if (type == RawDataFileSelectionType.SPECIFIC_FILES) {
                final MultiChoiceParameter<RawDataFile> filesParameter = new MultiChoiceParameter<RawDataFile>(
                        "Select files", "Select files", MZmineCore
                                .getProjectManager().getCurrentProject()
                                .getDataFiles(),
                        currentValue.getSpecificFiles());
                final SimpleParameterSet paramSet = new SimpleParameterSet(
                        new Parameter[] { filesParameter });
                final Window parent = (Window) SwingUtilities
                        .getAncestorOfClass(Window.class, this);
                final ExitCode exitCode = paramSet
                        .showSetupDialog(parent, true);
                if (exitCode == ExitCode.OK) {
                    RawDataFile files[] = paramSet.getParameter(filesParameter)
                            .getValue();
                    currentValue.setSpecificFiles(files);
                }

            }

            if (type == RawDataFileSelectionType.NAME_PATTERN) {
                final StringParameter nameParameter = new StringParameter(
                        "Name pattern",
                        "Set name pattern that may include wildcards (*), e.g. *mouse* matches any name that contains mouse",
                        currentValue.getNamePattern());
                final SimpleParameterSet paramSet = new SimpleParameterSet(
                        new Parameter[] { nameParameter });
                final Window parent = (Window) SwingUtilities
                        .getAncestorOfClass(Window.class, this);
                final ExitCode exitCode = paramSet
                        .showSetupDialog(parent, true);
                if (exitCode == ExitCode.OK) {
                    String namePattern = paramSet.getParameter(nameParameter)
                            .getValue();
                    currentValue.setNamePattern(namePattern);
                }

            }

        }

        if (src == typeCombo) {
            RawDataFileSelectionType type = (RawDataFileSelectionType) typeCombo
                    .getSelectedItem();
            currentValue.setSelectionType(type);
            detailsButton
                    .setEnabled((type == RawDataFileSelectionType.NAME_PATTERN)
                            || (type == RawDataFileSelectionType.SPECIFIC_FILES));
        }

        updateNumFiles();

    }

    @Override
    public void setToolTipText(String toolTip) {
        typeCombo.setToolTipText(toolTip);
    }

    private void updateNumFiles() {
        if (currentValue.getSelectionType() == RawDataFileSelectionType.BATCH_LAST_FILES) {
            numFilesLabel.setText("");
            numFilesLabel.setToolTipText("");
        } else {
            RawDataFile files[] = currentValue.getMatchingRawDataFiles();
            numFilesLabel.setText("(" + files.length + " selected)");
            StringBuilder toolTip = new StringBuilder();
            for (RawDataFile file : files) {
                toolTip.append(file.getName());
                toolTip.append("\n");
            }
            numFilesLabel.setToolTipText(toolTip.toString());
        }
    }
}
