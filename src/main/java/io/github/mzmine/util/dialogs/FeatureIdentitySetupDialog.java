/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util.dialogs;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import io.github.mzmine.gui.Desktop;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.ExitCode;

public class FeatureIdentitySetupDialog extends JDialog implements ActionListener {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public static final int TEXTFIELD_COLUMNS = 20;

  // TextField
  private JTextField compoundName, compoundFormula, compoundID;

  // TextArea
  private JTextArea comments;

  // Buttons
  private JButton btnOK, btnCancel;

  private FeatureListRow featureListRow;
  private FeatureIdentity editIdentity;

  private ExitCode exitCode = ExitCode.UNKNOWN;

  // Desktop
  private Desktop desktop = MZmineCore.getDesktop();

  public FeatureIdentitySetupDialog(JFrame parent, FeatureListRow featureListRow) {
    this(parent, featureListRow, null);
  }

  public FeatureIdentitySetupDialog(JFrame parent, FeatureListRow featureListRow,
      FeatureIdentity editIdentity) {

    // Make dialog modal
    super(parent, true);

    this.featureListRow = featureListRow;

    JPanel pnlLabels, pnlFields, pnlButtons, labelsAndFields, pnlAll;

    // panels for labels, text fields and units
    pnlLabels = new JPanel(new GridLayout(0, 1));
    pnlFields = new JPanel(new GridLayout(0, 1));

    pnlFields.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
    pnlLabels.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

    JLabel lblName = new JLabel("Name");
    pnlLabels.add(lblName);
    JLabel lblFormula = new JLabel("Formula");
    pnlLabels.add(lblFormula);
    JLabel lblID = new JLabel("ID");
    pnlLabels.add(lblID);

    compoundName = new JTextField();
    compoundName.setColumns(TEXTFIELD_COLUMNS);
    pnlFields.add(compoundName);
    compoundFormula = new JTextField();
    compoundFormula.setColumns(TEXTFIELD_COLUMNS);
    pnlFields.add(compoundFormula);
    compoundID = new JTextField();
    compoundFormula.setColumns(TEXTFIELD_COLUMNS);
    pnlFields.add(compoundID);

    comments = new JTextArea(5, TEXTFIELD_COLUMNS);
    comments.setText(featureListRow.getComment());
    JScrollPane scrollPane = new JScrollPane(comments);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setSize(300, 200);

    JPanel pnlComments = new JPanel(new BorderLayout());
    pnlComments.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
    pnlComments.add(new JLabel("Comments"), BorderLayout.NORTH);
    pnlComments.add(scrollPane, BorderLayout.CENTER);

    if (editIdentity != null) {
      this.editIdentity = editIdentity;
      String name = editIdentity.getName();
      compoundName.setText(name);
      String formula = editIdentity.getPropertyValue(FeatureIdentity.PROPERTY_FORMULA);
      if (formula != null)
        compoundFormula.setText(formula);
      String id = editIdentity.getPropertyValue(FeatureIdentity.PROPERTY_ID);
      if (id != null)
        compoundID.setText(id);

    }

    // Buttons
    pnlButtons = new JPanel();
    btnOK = new JButton("OK");
    btnOK.addActionListener(this);
    btnCancel = new JButton("Cancel");
    btnCancel.addActionListener(this);
    pnlButtons.add(btnOK);
    pnlButtons.add(btnCancel);

    // Panel collecting all labels, fileds and units
    labelsAndFields = new JPanel(new BorderLayout());
    labelsAndFields.add(pnlLabels, BorderLayout.WEST);
    labelsAndFields.add(pnlFields, BorderLayout.CENTER);

    // Panel where everything is collected
    pnlAll = new JPanel(new BorderLayout());
    pnlAll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    pnlAll.add(labelsAndFields, BorderLayout.NORTH);
    pnlAll.add(pnlComments, BorderLayout.CENTER);
    pnlAll.add(pnlButtons, BorderLayout.SOUTH);

    add(pnlAll);
    setTitle("Compound Identity setup dialog");
    pack();
    setLocationRelativeTo(parent);

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object src = e.getSource();

    if (src == btnOK) {

      String name = null, id = null, formula = null, note = null;
      name = compoundName.getText();
      formula = compoundFormula.getText();
      id = compoundID.getText();
      note = comments.getText();

      if ((name == null) || (name.length() == 0)) {
        String message = "Name not valid";
        desktop.displayErrorMessage(message);
        return;
      }

      SimpleFeatureIdentity compound = new SimpleFeatureIdentity(name, formula, "User defined", id, null);

      if (editIdentity != null)
        featureListRow.removeFeatureIdentity(editIdentity);
      featureListRow.addFeatureIdentity(compound, true);
      featureListRow.setComment(note);

      // Notify the GUI about the change in the project
      // MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(peakListRow, false);

      exitCode = ExitCode.OK;
      dispose();
    }

    if (src == btnCancel) {
      exitCode = ExitCode.CANCEL;
      dispose();
    }
  }

  /**
   * Method for reading exit code
   *
   */
  public ExitCode getExitCode() {
    return exitCode;
  }

}
