package io.github.mzmine.modules.visualization.lipidannotationsummary;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.MatchedLipid;
import io.github.mzmine.parameters.ParameterSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
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
    List<FeatureListRow> rowsWithLipidID = featureListRows.stream()
        .filter(this::rowHasMatchedLipidSignals).toList();
    List<MatchedLipid> matchedLipids = new ArrayList<>();
    for (FeatureListRow featureListRow : rowsWithLipidID) {
      if (featureListRow instanceof ModularFeatureListRow) {
        matchedLipids.addAll(featureListRow.get(LipidMatchListType.class));
      }
    }

    LipidAnnotationSunburstPlot lipidAnnotationSunburstPlotAllLipids = new LipidAnnotationSunburstPlot(
        matchedLipids, true, true, true, false);
    Pane titlePaneAllLipids = createTitlePane(matchedLipids, "Total Lipid Annotations");
    lipidIDsPane.setCenter(lipidAnnotationSunburstPlotAllLipids.getSunburstChart());
    lipidIDsPane.setTop(titlePaneAllLipids);

    List<MatchedLipid> bestLipidMatches = new ArrayList<>();
    for (FeatureListRow featureListRow : rowsWithLipidID) {
      if (featureListRow instanceof ModularFeatureListRow) {
        bestLipidMatches.add(featureListRow.get(LipidMatchListType.class).get(0));
      }
    }

    LipidAnnotationSunburstPlot lipidAnnotationSunburstPlotBestLipids = new LipidAnnotationSunburstPlot(
        bestLipidMatches, true, true, true, false);
    Pane titlePaneUniqueLipids = createTitlePane(bestLipidMatches, "Unique Lipid Annotations");
    bestLipidIDsPane.setCenter(lipidAnnotationSunburstPlotBestLipids.getSunburstChart());
    bestLipidIDsPane.setTop(titlePaneUniqueLipids);
  }

  private boolean rowHasMatchedLipidSignals(FeatureListRow row) {
    List<MatchedLipid> matches = row.get(LipidMatchListType.class);
    return matches != null && !matches.isEmpty();
  }

  private Pane createTitlePane(List<MatchedLipid> matchedLipids, String lipidDescription) {
    Label title = new Label(matchedLipids.size() + " " + lipidDescription);
    title.setFont(
        new Font(MZmineCore.getConfiguration().getDefaultChartTheme().getMasterFont().getName(),
            20));
    var titlePane = new BorderPane(title);
    titlePane.setPadding(new Insets(2));
    return titlePane;
  }

}
