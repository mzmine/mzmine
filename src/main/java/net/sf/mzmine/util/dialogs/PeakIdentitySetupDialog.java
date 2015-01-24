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

package net.sf.mzmine.util.dialogs;

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

import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.ExitCode;

public class PeakIdentitySetupDialog extends JDialog implements ActionListener {

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

    private PeakListRow peakListRow;
    private PeakIdentity editIdentity;

    private ExitCode exitCode = ExitCode.UNKNOWN;

    // Desktop
    private Desktop desktop = MZmineCore.getDesktop();

    public PeakIdentitySetupDialog(JFrame parent, PeakListRow peakListRow) {
	this(parent, peakListRow, null);
    }

    public PeakIdentitySetupDialog(JFrame parent, PeakListRow peakListRow,
	    PeakIdentity editIdentity) {

	// Make dialog modal
	super(parent, true);

	this.peakListRow = peakListRow;

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
	comments.setText(peakListRow.getComment());
	JScrollPane scrollPane = new JScrollPane(comments);
	scrollPane
		.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	scrollPane
		.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	scrollPane.setSize(300, 200);

	JPanel pnlComments = new JPanel(new BorderLayout());
	pnlComments.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
	pnlComments.add(new JLabel("Comments"), BorderLayout.NORTH);
	pnlComments.add(scrollPane, BorderLayout.CENTER);

	if (editIdentity != null) {
	    this.editIdentity = editIdentity;
	    String name = editIdentity.getName();
	    compoundName.setText(name);
	    String formula = editIdentity
		    .getPropertyValue(PeakIdentity.PROPERTY_FORMULA);
	    if (formula != null)
		compoundFormula.setText(formula);
	    String id = editIdentity.getPropertyValue(PeakIdentity.PROPERTY_ID);
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
		desktop.displayErrorMessage(this, message);
		return;
	    }

	    SimplePeakIdentity compound = new SimplePeakIdentity(name, formula,
		    "User defined", id, null);

	    if (editIdentity != null)
		peakListRow.removePeakIdentity(editIdentity);
	    peakListRow.addPeakIdentity(compound, true);
	    peakListRow.setComment(note);

	    // Notify the GUI about the change in the project
	    MZmineCore.getProjectManager().getCurrentProject()
		    .notifyObjectChanged(peakListRow, false);

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
