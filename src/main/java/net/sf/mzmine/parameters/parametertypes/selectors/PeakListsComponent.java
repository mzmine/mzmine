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

package net.sf.mzmine.parameters.parametertypes.selectors;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.util.ExitCode;

public class PeakListsComponent extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private final JComboBox<PeakListsSelectionType> typeCombo;
    private final JButton detailsButton;
    private final JLabel numPeakListsLabel;
    private PeakListsSelection currentValue = new PeakListsSelection();

    public PeakListsComponent() {

        super(new BorderLayout());

        setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        numPeakListsLabel = new JLabel();
        add(numPeakListsLabel, BorderLayout.WEST);

        typeCombo = new JComboBox<>(PeakListsSelectionType.values());
        typeCombo.addActionListener(this);
        add(typeCombo, BorderLayout.CENTER);

        detailsButton = new JButton("...");
        detailsButton.setEnabled(false);
        detailsButton.addActionListener(this);
        add(detailsButton, BorderLayout.EAST);

    }

    void setValue(PeakListsSelection newValue) {
        currentValue = newValue.clone();
        PeakListsSelectionType type = newValue.getSelectionType();
        if (type != null)
            typeCombo.setSelectedItem(type);
        updateNumPeakLists();
    }

    PeakListsSelection getValue() {
        return currentValue;
    }

    public void actionPerformed(ActionEvent event) {

        Object src = event.getSource();

        if (src == detailsButton) {
            PeakListsSelectionType type = (PeakListsSelectionType) typeCombo
                    .getSelectedItem();

            if (type == PeakListsSelectionType.SPECIFIC_PEAKLISTS) {
                final MultiChoiceParameter<PeakList> plsParameter = new MultiChoiceParameter<PeakList>(
                        "Select peak lists", "Select peak lists",
                        MZmineCore.getProjectManager().getCurrentProject()
                                .getPeakLists(),
                        currentValue.getSpecificPeakLists());
                final SimpleParameterSet paramSet = new SimpleParameterSet(
                        new Parameter[] { plsParameter });
                final Window parent = (Window) SwingUtilities
                        .getAncestorOfClass(Window.class, this);
                final ExitCode exitCode = paramSet.showSetupDialog(parent,
                        true);
                if (exitCode == ExitCode.OK) {
                    PeakList pls[] = paramSet.getParameter(plsParameter)
                            .getValue();
                    currentValue.setSpecificPeakLists(pls);
                }

            }

            if (type == PeakListsSelectionType.NAME_PATTERN) {
                final StringParameter nameParameter = new StringParameter(
                        "Name pattern",
                        "Set name pattern that may include wildcards (*), e.g. *mouse* matches any name that contains mouse",
                        currentValue.getNamePattern());
                final SimpleParameterSet paramSet = new SimpleParameterSet(
                        new Parameter[] { nameParameter });
                final Window parent = (Window) SwingUtilities
                        .getAncestorOfClass(Window.class, this);
                final ExitCode exitCode = paramSet.showSetupDialog(parent,
                        true);
                if (exitCode == ExitCode.OK) {
                    String namePattern = paramSet.getParameter(nameParameter)
                            .getValue();
                    currentValue.setNamePattern(namePattern);
                }

            }

        }

        if (src == typeCombo) {
            PeakListsSelectionType type = (PeakListsSelectionType) typeCombo
                    .getSelectedItem();
            currentValue.setSelectionType(type);
            detailsButton
                    .setEnabled((type == PeakListsSelectionType.NAME_PATTERN)
                            || (type == PeakListsSelectionType.SPECIFIC_PEAKLISTS));
        }

        updateNumPeakLists();

    }

    @Override
    public void setToolTipText(String toolTip) {
        typeCombo.setToolTipText(toolTip);
    }

    private void updateNumPeakLists() {
        if (currentValue
                .getSelectionType() == PeakListsSelectionType.BATCH_LAST_PEAKLISTS) {
            numPeakListsLabel.setText("");
            numPeakListsLabel.setToolTipText("");
        } else {
            PeakList pls[] = currentValue.getMatchingPeakLists();
            if (pls.length == 1) {
                String plName = pls[0].getName();
                if (plName.length() > 22)
                    plName = plName.substring(0, 20) + "...";
                numPeakListsLabel.setText(plName);
            } else {
                numPeakListsLabel.setText(pls.length + " selected");
            }
            numPeakListsLabel.setToolTipText(currentValue.toString());
        }
    }
}
