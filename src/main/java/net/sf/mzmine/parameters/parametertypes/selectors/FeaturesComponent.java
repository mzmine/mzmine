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
package net.sf.mzmine.parameters.parametertypes.selectors;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import net.sf.mzmine.datamodel.Feature;

public class FeaturesComponent extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;
    public List<Feature> featuresList;
    private JList<Feature> jlist;
    private final JButton addButton;
    private final JButton removeButton;

    private Logger LOG = Logger.getLogger(this.getClass().getName());

    public FeaturesComponent() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        JScrollPane scrollPane = new JScrollPane(jlist);
        scrollPane.setSize(30, 10);
        add(scrollPane, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();

        add(toolBar, BorderLayout.EAST);
        addButton = new JButton("Add");
        addButton.setEnabled(true);
        addButton.addActionListener(this);

        removeButton = new JButton("Remove");
        removeButton.setEnabled(true);
        removeButton.addActionListener(this);
        toolBar.add(addButton);
        toolBar.add(removeButton);
    }

    public void setValue(List<Feature> newValue) {
        featuresList = newValue;
    }

    public List<Feature> getValue() {
        return featuresList;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        Object src = event.getSource();
        if (src == addButton) {
            LOG.finest("Add Button Clicked!");
            FeaturesSelectionDialog featuresSelectionDialog = new FeaturesSelectionDialog();
            featuresSelectionDialog.setVisible(true);
            jlist.setListData(
                    (Feature[]) featuresSelectionDialog.getSelectedFeatures());
        }

        if (src == removeButton) {

        }

    }

}
