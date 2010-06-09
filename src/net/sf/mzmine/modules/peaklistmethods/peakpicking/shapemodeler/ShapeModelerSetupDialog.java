package net.sf.mzmine.modules.peaklistmethods.peakpicking.shapemodeler;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Constructor;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.deconvolution.ChromatogramTICDataSet;
import net.sf.mzmine.modules.peaklistmethods.deconvolution.PeakPreviewComboRenderer;
import net.sf.mzmine.modules.visualization.tic.PeakDataSet;
import net.sf.mzmine.modules.visualization.tic.TICPlot;
import net.sf.mzmine.modules.visualization.tic.TICToolBar;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

public class ShapeModelerSetupDialog extends ParameterSetupDialog implements
        ActionListener, PropertyChangeListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    // Dialog components
    static final Font comboFont = new Font("SansSerif", Font.PLAIN, 10);
    
    private JPanel pnlPlotXY, pnlVisible, pnlLabelsFields;
    private JComboBox comboPeakList, comboPeak;
    private JCheckBox preview;

    // XYPlot
    private TICToolBar toolBar;
    private TICPlot ticPlot;
    private ChromatogramTICDataSet ticDataset;

    // Peak resolver
    private SimpleParameterSet smParameters;

    /**
     * @param parameters
     * @param massDetectorTypeNumber
     */
	public ShapeModelerSetupDialog(String title, ShapeModelerParameters parameters) {

        super(title, parameters);

        // Parameters of local mass detector to get preview values
        smParameters = parameters;

        // Set a listener in all parameters's fields to add functionality to
        // this dialog
        for (Parameter p : smParameters.getParameters()) {
        	
        	if ((p.getName().equals(ShapeModelerParameters.suffix.getName())) ||
        			(p.getName().equals(ShapeModelerParameters.autoRemove.getName())))
        		continue;

            JComponent field = getComponentForParameter(p);
            field.addPropertyChangeListener("value", this);
            if (field instanceof JCheckBox)
                ((JCheckBox) field).addActionListener(this);
            if (field instanceof JComboBox)
                ((JComboBox) field).addActionListener(this);
        }

        addComponents();

    }

    public void actionPerformed(ActionEvent event) {

        super.actionPerformed(event);

        Object src = event.getSource();

        if (src == comboPeakList) {
            PeakList selectedPeakList = (PeakList) comboPeakList.getSelectedItem();
            PeakListRow rows[] = selectedPeakList.getRows();
            comboPeak.removeActionListener(this);
            comboPeak.removeAllItems();
            for (PeakListRow row : rows){
                comboPeak.addItem(row);
            }
            comboPeak.addActionListener(this);
            comboPeak.setSelectedIndex(0);
            return;
        }

        if (src == preview) {
            if (preview.isSelected()) {
                mainPanel.add(pnlPlotXY, BorderLayout.EAST);
                pnlVisible.add(pnlLabelsFields, BorderLayout.CENTER);
                pack();
                PeakList selected[] = MZmineCore.getDesktop().getSelectedPeakLists();
                if (selected.length > 0)
                    comboPeakList.setSelectedItem(selected[0]);
                else
                    comboPeakList.setSelectedIndex(0);
                setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
                this.setResizable(true);
            } else {
                mainPanel.remove(pnlPlotXY);
                pnlVisible.remove(pnlLabelsFields);
                pack();
                setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
                this.setResizable(false);
            }
            return;
        }

        // Any other event will cause reloading the preview
        if (preview.isSelected()) {
            loadPreviewPeak();
        }

    }

    public void propertyChange(PropertyChangeEvent e) {
        if (preview.isSelected()) {
            loadPreviewPeak();
        }
    }

    private void loadPreviewPeak() {

        PeakListRow previewRow = (PeakListRow) comboPeak.getSelectedItem();
        if (previewRow == null)
            return;
        logger.finest("Loading new preview peak " + previewRow);
        ChromatographicPeak previewPeak = previewRow.getPeaks()[0];

        int dataSetCount = ticPlot.getXYPlot().getDatasetCount();
        for (int index = 0; index < dataSetCount; index++) {
            ticPlot.getXYPlot().setDataset(index, null);
        }
        ticPlot.startDatasetCounter();

        // Load the intensities into array
        RawDataFile dataFile = previewPeak.getDataFile();
        int scanNumbers[] = previewPeak.getScanNumbers();
        double retentionTimes[] = new double[scanNumbers.length];
        for (int i = 0; i < scanNumbers.length; i++)
            retentionTimes[i] = dataFile.getScan(scanNumbers[i]).getRetentionTime();
        double intensities[] = new double[scanNumbers.length];
        for (int i = 0; i < scanNumbers.length; i++) {
        	DataPoint dp = previewPeak.getDataPoint(scanNumbers[i]);
			if (dp != null)
				intensities[i] = dp.getIntensity();
            else
                intensities[i] = 0;
        }

        // Create shape model
        updateParameterSetFromComponents();
        JComponent component = (JComponent) getComponentForParameter(smParameters.getParameter("Shape model"));
        int index = ((JComboBox) component).getSelectedIndex();
        String shapeModelClassName = ShapeModelerParameters.shapeModelerClasses[index];

        component = (JComponent) getComponentForParameter(smParameters.getParameter("Mass resolution"));
        Number value = (Number) ((JFormattedTextField)component).getValue();
        double resolution = value.doubleValue();
        
        try {
            Class shapeModelClass = Class.forName(shapeModelClassName);
            Constructor shapeModelConstruct = shapeModelClass.getConstructors()[0];

            // shapePeakModel(ChromatographicPeak originalDetectedShape, int[] scanNumbers, 
            // double[] intensities, double[] retentionTimes, double resolution)
            ChromatographicPeak shapePeak = (ChromatographicPeak) shapeModelConstruct.newInstance (
                    previewPeak, scanNumbers, intensities, retentionTimes, resolution);
            
            PeakDataSet peakDataSet = new PeakDataSet(shapePeak);
            ticPlot.addPeakDataset(peakDataSet);

            ticDataset = new ChromatogramTICDataSet(previewRow.getPeaks()[0]);
            ticPlot.addTICDataset(ticDataset);

            // Set auto range to axes
            ticPlot.getXYPlot().getDomainAxis().setAutoRange(true);
            ticPlot.getXYPlot().getDomainAxis().setAutoTickUnitSelection(true);
            ticPlot.getXYPlot().getRangeAxis().setAutoRange(true);
            ticPlot.getXYPlot().getRangeAxis().setAutoTickUnitSelection(true);

        } catch (Exception e) {
            String message = "Error trying to make an instance of Peak Builder "
                    + shapeModelClassName;
            MZmineCore.getDesktop().displayErrorMessage(message);
            logger.severe(message);
            e.printStackTrace();
            return;
        }
        

    }

    /**
     * This function add all the additional components for this dialog over the
     * original ParameterSetupDialog.
     * 
     */
    private void addComponents() {

        PeakList peakLists[] = MZmineCore.getCurrentProject().getPeakLists();

        // Elements of pnlpreview
        JPanel pnlpreview = new JPanel(new BorderLayout());

        preview = new JCheckBox(" Show preview of peak building ");
        preview.addActionListener(this);
        preview.setHorizontalAlignment(SwingConstants.CENTER);
        preview.setEnabled(peakLists.length > 0);

        pnlpreview.add(new JSeparator(), BorderLayout.NORTH);
        pnlpreview.add(preview, BorderLayout.CENTER);
        pnlpreview.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);

        JComponent tableComponents[] = new JComponent[4];
        tableComponents[0] =  new JLabel("Peak list");

        comboPeakList = new JComboBox();
        for (PeakList peakList : peakLists) {
            comboPeakList.addItem(peakList);
        }
        comboPeakList.setFont(comboFont);
        comboPeakList.addActionListener(this);
        tableComponents[1] = comboPeakList;

        comboPeak = new JComboBox();
        comboPeak.setFont(comboFont);
        comboPeak.setRenderer(new PeakPreviewComboRenderer());
        
        tableComponents[2] = new JLabel("Peak");

        tableComponents[3] = comboPeak;

        pnlLabelsFields = GUIUtils.makeTablePanel(2, 2, tableComponents);

        // Put all together
        pnlVisible = new JPanel(new BorderLayout());
        pnlVisible.add(pnlpreview, BorderLayout.NORTH);

        // Panel for XYPlot
        pnlPlotXY = new JPanel(new BorderLayout());
        GUIUtils.addMarginAndBorder(pnlPlotXY, 10);
        pnlPlotXY.setBackground(Color.white);

        ticPlot = new TICPlot((ActionListener) this);
        pnlPlotXY.add(ticPlot, BorderLayout.CENTER);

        toolBar = new TICToolBar(ticPlot);
        toolBar.getComponentAtIndex(0).setVisible(false);
        pnlPlotXY.add(toolBar, BorderLayout.EAST);

        componentsPanel.add(pnlVisible, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

    }

}
