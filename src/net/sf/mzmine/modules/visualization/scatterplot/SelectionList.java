/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.scatterplot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.table.TableModel;

public class SelectionList extends JList implements ActionListener,
		MouseListener {

	private JMenuItem popupItemPrint;
	private JPopupMenu popupMenu;
	private ScatterPlotSearchPanel master;

	public SelectionList(DefaultListModel listModel) {
		super(listModel);
		popupMenu = new JPopupMenu();
		popupMenu.add(popupItemPrint = new JMenuItem("Print selection"));

		this.addMouseListener(this);
		popupItemPrint.addActionListener(this);
	}

	public void mouseClicked(MouseEvent me) {
		// if right mouse button clicked (or me.isPopupTrigger())
		if (SwingUtilities.isRightMouseButton(me)
				&& !this.isSelectionEmpty()
				&& this.locationToIndex(me.getPoint()) == this
						.getSelectedIndex()) {
			popupMenu.show(this, me.getX(), me.getY());
		}
	}

	public void actionPerformed(ActionEvent e) {

		if (e.getSource() ==  popupItemPrint){
			
            try {
            	/*SelectionReportGenerator reportGenerator = new SelectionReportGenerator();
                
            	TableModel tableModel = getFilledTableModel(this.getSelectedIndex());
            	
                JFreeReport report = reportGenerator.generateReport(tableModel);

                JDialog dial = new PreviewDialog(report);
                dial.pack();
                dial.setLocationRelativeTo(null);
                dial.setVisible(true);*/
            } catch (Exception ex) {
                System.out.println("Could not generate report for printing");
                ex.printStackTrace();
            }
		}
			
	
	
	}
	
    public String getToolTipText(MouseEvent evt) {
    	
    	ToolTipManager.sharedInstance().setInitialDelay(1000);
    	ToolTipManager.sharedInstance().setDismissDelay(5000);
        int index = locationToIndex(evt.getPoint());
        if (this.getModel().getSize()>0){
        Object item = getModel().getElementAt(index);
        return ((ListSelectionItem) item).getTiptext();
        }
        return null;
    }
    
   /* private TableModel getFilledTableModel(int index){
		
    	System.out.println("Number of index = " + index);
    	Object item = getModel().getElementAt(index);
    	
    	String[][] values = ((ListSelectionItem) item).getSearchValues();
		for (int i=0; i<values.length; i++){
	    	System.out.println("Data = " + values[i][0]);
		}
		
    	ScatterPlotPanel panel = (ScatterPlotPanel)master.getMaster();
    	DataFile dataFile = panel.getDataFile();
		Integer[] indexes = ((ListSelectionItem) item).getMatches();
    	SelectionTableModel tableModel = new SelectionTableModel(dataFile, indexes);
    	
    	return tableModel;
    	
    }*/

    public void setMaster(ScatterPlotSearchPanel master){
    	this.master = master;
    }

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

}
