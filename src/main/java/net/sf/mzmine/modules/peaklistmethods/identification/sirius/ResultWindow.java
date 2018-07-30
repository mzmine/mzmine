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

import java.util.List;

import javax.annotation.Nonnull;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.sirius.table.db.DBFrame;
import net.sf.mzmine.modules.peaklistmethods.identification.sirius.table.ResultTable;
import net.sf.mzmine.modules.peaklistmethods.identification.sirius.table.ResultTableModel;
import net.sf.mzmine.modules.peaklistmethods.identification.sirius.table.SiriusCompound;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.GUIUtils;

public class ResultWindow extends JFrame implements ActionListener {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private final JButton browse;

  private ResultTableModel listElementModel;
  private PeakListRow peakListRow;
  private ResultTable compoundsTable;
  private Task searchTask;

  public ResultWindow(PeakListRow peakListRow, Task searchTask) {
    super("");

    this.peakListRow = peakListRow;
    this.searchTask = searchTask;

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setBackground(Color.white);

    JPanel pnlLabelsAndList = new JPanel(new BorderLayout());
    pnlLabelsAndList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    pnlLabelsAndList.add(new JLabel("List of possible identities"), BorderLayout.NORTH);


    listElementModel = new ResultTableModel();
    compoundsTable = new ResultTable(listElementModel);
    listElementModel.setTable(compoundsTable);

    JScrollPane listScroller = new JScrollPane(compoundsTable);
    listScroller.setPreferredSize(new Dimension(800, 400));
    listScroller.setAlignmentX(LEFT_ALIGNMENT);
    JPanel listPanel = new JPanel();
    listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.PAGE_AXIS));
    listPanel.add(listScroller);
    listPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
    pnlLabelsAndList.add(listPanel, BorderLayout.CENTER);

    JPanel pnlButtons = new JPanel();
    pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.X_AXIS));
    pnlButtons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    GUIUtils.addButton(pnlButtons, "Add identity", null, this, "ADD");
    GUIUtils.addButton(pnlButtons, "Copy SMILES string", null, this, "COPY_SMILES");
    GUIUtils.addButton(pnlButtons, "Copy Formula string", null, this, "COPY_FORMULA");
    browse = new JButton("Display DB links");
    browse.setActionCommand("DB_LIST");
    browse.addActionListener(this);
    pnlButtons.add(browse);
//    GUIUtils.addButton(pnlButtons, "Display DB links", null, this, "DB_LIST");

    setLayout(new BorderLayout());
    setSize(500, 200);
    add(pnlLabelsAndList, BorderLayout.CENTER);
    add(pnlButtons, BorderLayout.SOUTH);
    pack();
  }

  public void actionPerformed(ActionEvent e) {
    String command = e.getActionCommand();

    if (command.equals("ADD")) {
      int index = compoundsTable.getSelectedRow();

      if (index < 0) {
        MZmineCore.getDesktop().displayMessage(this, "Select one result to add as compound identity");
        return;
      }
      index = compoundsTable.convertRowIndexToModel(index);
      peakListRow.addPeakIdentity(listElementModel.getCompoundAt(index),
          false);

      // Notify the GUI about the change in the project
      MZmineCore.getProjectManager().getCurrentProject()
          .notifyObjectChanged(peakListRow, false);

      // Repaint the window to reflect the change in the peak list
      MZmineCore.getDesktop().getMainWindow().repaint();

      dispose();
    }

    if (command.equals("COPY_SMILES")) {
      int row = compoundsTable.getSelectedRow();

      if (row < 0) {
        MZmineCore.getDesktop().displayMessage(this, "Select one result to copy SMILES value");
        return;
      }
      int realRow = compoundsTable.convertRowIndexToModel(row);

      String smiles = listElementModel.getCompoundAt(realRow).getSMILES();
      copyToClipboard(smiles, "Selected compound does not contain identified SMILES");
    }

    if (command.equals("COPY_FORMULA")) {
      int row = compoundsTable.getSelectedRow();

      if (row < 0) {
        MZmineCore.getDesktop().displayMessage(this, "Select one result to copy FORMULA value");
        return;
      }
      int realRow = compoundsTable.convertRowIndexToModel(row);

      String formula = listElementModel.getCompoundAt(realRow).getStringFormula();
      copyToClipboard(formula, "Formula value is null...");
    }

    if (command.equals("DB_LIST")) {
      int row = compoundsTable.getSelectedRow();

      if (row < 0) {
        MZmineCore.getDesktop().displayMessage(this, "Select one row to display the list DBs");
        return;
      }
      int realRow = compoundsTable.convertRowIndexToModel(row);
      final SiriusCompound compound = listElementModel.getCompoundAt(realRow);

      DBFrame dbFrame = new DBFrame(compound, browse);
      dbFrame.setVisible(true);
    }
  }

  /**
   * Method sets value of clipboard to `content`
   * @param content - Formula or SMILES string
   * @param errorMessage - to print in a message if value of `content` is null
   */
  private void copyToClipboard(String content, String errorMessage) {
    if (content == null) {
      MZmineCore.getDesktop().displayMessage(this, errorMessage);
      return;
    }

    StringSelection stringSelection = new StringSelection(content);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, null);
  }

  /**
   * Update content of a table using swing-thread
   * @param compound
   */
  public void addNewListItem(@Nonnull final SiriusCompound compound) {
    // Update the model in swing thread to avoid exceptions
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        listElementModel.addElement(compound);
        compoundsTable.generateIconImage(compound);
      }
    });
  }

  /**
   * Method adds a new SiriusCompound to a table
   * @param annotations - SiriusIonAnnotation results processed by Sirius/FingerId methods
   */
  public void addListofItems(@Nonnull final List<IonAnnotation> annotations) {
    for (IonAnnotation ann: annotations) {
      SiriusIonAnnotation annotation = (SiriusIonAnnotation) ann;
      SiriusCompound compound = new SiriusCompound(annotation);
      addNewListItem(compound);
    }
  }

  /**
   * Releases the list of subtasks and disposes windows related to it.
   */
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
