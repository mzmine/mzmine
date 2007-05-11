package net.sf.mzmine.modules.visualization.peaklist.table;

import java.util.Vector;

public class PeakListTableColumnSelection {
	
    public enum PeakListColumnType {
    	/*STDCOMPOUND ("Std", Boolean.class),*/
    	MZ ("M/Z", Double.class),
        RT ("RT", Double.class),
        HEIGHT ("Height", Double.class),
        AREA ("Area", Double.class),
        DURATION ("Duration", Double.class),
        MZDIFF ("M/Z difference", Double.class),
        NORMMZ ("Norm. M/Z", Double.class),
    	NORMRT ("Norm. RT", Double.class),
    	NORMHEIGHT ("Norm. Height", Double.class),
    	NORMAREA ("Norm. Area", Double.class),
    	ISOTOPEPATTERNNUMBER ("Isotope pattern #", Integer.class),
    	ISOTOPEPEAKNUMBER ("Isotope peak #", Integer.class),
    	CHARGE ("Charge", Integer.class);

		private final String columnName;
		private final Class columnClass;
		PeakListColumnType(String columnName, Class columnClass) {
			this.columnName = columnName;
			this.columnClass = columnClass;
		}
		public String getColumnName() { return columnName; }
		public Class getColumnClass() { return columnClass; }
    };
    
    private Vector<PeakListColumnType> selectedColumns;
	
	public PeakListTableColumnSelection() {

		selectedColumns = new Vector<PeakListColumnType>();
		//selectedColumns.add(PeakListColumnType.STDCOMPOUND);
		selectedColumns.add(PeakListColumnType.MZ);
		selectedColumns.add(PeakListColumnType.RT);
		selectedColumns.add(PeakListColumnType.HEIGHT);
		selectedColumns.add(PeakListColumnType.AREA);
		selectedColumns.add(PeakListColumnType.DURATION);
		selectedColumns.add(PeakListColumnType.MZDIFF);
		selectedColumns.add(PeakListColumnType.NORMMZ);
		selectedColumns.add(PeakListColumnType.NORMRT);
		selectedColumns.add(PeakListColumnType.NORMHEIGHT);
		selectedColumns.add(PeakListColumnType.NORMAREA);
		selectedColumns.add(PeakListColumnType.ISOTOPEPATTERNNUMBER);
		selectedColumns.add(PeakListColumnType.ISOTOPEPEAKNUMBER);
		selectedColumns.add(PeakListColumnType.CHARGE);
		
	}

	public int getNumberOfColumns() {
		return selectedColumns.size();
	}

	public PeakListColumnType[] getSelectedColumns() {
		return selectedColumns.toArray(new PeakListColumnType[0]);
	}

	public PeakListColumnType getSelectedColumn(int index) {
		return selectedColumns.get(index);
	}
	
    
}
