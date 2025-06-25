/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.spectra.matchedlipid;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.framework.fx.features.AbstractFeatureListRowsPane;
import io.github.mzmine.gui.framework.fx.features.ParentFeatureListPaneGroup;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.javafx.util.color.ColorScaleUtil;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.util.scans.ScanUtils;
import java.text.DecimalFormat;
import java.util.List;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class LipidAnnotationMatchPane extends AbstractFeatureListRowsPane {

  private final ScrollPane scrollPane;
  private int matches = 0;

  public LipidAnnotationMatchPane(final ParentFeatureListPaneGroup parentGroup) {
    super(parentGroup);
    scrollPane = new ScrollPane();
    scrollPane.setFitToHeight(true);
    scrollPane.setFitToWidth(true);
    scrollPane.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
    scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
    setCenter(scrollPane);
  }

  @Override
  public void onRowsChanged(List<? extends FeatureListRow> rows) {
    super.onRowsChanged(rows);
  }

  @Override
  public void onSelectedRowsChanged(List<? extends FeatureListRow> selectedRows) {
    super.onSelectedRowsChanged(selectedRows);
    matches = 0;
    GridPane pane = new GridPane();
    GridPane.setHgrow(pane, Priority.ALWAYS);
    GridPane.setVgrow(pane, Priority.ALWAYS);
    pane.setHgap(10);
    pane.setVgap(10);
    int i = 0;
    for (var row : selectedRows) {
      if (!(row instanceof ModularFeatureListRow selectedRow)) {
        continue;
      }
      final List<MatchedLipid> matchedLipids = selectedRow.get(LipidMatchListType.class);
      if (matchedLipids != null && !matchedLipids.isEmpty()) {
        for (MatchedLipid matchedLipid : matchedLipids) {
          if (matchedLipid.getComment() == null || matchedLipid.getComment().isEmpty()) {
            MatchedLipidPane lipidAnnotationMatchPane = new MatchedLipidPane(matchedLipid);
            GridPane.setHgrow(lipidAnnotationMatchPane, Priority.ALWAYS);
            GridPane.setVgrow(lipidAnnotationMatchPane, Priority.ALWAYS);
            pane.add(lipidAnnotationMatchPane, 0, i);
            i++;
            matches++;
          }
        }
      }
    }
    scrollPane.setContent(pane);

  }

  @Override
  public boolean hasContent() {
    return matches > 0;
  }

  static class MatchedLipidPane extends GridPane {

    public static final int META_WIDTH = 300;
    public static final int ENTRY_HEIGHT = 400;
    public static final double MIN_MSMS_SCORE_COLOR_VALUE = 0.0;
    public static final double MAX_MSMS_SCORE_COLOR_VALUE = 100.0;

    private static final DecimalFormat MSMS_SCORE_FORM = new DecimalFormat("0.0");
    public static Color MAX_MSMS_SCORE_COLOR = MZmineCore.getConfiguration()
        .getDefaultColorPalette().getPositiveColor();
    public static Color MIN_MSMS_SCORE_COLOR = MZmineCore.getConfiguration()
        .getDefaultColorPalette().getNegativeColor();
    private final MatchedLipid matchedLipid;
    private final LipidSpectrumPlot lipidSpectrumPlot;
    private VBox metaDataPanel;
    private ScrollPane metaDataScroll;

    public MatchedLipidPane(MatchedLipid matchedLipid) {
      super();

      this.matchedLipid = matchedLipid;

      setMinSize(750, 500);
      var pnTitle = createTitlePane();
      metaDataScroll = createMetaDataPane();

      lipidSpectrumPlot = new LipidSpectrumPlot(matchedLipid, true, RunOption.THIS_THREAD);

      // put into main
      ColumnConstraints ccSpectrum = new ColumnConstraints(400, -1, Region.USE_COMPUTED_SIZE,
          Priority.ALWAYS, HPos.CENTER, true);
      ColumnConstraints ccMetadata = new ColumnConstraints(META_WIDTH + 30, META_WIDTH + 30,
          Region.USE_COMPUTED_SIZE, Priority.NEVER, HPos.LEFT, false);

      add(pnTitle, 0, 0, 2, 1);
      add(lipidSpectrumPlot, 0, 1);
      add(metaDataScroll, 1, 1);

      getColumnConstraints().add(0, ccSpectrum);
      getColumnConstraints().add(1, ccMetadata);
    }

    private Pane createTitlePane() {
      String styleWhiteScoreSmall = "white-score-label-small";
      // create Top panel
      double msMsScore = matchedLipid.getMsMsScore() * 100;
      Color gradientCol = FxColorUtil.awtColorToFX(
          ColorScaleUtil.getColor(FxColorUtil.fxColorToAWT(MIN_MSMS_SCORE_COLOR),
              FxColorUtil.fxColorToAWT(MAX_MSMS_SCORE_COLOR), MIN_MSMS_SCORE_COLOR_VALUE,
              MAX_MSMS_SCORE_COLOR_VALUE, msMsScore));

      Label lblMatchedLipid = createLabel(matchedLipid.getLipidAnnotation().getAnnotation(),
          "white-larger-label");

      String simScoreTooltip =
          "Explained intensity [%] of all signals in MS/MS spectrum: " + MSMS_SCORE_FORM.format(
              msMsScore);
      Label lblScore = createLabel(MSMS_SCORE_FORM.format(msMsScore), simScoreTooltip,
          "white-score-label");

      var totalSignals = matchedLipid.getMatchedFragments().size();
      var lblMatched = createLabel(String.valueOf(totalSignals), styleWhiteScoreSmall);

      var leftScores = new VBox(0, lblMatched);
      leftScores.setAlignment(Pos.CENTER);

      var scoreDef = new VBox(0, createLabel("Matched signals:", styleWhiteScoreSmall));
      scoreDef.setAlignment(Pos.CENTER_RIGHT);

      var scoreBox = new HBox(5, scoreDef, leftScores, lblScore);
      scoreBox.setPadding(new Insets(0, 5, 0, 10));
      scoreBox.setAlignment(Pos.CENTER);

      var titlePane = new BorderPane(lblMatchedLipid);
      titlePane.setRight(scoreBox);

      titlePane.setPadding(new Insets(2));

      titlePane.setStyle("-fx-background-color: " + FxColorUtil.colorToHex(gradientCol));

      return titlePane;
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

    private ScrollPane createMetaDataPane() {
      metaDataPanel = new VBox();
      metaDataPanel.getStyleClass().add("region");

      ColumnConstraints ccMetadata1 = new ColumnConstraints(META_WIDTH / 2d, -1, Double.MAX_VALUE,
          Priority.ALWAYS, HPos.LEFT, false);
      ColumnConstraints ccMetadata2 = new ColumnConstraints(META_WIDTH / 2d, -1, Double.MAX_VALUE,
          Priority.ALWAYS, HPos.LEFT, false);
      ccMetadata1.setPercentWidth(50);
      ccMetadata2.setPercentWidth(50);
      BorderPane lipidInformationPanel = extractLipidInformation();

      metaDataPanel.getChildren().add(lipidInformationPanel);
      metaDataPanel.setMinSize(META_WIDTH, ENTRY_HEIGHT);
      metaDataPanel.setPrefSize(META_WIDTH, -1);
      metaDataScroll = new ScrollPane(metaDataPanel);
      metaDataScroll.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
      metaDataScroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
      metaDataScroll.setFitToWidth(true);
      metaDataScroll.setFitToHeight(true);
      int margin = 20;
      metaDataScroll.setMinSize(META_WIDTH + margin, ENTRY_HEIGHT + margin);
      metaDataScroll.setMaxSize(META_WIDTH + margin, ENTRY_HEIGHT + margin);
      metaDataScroll.setPrefSize(META_WIDTH + margin, ENTRY_HEIGHT + margin);

      return metaDataScroll;
    }

    private BorderPane extractLipidInformation() {
      VBox panelOther = new VBox();
      panelOther.setAlignment(Pos.TOP_LEFT);
      Label annotation = new Label("Name: " + matchedLipid.getLipidAnnotation().getAnnotation());
      annotation.setWrapText(true);
      panelOther.getChildren().addAll(annotation);

      Label formula = new Label("Formula: " + MolecularFormulaManipulator.getString(
          matchedLipid.getLipidAnnotation().getMolecularFormula()));
      formula.setWrapText(true);
      panelOther.getChildren().addAll(formula);

      Label ion = new Label(
          "Ion notation: " + matchedLipid.getIonizationType().getAdductName() + " "
              + MZmineCore.getConfiguration().getMZFormat().format(FormulaUtils.calculateMzRatio(
              FormulaUtils.ionizeFormula(MolecularFormulaManipulator.getString(
                      matchedLipid.getLipidAnnotation().getMolecularFormula()),
                  matchedLipid.getIonizationType()))));
      ion.setWrapText(true);
      panelOther.getChildren().addAll(ion);

      Label lipidClass = new Label(
          "Lipid class: " + matchedLipid.getLipidAnnotation().getLipidClass().getName());
      lipidClass.setWrapText(true);
      panelOther.getChildren().addAll(lipidClass);

      Label lipidMainClass = new Label(
          "Lipid main class: " + matchedLipid.getLipidAnnotation().getLipidClass().getMainClass()
              .getName());
      lipidMainClass.setWrapText(true);
      panelOther.getChildren().addAll(lipidMainClass);

      Label lipidCategory = new Label(
          "Lipid category: " + matchedLipid.getLipidAnnotation().getLipidClass().getMainClass()
              .getLipidCategory().getName());
      lipidCategory.setWrapText(true);
      panelOther.getChildren().addAll(lipidCategory);

      Label space = new Label("");
      panelOther.getChildren().addAll(space);

      Label rawDataTitle = new Label("Raw data information");
      rawDataTitle.getStyleClass().add("bold-title-label");
      panelOther.getChildren().addAll(rawDataTitle);

      if (!matchedLipid.getMatchedFragments().isEmpty()) {
        LipidFragment lipidFragment = matchedLipid.getMatchedFragments().stream().findFirst()
            .orElse(null);
        if (lipidFragment != null) {
          Label rawFile = new Label(
              "Raw data file: " + lipidFragment.getMsMsScan().getDataFile().getName());
          rawFile.setWrapText(true);
          panelOther.getChildren().addAll(rawFile);

          Label matchedScan = new Label(
              "Matched Scan number: " + ScanUtils.extractScanIdString(lipidFragment.getMsMsScan(),
                  false));
          matchedScan.setWrapText(true);
          panelOther.getChildren().addAll(matchedScan);
        }
      }

      Label otherInfo = new Label("Lipid information");
      otherInfo.getStyleClass().add("bold-title-label");
      BorderPane pn = new BorderPane(panelOther);
      pn.setTop(otherInfo);
      return pn;
    }

  }
}
