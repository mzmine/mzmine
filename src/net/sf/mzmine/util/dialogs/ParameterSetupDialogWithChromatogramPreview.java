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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.util.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.rawdata.datasetfilters.preview.RawDataFilterVisualizerParameters;
import net.sf.mzmine.modules.visualization.tic.TICDataSet;
import net.sf.mzmine.modules.visualization.tic.TICPlot;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.components.ExtendedCheckBox;

public class ParameterSetupDialogWithChromatogramPreview extends ParameterSetupDialog
		implements ActionListener, PropertyChangeListener {

	private SimpleParameterSet TICParameters;
	private RawDataFile[] selectedFiles;
	private JCheckBox preview;
	private TICPlot ticPlot;
	private JPanel pnlPlotXY,  pnlFileNameScanNumber;
	private JPanel components;
	private Hashtable<RawDataFile, TICDataSet> ticDataSets;

	public ParameterSetupDialogWithChromatogramPreview(String name,
			SimpleParameterSet parameters, String helpFile) {
		super(name, parameters, helpFile);

		TICParameters = new RawDataFilterVisualizerParameters();
		this.ticDataSets = new Hashtable<RawDataFile, TICDataSet>();
		selectedFiles = MZmineCore.getDesktop().getSelectedDataFiles();

		if (selectedFiles.length != 0) {
			TICParameters.setMultipleSelection(RawDataFilterVisualizerParameters.dataFiles,
					selectedFiles);
			TICParameters.setParameterValue(RawDataFilterVisualizerParameters.dataFiles,
					selectedFiles);
			addActionListener(parameters);
		}

		addComponents();
		addActionListener(TICParameters);
	}

	/**
	 * Set a listener in all parameters's fields to add functionality to this dialog
	 *
	 */
	private void addActionListener(SimpleParameterSet parameters) {
		for (Parameter p : parameters.getParameters()) {

			JComponent field = getComponentForParameter(p);
			field.addPropertyChangeListener("value", this);
			if (field instanceof JCheckBox) {
				((JCheckBox) field).addActionListener(this);
			}
			if (field instanceof JComboBox) {
				((JComboBox) field).addActionListener(this);
			}
			if (field instanceof JPanel) {
				Component[] panelComponents = field.getComponents();
				for (Component component : panelComponents) {
					if (component instanceof JTextField) {
						component.addKeyListener(new KeyListener() {

							public void keyTyped(KeyEvent e) {
							}

							public void keyPressed(KeyEvent e) {
								if (e.getKeyCode() == KeyEvent.VK_ENTER && preview.isSelected()) {
									loadPreview();
								}
							}

							public void keyReleased(KeyEvent e) {
							}
						});
					}
				}
			}
			if (field instanceof JScrollPane) {

				Component[] panelComponents = field.getComponents();
				for (Component component : panelComponents) {
					if (component instanceof JViewport) {
						Component[] childComponents = ((JViewport) component).getComponents();
						JPanel panel = (JPanel) childComponents[0];
						for (Component childs : panel.getComponents()) {
							System.out.println("component: " + childs.getClass().getName());

							if (childs instanceof JCheckBox) {
								((JCheckBox) childs).addActionListener(this);
							}
						}
					}
				}
			}

		}
	}

	/**
	 * This function add all the additional components for this dialog over the
	 * original ParameterSetupDialog.
	 *
	 */
	private void addComponents() {

		// Elements of pnlpreview
		JPanel pnlpreview = new JPanel(new BorderLayout());

		preview = new JCheckBox(" Show preview of raw data filter ");
		preview.addActionListener(this);
		preview.setHorizontalAlignment(SwingConstants.CENTER);
		pnlpreview.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);

		pnlpreview.add(new JSeparator(), BorderLayout.NORTH);
		pnlpreview.add(preview, BorderLayout.CENTER);

		components = addDialogComponents();
		pnlFileNameScanNumber = new JPanel(new BorderLayout());
		pnlFileNameScanNumber.add(pnlpreview, BorderLayout.NORTH);
		pnlFileNameScanNumber.add(components, BorderLayout.SOUTH);
		pnlFileNameScanNumber.setVisible(false);

		JPanel pnlVisible = new JPanel(new BorderLayout());
		pnlVisible.add(pnlpreview, BorderLayout.NORTH);

		JPanel tmp = new JPanel();
		tmp.add(pnlFileNameScanNumber);
		pnlVisible.add(tmp, BorderLayout.CENTER);

		// Panel for XYPlot
		pnlPlotXY = new JPanel(new BorderLayout());
		Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		Border two = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		pnlPlotXY.setBorder(BorderFactory.createCompoundBorder(one, two));
		pnlPlotXY.setBackground(Color.white);

		ticPlot = new TICPlot(this);

		// Hide legend for the preview purpose
		ticPlot.getChart().removeLegend();

		pnlPlotXY.add(ticPlot, BorderLayout.CENTER);
		componentsPanel.add(pnlVisible, BorderLayout.CENTER);

		pack();
		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
	}

	private JPanel addDialogComponents() {

		JComponent component[] = new JComponent[TICParameters.getParameters().length * 3];
		int componentCounter = 0;

		// Create labels and components for each parameter
		for (Parameter p : TICParameters.getParameters()) {
			JLabel label = new JLabel(p.getName());
			component[componentCounter++] = label;
			JComponent comp = null;
			if (p.getType() == ParameterType.MULTIPLE_SELECTION) {
				comp = createMultipleSelectionComponent(p);
			} else {
				comp = createComponentForParameter(p);
			}
			comp.setToolTipText(p.getDescription());
			label.setLabelFor(comp);

			parametersAndComponents.put(p, comp);

			component[componentCounter++] = comp;

			String unitStr = "";
			if (p.getUnits() != null) {
				unitStr = p.getUnits();
			}
			component[componentCounter++] = new JLabel(unitStr);
			setComponentValue(p, TICParameters.getParameterValue(p));

		}

		// Panel collecting all labels, fields and units
		JPanel labelsAndFields = GUIUtils.makeTablePanel(TICParameters.getParameters().length, 3, 1, component);

		return labelsAndFields;
	}

	private JComponent createMultipleSelectionComponent(Parameter p) {

		JComponent comp = null;

		JPanel checkBoxesPanel = new JPanel();
		checkBoxesPanel.setBackground(Color.white);
		checkBoxesPanel.setLayout(new BoxLayout(checkBoxesPanel,
				BoxLayout.Y_AXIS));


		int vertSize = 0,
				numCheckBoxes = 0;
		ExtendedCheckBox<Object> ecb = null;
		Object multipleValues[] = TICParameters.getMultipleSelection(p);
		if (multipleValues == null) {
			multipleValues = p.getPossibleValues();
		}
		if (multipleValues == null) {
			multipleValues = new Object[0];
		}

		for (Object genericObject : multipleValues) {

			ecb = new ExtendedCheckBox<Object>(genericObject, false);
			ecb.setAlignmentX(Component.LEFT_ALIGNMENT);
			checkBoxesPanel.add(ecb);

			if (numCheckBoxes < 7) {
				vertSize += (int) ecb.getPreferredSize().getHeight() + 2;
			}

			numCheckBoxes++;
		}

		if (numCheckBoxes < 3) {
			vertSize += 30;
		}

		JScrollPane peakPanelScroll = new JScrollPane(checkBoxesPanel,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		peakPanelScroll.setPreferredSize(new Dimension(0, vertSize));
		comp = peakPanelScroll;

		return comp;

	}

	private void updateParameterValue()throws IllegalArgumentException  {
		for (Parameter p : TICParameters.getParameters()) {
			Object value = getComponentValue(p);
			if (value != null)
				TICParameters.setParameterValue(p, value);
		}
	}

	private void loadPreview() {
		updateParameterValue();

		// Hide legend for the preview purpose
		ticPlot.getChart().removeLegend();

		Object dataFileObjects[] = (Object[]) TICParameters.getParameterValue(RawDataFilterVisualizerParameters.dataFiles);

		RawDataFile selectedDataFiles[] = CollectionUtils.changeArrayType(
				dataFileObjects, RawDataFile.class);

		for (RawDataFile dataFile : getRawDataFiles()) {
			removeRawDataFile(dataFile);
		}

		for (RawDataFile dataFile : selectedDataFiles) {
			Range rtRange = (Range) TICParameters.getParameterValue(RawDataFilterVisualizerParameters.retentionTimeRange);
			Range mzRange = (Range) TICParameters.getParameterValue(RawDataFilterVisualizerParameters.mzRange);
			int level = (Integer) TICParameters.getParameterValue(RawDataFilterVisualizerParameters.msLevel);
			this.addRawDataFile(dataFile, level, mzRange, rtRange);
		}
	}

	public RawDataFile[] getRawDataFiles() {
		return ticDataSets.keySet().toArray(new RawDataFile[0]);
	}

	public void addRawDataFile(RawDataFile newFile, int level, Range mzRange, Range rtRange) {
		int scanNumbers[] = newFile.getScanNumbers(level, rtRange);
		TICDataSet ticDataset = new TICDataSet(newFile, scanNumbers, mzRange, this);

		ticDataSets.put(newFile, ticDataset);
		ticPlot.addTICDataset(ticDataset);

		if (ticDataSets.size() == 1) {
			// when adding first file, set the retention time range
			setRTRange(rtRange);
		}
	}

	/**
	 */
	public void setRTRange(Range rtRange) {
		ticPlot.getXYPlot().getDomainAxis().setRange(rtRange.getMin(),
				rtRange.getMax());
	}

	public void removeRawDataFile(RawDataFile file) {
		TICDataSet dataset = ticDataSets.get(file);
		ticPlot.getXYPlot().setDataset(ticPlot.getXYPlot().indexOf(dataset),
				null);
		ticDataSets.remove(file);
	}

	public void propertyChange(PropertyChangeEvent evt) {		
		if (preview.isSelected()) {		
			loadPreview();
		}
	}

	public void actionPerformed(ActionEvent event) {

		super.actionPerformed(event);

		Object src = event.getSource();

		if (src == preview) {
			if (preview.isSelected()) {
				mainPanel.add(pnlPlotXY, BorderLayout.CENTER);
				loadPreview();
				pnlFileNameScanNumber.setVisible(true);
				pack();
				this.setResizable(true);
				setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
			} else {
				mainPanel.remove(pnlPlotXY);
				pnlFileNameScanNumber.setVisible(false);
				this.setResizable(false);
				pack();
				setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
			}
		}

		if (((src instanceof JCheckBox) && (src != preview)) || ((src instanceof JComboBox))) {
			if (preview.isSelected()) {
				loadPreview();
			}
		}

	}
}
