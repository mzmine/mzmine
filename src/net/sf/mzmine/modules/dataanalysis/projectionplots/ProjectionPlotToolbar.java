package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

import net.sf.mzmine.util.GUIUtils;

public class ProjectionPlotToolbar extends JToolBar {

	
	static final Icon axesIcon = new ImageIcon("icons/axesicon.png");
	
	static final Icon labelsIcon = new ImageIcon("icons/annotationsicon.png");
	
	private JButton axesButton;
	private JButton labelsButton;
	
	public ProjectionPlotToolbar(ProjectionPlotWindow masterFrame) {
        super(JToolBar.VERTICAL);

        setFloatable(false);
        setFocusable(false);
        setMargin(new Insets(5, 5, 5, 5));
        setBackground(Color.white);
      
        axesButton = GUIUtils.addButton(this, null, axesIcon, masterFrame,
                "SETUP_AXES", "Setup ranges for axes");
        
        addSeparator();
        
        labelsButton = GUIUtils.addButton(this, null, labelsIcon, masterFrame,
                "TOGGLE_LABELS", "Toggle sample names");

	}
	
	
}
