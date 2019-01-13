package net.sf.mzmine.modules.visualization.productionfilter;

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

import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;

import org.jfree.data.general.DatasetChangeEvent;

import com.google.common.collect.Range;

/**
 * Dialog for selection of highlighted precursor m/z range
 */
public class ProductIonFilterSetHighlightDialog extends JDialog implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Logger logger = Logger.getLogger(this.getClass().getName());

	static final int PADDING_SIZE = 5;

	// dialog components
	private JButton btnOK, btnCancel;
	private JFormattedTextField fieldMinMZ, fieldMaxMZ;

	private Desktop desktop;

	private String rangeType;

	private ProductIonFilterPlot plot;

	public ProductIonFilterSetHighlightDialog(ProductIonFilterPlot plot, String command) {

		// Make dialog modal
		super(MZmineCore.getDesktop().getMainWindow(), "", true);

		this.desktop = MZmineCore.getDesktop();
		this.plot = plot;
		this.rangeType = command;

		String title = "Highlight ";
		if (command.equals("HIGHLIGHT_PRECURSOR"))
			title += "precursor m/z range";
		else if (command.equals("HIGHLIGHT_NEUTRALLOSS"))
			title += "neutral loss m/z range";
		setTitle(title);

		GridBagConstraints constraints = new GridBagConstraints();

		// set default layout constraints
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(PADDING_SIZE, PADDING_SIZE, PADDING_SIZE, PADDING_SIZE);

		JComponent comp;
		GridBagLayout layout = new GridBagLayout();

		JPanel components = new JPanel(layout);

		NumberFormat format = NumberFormat.getNumberInstance();

		comp = GUIUtils.addLabel(components, "Minimum m/z");
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

		comp = GUIUtils.addLabel(components, "Maximum m/z");
		constraints.gridx = 0;
		constraints.gridy = 1;
		layout.setConstraints(comp, constraints);

		constraints.weightx = 1;
		fieldMaxMZ = new JFormattedTextField(format);
		constraints.gridx = 1;
		components.add(fieldMaxMZ, constraints);
		constraints.weightx = 0;

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
		setLocationRelativeTo(MZmineCore.getDesktop().getMainWindow());
		setResizable(false);

	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {

		Object src = ae.getSource();

		if (src == btnOK) {

			try {

				if ((fieldMinMZ.getValue() == null) || (fieldMinMZ.getValue() == null)) {
					desktop.displayErrorMessage(this, "Invalid bounds");
					return;
				}

				double mzMin = ((Number) fieldMinMZ.getValue()).doubleValue();
				double mzMax = ((Number) fieldMaxMZ.getValue()).doubleValue();

				Range<Double> range = Range.closed(mzMin, mzMax);
				if (rangeType.equals("HIGHLIGHT_PRECURSOR"))
					plot.setHighlightedPrecursorRange(range);
				else if (rangeType.equals("HIGHLIGHT_NEUTRALLOSS"))
					plot.setHighlightedNeutralLossRange(range);
				logger.info("Updating Neutral loss plot window");

				ProductIonFilterDataSet dataSet = (ProductIonFilterDataSet) plot.getXYPlot().getDataset();
				dataSet.updateOnRangeDataPoints(rangeType);
				plot.getXYPlot().datasetChanged(new DatasetChangeEvent(plot, dataSet));

				dispose();

			} catch (IllegalArgumentException iae) {
				desktop.displayErrorMessage(this, iae.getMessage());
			} catch (Exception e) {
				logger.log(Level.FINE, "Error while setting highlighted range", e);
			}
		}

		if (src == btnCancel) {
			dispose();
		}

	}
}