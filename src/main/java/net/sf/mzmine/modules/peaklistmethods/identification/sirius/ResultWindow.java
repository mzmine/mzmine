/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.sirius;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import io.github.msdk.datamodel.IonAnnotation;
import io.github.msdk.id.sirius.SiriusIonAnnotation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableRowSorter;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.DBCompound;
import net.sf.mzmine.modules.visualization.molstructure.MolStructureViewer;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.GUIUtils;
import org.openscience.cdk.atomtype.CDKAtomTypeMatcher;

public class ResultWindow extends JFrame implements ActionListener {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private ResultTableModel listElementModel;

  private PeakListRow peakListRow;
  private JTable IDList;
  private Task searchTask;

  public ResultWindow(PeakListRow peakListRow, double searchedMass,
      Task searchTask) {

    super("");

    this.peakListRow = peakListRow;
    this.searchTask = searchTask;

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setBackground(Color.white);

    JPanel pnlLabelsAndList = new JPanel(new BorderLayout());
    pnlLabelsAndList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    pnlLabelsAndList.add(new JLabel("List of possible identities"),
        BorderLayout.NORTH);

    listElementModel = new ResultTableModel();
    IDList = new JTable();
    IDList.setModel(listElementModel);
    IDList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    IDList.getTableHeader().setReorderingAllowed(false);

    TableRowSorter<ResultTableModel> sorter = new TableRowSorter<ResultTableModel>(
        listElementModel);
    IDList.setRowSorter(sorter);

    JScrollPane listScroller = new JScrollPane(IDList);
    listScroller.setPreferredSize(new Dimension(350, 100));
    listScroller.setAlignmentX(LEFT_ALIGNMENT);
    JPanel listPanel = new JPanel();
    listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.PAGE_AXIS));
    listPanel.add(listScroller);
    listPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
    pnlLabelsAndList.add(listPanel, BorderLayout.CENTER);

    JPanel pnlButtons = new JPanel();
    pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.X_AXIS));
    pnlButtons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    GUIUtils.addButton(pnlButtons, "Add identity", null, this, "ADD");
    GUIUtils.addButton(pnlButtons, "Copy SMILES string", null, this, "SMILES");
    GUIUtils.addButton(pnlButtons, "View structure", null, this, "VIEWER");

    setLayout(new BorderLayout());
    setSize(500, 200);
    add(pnlLabelsAndList, BorderLayout.CENTER);
    add(pnlButtons, BorderLayout.SOUTH);
    pack();
  }

  public void actionPerformed(ActionEvent e) {

    String command = e.getActionCommand();

    if (command.equals("ADD")) {

      int index = IDList.getSelectedRow();

      if (index < 0) {
        MZmineCore.getDesktop().displayMessage(this,
            "Select one result to add as compound identity");
        return;
      }
      index = IDList.convertRowIndexToModel(index);
      peakListRow.addPeakIdentity(listElementModel.getCompoundAt(index),
          false);


      // Notify the GUI about the change in the project
      MZmineCore.getProjectManager().getCurrentProject()
          .notifyObjectChanged(peakListRow, false);

      // Repaint the window to reflect the change in the peak list
      MZmineCore.getDesktop().getMainWindow().repaint();

      dispose();
    }

    if (command.equals("SMILES")) {
      int index = IDList.getSelectedRow();

      if (index < 0) {
        MZmineCore.getDesktop().displayMessage(this,
            "Select one result to copy SMILES value");
        return;
      }
      // SMILES column index == 3
      StringSelection stringSelection = new StringSelection((String) listElementModel.getValueAt(index, 3));

      // todo: May be make clipboard static?
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      clipboard.setContents(stringSelection, null);
    }

    if (command.equals("VIEWER")) {

      int index = IDList.getSelectedRow();

      if (index < 0) {
        MZmineCore.getDesktop().displayMessage(this,
            "Select one result to display molecule structure");
        return;
      }
      index = IDList.convertRowIndexToModel(index);

//      DBCompound compound = listElementModel.getCompoundAt(index);
//      URL url2D = compound.get2DStructureURL();
//      URL url3D = compound.get3DStructureURL();
//      String name = compound.getName() + " ("
//          + compound.getPropertyValue(PeakIdentity.PROPERTY_ID) + ")";
//      MolStructureViewer viewer = new MolStructureViewer(name, url2D,
//          url3D);
//      viewer.setVisible(true);

    }
  }

  public void addNewListItem(final SiriusCompound compound) {

    // Update the model in swing thread to avoid exceptions
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        listElementModel.addElement(compound);
      }
    });

  }

  public void addListofItems(final List<IonAnnotation> annotations) {
    for (IonAnnotation ann: annotations) {
      SiriusIonAnnotation annotation = (SiriusIonAnnotation) ann;
      SiriusCompound compound = new SiriusCompound(annotation, annotation.getFingerIdScore());
      addNewListItem(compound);
    }
  }

  public void dispose() {

    // Cancel the search task if it is still running
    TaskStatus searchStatus = searchTask.getStatus();
    if ((searchStatus == TaskStatus.WAITING)
        || (searchStatus == TaskStatus.PROCESSING)) {
      searchTask.cancel();
    }

    super.dispose();

  }

}
