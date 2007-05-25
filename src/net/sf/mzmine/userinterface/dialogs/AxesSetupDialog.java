package net.sf.mzmine.userinterface.dialogs;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog.ExitCode;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;

public class AxesSetupDialog extends JDialog implements ActionListener {

	private XYPlot plot;
	private ValueAxis xAxis;
	private ValueAxis yAxis;
	
    public static final int TEXTFIELD_COLUMNS = 8;

    public static enum ExitCode {
        OK, CANCEL
    };

    private ExitCode exitCode = ExitCode.CANCEL;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    // Buttons
    private JButton btnOK, btnApply, btnCancel;

    private JPanel pnlAll, pnlLabels, pnlFields, pnlButtons;
    
    private JFormattedTextField fieldXMin;
    private JFormattedTextField fieldXMax;
    private JFormattedTextField fieldYMin;
    private JFormattedTextField fieldYMax;    
    
    private JCheckBox checkXAutoRange;
    private JCheckBox checkYAutoRange;
    


    /**
     * Constructor
     */
	public AxesSetupDialog(Frame owner, XYPlot plot) {

        // Make dialog modal
        super(owner, true);
        
        this.plot = plot;

        // Panel where everything is collected
        pnlAll = new JPanel(new BorderLayout());
        pnlAll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(pnlAll);

        // panels for labels, text fields and units
        pnlLabels = new JPanel(new GridLayout(0, 1));
        pnlFields = new JPanel(new GridLayout(0, 1));

        pnlFields.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        pnlLabels.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

        xAxis = plot.getDomainAxis();
        yAxis = plot.getRangeAxis();
        
        JLabel lblXAuto = new JLabel("" + xAxis.getLabel() + " auto range"); pnlLabels.add(lblXAuto);
        JLabel lblXMin = new JLabel("" + xAxis.getLabel() + " minimum"); pnlLabels.add(lblXMin);
        JLabel lblXMax = new JLabel("" + xAxis.getLabel() + " maximum"); pnlLabels.add(lblXMax);
        
        pnlLabels.add(new JPanel());
        
        JLabel lblYAuto = new JLabel("" + yAxis.getLabel() + " auto range"); pnlLabels.add(lblYAuto);
        JLabel lblYMin = new JLabel("" + yAxis.getLabel() + " minimum"); pnlLabels.add(lblYMin);
        JLabel lblYMax = new JLabel("" + yAxis.getLabel() + " maximum"); pnlLabels.add(lblYMax);
                    
        NumberFormat defaultFormatter = NumberFormat.getNumberInstance();
        NumberFormat xAxisFormatter = defaultFormatter;
        if (xAxis instanceof NumberAxis) xAxisFormatter = ((NumberAxis)xAxis).getNumberFormatOverride();
        NumberFormat yAxisFormatter = defaultFormatter;
        if (yAxis instanceof NumberAxis) yAxisFormatter = ((NumberAxis)yAxis).getNumberFormatOverride();
        
        
        checkXAutoRange = new JCheckBox();
        checkXAutoRange.addActionListener(this);
        pnlFields.add(checkXAutoRange);
        fieldXMin = new JFormattedTextField(xAxisFormatter);
        pnlFields.add(fieldXMin);
        fieldXMax = new JFormattedTextField(xAxisFormatter); 
        pnlFields.add(fieldXMax);
        
        pnlFields.add(new JPanel());

        checkYAutoRange = new JCheckBox();
        checkYAutoRange.addActionListener(this);
        pnlFields.add(checkYAutoRange);
        fieldYMin = new JFormattedTextField(yAxisFormatter);
        pnlFields.add(fieldYMin);
        
        fieldYMax = new JFormattedTextField(yAxisFormatter); 
        pnlFields.add(fieldYMax);

        getValuesToControls();
        
        // Buttons
        pnlButtons = new JPanel();
        btnOK = new JButton("OK");
        btnOK.addActionListener(this);
        btnApply = new JButton("Apply");
        btnApply.addActionListener(this);
        btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(this);
        pnlButtons.add(btnOK);
        pnlButtons.add(btnApply);
        pnlButtons.add(btnCancel);

        pnlAll.add(pnlLabels, BorderLayout.WEST);
        pnlAll.add(pnlFields, BorderLayout.CENTER);
        pnlAll.add(pnlButtons, BorderLayout.SOUTH);

        pack();
        setTitle("Please set ranges for axes");
        setResizable(false);
        setLocationRelativeTo(owner);

    }

    /**
     * Implementation for ActionListener interface
     */
    public void actionPerformed(ActionEvent ae) {

        Object src = ae.getSource();

        if (src == btnOK) {
        	if (setValuesToPlot()) {
        		exitCode = ExitCode.OK;
                dispose();
        	}
        }
        
        if (src == btnApply) {
        	if (setValuesToPlot()) 
        		getValuesToControls();
        }

        if (src == btnCancel) {
            exitCode = ExitCode.CANCEL;
            dispose();
        }
        
        if ( (src == checkXAutoRange) || (src == checkYAutoRange) )
        	updateAutoRangeAvailability();
        
    }
    
    private void getValuesToControls() {
    	
    	checkXAutoRange.setSelected(xAxis.isAutoRange());
    	fieldXMin.setValue(xAxis.getRange().getLowerBound());
        fieldXMax.setValue(xAxis.getRange().getUpperBound());
        
        checkYAutoRange.setSelected(yAxis.isAutoRange());
        fieldYMin.setValue(yAxis.getRange().getLowerBound());
        fieldYMax.setValue(yAxis.getRange().getUpperBound());
        
        updateAutoRangeAvailability();        
    }
    
    private void updateAutoRangeAvailability() {
        if (checkXAutoRange.isSelected()) {
        	fieldXMax.setEnabled(false);
        	fieldXMin.setEnabled(false);      	
        } else {
        	fieldXMax.setEnabled(true);
        	fieldXMin.setEnabled(true);        	
        }
        
        if (checkYAutoRange.isSelected()) {
        	fieldYMax.setEnabled(false);
        	fieldYMin.setEnabled(false);
        } else {
        	fieldYMax.setEnabled(true);
        	fieldYMin.setEnabled(true);        	
        }        
    	
    }
    
    private boolean setValuesToPlot() {
    	if (checkXAutoRange.isSelected()) {
    		xAxis.setAutoRange(true);
    	} else {
    		
    		double lower = ((Number)fieldXMin.getValue()).doubleValue();
    		double upper = ((Number)fieldXMax.getValue()).doubleValue();
    		if (lower>upper) {
    			displayMessage("Invalid " + xAxis.getLabel() + " range.");
    			return false;
    		}
    		xAxis.setAutoRange(false);
    		xAxis.setRange(lower,upper);
    	}
    	
    	if (checkYAutoRange.isSelected()) {
    		yAxis.setAutoRange(true);
    	} else {
    		double lower = ((Number)fieldYMin.getValue()).doubleValue();
    		double upper = ((Number)fieldYMax.getValue()).doubleValue();
    		if (lower>upper) {
    			displayMessage("Invalid " + yAxis.getLabel() + " range.");
    			return false;
    		}
    		yAxis.setAutoRange(false);
    		yAxis.setRange(lower,upper);
    	}
    	return true;
    }

    private void displayMessage(String msg) {
        try {
            logger.info(msg);
            JOptionPane.showMessageDialog(this, msg, "Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception exce) {
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
