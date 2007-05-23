package net.sf.mzmine.modules.dataanalysis.logratioplot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.modules.visualization.alignmentresult.AlignmentResultTableVisualizerWindow;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;

public class LogratioPlot implements MZmineModule, 
		 ActionListener, ListSelectionListener {

	private Desktop desktop;
	private JMenuItem myMenuItem;

	private Logger logger = Logger.getLogger(this.getClass().getName());	
	
	public void initModule(MZmineCore core) {

        this.desktop = core.getDesktop();

        myMenuItem = desktop.addMenuItem(MZmineMenu.ANALYSIS, "Alignment result logratio plot", this, null, KeyEvent.VK_L, false, false);
        desktop.addSelectionListener(this);

	}

	public void actionPerformed(ActionEvent e) {
		
        AlignmentResult[] alignmentResults = desktop.getSelectedAlignmentResults();

        for (AlignmentResult alignmentResult : alignmentResults) {

			logger.finest("Showing a new alignment result logratio plot");

            //AlignmentResultTableVisualizerWindow alignmentResultView = new AlignmentResultTableVisualizerWindow(alignmentResult);
            //desktop.addInternalFrame(alignmentResultView);
        }
        
	}

	public void valueChanged(ListSelectionEvent e) {

		AlignmentResult[] alignmentResults = desktop.getSelectedAlignmentResults();
		if (alignmentResults.length>0) myMenuItem.setEnabled(true);

	}

    /**
     * @see net.sf.mzmine.main.MZmineModule#toString()
     */
    public String toString() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getCurrentParameters()
     */
    public ParameterSet getCurrentParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setCurrentParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setCurrentParameters(ParameterSet parameterValues) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setParameters(ParameterSet parameterValues) {
        // TODO Auto-generated method stub
        
    }

}
