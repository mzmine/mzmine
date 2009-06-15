package net.sf.mzmine.modules.visualization.infovisualizer;

import java.awt.event.MouseEvent;

import javax.swing.JList;

import net.sf.mzmine.data.PeakListAppliedMethod;

public class AppliedMethodList extends JList {

	private String parameters;

	AppliedMethodList(PeakListAppliedMethod[] methods) {
		super(methods);
	}
	
	public String getToolTipText(MouseEvent e) {
		
		int index = locationToIndex(e.getPoint());
		if (index > -1) {
			parameters = ((PeakListAppliedMethod) getModel().getElementAt(index)).getParameters();
		}
		if (parameters != null){
        String toolTipText = parameters.toString().replace(", ", "\n");
		return toolTipText;
		}
		else
			return null;
	}
	
}
