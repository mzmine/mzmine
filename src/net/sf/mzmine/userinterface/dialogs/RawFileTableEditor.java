package net.sf.mzmine.userinterface.dialogs;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JButton;
import javax.swing.table.TableCellEditor;
import javax.swing.JFileChooser;
import com.sun.java.ExampleFileFilter;
import net.sf.mzmine.userinterface.components.RawFileSettingTableModel;

public class RawFileTableEditor extends AbstractCellEditor implements
		TableCellEditor, ActionListener {
	private JButton button;
	private File filePath;
	private JTable table;
	private int row;
	private int col;
	
	public RawFileTableEditor() {
		button = new JButton();
		button.setActionCommand("EDIT");
		button.addActionListener(this);

	}

	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int col) {
		filePath = new File((String) table.getValueAt(row, RawFileSettingTableModel.Column.FILEPATH.getValue()));
		this.row=row;
		this.col=col;
		this.table=table;
		return button;
	}

	public Object getCellEditorValue() {
		return filePath;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("EDIT")) {
			final JFileChooser fc = new JFileChooser();
			ExampleFileFilter filter = new ExampleFileFilter();
			String fileName = filePath.getName();
			if (fileName.contains(".")) {
				int index=fileName.split("\\.").length-1;
				String fileExt = fileName.split("\\.")[index];
				filter.addExtension(fileExt);
				fc.setFileFilter(filter);
			}
			int returnVal = fc.showDialog(button, "Select");
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				this.filePath = fc.getSelectedFile();
				this.table.setValueAt(this.filePath,this.row,RawFileSettingTableModel.Column.FILEPATH.getValue());
				this.table.setValueAt(this.filePath.getName(),this.row,RawFileSettingTableModel.Column.FILENAME.getValue());
				this.table.setValueAt("OK",this.row,RawFileSettingTableModel.Column.STATUS.getValue());
				this.table.setValueAt("OK",this.row,RawFileSettingTableModel.Column.MARKER.getValue());
			}
			fireEditingStopped();
		}

	}

}
