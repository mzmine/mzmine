package net.sf.mzmine.modules.identification.pubchem;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class PubChemResultTableModel extends AbstractTableModel{

	private Vector<PubChemCompound> compounds = new Vector<PubChemCompound>();
    private static String[] columnNames = {"CID",
        "Common Name",
        "Formula",
        "Mass difference",
        "Isotope pattern score"};

    public static final NumberFormat percentFormat = NumberFormat
	.getPercentInstance();
    public static final DecimalFormat massFormat = new DecimalFormat("##.#####");
	
    public String getColumnName(int col) {
        return columnNames[col].toString();
    }
    
    public int getRowCount() { 
    	return compounds.size();
    }
    
    public int getColumnCount() { 
    	return columnNames.length;
    }
    
    public Object getValueAt(int row, int col) {
    	Object value = null;
    	PubChemCompound comp = compounds.get(row);
    	switch(col){
    	case (0):
    		value = comp.getID();
    		break;
    	case(1): 
    		value = comp.getName();
			break;
    	case(2): 
    		value = comp.getCompoundFormula();
			break;
    	case(3):
    		double mass = Double.parseDouble(comp.getExactMassDifference());
    		value = massFormat.format(mass);
			break;
    	case(4):
    		String text = comp.getIsotopePatternScore();
    		if (text.length() == 0)
    			break;
    		double score = Double.parseDouble(text);
    		value = percentFormat.format(score);
			break;
    	}

    	return value;
    }
    
    public PubChemCompound getElementAt(int row){
    	return compounds.get(row);
    }
    
    public boolean isCellEditable(int row, int col){ 
    	return false;
    }
    
    public void addElement(PubChemCompound compound){
    	compounds.add(compound);
    	fireTableRowsInserted(0,compounds.size()-1);
    }
    
    public void setValueAt(Object value, int row, int col){
    }


}
