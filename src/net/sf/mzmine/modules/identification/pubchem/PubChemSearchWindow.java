package net.sf.mzmine.modules.identification.pubchem;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.sf.mzmine.data.CompoundIdentity;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.util.components.DragOrderedJList;
import net.sf.mzmine.util.molstructureviewer.MolStructureViewer;

public class PubChemSearchWindow extends JInternalFrame implements TaskListener,
		ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private Desktop desktop;
	private DefaultListModel listIDModel;
	private JButton btnAdd, btnAddAll, btnViewer;
	private PeakListRow peakListRow;
	private DragOrderedJList IDList;

	public PubChemSearchWindow(PeakListRow peakListRow) {

		super(null, true, true, true, true);

		this.peakListRow = peakListRow;
		
		desktop = MZmineCore.getDesktop();

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setBackground(Color.white);

		JPanel pnlLabelsAndList = new JPanel(new BorderLayout());
		pnlLabelsAndList.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

		pnlLabelsAndList.add(new JLabel("List of possible identities"),
				BorderLayout.NORTH);

		listIDModel = new DefaultListModel();
		IDList = new DragOrderedJList(listIDModel);
		IDList.addMouseListener(new MouseListener(){
			public void mouseClicked(MouseEvent event) {
				requestFocus();
				if ((event.getButton() == MouseEvent.BUTTON1)
						&& (event.getClickCount() == 2)) {
					int index = IDList.locationToIndex(event.getPoint());
		            if (IDList.getModel().getSize()>0){
		            	IDList.setSelectedIndex(index);
		            	PubChemSearchWindow.this.actionPerformed(new ActionEvent(this,0,"ADD"));
		            //Object item = IDList.getModel().getElementAt(index);
					//desktop.displayMessage("Test of double click on JList " + ((CompoundIdentity)item).getCompoundName());
		            }
				}
			}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
		});
		JScrollPane listScroller = new JScrollPane(IDList);
		listScroller.setPreferredSize(new Dimension(350, 100));
		listScroller.setAlignmentX(LEFT_ALIGNMENT);
		JPanel listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.PAGE_AXIS));
		listPanel.add(listScroller);
		listPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		pnlLabelsAndList.add(listPanel, BorderLayout.CENTER);

		JPanel pnlButtons = new JPanel();
		btnAdd = new JButton("Add identity");
		btnAdd.addActionListener(this);
		btnAdd.setActionCommand("ADD");
		btnAddAll = new JButton("Add all");
		btnAddAll.addActionListener(this);
		btnAddAll.setActionCommand("ADD_ALL");
		btnViewer = new JButton("View structure");
		btnViewer.addActionListener(this);
		btnViewer.setActionCommand("VIEWER");
		pnlButtons.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		pnlButtons.add(btnAdd);
		pnlButtons.add(btnAddAll);
		pnlButtons.add(btnViewer);

		setLayout(new BorderLayout());
		setSize(500, 200);
		add(pnlLabelsAndList, BorderLayout.CENTER);
		add(pnlButtons, BorderLayout.SOUTH);
		pack();

	}

	public void actionPerformed(ActionEvent e) {
		
		String command = e.getActionCommand();
		
		if (command.equals("ADD")){
			int[] indices = IDList.getSelectedIndices();
			Object item;
			for (int ind: indices){
				item = listIDModel.getElementAt(ind);
				peakListRow.addCompoundIdentity((CompoundIdentity)item);
			}
			dispose();
		}
		
		if (command.equals("ADD_ALL")){
			int length  = listIDModel.size();
			Object item;
			for (int i=0; i< length; i++){
				item = listIDModel.getElementAt(i);
				peakListRow.addCompoundIdentity((CompoundIdentity)item);
			}
			dispose();
		}

		if (command.equals("VIEWER")){
			int[] indices = IDList.getSelectedIndices();
			Object item;
			MolStructureViewer viewer;
			int CID;
			for (int ind: indices){
				item = listIDModel.getElementAt(ind);
				CID = Integer.parseInt(((CompoundIdentity)item).getCompoundID());
				viewer = new MolStructureViewer(CID, ((CompoundIdentity)item).getCompoundName());
				viewer.setVisible(true);
			}
		}

	}

	public void addNewListItem(CompoundIdentity compound) {
		listIDModel.addElement(compound);
	}

	public void taskFinished(Task task) {
        if (task.getStatus() == TaskStatus.ERROR) {
            desktop.displayErrorMessage("Error while updating PubChem search results window: "
                    + task.getErrorMessage());
        }
	}

	public void taskStarted(Task task) {
        // if we have not added this frame before, do it now
        if (getParent() == null)
            desktop.addInternalFrame(this);
	}
}
