package net.sf.mzmine.modules.visualization.oldtwod;

import java.text.Format;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.util.CursorPosition;

public class OldTwoDTitlePanel extends JPanel {
	
	private JLabel labelTitle;
	private OldTwoDVisualizerWindow visualizer;
	
	public OldTwoDTitlePanel(OldTwoDVisualizerWindow visualizer) {
		
		this.visualizer = visualizer;
		
		labelTitle = new JLabel("Testing 2D");
		this.add(labelTitle);
		this.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
	}
	
	public void updateTitle() {
		Desktop desktop = MZmineCore.getDesktop();
		Format mzFormat = MZmineCore.getMZFormat();
		Format rtFormat = MZmineCore.getRTFormat();
	
		String title = "";
		
		CursorPosition cursorPosition = visualizer.getCursorPosition();
		CursorPosition rangeCursorPosition = visualizer.getRangeCursorPosition();
		
		if (cursorPosition!=null)
			title = title.concat("Cursor m/z=" + mzFormat.format(cursorPosition.getMzValue()) + " rt=" + rtFormat.format(cursorPosition.getRetentionTime())); 

		if (rangeCursorPosition!=null) 
			title = title.concat(", range m/z=" + mzFormat.format(rangeCursorPosition.getMzValue()) + " rt=" + rtFormat.format(rangeCursorPosition.getRetentionTime()));

		labelTitle.setText(title);
		
	}	
	
}
