/*
 * Copyright 2006-2020 The MZmine Development Team
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
package io.github.mzmine.parameters.parametertypes.selectors;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.util.GUIUtils;

/**
 * @author akshaj This class represents the component which shows Features in
 *         the parameter setup dialog of Fx3DVisualizer.
 */
public class FeaturesComponent extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;
    public List<FeatureSelection> currentValue = new ArrayList<FeatureSelection>();
    final DefaultListModel<String> model = new DefaultListModel<>();
    private JList<String> jlist = new JList<String>(model);
    private final JButton addButton;
    private final JButton removeButton;
    private JPanel buttonPane;

    private Logger LOG = Logger.getLogger(this.getClass().getName());

    public FeaturesComponent() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        JScrollPane scrollPane = new JScrollPane(jlist);
        scrollPane.setPreferredSize(new Dimension(300, 60));
        add(scrollPane, BorderLayout.CENTER);

        buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.Y_AXIS));
        addButton = GUIUtils.addButton(buttonPane, "Add", null, this);
        removeButton = GUIUtils.addButton(buttonPane, "Remove", null, this);
        this.add(buttonPane, BorderLayout.EAST);

        for (FeatureSelection features : currentValue) {
            model.addElement(features.getFeature().toString());
        }

    }

    public void setValue(List<FeatureSelection> newValue) {
        currentValue = newValue;
        for (FeatureSelection featureSelection : newValue) {
            model.addElement(featureSelection.getFeature().toString());
        }
    }

    public List<FeatureSelection> getValue() {
        return currentValue;
    }

    /*
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        Object src = event.getSource();
        if (src == addButton) {
            currentValue.clear();
            LOG.finest("Add Button Clicked!");
            FeaturesSelectionDialog featuresSelectionDialog = new FeaturesSelectionDialog();
            featuresSelectionDialog.setModal(true);
            featuresSelectionDialog.setVisible(true);
            if (featuresSelectionDialog.getReturnState() == true) {
                jlist.setVisible(true);
                PeakList selectedPeakList = featuresSelectionDialog
                        .getSelectedPeakList();
                RawDataFile selectedRawDataFile = featuresSelectionDialog
                        .getSelectedRawDataFile();
                LOG.finest(
                        "Selected PeakList is:" + selectedPeakList.getName());
                LOG.finest("Selected RawDataFile is:"
                        + selectedRawDataFile.getName());
                for (Feature feature : featuresSelectionDialog
                        .getSelectedFeatures()) {
                    PeakListRow selectedRow = selectedPeakList
                            .getPeakRow(feature);
                    FeatureSelection featureSelection = new FeatureSelection(
                            selectedPeakList, feature, selectedRow,
                            selectedRawDataFile);
                    currentValue.add(featureSelection);
                    model.addElement(feature.toString());
                }
            }
        }
        if (src == removeButton) {
            LOG.finest("Remove Button Clicked!");
            int[] indices = jlist.getSelectedIndices();
            int k = 0;
            for (int i : indices) {
                model.remove(i - k);
                currentValue.remove(i - k);
                k++;
            }
        }
    }

}
