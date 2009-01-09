/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.mzmine.main.MZmineCore;

public class NameChangeDialog extends JDialog implements ActionListener {

    public static final int NAMEFIELD_COLUMNS = 20;
    
    private JTextField fieldName;
    private JButton btnOK, btnCancel;
    private NameChangeable component;

    public NameChangeDialog(NameChangeable component) {

        // Make dialog modal
        super(MZmineCore.getDesktop().getMainFrame(), true);

        this.component = component;

        fieldName = new JTextField();
        fieldName.setColumns(NAMEFIELD_COLUMNS);
        fieldName.setText(component.getName());
        JLabel label = new JLabel("Name ");

        // Create a panel for labels and fields
        JPanel pnlLabelsAndFields = new JPanel();
        pnlLabelsAndFields.add(label);
        pnlLabelsAndFields.add(fieldName);

        // Create buttons
        JPanel pnlButtons = new JPanel();
        btnOK = new JButton("OK");
        btnOK.addActionListener(this);
        btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(this);

        pnlButtons.add(btnOK);
        pnlButtons.add(btnCancel);

        // Put everything into a main panel
        JPanel pnlAll = new JPanel(new BorderLayout());
        pnlAll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(pnlAll);

        pnlAll.add(pnlLabelsAndFields, BorderLayout.CENTER);
        pnlAll.add(pnlButtons, BorderLayout.SOUTH);

        pack();

        setTitle("Please set new name");
        setResizable(false);
        setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

    }

    public void actionPerformed(ActionEvent ae) {

        Object src = ae.getSource();

        if (src == btnOK) {
            component.setName(fieldName.getText());
            
            // Repaint the main window to reflect the change of the name
            MZmineCore.getDesktop().getMainFrame().repaint();
        }

        dispose();

    }

}
