/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.modules.visualization.scatterplot;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Scanner;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class CustomSearchDialog extends JDialog implements ActionListener {

	private String[][] searchValues;
	private JButton btnOK;
	private JTextArea textArea;
	private Vector<String> searchValuesConst;

	public CustomSearchDialog(JFrame owner) {
		super(owner, true);
		
		setLayout(new BorderLayout());
		searchValuesConst = new Vector<String>();

		JPanel pnlTxt = new JPanel(new BorderLayout());
		pnlTxt.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		textArea = new JTextArea();

		setTitle("TextFieldDemo");

		textArea.setColumns(20);
		textArea.setLineWrap(true);
		textArea.setRows(20);
		textArea.setWrapStyleWord(true);
		textArea.setEditable(true);
		JScrollPane listScroller = new JScrollPane(textArea);
		pnlTxt.add(listScroller);

		btnOK = new JButton("Add search");
		btnOK.setActionCommand("ADD_SEARCH");
		btnOK.addActionListener(this);

		JPanel pnlButtons = new JPanel(new BorderLayout());
		pnlButtons.setBorder(BorderFactory.createEmptyBorder(40, 20, 20, 20));
		pnlButtons.add(btnOK);

		add(pnlTxt, BorderLayout.NORTH);
		add(pnlButtons, BorderLayout.SOUTH);
		
		pack();
		setTitle("Customized search dialog");
		setResizable(true);
		setLocationRelativeTo(owner);

	}

	public String[][] getSearchValues() {
		return searchValues;
	}

	public void actionPerformed(ActionEvent event) {
		String command = event.getActionCommand();

		if (command.equals("ADD_SEARCH")) {
			BufferedReader reader = new BufferedReader(new StringReader(
					textArea.getText()));
			String str = null;

			try {
				while ((str = reader.readLine()) != null) {
					if (str.length() > 0)
						processLine(str);
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
			
			int lenght = searchValuesConst.size();
			
			searchValues = new String[lenght][1];
			
			for (int i=0; i< lenght; i++){
				searchValues[i][0] = searchValuesConst.get(i);
			}

			dispose();
			return;
		}

	}
	
	protected void processLine(String aLine){

		//use a second Scanner to parse the content of each line 
	    Scanner scanner = new Scanner(aLine);
	    scanner.useDelimiter(",");
	    String str;
	    while ( scanner.hasNext() ){
	    	str = scanner.next();
	    	searchValuesConst.add(str);
	    }

	    scanner.close();
	  }

}

