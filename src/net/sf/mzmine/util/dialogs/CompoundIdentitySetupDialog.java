package net.sf.mzmine.util.dialogs;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimpleCompoundIdentity;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;

public class CompoundIdentitySetupDialog extends JDialog implements ActionListener{
	
	public static final int TEXTFIELD_COLUMNS = 20;
	
	//TextField
	private JTextField compoundName, compoundFormula, compoundID;
	
	//TextArea
	private JTextArea comments;
	
	// Buttons
	private JButton btnOK, btnCancel;
	
    private PeakListRow peakListRow;
	
	private ExitCode exitCode = ExitCode.UNKNOWN;
	
	// Desktop
	private Desktop desktop = MZmineCore.getDesktop();

	public CompoundIdentitySetupDialog (PeakListRow peakListRow){

		// Make dialog modal
		super(MZmineCore.getDesktop().getMainFrame(), true);
		
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
		
		comments = new JTextArea(5,TEXTFIELD_COLUMNS);
		JScrollPane scrollPane = new JScrollPane(comments);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setSize(300, 200);
		
		JPanel pnlComments = new JPanel(new BorderLayout());
		pnlComments.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
		pnlComments.add(new JLabel("Comments"), BorderLayout.NORTH);
		pnlComments.add(scrollPane, BorderLayout.CENTER);
		
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
		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
	
	}
	
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();

		if (src == btnOK) {
			
			String name = null, id = null, formula = null, note = null;
			name = compoundName.getText();
			formula = compoundFormula.getText();
			id = compoundID.getText();
			note = comments.getText();

			if ((name == null) || (formula == null)){
				String message = "Name or formula not valid";
				desktop.displayErrorMessage(message);
				return;
			}
				
			if ((name.length() == 0) || (formula.length() == 0)){
				String message = "Name or formula not valid";
				desktop.displayErrorMessage(message);
				return;
			}
			
			SimpleCompoundIdentity compound;
			compound = new SimpleCompoundIdentity(id, name, null,
					formula, null, "User defined", note);

			peakListRow.addCompoundIdentity(compound);
			peakListRow.setComment(note);
			
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
