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
	
	public static enum Column{
		MARKER(0),STATUS(1),FILENAME(2),FILEPATH(3),BUTTON(4);
		private final int value;
		Column(int value){
			this.value=value;
		}
		public int getValue(){
			return this.value;
		}
		public static Column getByIndex(int value){
			switch (value){
			case 0:
				return Column.MARKER;	
			case 1:
				return Column.STATUS;
			case 2:
				return Column.FILENAME;
			case 3:
				return Column.FILEPATH;
			case 4:
				return Column.BUTTON;
			}return null;
		}
		
	}
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
			tableData[i][Column.MARKER.getValue()]=colorCircle;
			tableData[i][Column.STATUS.getValue()]=status;
			tableData[i][Column.FILENAME.getValue()]=file.getName();
			tableData[i][Column.FILEPATH.getValue()]=filePath;
			tableData[i][Column.BUTTON.getValue()]=button;
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
    	switch(Column.getByIndex(col)){
    	
    	case MARKER:
    		ColorCircle circle;
    		if (value.equals("OK")){
    			circle=CIRCLE_GOOD;
    		}else{
    			circle=CIRCLE_BAD;
    		}
    		tableData[row][col]=circle;
    		return;
    	case STATUS:
    		String msg;
    		if (value.equals("OK")){
    			msg=MSG_FOUND;
    		}else{
    			msg=MSG_NOT_FOUND;
    		}
    		tableData[row][col]=msg;
    		return;
    	case FILENAME:
    		tableData[row][col]=value;
    		return;
    	case FILEPATH:
    		tableData[row][col]=value;
    		return;
    	}
    	
    }

    public boolean isCellEditable(int row, int col){
    	switch (Column.getByIndex(col)){
    	case MARKER:
    		return false;
    	case STATUS:
    		return false;
    	case FILENAME:
    		return false;
    	case FILEPATH:
    		return false;
    	case BUTTON:
    		return true;
    		
    	}
    	return false;


			}
	}


