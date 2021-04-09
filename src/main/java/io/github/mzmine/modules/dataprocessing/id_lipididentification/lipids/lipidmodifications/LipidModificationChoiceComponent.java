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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidmodifications;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.controlsfx.control.CheckListView;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.CSVPrinter;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.util.ExitCode;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class LipidModificationChoiceComponent extends BorderPane {

  // Logger.
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final CheckListView<LipidModification> checkList = new CheckListView<LipidModification>();
  private final FlowPane buttonsPane = new FlowPane(Orientation.VERTICAL);
  private final Button addButton = new Button("Add...");
  private final Button importButton = new Button("Import...");
  private final Button exportButton = new Button("Export...");
  private final Button removeButton = new Button("Remove");

  // Filename extension.
  private static final String FILENAME_EXTENSION = "*.csv";

  public LipidModificationChoiceComponent(LipidModification[] choices) {

    ObservableList<LipidModification> choicesList =
        FXCollections.observableArrayList(Arrays.asList(choices));

    checkList.setItems(choicesList);
    setCenter(checkList);
	setMaxHeight(100);
    addButton.setOnAction(e -> {
      final ParameterSet parameters = new AddLipidModificationParameters();
      if (parameters.showSetupDialog(true) != ExitCode.OK)
        return;
      // Create new lipid modification
      LipidModification lipidModification = new LipidModification(
          parameters.getParameter(AddLipidModificationParameters.lipidModification).getValue(),
          parameters.getParameter(AddLipidModificationParameters.lipidModificationLabel)
              .getValue());

      // Add to list of choices (if not already present).
      if (!checkList.getItems().contains(lipidModification)) {
        checkList.getItems().add(lipidModification);
      }
    });

    importButton.setTooltip(new Tooltip("Import lipid modifications from a CSV file"));
    importButton.setOnAction(e -> {
      // Create the chooser if necessary.
      FileChooser chooser = new FileChooser();
      chooser.setTitle("Select lipid modification file");
      chooser.getExtensionFilters()
          .add(new ExtensionFilter("Comma-separated values files", FILENAME_EXTENSION));

      // Select a file.
      final File file = chooser.showOpenDialog(this.getScene().getWindow());
      if (file == null)
        return;

      // Read the CSV file into a string array.
      String[][] csvLines = null;
      try {

        csvLines = CSVParser.parse(new FileReader(file));
      } catch (IOException ex) {
        final String msg = "There was a problem reading the lipid modification file.";
        MZmineCore.getDesktop().displayErrorMessage(msg + "\n(" + ex.getMessage() + ')');
        logger.log(Level.SEVERE, msg, ex);
        return;
      }

      // Load adducts from CSV data into parent choices.
      for (final String line[] : csvLines) {
        try {

          // Create new modification and add it to the choices if it's
          // new.
          final LipidModification modification = new LipidModification(line[0], line[1]);
          if (!checkList.getItems().contains(modification)) {
            checkList.getItems();
          }
        } catch (final NumberFormatException ignored) {
          logger.warning("Couldn't find lipid modifier in line " + line[0]);
        }
      }

    });

    exportButton.setTooltip(new Tooltip("Export custom modifications to a CSV file"));
    exportButton.setOnAction(e -> {
      // Create the chooser if necessary.

      FileChooser chooser = new FileChooser();
      chooser.setTitle("Select lipid modification file");
      chooser.getExtensionFilters()
          .add(new ExtensionFilter("Comma-separated values files", FILENAME_EXTENSION));

      // Choose the file.
      final File file = chooser.showSaveDialog(this.getScene().getWindow());
      if (file == null)
        return;

      // Export the modifications.
      try {

        final CSVPrinter writer = new CSVPrinter(new FileWriter(file));
        for (final LipidModification modification : checkList.getItems()) {

          writer.writeln(new String[] {modification.getLipidModificatio(),
              modification.getLipidModificationLabel()});
        }

      } catch (IOException ex) {
        final String msg = "There was a problem writing the lipid modifications file.";
        MZmineCore.getDesktop().displayErrorMessage(msg + "\n(" + ex.getMessage() + ')');
        logger.log(Level.SEVERE, msg, ex);
      }

    });

    removeButton.setTooltip(new Tooltip("Remove all lipid modification"));
    removeButton.setOnAction(e -> {
      checkList.getItems().clear();
    });

    buttonsPane.getChildren().addAll(addButton, importButton, exportButton, removeButton);
    setRight(buttonsPane);

  }

  void setValue(List<LipidModification> checkedItems) {
    checkList.getSelectionModel().clearSelection();
    for (LipidModification mod : checkedItems)
      checkList.getSelectionModel().select(mod);
  }

  List<LipidModification> getValue() {
    return checkList.getSelectionModel().getSelectedItems();
  }

  /**
   * Represents a lipid modification.
   */
  private static class AddLipidModificationParameters extends SimpleParameterSet {

    // lipid modification
    private static final StringParameter lipidModification =
        new StringParameter("Lipid modification", "Lipid modification");

    private static final StringParameter lipidModificationLabel =
        new StringParameter("Lipid modification label", "Lipid modification label", "");

    private AddLipidModificationParameters() {
      super(new Parameter[] {lipidModification, lipidModificationLabel});
    }
  }

}
