package net.sf.mzmine.modules.dataanalysis.rtmzplots;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JToolBar;

import net.sf.mzmine.util.GUIUtils;

public class RTMZToolbar extends JToolBar {

	
	static final Icon axesIcon = new ImageIcon("icons/axesicon.png");
	static final Icon colorbarIcon = new ImageIcon("icons/colorbaricon.png");
	
	public RTMZToolbar(RTMZAnalyzerWindow masterFrame) {
        super(JToolBar.VERTICAL);

        setFloatable(false);
        setFocusable(false);
        setMargin(new Insets(5, 5, 5, 5));
        setBackground(Color.white);
      
        GUIUtils.addButton(this, null, axesIcon, masterFrame,
                "SETUP_AXES", "Setup ranges for axes");
	
        addSeparator();
        
        GUIUtils.addButton(this, null, colorbarIcon, masterFrame,
                "SETUP_COLORS", "Setup color palette");
        
        
	}
	
	
}
