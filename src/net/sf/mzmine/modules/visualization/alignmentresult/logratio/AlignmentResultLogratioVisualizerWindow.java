package net.sf.mzmine.modules.visualization.alignmentresult.logratio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.sf.mzmine.data.AlignmentResult;


class AlignmentResultLogratioVisualizerWindow extends JInternalFrame implements
		ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private AlignmentResult alignmentResult;
	
	
	
	public AlignmentResultLogratioVisualizerWindow(AlignmentResult alignmentResult) {

		super(alignmentResult.toString(), true, true, true, true);
		
		this.alignmentResult = alignmentResult;
		
		setResizable( true );
		setIconifiable( true );

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

		// Build toolbar
        /*
        AlignmentResultTableVisualizerToolBar toolBar = new AlignmentResultTableVisualizerToolBar(this);
        add(toolBar, BorderLayout.EAST);
        */

		// Build table
        /*
		table = new AlignmentResultTable(this, alignmentResult);
		table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		scrollPane = new JScrollPane(table);
		
		add(scrollPane, BorderLayout.CENTER);
		*/

		pack();
		
	}
	
	public void actionPerformed(ActionEvent event) {
		
		String command = event.getActionCommand();
		
        if (command.equals("MOVE_TO_NEAREST_PEAK")) {
			// TODO
		}		

	}

}
