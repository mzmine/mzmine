/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.batchmode;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.components.DragOrderedJList;
import net.sf.mzmine.util.dialogs.ExitCode;

class BatchSetupComponent extends JPanel implements ActionListener {

	private BatchQueue batchQueue;

	private JComboBox methodsCombo;
	private JList currentStepsList;
	private JButton btnAdd, btnConfig, btnRemove;

	public BatchSetupComponent() {

		super(new BorderLayout());

		batchQueue = new BatchQueue();

		currentStepsList = new DragOrderedJList();
		currentStepsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		methodsCombo = new JComboBox();

		MZmineModule allModules[] = MZmineCore.getAllModules();

		for (MZmineModuleCategory category : MZmineModuleCategory.values()) {

			boolean categoryItemAdded = false;
			for (MZmineModule module : allModules) {
				if (module.getClass() == BatchModeModule.class)
					continue;
				if (module instanceof MZmineProcessingModule) {
					MZmineProcessingModule step = (MZmineProcessingModule) module;
					if (step.getModuleCategory() == category) {
						if (!categoryItemAdded) {
							methodsCombo.addItem("--- " + category + " ---");
							categoryItemAdded = true;
						}
						methodsCombo.addItem(step);
					}
				}
			}
		}

		JPanel pnlRight = new JPanel();
		pnlRight.setLayout(new BoxLayout(pnlRight, BoxLayout.Y_AXIS));
		btnConfig = GUIUtils.addButton(pnlRight, "Configure", null, this);
		btnRemove = GUIUtils.addButton(pnlRight, "Remove", null, this);

		JPanel pnlBottom = new JPanel(new BorderLayout());
		btnAdd = GUIUtils.addButton(pnlBottom, "Add", null, this);
		pnlBottom.add(btnAdd, BorderLayout.EAST);
		pnlBottom.add(methodsCombo, BorderLayout.CENTER);

		add(new JScrollPane(currentStepsList), BorderLayout.CENTER);
		add(pnlBottom, BorderLayout.SOUTH);
		add(pnlRight, BorderLayout.EAST);

	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event) {

		Object src = event.getSource();

		if (src == btnAdd) {

			Object selectedItem = methodsCombo.getSelectedItem();

			if (!(selectedItem instanceof MZmineProcessingModule))
				return;

			MZmineProcessingModule selectedMethod = (MZmineProcessingModule) selectedItem;

			ParameterSet methodParams = selectedMethod.getParameterSet();
			ExitCode exitCode = methodParams.showSetupDialog();
			if (exitCode != ExitCode.OK)
				return;

			// clone the parameters
			ParameterSet paramsCopy = methodParams.clone();

			BatchStepWrapper newStep = new BatchStepWrapper(selectedMethod,
					paramsCopy);
			batchQueue.add(newStep);
			currentStepsList.setListData(batchQueue);
		}

		if (src == btnRemove) {

			BatchStepWrapper selected = (BatchStepWrapper) currentStepsList
					.getSelectedValue();
			if (selected == null)
				return;
			batchQueue.remove(selected);
			currentStepsList.setListData(batchQueue);
		}

		if (src == btnConfig) {

			BatchStepWrapper selected = (BatchStepWrapper) currentStepsList
					.getSelectedValue();
			if (selected == null)
				return;
			ParameterSet params = selected.getParameters();
			params.showSetupDialog();
		}

	}

	BatchQueue getValue() {
		return batchQueue;
	}

	void setValue(BatchQueue newValue) {
		this.batchQueue = newValue;
		currentStepsList.setListData(batchQueue);
	}

}