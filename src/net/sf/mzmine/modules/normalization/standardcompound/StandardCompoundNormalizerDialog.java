/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.modules.normalization.standardcompound;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.PeakListRowSorterByMZ;
import net.sf.mzmine.util.components.ExtendedCheckBox;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

class StandardCompoundNormalizerDialog extends ParameterSetupDialog {

    private StandardCompoundNormalizerParameters parameters;

    private Vector<PeakListRow> selectedPeaks;

    private Vector<ExtendedCheckBox<PeakListRow>> peakCheckBoxes;

    public StandardCompoundNormalizerDialog(PeakList peakList,
            StandardCompoundNormalizerParameters parameters) {

        // make dialog modal
        super("Standard compound normalizer setup", parameters);

        this.parameters = parameters;

        JPanel peakCheckBoxesPanel = new JPanel();
        peakCheckBoxesPanel.setBackground(Color.white);
        peakCheckBoxesPanel.setLayout(new BoxLayout(peakCheckBoxesPanel,
                BoxLayout.Y_AXIS));

        peakCheckBoxes = new Vector<ExtendedCheckBox<PeakListRow>>();
        int minimumHorizSize = 0;

        // Get all rows and sort them
        PeakListRow rows[] = peakList.getRows();
        Arrays.sort(rows, new PeakListRowSorterByMZ());

        List<PeakListRow> selectedRows;
        if (parameters.getSelectedStandardPeakListRows() != null)
            selectedRows = Arrays.asList(parameters.getSelectedStandardPeakListRows());
        else
            selectedRows = new ArrayList<PeakListRow>(0);
        for (int i = 0; i < rows.length; i++) {
            // Add only fully detected peaks to list of potential standard peaks
            if (rows[i].getNumberOfPeaks() == peakList.getNumberOfRawDataFiles()) {

                ExtendedCheckBox<PeakListRow> ecb = new ExtendedCheckBox<PeakListRow>(
                        rows[i], selectedRows.contains(rows[i]));
                peakCheckBoxes.add(ecb);
                minimumHorizSize = Math.max(minimumHorizSize,
                        ecb.getPreferredWidth());
                peakCheckBoxesPanel.add(ecb);
            }
        }

        int minimumVertSize = new JCheckBox().getHeight();
        if (peakCheckBoxes.size() > 0) {
            minimumVertSize = (int) peakCheckBoxes.get(0).getPreferredSize().getHeight() * 6;
        }

        JScrollPane peakPanelScroll = new JScrollPane(peakCheckBoxesPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        peakPanelScroll.setPreferredSize(new Dimension(minimumHorizSize,
                minimumVertSize));

        JPanel pnlStdSelection = new JPanel();
        pnlStdSelection.setLayout(new BoxLayout(pnlStdSelection,
                BoxLayout.X_AXIS));

        JLabel label = GUIUtils.addLabel(pnlStdSelection, "Standard compounds");
        label.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

        pnlStdSelection.add(peakPanelScroll);

        // TODO: ParameterSetupDialog needs interface to add our own components
        // with labels etc, using pnlAll is dirty
        pnlAll.add(pnlStdSelection, BorderLayout.CENTER);

        pack();

    }

    public void actionPerformed(ActionEvent ae) {

        Object src = ae.getSource();

        if (src == btnOK) {

            selectedPeaks = new Vector<PeakListRow>();

            for (ExtendedCheckBox<PeakListRow> box : peakCheckBoxes) {
                if (box.isSelected())
                    selectedPeaks.add(box.getObject());
            }

            if (selectedPeaks.size() == 0) {
                MZmineCore.getDesktop().displayErrorMessage(
                        "Please select at least one peak");
                return;
            }

            parameters.setSelectedStandardPeakListRows(selectedPeaks.toArray(new PeakListRow[0]));

        }

        super.actionPerformed(ae);

    }

    public PeakListRow[] getSelectedStandardPeakListRows() {
        return selectedPeaks.toArray(new PeakListRow[0]);
    }

}