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

/*
 * Code created was by or on behalf of Syngenta and is released under the open source license in use
 * for the pre-existing code or project. Syngenta does not assert ownership or copyright any over
 * pre-existing work.
 */

package io.github.mzmine.modules.dataprocessing.id_adductsearch;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.controlsfx.control.CheckListView;
import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.CSVPrinter;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * A component for selecting adducts.
 *
 */
public class AdductsComponent extends BorderPane {

  // Logger.
  private final Logger logger = Logger.getLogger(getClass().getName());

  // Filename extension.
  private static final ExtensionFilter csvFilter =
      new ExtensionFilter("Comma-separated values files", "*.csv");

  private final CheckListView<AdductType> adductsView =
      new CheckListView<>(FXCollections.observableArrayList());

  private final Button addButton = new Button("Add...");
  private final Button importButton = new Button("Import...");
  private final Button exportButton = new Button("Export...");
  private final Button defaultButton = new Button("Reset");
  private final VBox buttonBar = new VBox();


  /**
   * Create the component.
   *
   * @param choices the adduct choices.
   */
  public AdductsComponent(List<AdductType> choices) {

    adductsView.getItems().addAll(choices);

    buttonBar.setSpacing(10.0);
    buttonBar.getChildren().addAll(addButton, importButton, exportButton, defaultButton);

    setCenter(adductsView);
    setRight(buttonBar);


    addButton.setTooltip(new Tooltip("Add a custom adduct to the set of choices"));
    addButton.setOnAction(e -> {
      // Show dialog.
      final ParameterSet parameters = new AddAdductParameters();
      if (parameters.showSetupDialog(true) == ExitCode.OK) {

        // Create new adduct.
        final AdductType adduct =
            new AdductType(parameters.getParameter(AddAdductParameters.NAME).getValue(),
                parameters.getParameter(AddAdductParameters.MASS_DIFFERENCE).getValue());

        // Add to list of choices (if not already present).
        final Collection<AdductType> currentChoices = adductsView.getItems();
        if (!currentChoices.contains(adduct)) {
          currentChoices.add(adduct);
        }
      }
    });

    importButton.setTooltip(new Tooltip("Import custom adducts from a CSV file"));
    importButton.setOnAction(e -> {
      // Parent component.

      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Select Adducts File");
      fileChooser.getExtensionFilters().add(csvFilter);
      final File file = fileChooser.showOpenDialog(this.getScene().getWindow());

      // Select a file.
      if (file == null)
        return;

      // Read the CSV file into a string array.
      String[][] csvLines = null;
      try {
        csvLines = CSVParser.parse(new FileReader(file));
      } catch (IOException ex) {
        final String msg = "There was a problem reading the adducts file.";
        MZmineCore.getDesktop().displayErrorMessage(msg + "\n(" + ex.getMessage() + ')');
        logger.log(Level.SEVERE, msg, ex);
      }

      // Read the adducts data.
      if (csvLines == null)
        return;

      // Load adducts from CSV data into parent choices.
      loadAdductsIntoChoices(csvLines, adductsView.getItems());

    });

    exportButton.setTooltip(new Tooltip("Export custom adducts to a CSV file"));
    exportButton.setOnAction(e -> {

      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Select Adducts File");
      fileChooser.getExtensionFilters().add(csvFilter);
      final File file = fileChooser.showSaveDialog(this.getScene().getWindow());

      if (file == null)
        return;

      // Export the adducts.
      try {
        exportAdductsToFile(file, adductsView.getItems());
      } catch (IOException ex) {
        final String msg = "There was a problem writing the adducts file.";
        MZmineCore.getDesktop().displayErrorMessage(msg + "\n(" + ex.getMessage() + ')');
        logger.log(Level.SEVERE, msg, ex);
      }

    });

    defaultButton.setTooltip(new Tooltip("Reset adduct choices to default set"));
    defaultButton.setOnAction(e -> {
      ObservableList<AdductType> newAdducts = FXCollections.observableArrayList();
      newAdducts.addAll(AdductType.getDefaultValues());
      adductsView.setItems(newAdducts);
    });


  }

  public List<AdductType> getChoices() {
    return new ArrayList<>(adductsView.getItems());
  }

  public List<AdductType> getValue() {
    return new ArrayList<>(adductsView.getCheckModel().getCheckedItems());
  }

  public void setValue(List<AdductType> newValue) {
    adductsView.getCheckModel().clearChecks();;
    for (AdductType adduct : newValue)
      adductsView.getCheckModel().check(adduct);
  }

  /**
   * Load the adducts into the list of adduct choices.
   *
   * @param lines CSV lines to parse.
   * @param adducts the current adduct choices.
   * @return a new list of adduct choices that includes the original choices plus any new ones found
   *         by parsing the CSV lines.
   */
  private void loadAdductsIntoChoices(final String[][] lines,
      final Collection<AdductType> choices) {

    // Create a list of adducts.
    int i = 1;
    for (final String[] line : lines) {

      if (line.length >= 2) {

        try {

          // Create new adduct and add it to the choices if it's new.
          final AdductType adduct = new AdductType(line[0], Double.parseDouble(line[1]));
          if (!choices.contains(adduct)) {
            choices.add(adduct);
          }
        } catch (final NumberFormatException ignored) {

          logger.warning("Invalid numeric value (" + line[1] + ") - ignored.");
        }
      } else {

        logger.warning("Line #" + i + " contains too few fields - ignored.");
      }
      i++;
    }
  }


  /**
   * Writes the adducts to a CSV file.
   *
   * @param file the destination file.
   * @param adducts the adducts to export.
   * @throws IOException if there are i/o problems.
   */
  private void exportAdductsToFile(final File file, final Collection<AdductType> adducts)
      throws IOException {

    final CSVPrinter writer = new CSVPrinter(new FileWriter(file));
    for (final AdductType adduct : adducts) {
      writer.writeln(new String[] { //
          adduct.getName(), //
          String.valueOf(adduct.getMassDifference()) //
      });
    }
  }
}
