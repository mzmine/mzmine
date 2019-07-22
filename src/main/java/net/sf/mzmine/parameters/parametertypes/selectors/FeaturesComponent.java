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
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;

public class FeaturesComponent extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;
    public List<Feature> featuresList;
    private final JList<Feature> jlist;
    private final JButton addButton;
    private final JButton removeButton;
    private Logger LOG = Logger.getLogger(this.getClass().getName());

    public FeaturesComponent() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        // FeaturesParameter features = new FeaturesParameter();
        // currentValue = features.getValue();
        // jlist.setSize(5, 10);
        jlist = null;
        JScrollPane scrollPane = new JScrollPane(jlist);
        scrollPane.setSize(30, 10);
        add(scrollPane, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();

        // add(Box.createHorizontalStrut(10));
        // addButton = GUIUtils.addButton(this, "Add", null, this);
        // removeButton = GUIUtils.addButton(this, "Remove", null, this);

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

            JFrame frame = new JFrame();
            PeakList allPeakLists[] = MZmineCore.getProjectManager()
                    .getCurrentProject().getPeakLists();
            String[] allPeakListStrings = new String[allPeakLists.length];
            for (int i = 0; i < allPeakLists.length; i++) {
                allPeakListStrings[i] = allPeakLists[i].toString();
            }
            JPanel panel1 = new JPanel();
            frame.add(panel1, BorderLayout.NORTH);
            JPanel panel2 = new JPanel();
            frame.add(panel2, BorderLayout.SOUTH);
            JComboBox<Object> peakListsComboBox = new JComboBox<Object>(
                    allPeakListStrings);
            panel1.add(peakListsComboBox);
            JComboBox<Object> peakListRowComboBox = new JComboBox<Object>();

            peakListsComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    LOG.finest("Peak List Selected!");
                    String str = (String) peakListsComboBox.getSelectedItem();
                    for (int j = 0; j < allPeakLists.length; j++) {
                        if (str == allPeakLists[j].toString()) {
                            RawDataFile datafile = allPeakLists[j]
                                    .getRawDataFile(0);
                            Feature[] features = allPeakLists[j]
                                    .getPeaks(datafile);
                            String[] featuresStrings = new String[features.length];
                            for (int k = 0; k < features.length; k++) {
                                peakListRowComboBox.addItem(featuresStrings[k]);
                            }
                            panel2.add(peakListRowComboBox);
                            LOG.finest("PeakListRowComboBox is Added");
                        }
                    }
                }
            });
            frame.pack();
            Insets insets = frame.getInsets();
            frame.setSize(new Dimension(insets.left + insets.right + 600,
                    insets.top + insets.bottom + 100));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }

        if (src == removeButton) {

        }

    }

}
