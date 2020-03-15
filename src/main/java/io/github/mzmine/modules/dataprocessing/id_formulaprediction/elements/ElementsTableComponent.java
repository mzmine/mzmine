/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_formulaprediction.elements;

import io.github.mzmine.util.GUIUtils;
import io.github.mzmine.util.components.ComponentCellRenderer;
import io.github.mzmine.util.dialogs.PeriodicTableDialog;
import javafx.embed.swing.SwingNode;
import javafx.stage.Stage;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IIsotope;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ElementsTableComponent extends SwingNode implements ActionListener {

  private static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);

  private JTable elementsTable;
  private JButton addElementButton, removeElementButton;
  private ElementsTableModel elementsTableModel;

  public ElementsTableComponent() {

    JPanel mainPanel = new JPanel(new BorderLayout());


    elementsTableModel = new ElementsTableModel();

    elementsTable = new JTable(elementsTableModel);
    elementsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    elementsTable.setRowSelectionAllowed(true);
    elementsTable.setColumnSelectionAllowed(false);
    elementsTable.setDefaultRenderer(Object.class, new ComponentCellRenderer(smallFont));
    elementsTable.getTableHeader().setReorderingAllowed(false);

    elementsTable.getTableHeader().setResizingAllowed(false);
    elementsTable.setPreferredScrollableViewportSize(new Dimension(200, 80));

    JScrollPane elementsScroll = new JScrollPane(elementsTable);
    mainPanel.add(elementsScroll, BorderLayout.CENTER);

    // Add buttons
    JPanel buttonsPanel = new JPanel();
    BoxLayout buttonsPanelLayout = new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS);
    buttonsPanel.setLayout(buttonsPanelLayout);
    addElementButton = GUIUtils.addButton(buttonsPanel, "Add", null, this);
    removeElementButton = GUIUtils.addButton(buttonsPanel, "Remove", null, this);
    mainPanel.add(buttonsPanel, BorderLayout.EAST);

    mainPanel.setPreferredSize(new Dimension(300, 100));

    SwingUtilities.invokeLater(() -> setContent(mainPanel));

  }

  @Override
  public void actionPerformed(ActionEvent event) {

    assert SwingUtilities.isEventDispatchThread();

    Object src = event.getSource();

    if (src == addElementButton) {
      PeriodicTableDialog dialog = new PeriodicTableDialog();
      try {
        dialog.start(new Stage());
      }catch (Exception e){
        System.out.println(e.getMessage());
      }
      IIsotope chosenIsotope = dialog.getSelectedIsotope();
      if (chosenIsotope == null)
        return;
      elementsTableModel.addRow(chosenIsotope, 0, 100);
    }

    if (src == removeElementButton) {
      int selectedRow = elementsTable.getSelectedRow();
      if (selectedRow < 0)
        return;
      elementsTableModel.removeRow(selectedRow);
    }
  }

  public MolecularFormulaRange getElements() {

    MolecularFormulaRange newValue = new MolecularFormulaRange();

    for (int i = 0; i < elementsTableModel.getRowCount(); i++) {

      IIsotope isotope = (IIsotope) elementsTableModel.getValueAt(i, 0);
      int minCount = (Integer) elementsTableModel.getValueAt(i, 1);
      int maxCount = (Integer) elementsTableModel.getValueAt(i, 2);

      newValue.addIsotope(isotope, minCount, maxCount);
    }
    return newValue;
  }

  public void setElements(MolecularFormulaRange elements) {

    if (elements == null)
      return;

    for (IIsotope isotope : elements.isotopes()) {
      int minCount = elements.getIsotopeCountMin(isotope);
      int maxCount = elements.getIsotopeCountMax(isotope);
      elementsTableModel.addRow(isotope, minCount, maxCount);

    }
  }

}
