/*
 * Copyright (c) 2004-2024 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.lipidannotationsummary;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.parameters.ParameterSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Accordion;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.jetbrains.annotations.NotNull;

public class LipidAnnotationSummaryTab extends MZmineTab {

  private final ParameterSet parameters;
  private final FeatureList featureList;
  private final ObservableList<FeatureListRow> featureListRows;
  private BorderPane lipidIDsPane;
  private BorderPane bestLipidIDsPane;


  public LipidAnnotationSummaryTab(String title, boolean showBinding, boolean defaultBindingState,
      ParameterSet parameters, FeatureList featureList) {
    super(title, showBinding, defaultBindingState);
    this.parameters = parameters;
    this.featureList = featureList;
    this.featureListRows = featureList.getRows();
    init();
  }

  public LipidAnnotationSummaryTab(String title, ParameterSet parameters, FeatureList featureList) {
    super(title, true, false);
    this.parameters = parameters;
    this.featureList = featureList;
    this.featureListRows = featureList.getRows();
    init();
  }


  public LipidAnnotationSummaryTab(String title, ParameterSet parameters, FeatureList featureList,
      ObservableList<FeatureListRow> featureListRows) {
    super(title, true, false);
    this.parameters = parameters;
    this.featureList = featureList;
    this.featureListRows = featureListRows;
    init();
  }

  private void init() {
    BorderPane mainPane = new BorderPane();
    lipidIDsPane = new BorderPane();
    bestLipidIDsPane = new BorderPane();
    SplitPane splitPane = new SplitPane(lipidIDsPane, bestLipidIDsPane);
    splitPane.setOrientation(Orientation.HORIZONTAL);
    mainPane.setCenter(splitPane);
    buildLLipidIDSunburstPlot();
    this.setContent(mainPane);
  }

  @Override
  public @NotNull Collection<? extends RawDataFile> getRawDataFiles() {
    return List.of();
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getFeatureLists() {
    return List.of(featureList);
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getAlignedFeatureLists() {
    return List.of();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {

  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }


  private void buildLLipidIDSunburstPlot() {
    // Prepare data for the plots
    List<FeatureListRow> rowsWithLipidID = featureListRows.stream()
        .filter(this::rowHasMatchedLipidSignals).toList();

    // Build plot for all lipid annotations with filter
    List<MatchedLipid> matchedLipidsTotal = extractMatchedLipids(rowsWithLipidID);
    Pane filteredPaneAllLipids = createFilteredPane(
        new LipidAnnotationSunburstPlot(matchedLipidsTotal, true, true, true, false),
        matchedLipidsTotal, "Total Lipid Annotations");

    // Build plot for unique lipid annotations with filter
    List<MatchedLipid> matchedLipidsBest = extractBestLipidMatches(rowsWithLipidID);
    Pane filteredPaneUniqueLipids = createFilteredPane(
        new LipidAnnotationSunburstPlot(matchedLipidsBest, true, true, true, false),
        matchedLipidsBest, "Unique Lipid Annotations");

    // Set the filtered panes into the split pane
    lipidIDsPane.setCenter(filteredPaneAllLipids);
    bestLipidIDsPane.setCenter(filteredPaneUniqueLipids);
  }

  private boolean rowHasMatchedLipidSignals(FeatureListRow row) {
    List<MatchedLipid> matches = row.getLipidMatches();
    return !matches.isEmpty();
  }

  private List<MatchedLipid> extractMatchedLipids(List<FeatureListRow> rowsWithLipidID) {
    return rowsWithLipidID.stream().flatMap(row -> row.getLipidMatches().stream()).toList();
  }

  private List<MatchedLipid> extractBestLipidMatches(List<FeatureListRow> rowsWithLipidID) {
    return rowsWithLipidID.stream().<MatchedLipid>mapMulti((row, consumer) -> {
      var matches = row.getLipidMatches();
      if (!matches.isEmpty()) {
        consumer.accept(matches.getFirst());
      }
    }).toList();
  }

  private Pane createTitlePane(List<MatchedLipid> matchedLipids, String lipidDescription) {
    Label title = createTitleLabel(matchedLipids.size(), lipidDescription);
    var titlePane = new BorderPane(title);
    titlePane.setPadding(new Insets(2));
    return titlePane;
  }

  private Label createTitleLabel(int lipidCount, String lipidDescription) {
    Label title = new Label(lipidCount + " " + lipidDescription);
    title.setFont(
        new Font(MZmineCore.getConfiguration().getDefaultChartTheme().getMasterFont().getName(),
            20));
    return title;
  }

  private Pane createFilteredPane(LipidAnnotationSunburstPlot plot,
      List<MatchedLipid> originalLipids, String lipidDescription) {

    ComboBox<AnnotationFilter> filterComboBox = new ComboBox<>();
    filterComboBox.getItems().addAll(AnnotationFilter.values());
    filterComboBox.setValue(AnnotationFilter.ALL);

    // Create the title pane and get its label
    Pane titlePane = createTitlePane(originalLipids, lipidDescription);
    Label titleLabel = (Label) ((BorderPane) titlePane).getCenter();

    // Main plot container
    BorderPane plotPane = new BorderPane(plot.getSunburstChart());

    // Checkboxes for plot options
    CheckBox includeCategoryCheckbox = new CheckBox("Lipid category");
    includeCategoryCheckbox.setSelected(true);
    CheckBox includeMainClassCheckbox = new CheckBox("Lipid main class");
    includeMainClassCheckbox.setSelected(true);
    CheckBox includeSubClassCheckbox = new CheckBox("Lipid sub class");
    includeSubClassCheckbox.setSelected(true);
    CheckBox includeSpeciesCheckbox = new CheckBox("Lipid species");
    includeSpeciesCheckbox.setSelected(false);

    // HBox to hold filter combo box and checkboxes
    HBox filterOptions = new HBox(5, filterComboBox, includeCategoryCheckbox,
        includeMainClassCheckbox, includeSubClassCheckbox, includeSpeciesCheckbox);
    filterOptions.setPadding(new Insets(5));

    // Accordion and filter combo box
    TitledPane filterPane = new TitledPane("Filter annotations", filterOptions);
    Accordion accordion = new Accordion(filterPane);

    // Set action for combo box and checkboxes to update the plot based on filter and options
    PropertyUtils.onChange(
        () -> updatePlot(plotPane, titleLabel, originalLipids, filterComboBox, lipidDescription,
            includeCategoryCheckbox, includeMainClassCheckbox, includeSubClassCheckbox,
            includeSpeciesCheckbox),//
        filterComboBox.getSelectionModel().selectedItemProperty(),
        includeCategoryCheckbox.selectedProperty(), includeMainClassCheckbox.selectedProperty(),
        includeSubClassCheckbox.selectedProperty(), includeSpeciesCheckbox.selectedProperty());

    // Layout structure: VBox to stack title, accordion, and plot
    VBox layout = new VBox();
    layout.setSpacing(10); // Optional: Adjust spacing as needed
    layout.getChildren().addAll(titlePane, accordion, plotPane);
    VBox.setVgrow(plotPane, Priority.ALWAYS);

    BorderPane filteredPane = new BorderPane();
    filteredPane.setCenter(layout);
    return filteredPane;
  }

  private void updatePlot(BorderPane plotPane, Label titleLabel, List<MatchedLipid> originalLipids,
      ComboBox<AnnotationFilter> filterComboBox, String lipidDescription,
      CheckBox includeCategoryCheckbox, CheckBox includeMainClassCheckbox,
      CheckBox includeSubClassCheckbox, CheckBox includeSpeciesCheckbox) {

    // Get filter and checkbox states
    AnnotationFilter filter = filterComboBox.getValue();
    List<MatchedLipid> filteredLipids = filter.filterLipids(new ArrayList<>(originalLipids));

    boolean includeCategory = includeCategoryCheckbox.isSelected();
    boolean includeMainClass = includeMainClassCheckbox.isSelected();
    boolean includeSubClass = includeSubClassCheckbox.isSelected();
    boolean includeSpecies = includeSpeciesCheckbox.isSelected();

    // Recreate the plot with the new settings
    LipidAnnotationSunburstPlot newPlot = new LipidAnnotationSunburstPlot(filteredLipids,
        includeCategory, includeMainClass, includeSubClass, includeSpecies);

    // Update the plot in the UI by setting a new chart in the plotPane center
    plotPane.setCenter(newPlot.getSunburstChart());
    titleLabel.setText(filteredLipids.size() + " " + lipidDescription);
  }


  enum AnnotationFilter {
    ALL, CONFIRMED_BY_MS2, UNCONFIRMED;

    @Override
    public String toString() {
      return switch (this) {
        case ALL -> "All";
        case UNCONFIRMED -> "Unconfirmed (MS1-only)";
        case CONFIRMED_BY_MS2 -> "Confirmed (by MS2)";
      };
    }

    public List<MatchedLipid> filterLipids(List<MatchedLipid> lipids) {
      return switch (this) {
        case ALL -> new ArrayList<>(lipids);
        case CONFIRMED_BY_MS2 ->
            lipids.stream().filter(lipid -> lipid.getStatus().isConfirmedByMS2()).toList();
        case UNCONFIRMED ->
            lipids.stream().filter(lipid -> !lipid.getStatus().isConfirmedByMS2()).toList();
      };
    }
  }
}
