package net.sf.mzmine.modules.identification.pubchem;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.TreeMap;

import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.data.CompoundIdentity;

public class PubChemResultTableModel extends AbstractTableModel{

	private TreeMap<Integer, PubChemCompound> compounds = new TreeMap<Integer, PubChemCompound>();
	private int row = 0;
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
    	return row;
    }
    
    public int getColumnCount() { 
    	return columnNames.length;
    }
    
    public Object getValueAt(int row, int col) {
    	Object value = null;
    	PubChemCompound comp = compounds.get(row);
    	switch(col){
    	case (0):
    		value = comp.getCompoundID();
    		break;
    	case(1): 
    		value = comp.getCompoundName();
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
    	setValueAt(compound, row, 0);
    }
    
    public void setValueAt(Object value, int row, int col){
    	compounds.put(row, (PubChemCompound) value);
    	fireTableRowsInserted(0,row);
    	this.row++;
    }


}
