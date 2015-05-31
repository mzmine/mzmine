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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.tic.PeakDataSet;
import net.sf.mzmine.modules.visualization.tic.TICPlot;
import net.sf.mzmine.modules.visualization.tic.TICToolBar;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.R.RSessionWrapper;
import net.sf.mzmine.util.R.RSessionWrapperException;

import org.jfree.data.xy.XYDataset;

/**
 * This class extends ParameterSetupDialog class.
 */
public class PeakResolverSetupDialog extends ParameterSetupDialog {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    // Logger.
    private static final Logger LOG = Logger
	    .getLogger(PeakResolverSetupDialog.class.getName());

    // Combo-box font.
    private static final Font COMBO_FONT = new Font("SansSerif", Font.PLAIN, 10);

    // Maximum peak count.
    private static final int MAX_PEAKS = 30;

    // TIC minimum size.
    private static final Dimension MINIMUM_TIC_DIMENSIONS = new Dimension(400,
	    300);

    // Preferred width of peak combo-box
    private static final int PREFERRED_PEAK_COMBO_WIDTH = 250;

    // Dialog components.
    private JPanel pnlPlotXY;
    private JPanel pnlVisible;
    private JPanel pnlLabelsFields;
    private JComboBox<PeakList> comboPeakList;
    private JComboBox<PeakListRow> comboPeak;
    private JCheckBox preview;

    private TICPlot ticPlot;

    private PeakResolver peakResolver;
    private final ParameterSet parameters;

    /**
     * Create the dialog.
     *
     * @param resolverParameters
     *            resolver parameters.
     * @param resolverClass
     *            resolver class.
     */
    public PeakResolverSetupDialog(Window parent, boolean valueCheckRequired,
	    final ParameterSet resolverParameters,
	    final Class<? extends PeakResolver> resolverClass) {

	super(parent, valueCheckRequired, resolverParameters);

	// Instantiate resolver.
	try {

	    peakResolver = resolverClass.newInstance();
	} catch (Throwable t) {

	    LOG.log(Level.SEVERE, "Peak deconvolution error", t);
	    MZmineCore.getDesktop().displayErrorMessage(this,
		    "Couldn't create peak resolver (" + t.getMessage() + ')');
	}

	parameters = resolverParameters;
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {

	super.actionPerformed(ae);

	final Object src = ae.getSource();

	if (src.equals(comboPeakList)) {

	    // Remove current peaks (suspend listener).
	    comboPeak.removeActionListener(this);
	    comboPeak.removeAllItems();

	    // Add peaks to menu.
	    for (final PeakListRow peak : ((PeakList) comboPeakList
		    .getSelectedItem()).getRows()) {

		comboPeak.addItem(peak);
	    }

	    // Resume listener.
	    comboPeak.addActionListener(this);

	    // Select first item.
	    if (comboPeak.getItemCount() > 0
		    && comboPeak.getSelectedIndex() != -1) {

		comboPeak.setSelectedIndex(0);
	    }
	} else if (src.equals(preview)) {

	    if (preview.isSelected()) {

		// Set the height of the preview to 200 cells, so it will span
		// the whole vertical length of the dialog (buttons are at row
		// no 100). Also, we set the weight to 10, so the preview
		// component will consume most of the extra available space.
		mainPanel.add(pnlPlotXY, 3, 0, 1, 200, 10, 10,
			GridBagConstraints.BOTH);
		pnlVisible.add(pnlLabelsFields, BorderLayout.CENTER);
		updateMinimumSize();
		pack();

		// Set selections.
		final PeakList[] selected = MZmineCore.getDesktop()
			.getSelectedPeakLists();
		if (selected.length > 0) {

		    comboPeakList.setSelectedItem(selected[0]);
		} else {

		    comboPeakList.setSelectedIndex(0);
		}
		setLocationRelativeTo(MZmineCore.getDesktop().getMainWindow());
	    } else {

		mainPanel.remove(pnlPlotXY);
		pnlVisible.remove(pnlLabelsFields);
		updateBounds();
	    }
	}
    }

    @Override
    public void parametersChanged() {

	if (preview != null && preview.isSelected()) {

	    final PeakListRow previewRow = (PeakListRow) comboPeak
		    .getSelectedItem();
	    if (previewRow != null) {

		LOG.finest("Loading new preview peak " + previewRow);

		ticPlot.removeAllTICDataSets();
		ticPlot.addTICDataset(new ChromatogramTICDataSet(previewRow
			.getPeaks()[0]));

		// Auto-range to axes.
		ticPlot.getXYPlot().getDomainAxis().setAutoRange(true);
		ticPlot.getXYPlot().getDomainAxis()
			.setAutoTickUnitSelection(true);
		ticPlot.getXYPlot().getRangeAxis().setAutoRange(true);
		ticPlot.getXYPlot().getRangeAxis()
			.setAutoTickUnitSelection(true);

		updateParameterSetFromComponents();

		// If there is some illegal value, do not load the preview but
		// just exit.
		if (parameterSet.checkParameterValues(new ArrayList<String>(0))) {

		    // Load the intensities and RTs into array.
		    final Feature previewPeak = previewRow.getPeaks()[0];
		    final RawDataFile dataFile = previewPeak.getDataFile();
		    final int[] scanNumbers = dataFile.getScanNumbers(1);
		    final int scanCount = scanNumbers.length;
		    final double[] retentionTimes = new double[scanCount];
		    final double[] intensities = new double[scanCount];
		    for (int i = 0; i < scanCount; i++) {

			final int scanNumber = scanNumbers[i];
			final DataPoint dp = previewPeak
				.getDataPoint(scanNumber);
			intensities[i] = dp != null ? dp.getIntensity() : 0.0;
			retentionTimes[i] = dataFile.getScan(scanNumber)
				.getRetentionTime();
		    }

		    // Resolve peaks.
		    Feature[] resolvedPeaks = {};
			RSessionWrapper rSession;
		    try {

			if (peakResolver.getRequiresR()) {
				// Check R availability, by trying to open the connection.
				String[] reqPackages = peakResolver.getRequiredRPackages();
				String[] reqPackagesVersions = peakResolver.getRequiredRPackagesVersions();
				String callerFeatureName = peakResolver.getName();
				rSession = new RSessionWrapper(callerFeatureName, /*this.rEngineType,*/ reqPackages, reqPackagesVersions);
				rSession.open();
			} else {
				rSession = null;
			}

			resolvedPeaks = peakResolver.resolvePeaks(previewPeak,
				scanNumbers, retentionTimes, intensities,
								parameters, rSession);					
						
						// Turn off R instance.
						if (rSession != null) rSession.close(false);

						
					} catch (RSessionWrapperException e) {
						
						throw new IllegalStateException(e.getMessage());
		    } catch (Throwable t) {

			LOG.log(Level.SEVERE, "Peak deconvolution error", t);
			MZmineCore.getDesktop().displayErrorMessage(this,
				t.getMessage());
		    }

		    // Add resolved peaks to TIC plot.
		    final int peakCount = Math.min(MAX_PEAKS,
			    resolvedPeaks.length);
		    for (int i = 0; i < peakCount; i++) {

			final XYDataset peakDataSet = new PeakDataSet(
				resolvedPeaks[i]);
			ticPlot.addPeakDataset(peakDataSet);
		    }

		    // Check peak count.
		    if (resolvedPeaks.length > MAX_PEAKS) {
			MZmineCore
				.getDesktop()
				.displayMessage(this,
					"Too many peaks detected, please adjust parameter values");

		    }
		}
	    }
	}
    }

    /**
     * This function add all the additional components for this dialog over the
     * original ParameterSetupDialog.
     */
    @Override
    protected void addDialogComponents() {

	super.addDialogComponents();

	final PeakList[] peakLists = MZmineCore.getProjectManager()
		.getCurrentProject().getPeakLists();

	// Elements of panel.
	preview = new JCheckBox("Show preview");
	preview.addActionListener(this);
	preview.setHorizontalAlignment(SwingConstants.CENTER);
	preview.setEnabled(peakLists.length > 0);

	// Preview panel.
	final JPanel previewPanel = new JPanel(new BorderLayout());
	previewPanel.add(new JSeparator(), BorderLayout.NORTH);
	previewPanel.add(preview, BorderLayout.CENTER);
	previewPanel.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);

	// Peak list combo-box.
	comboPeakList = new JComboBox<PeakList>();
	comboPeakList.setFont(COMBO_FONT);
	for (final PeakList peakList : peakLists) {
	    if (peakList.getNumberOfRawDataFiles() == 1) {
		comboPeakList.addItem(peakList);
	    }
	}
	comboPeakList.addActionListener(this);

	// Peaks combo box.
	comboPeak = new JComboBox<PeakListRow>();
	comboPeak.setFont(COMBO_FONT);
	comboPeak.setRenderer(new PeakPreviewComboRenderer());
	comboPeak.setPreferredSize(new Dimension(PREFERRED_PEAK_COMBO_WIDTH,
		comboPeak.getPreferredSize().height));

	pnlLabelsFields = GUIUtils.makeTablePanel(2, 2, new JComponent[] {
		new JLabel("Peak list"), comboPeakList,
		new JLabel("Chromatogram"), comboPeak });

	// Put all together.
	pnlVisible = new JPanel(new BorderLayout());
	pnlVisible.add(previewPanel, BorderLayout.NORTH);

	// TIC plot.
	ticPlot = new TICPlot(this);
	ticPlot.setMinimumSize(MINIMUM_TIC_DIMENSIONS);

	// Tool bar.
	final TICToolBar toolBar = new TICToolBar(ticPlot);
	toolBar.getComponentAtIndex(0).setVisible(false);

	// Panel for XYPlot.
	pnlPlotXY = new JPanel(new BorderLayout());
	pnlPlotXY.setBackground(Color.white);
	pnlPlotXY.add(ticPlot, BorderLayout.CENTER);
	pnlPlotXY.add(toolBar, BorderLayout.EAST);
	GUIUtils.addMarginAndBorder(pnlPlotXY, 10);

	mainPanel.add(pnlVisible, 0, getNumberOfParameters() + 3, 2, 1, 0, 0,
		GridBagConstraints.HORIZONTAL);

	// Layout and position.
	updateBounds();
    }

    private void updateBounds() {

	updateMinimumSize();
	pack();
	setLocationRelativeTo(MZmineCore.getDesktop().getMainWindow());
    }
}