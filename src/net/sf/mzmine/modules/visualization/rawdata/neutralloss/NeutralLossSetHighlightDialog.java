/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.modules.visualization.rawdata.neutralloss;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;

import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.util.GUIUtils;

/**
 * Dialog for selection of highlighted precursor m/z range
 */
public class NeutralLossSetHighlightDialog extends JDialog implements
        ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    static final int PADDING_SIZE = 5;

    // dialog components
    private JButton btnOK, btnCancel;
    private JFormattedTextField fieldMinMZ, fieldMaxMZ;

    private NeutralLossPlot plot;
    private Desktop desktop;

    public NeutralLossSetHighlightDialog(Desktop desktop, NeutralLossPlot plot) {

        // Make dialog modal
        super(desktop.getMainFrame(), "Highlight precursor m/z range", true);
        
        this.desktop = desktop;
        this.plot = plot;

        GridBagConstraints constraints = new GridBagConstraints();

        // set default layout constraints
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(PADDING_SIZE, PADDING_SIZE,
                PADDING_SIZE, PADDING_SIZE);

        JComponent comp;
        GridBagLayout layout = new GridBagLayout();

        JPanel components = new JPanel(layout);

        NumberFormat format = NumberFormat.getNumberInstance();

        comp = GUIUtils.addLabel(components, "Minimum parent m/z");
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        constraints.weightx = 1;
        fieldMinMZ = new JFormattedTextField(format);
        fieldMinMZ.setPreferredSize(new Dimension(50, fieldMinMZ.getPreferredSize().height));
        constraints.gridx = 1;
        components.add(fieldMinMZ, constraints);
        constraints.weightx = 0;

        comp = GUIUtils.addLabel(components, "m/q (Th)");
        constraints.gridx = 2;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components, "Maximum parent m/z");
        constraints.gridx = 0;
        constraints.gridy = 1;
        layout.setConstraints(comp, constraints);

        constraints.weightx = 1;
        fieldMaxMZ = new JFormattedTextField(format);
        constraints.gridx = 1;
        components.add(fieldMaxMZ, constraints);
        constraints.weightx = 0;

        comp = GUIUtils.addLabel(components, "m/q (Th)");
        constraints.gridx = 2;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addSeparator(components, PADDING_SIZE);
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        JPanel buttonsPanel = new JPanel();
        btnOK = GUIUtils.addButton(buttonsPanel, "OK", null, this);
        btnCancel = GUIUtils.addButton(buttonsPanel, "Cancel", null, this);
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        components.add(buttonsPanel, constraints);

        GUIUtils.addMargin(components, PADDING_SIZE);
        add(components);

        // finalize the dialog
        pack();
        setLocationRelativeTo(desktop.getMainFrame());
        setResizable(false);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent ae) {

        Object src = ae.getSource();

        if (src == btnOK) {

            try {

                if ((fieldMinMZ.getValue() == null)
                        || (fieldMinMZ.getValue() == null)) {
                    desktop.displayErrorMessage("Invalid bounds");
                    return;
                }

                double mzMin = ((Number) fieldMinMZ.getValue()).doubleValue();
                double mzMax = ((Number) fieldMaxMZ.getValue()).doubleValue();

                if (mzMax <= mzMin) {
                    desktop.displayErrorMessage("Invalid bounds");
                    return;
                }

                plot.setHighlightedMin(mzMin);
                plot.setHighlightedMax(mzMax);
                plot.repaint();

                dispose();

            } catch (Exception e) {
                logger.log(Level.FINE, "Error while setting highlighted range",
                        e);
                desktop.displayErrorMessage("Invalid input");
            }
        }

        if (src == btnCancel) {
            dispose();
        }

    }
}
