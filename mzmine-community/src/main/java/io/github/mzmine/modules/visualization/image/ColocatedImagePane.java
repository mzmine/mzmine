/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.image;

import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.identities.MolecularFormulaIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.gui.chartbasics.chartgroups.ChartGroup;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.javafx.util.color.ColorScaleUtil;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Displays all correlated images in a grid
 */
public class ColocatedImagePane extends StackPane {

  public static final Color MAX_COS_COLOR = MZmineCore.getConfiguration().getDefaultColorPalette()
      .getPositiveColor();
  public static final Color MIN_COS_COLOR = MZmineCore.getConfiguration().getDefaultColorPalette()
      .getNegativeColor();
  public static final double MIN_COS_COLOR_VALUE = 0.5;
  public static final double MAX_COS_COLOR_VALUE = 1.0;
  private static final Logger logger = Logger.getLogger(ColocatedImagePane.class.getName());
  private final GridPane contentGrid = new GridPane();
  private final Label noContentLabel = new Label("Selected feature has no correlated images.");
  private ChartGroup chartGroup = null;


  public ColocatedImagePane(@Nullable List<RowsRelationship> sortedRelationships,
      @Nullable FeatureListRow selectedRow, @Nullable RawDataFile file) {
    super();
    this.getChildren().add(contentGrid);
    contentGrid.setHgap(5);
    contentGrid.setVgap(5);
    chartGroup = new ChartGroup(false, false, true, true);
    updateContent(sortedRelationships, selectedRow, file);
  }

  public void updateContent(List<RowsRelationship> sortedRelationships, FeatureListRow selectedRow,
      RawDataFile file) {
    getChildren().remove(noContentLabel);
    contentGrid.getChildren().clear();
    chartGroup = new ChartGroup(false, false, true, true);

    if (sortedRelationships == null || sortedRelationships.isEmpty() || selectedRow == null
        || !(file instanceof ImagingRawDataFile imgFile)) {
      getChildren().add(noContentLabel);
      return;
    }

    int i = 0;
    for (var relationship : sortedRelationships) {
      final Node imagePlot = createImagePlotForRelatedRow(relationship, selectedRow, imgFile);
      if (imagePlot == null) {
        continue;
      }
      final int rowPosition = i / 4;
      final int colPosition = i % 4;
      contentGrid.add(imagePlot, colPosition, rowPosition);
      if (i > 100) {
        logger.log(Level.WARNING, "Only show first 100 correlations");
        break;
      }
      i++;
    }
  }

  @Nullable
  private BorderPane createImagePlotForRelatedRow(RowsRelationship relationship,
      FeatureListRow selectedRow, ImagingRawDataFile file) {

    final FeatureListRow relatedRow = relationship.getOtherRow(selectedRow);
    if (relatedRow == null) {
      return null;
    }
    final Feature relatedFeature = relatedRow.getFeature(file);
    if (relatedFeature == null) {
      return null;
    }
    final double score = relationship.getScore();

    return createImagePane(relatedRow, (ModularFeature) relatedFeature, score);
  }

  /**
   * Creates an image plot that whose zoom is bound to all images in this
   * {@link ColocatedImagePane}.
   */
  @NotNull
  public BorderPane createImagePane(FeatureListRow row, ModularFeature feature, double score) {
    BorderPane borderPane = new BorderPane();
    ParameterSet params = MZmineCore.getConfiguration()
        .getModuleParameters(ImageVisualizerModule.class).cloneParameterSet();
    params.setParameter(ImageVisualizerParameters.imageNormalization,
        MZmineCore.getConfiguration().getImageNormalization()); // same as in the feature table.
    params.setParameter(ImageVisualizerParameters.imageTransformation,
        MZmineCore.getConfiguration().getImageTransformation());

    var imagePlot = new ImagingPlot((ImageVisualizerParameters) params);
    imagePlot.getChart().getXYPlot().setShowCursorCrosshair(false, false);
    imagePlot.setData(feature);

    EChartViewer chart = imagePlot.getChart();
    chart.setMinSize(200, 200);
    chartGroup.add(new ChartViewWrapper(chart));
    GridPane.setHgrow(chart, Priority.ALWAYS);
    GridPane.setVgrow(chart, Priority.ALWAYS);
    borderPane.setCenter(chart);
    borderPane.setTop(createTitlePane(row, score));
    GridPane.setHgrow(borderPane, Priority.ALWAYS);
    GridPane.setVgrow(borderPane, Priority.ALWAYS);
    return borderPane;
  }

  private Pane createTitlePane(FeatureListRow row, Double score) {
    String styleWhiteScoreSmall = "white-score-label-small";
    var featureInfo = new VBox(0, createLabel("Feature", styleWhiteScoreSmall),
        createLabel("#" + row.getID(), styleWhiteScoreSmall));
    featureInfo.setAlignment(Pos.CENTER_RIGHT);
    Color gradientCol = FxColorUtil.awtColorToFX(
        ColorScaleUtil.getColor(FxColorUtil.fxColorToAWT(MIN_COS_COLOR),
            FxColorUtil.fxColorToAWT(MAX_COS_COLOR), MIN_COS_COLOR_VALUE, MAX_COS_COLOR_VALUE,
            score));
    String name = extractName(row);
    Label lblHit = createLabel(name, "white-larger-label");
    String simScoreTooltip =
        "Similarity with selected image: " + MZmineCore.getConfiguration().getGuiFormats()
            .score(score);
    Label lblScore = createLabel(MZmineCore.getConfiguration().getGuiFormats().score(score),
        simScoreTooltip, "white-score-label");
    var scoreBox = new HBox(5, featureInfo, lblScore);
    scoreBox.setPadding(new Insets(0, 5, 0, 10));
    scoreBox.setAlignment(Pos.CENTER);
    var titlePane = new BorderPane(lblHit);
    titlePane.setRight(scoreBox);
    titlePane.setPadding(new Insets(2));
    titlePane.setStyle("-fx-background-color: " + FxColorUtil.colorToHex(gradientCol));
    return titlePane;
  }

  private String extractName(FeatureListRow row) {
    String mz = "m/z " + MZmineCore.getConfiguration().getGuiFormats().mz(row.getAverageMZ());
    if (row.getPreferredAnnotationName() != null) {
      return row.getPreferredAnnotationName() + " " + mz;
    } else {
      final IonIdentity bestIon = row.getBestIonIdentity();
      if (bestIon != null) {
        return bestIon.toString() + " " + bestIon.getBestMolFormula()
            .map(MolecularFormulaIdentity::getFormulaAsString) + " " + mz;
      } else {
        return mz;
      }
    }
  }

  private Label createLabel(final String label, final String styleClass) {
    return createLabel(label, null, styleClass);
  }

  private Label createLabel(final String label, String tooltip, final String styleClass) {
    Label lbl = new Label(label);
    lbl.getStyleClass().add(styleClass);
    if (tooltip != null) {
      lbl.setTooltip(new Tooltip(tooltip));
    }
    return lbl;
  }

}
