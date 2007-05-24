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
import org.jfree.chart.axis.ValueAxis;

public class AxesSetupDialog extends JDialog implements ActionListener {
	
	

	private JFreeChart chart;
	
    public static final int TEXTFIELD_COLUMNS = 8;

    public static enum ExitCode {
        OK, CANCEL
    };

    private ExitCode exitCode = ExitCode.CANCEL;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    // Buttons
    private JButton btnOK, btnCancel;

    private JPanel pnlAll, pnlLabels, pnlFields, pnlUnits, pnlButtons;

    private SimpleParameterSet parameters;

    /**
     * Constructor
     */
	public AxesSetupDialog(Frame owner, String title, JFreeChart chart) {

        // Make dialog modal
        super(owner, true);

        // Panel where everything is collected
        pnlAll = new JPanel(new BorderLayout());
        pnlAll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(pnlAll);

        // panels for labels, text fields and units
        pnlLabels = new JPanel(new GridLayout(0, 1));
        pnlFields = new JPanel(new GridLayout(0, 1));
        pnlUnits = new JPanel(new GridLayout(0, 1));

        pnlFields.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        pnlLabels.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        pnlUnits.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

        ValueAxis xAxis = chart.getXYPlot().getDomainAxis();
        ValueAxis yAxis = chart.getXYPlot().getRangeAxis();
        
        JLabel lblXMin = new JLabel("X-axis range minimum"); pnlLabels.add(lblXMin);
        JLabel lblXMax = new JLabel("X-axis range maximum"); pnlLabels.add(lblXMax);
        JLabel lblYMin = new JLabel("Y-axis range minimum"); pnlLabels.add(lblYMin);      
        JLabel lblYMax = new JLabel("Y-axis range maximum"); pnlLabels.add(lblYMax);
        
        JLabel lblXMinUnit = new JLabel("" + xAxis.getLabel()); pnlUnits.add(lblXMinUnit);
        JLabel lblXMaxUnit = new JLabel("" + xAxis.getLabel()); pnlUnits.add(lblXMaxUnit);
        JLabel lblYMinUnit = new JLabel("" + yAxis.getLabel()); pnlUnits.add(lblYMinUnit);
        JLabel lblYMaxUnit = new JLabel("" + yAxis.getLabel()); pnlUnits.add(lblYMaxUnit);
              
        NumberFormat format = NumberFormat.getNumberInstance();

        JFormattedTextField fieldXMin = new JFormattedTextField(format);
        fieldXMin.setValue(xAxis.getRange().getLowerBound());
        pnlFields.add(fieldXMin);
        
        JFormattedTextField fieldXMax = new JFormattedTextField(format); 
        fieldXMax.setValue(xAxis.getRange().getUpperBound());
        pnlFields.add(fieldXMax);

        JFormattedTextField fieldYMin = new JFormattedTextField(format); 
        fieldYMin.setValue(yAxis.getRange().getLowerBound());
        pnlFields.add(fieldYMin);        
        
        JFormattedTextField fieldYMax = new JFormattedTextField(format); 
        fieldYMax.setValue(yAxis.getRange().getUpperBound());
        pnlFields.add(fieldYMax);        

        
        // Buttons
        pnlButtons = new JPanel();
        btnOK = new JButton("OK");
        btnOK.addActionListener(this);
        btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(this);
        pnlButtons.add(btnOK);
        pnlButtons.add(btnCancel);

        pnlAll.add(pnlLabels, BorderLayout.WEST);
        pnlAll.add(pnlFields, BorderLayout.CENTER);
        pnlAll.add(pnlUnits, BorderLayout.EAST);
        pnlAll.add(pnlButtons, BorderLayout.SOUTH);

        pack();
        setTitle(title);
        setResizable(false);
        setLocationRelativeTo(owner);

    }

    /**
     * Implementation for ActionListener interface
     */
    public void actionPerformed(ActionEvent ae) {

        Object src = ae.getSource();

        if (src == btnOK) {

            exitCode = ExitCode.OK;
            dispose();
        }

        if (src == btnCancel) {
            exitCode = ExitCode.CANCEL;
            dispose();
        }

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
