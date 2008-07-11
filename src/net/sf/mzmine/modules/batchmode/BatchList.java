package net.sf.mzmine.modules.batchmode;

import java.awt.event.MouseEvent;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JList;
import javax.swing.JToolTip;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.util.tooltip.MZmineToolTip;

public class BatchList extends JList {
	
	//private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private ParameterSet parameters;
	
	BatchList(Vector<BatchStepWrapper> batchSteps) {
		super(batchSteps);
	}
	
	public JToolTip createToolTip() {
    	  MZmineToolTip tip = new MZmineToolTip();
    	  tip.setComponent(this);
    	  //tip.setParameterSet(parameters);
    	  return tip;
   }

	public String getToolTipText(MouseEvent e) {
		
		int index = locationToIndex(e.getPoint());
		if (index > -1) {
			parameters = ((BatchStepWrapper) getModel().getElementAt(index)).getParameters();
		}
		return "item";
	}
	
	public ParameterSet getParameterSet(){
		return parameters;
	}
	
}
