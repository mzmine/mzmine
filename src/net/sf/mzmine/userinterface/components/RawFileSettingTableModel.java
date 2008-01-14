package net.sf.mzmine.userinterface.components;

import java.io.File;
import java.util.ArrayList;
import java.awt.Color;
import javax.swing.table.AbstractTableModel;
import javax.swing.JButton;

import net.sf.mzmine.io.RawDataFile;


public class RawFileSettingTableModel extends AbstractTableModel{

	private Object[][] tableData;
	private String columnNames[]={"","Status","File Name","File Path","Select"};
	private static ColorCircle CIRCLE_GOOD=new ColorCircle(new Color(0,255,0));
	private static ColorCircle CIRCLE_BAD=new ColorCircle(new Color(255,0,0));
	private static final String MSG_FOUND="Found";
	private static final String MSG_NOT_FOUND="Not found";
	
	public static final int COL_MARKER=0;
	public static final int COL_STATUS=1;
	public static final int COL_FILENAME=2;
	public static final int COL_FILEPATH=3;
	public static final int COL_BUTTON=4;
	
	public RawFileSettingTableModel(ArrayList <RawDataFile> lostFiles){
		tableData=new Object[lostFiles.size()][columnNames.length];
		int i;	
		for (i=0;i<lostFiles.size();i++){
			File file=lostFiles.get(i).getFilePath();
			String status;
			String filePath;
			String message;
			ColorCircle colorCircle;
			if (file.exists()){
				status=MSG_FOUND;
				filePath=file.toString();
				colorCircle=CIRCLE_GOOD;
				message="Change filePath";
			}else{
				status=MSG_NOT_FOUND;
				filePath="Please select";
				colorCircle=CIRCLE_BAD;
				message="Select filePath";
			}
			JButton button=new JButton(message);
			//button.setActionCommand("SetFile_"+i);
			//button.addActionListener(this);
			tableData[i][COL_MARKER]=colorCircle;
			tableData[i][COL_STATUS]=status;
			tableData[i][COL_FILENAME]=file.getName();
			tableData[i][COL_FILEPATH]=filePath;
			tableData[i][COL_BUTTON]=button;
		}
	}
	public String getColumnName(int col) {
        return columnNames[col].toString();
    }
	public int getColumnCount() {
		return tableData[0].length;
	}

	public int getRowCount() {
		return tableData.length;
	}

	public Object getValueAt(int row, int col) {
		return tableData[row][col];
	}
    public Class<?> getColumnClass(int col) {
    	return getValueAt(0, col).getClass();
    }
    
    public void setValueAt(Object value, int row, int col) {
    	switch(col){
    	
    	case COL_MARKER:
    		ColorCircle circle;
    		if (value.equals("OK")){
    			circle=CIRCLE_GOOD;
    		}else{
    			circle=CIRCLE_BAD;
    		}
    		tableData[row][col]=circle;
    		return;
    	case COL_STATUS:
    		String msg;
    		if (value.equals("OK")){
    			msg=MSG_FOUND;
    		}else{
    			msg=MSG_NOT_FOUND;
    		}
    		tableData[row][col]=msg;
    		return;
    	case COL_FILENAME:
    		tableData[row][col]=value;
    		return;
    	case COL_FILEPATH:
    		tableData[row][col]=value;
    		return;
    	}
    	
    }

    public boolean isCellEditable(int row, int col){
    	switch (col){
    	case COL_MARKER:
    		return false;
    	case COL_STATUS:
    		return false;
    	case COL_FILENAME:
    		return false;
    	case COL_FILEPATH:
    		return false;
    	case COL_BUTTON:
    		return true;
    		
    	}
    	return false;


			}
	}


