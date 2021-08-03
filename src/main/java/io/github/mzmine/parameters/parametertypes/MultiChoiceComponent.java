/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.parameters.parametertypes;


import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.CSVPrinter;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.StringMapParser;
import java.util.function.Supplier;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.controlsfx.control.CheckListView;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public class MultiChoiceComponent<T extends StringMapParser<T>> extends BorderPane {
  private final Logger logger = Logger.getLogger(getClass().getName());
  // Filename extension.
  private static final FileChooser.ExtensionFilter csvFilter =
          new FileChooser.ExtensionFilter("Comma-separated values files", "*.csv");

  private final CheckListView<T> adductsView =
          new CheckListView<>(FXCollections.observableArrayList());

  private final Label lbTitle;
  private final Button addButton = new Button("Add...");
  private final Button importButton = new Button("Import...");
  private final Button exportButton = new Button("Export...");
  private final Button defaultButton = new Button("Reset");
  private final VBox buttonBar = new VBox();

  private final List<T> defaultChoices;

  private StringMapParser<T> parser;


  /**
   * Create the component.
   *
   * @param choices the adduct choices.
   */
  public MultiChoiceComponent(List<T> choices, List<T> defaultChoices, Supplier<T> addChoiceParam,
                              StringMapParser<T> parser) {
    this(choices, defaultChoices, addChoiceParam, parser, true, true, true, true);
  }
  /**
   * Create the component.
   *
   * @param choices the adduct choices.
   * @param addChoiceParam usually a ParameterSet as ObjectGenerator to add new choices
   */
  public MultiChoiceComponent(List<T> choices, List<T> defaultChoices, Supplier<T> addChoiceParam, StringMapParser<T> parser,
                              boolean btnAdd, boolean btnImport, boolean btnExport, boolean btnDefault) {
    this.defaultChoices = defaultChoices;
    this.parser = parser;

    setChoices(choices);

    buttonBar.setSpacing(10.0);
    if(btnAdd)
      buttonBar.getChildren().addAll(addButton);
    if(btnImport)
      buttonBar.getChildren().addAll(importButton);
    if(btnExport)
      buttonBar.getChildren().addAll(exportButton);
    if(btnDefault)
      buttonBar.getChildren().addAll(defaultButton);

    lbTitle = new Label("");
    setTop(lbTitle);
    setCenter(adductsView);
    setRight(buttonBar);

    addButton.setTooltip(new Tooltip("Add a custom choice to the set of choices"));
    addButton.setOnAction(e -> {
      if(addChoiceParam==null) {
        return;
      }
      // Show dialog.
      if(addChoiceParam instanceof ParameterSet) {
        final ParameterSet parameters = ((ParameterSet)addChoiceParam);
        if (parameters.showSetupDialog(true) == ExitCode.OK) {
          // Add to list of choices (if not already present).
          T choice = addChoiceParam.get();
          final Collection<T> currentChoices = adductsView.getItems();
          if (!currentChoices.contains(choice)) {
            currentChoices.add(choice);
          }
        }
      }
      else {
        try {
          T choice = addChoiceParam.get();
          final Collection<T> currentChoices = adductsView.getItems();
          if (!currentChoices.contains(choice)) {
            currentChoices.add(choice);
          }
        } catch(Exception ex) {
          logger.warning("Cannot create new choice");
        }
      }
    });

    importButton.setTooltip(new Tooltip("Import custom adducts from a CSV file"));
    importButton.setOnAction(e -> {
      // Parent component.
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Select File");
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
        final String msg = "There was a problem reading the choices file.";
        MZmineCore.getDesktop().displayErrorMessage(msg + "\n(" + ex.getMessage() + ')');
        logger.log(Level.SEVERE, msg, ex);
      }

      // Read the adducts data.
      if (csvLines == null)
        return;

      // Load adducts from CSV data into parent choices.
      loadChoices(csvLines, adductsView.getItems());
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
        exportToFile(file, adductsView.getItems());
      } catch (IOException ex) {
        final String msg = "There was a problem writing the choices file.";
        MZmineCore.getDesktop().displayErrorMessage(msg + "\n(" + ex.getMessage() + ')');
        logger.log(Level.SEVERE, msg, ex);
      }
    });

    defaultButton.setTooltip(new Tooltip("Reset choices to default set"));
    defaultButton.setOnAction(e -> {
      ObservableList<T> newAdducts = FXCollections.observableArrayList();
      newAdducts.addAll(defaultChoices);
      adductsView.setItems(newAdducts);
    });
  }

  public List<T> getChoices() {
    return new ArrayList<>(adductsView.getItems());
  }

  public List<T> getValue() {
    return new ArrayList<>(adductsView.getCheckModel().getCheckedItems());
  }

  public void setValue(List<T> newValue) {
    adductsView.getCheckModel().clearChecks();;
    for (T adduct : newValue)
      adductsView.getCheckModel().check(adduct);
  }

  public String getTitle() {
    return lbTitle.getText();
  }

  public void setTitle(String title) {
    lbTitle.setText(title);
  }

  public void addButton(String text, EventHandler<ActionEvent> handler) {
    Button btn = new Button(text);
    btn.setOnAction(handler);
    buttonBar.getChildren().add(btn);
  }

  public CheckListView<T> getCheckListView() {
    return adductsView;
  }

  /**
   * Load the adducts into the list of adduct choices.
   *
   * @param lines CSV lines to parse.
   * @param choices the current adduct choices.
   * @return a new list of adduct choices that includes the original choices plus any new ones found
   *         by parsing the CSV lines.
   */
  private void loadChoices(final String[][] lines, final Collection<T> choices) {
    if(lines.length<2)
      return;
    // load a list of choices.
    String[] header = lines[0];
    for (int l=1; l<lines.length; l++) {
      String[] line = lines[l];
      if(lines.length!=header.length) {
        logger.warning("Line length is different to header length: "+line.length+" to "+header.length);
        continue;
      }
      // map with values
      Map<String,String> map = new HashMap<>();
      for(int f=0; f<line.length; f++) {
        map.put(header[f], line[f]);
      }
      try {
        // Create new choice
        if(parser!=null) {
          final T choice = parser.parseDataMap(map);
          if (!choices.contains(choice)) {
            choices.add(choice);
          }
        }
      } catch (Exception ignored) {
        logger.warning("Cannot parse new choice: "+line);
      }
    }
  }


  /**
   * Writes the choices to a CSV file.
   *
   * @param file the destination file.
   * @param choices the choices to export.
   * @throws IOException if there are i/o problems.
   */
  private void exportToFile(final File file, final Collection<T> choices) throws IOException {
    final CSVPrinter writer = new CSVPrinter(new FileWriter(file));
    boolean firstLine = true;
    for (final T choice : choices) {
      Map<String, String> map = choice.getDataMap();
      // write header
      if (firstLine) {
        writer.writeln(map.keySet().toArray(String[]::new));
        firstLine = false;
      }
      // write values
      writer.writeln(map.values().toArray(String[]::new));
    }
  }

  public void setChoices(List<T> choices) {
    adductsView.getItems().clear();
    adductsView.getItems().addAll(choices);
  }
  public void setChoices(T... choices) {
    adductsView.getItems().clear();
    adductsView.getItems().addAll(choices);
  }
}
