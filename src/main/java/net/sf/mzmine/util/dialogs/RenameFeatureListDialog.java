/*
 * Copyright 2006-2019 The MZmine 2 Development Team
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

package net.sf.mzmine.util.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import net.sf.mzmine.datamodel.PeakList;

/**
 * Simple dialog to rename a feature list
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class RenameFeatureListDialog extends JDialog {

  private static final long serialVersionUID = 1L;

  private JPanel panel;
  private JLabel nameLabel;
  private JTextField nameTextField;
  private JButton ok;
  private JButton cancel;

  public RenameFeatureListDialog(PeakList peakList) {

    setSize(new Dimension(400, 100));
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setLocationRelativeTo(null);
    setTitle("Rename feature list: " + peakList.getName());
    panel = new JPanel(new BorderLayout());
    FlowLayout layout = new FlowLayout();
    layout.setAlignment(FlowLayout.CENTER);
    JPanel panelTop = new JPanel(layout);
    JPanel panelBottom = new JPanel(layout);

    // label and text field
    nameLabel = new JLabel("Enter new name: ");
    nameTextField = new JTextField();
    nameTextField.setPreferredSize(new Dimension(200, 30));

    // buttons
    ok = new JButton("OK");
    ok.addActionListener(event -> {
      peakList.setName(nameTextField.getText());
      this.dispose();
    });

    cancel = new JButton("Cancel");
    cancel.addActionListener(event -> {
      this.dispose();
    });

    // add components
    panelTop.add(nameLabel);
    panelTop.add(nameTextField);
    panelBottom.add(ok);
    panelBottom.add(cancel);
    panel.add(panelTop, BorderLayout.NORTH);
    panel.add(panelBottom, BorderLayout.SOUTH);
    add(panel);
    setVisible(true);
    validate();
  }
}
