package net.sf.mzmine.modules.dataanalysis.cvplot;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

import net.sf.mzmine.util.GUIUtils;

public class CVToolbar extends JToolBar {

	
	static final Icon axesIcon = new ImageIcon("icons/axesicon.png");
	
	private JButton axesButton;
	
	public CVToolbar(CVAnalyzerWindow masterFrame) {
        super(JToolBar.VERTICAL);

        setFloatable(false);
        setFocusable(false);
        setMargin(new Insets(5, 5, 5, 5));
        setBackground(Color.white);
      
        axesButton = GUIUtils.addButton(this, null, axesIcon, masterFrame,
                "SETUP_AXES", "Setup ranges for axes");
	
	}
	
	
}
