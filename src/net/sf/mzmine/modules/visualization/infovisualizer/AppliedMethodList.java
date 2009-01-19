package net.sf.mzmine.modules.visualization.infovisualizer;

import java.awt.event.MouseEvent;

import javax.swing.JList;
import javax.swing.JToolTip;
import javax.swing.ToolTipManager;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakListAppliedMethod;
import net.sf.mzmine.util.tooltip.MZmineToolTip;

public class AppliedMethodList extends JList implements PeakListAppliedMethod {

	private ParameterSet parameters;

	AppliedMethodList(PeakListAppliedMethod[] methods) {
		super(methods);
		ToolTipManager.sharedInstance().setInitialDelay(500);
		ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
	}
	
	public JToolTip createToolTip() {
    	  MZmineToolTip tip = new MZmineToolTip();
    	  tip.setComponent(this);
    	  return tip;
   }

	public String getToolTipText(MouseEvent e) {
		
		int index = locationToIndex(e.getPoint());
		if (index > -1) {
			parameters = ((PeakListAppliedMethod) getModel().getElementAt(index)).getParameterSet();
		}
		return "item";
	}
	
	public ParameterSet getParameterSet(){
		return parameters;
	}

	public String getDescription() {
		return null;
	}
	

}
