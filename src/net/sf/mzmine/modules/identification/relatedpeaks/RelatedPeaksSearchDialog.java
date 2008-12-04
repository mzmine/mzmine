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
package net.sf.mzmine.modules.identification.relatedpeaks;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
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
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.components.ExtendedCheckBox;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

class RelatedPeaksSearchDialog extends ParameterSetupDialog {

    private RelatedPeaksSearchParameters parameters;
    private Vector<ExtendedCheckBox<String>> adductsCheckBoxes;
    private Vector<CommonAdducts> selectedAdducts;
    private JTextField customAdduct;

    public RelatedPeaksSearchDialog(RelatedPeaksSearchParameters parameters) {

        // make dialog modal
        super("Related Peaks Search Setup", parameters);

        this.parameters = parameters;


        JPanel peakCheckBoxesPanel = new JPanel();
        peakCheckBoxesPanel.setBackground(Color.white);
        peakCheckBoxesPanel.setLayout(new GridLayout(CommonAdducts.values().length, 2));

        adductsCheckBoxes = new Vector<ExtendedCheckBox<String>>();
        int minimumHorizSize = 0;

        customAdduct = new JTextField("0.0");
        List<CommonAdducts> selectedRows;
        if (parameters.getSelectedAdducts() != null) {
            selectedRows = Arrays.asList(parameters.getSelectedAdducts());
        } else {
            selectedRows = new ArrayList<CommonAdducts>(0);
        }
        for (CommonAdducts adducts : CommonAdducts.values()) {
            ExtendedCheckBox<String> ecb = new ExtendedCheckBox<String>(
                    adducts.getName(), selectedRows.contains(adducts));
            adductsCheckBoxes.add(ecb);
            minimumHorizSize = Math.max(minimumHorizSize,
                    ecb.getPreferredWidth());
            peakCheckBoxesPanel.add(ecb, BorderLayout.WEST);
            if (adducts.getName().matches("Custom")) {
                peakCheckBoxesPanel.add(customAdduct, BorderLayout.EAST);
            } else {
                peakCheckBoxesPanel.add(new JLabel(String.valueOf(adducts.getMassDifference())), BorderLayout.EAST);
            }
        }

        int minimumVertSize = new JCheckBox().getHeight();
        if (adductsCheckBoxes.size() > 0) {
            minimumVertSize = (int) adductsCheckBoxes.get(0).getPreferredSize().getHeight() * 6;
        }

        JScrollPane peakPanelScroll = new JScrollPane(peakCheckBoxesPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        peakPanelScroll.setPreferredSize(new Dimension(minimumHorizSize,
                minimumVertSize));
        JPanel pnlStdSelection = new JPanel();
        pnlStdSelection.setLayout(new BoxLayout(pnlStdSelection,
                BoxLayout.X_AXIS));

        JLabel label = GUIUtils.addLabel(pnlStdSelection, "Adducts");
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
            selectedAdducts = new Vector<CommonAdducts>();

            for (ExtendedCheckBox<String> box : adductsCheckBoxes) {
                if (box.isSelected()) {
                    for (CommonAdducts adduct : CommonAdducts.values()) {
                        if (box.getObject().compareTo(adduct.getName()) == 0) {
                            selectedAdducts.add(adduct);
                        }
                    }
                }
            }

            parameters.setSelectedAdducts(selectedAdducts.toArray(new CommonAdducts[0]));
            parameters.setCustomMassDifference(Double.valueOf(customAdduct.getText()));
        }

        super.actionPerformed(ae);

    }

    public String[] getSelectedStandardPeakListRows() {
        return selectedAdducts.toArray(new String[0]);
    }
}
