package io.github.mzmine.modules.visualization.spectra.matchedlipid;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.features.types.graphicalnodes.LipidSpectrumChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.MatchedLipid;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.color.ColorScaleUtil;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.text.DecimalFormat;
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

public class LipidAnnotationMatchPane extends GridPane {

  public static final int META_WIDTH = 300;
  public static final int ENTRY_HEIGHT = 400;
  public static final double MIN_MSMS_SCORE_COLOR_VALUE = 0.0;
  public static final double MAX_MSMS_SCORE_COLOR_VALUE = 100.0;

  private static final DecimalFormat MSMS_SCORE_FORM = new DecimalFormat("0.0");
  public static Color MAX_MSMS_SCORE_COLOR = MZmineCore.getConfiguration().getDefaultColorPalette()
      .getPositiveColor();
  public static Color MIN_MSMS_SCORE_COLOR = MZmineCore.getConfiguration().getDefaultColorPalette()
      .getNegativeColor();
  private final MatchedLipid matchedLipid;
  private final LipidSpectrumChart lipidSpectrumChart;
  private VBox metaDataPanel;
  private ScrollPane metaDataScroll;

  public LipidAnnotationMatchPane(MatchedLipid matchedLipid) {
    super();

    this.matchedLipid = matchedLipid;

    setMinSize(750, 500);
    var pnTitle = createTitlePane();
    metaDataScroll = createMetaDataPane();

    //Maybe wrap into Border Pane?
    lipidSpectrumChart = new LipidSpectrumChart(matchedLipid, new AtomicDouble(0d),
        RunOption.THIS_THREAD, false);

    // put into main
    ColumnConstraints ccSpectrum = new ColumnConstraints(400, -1, Region.USE_COMPUTED_SIZE,
        Priority.ALWAYS, HPos.CENTER, true);
    ColumnConstraints ccMetadata = new ColumnConstraints(META_WIDTH + 30, META_WIDTH + 30,
        Region.USE_COMPUTED_SIZE, Priority.NEVER, HPos.LEFT, false);

    add(pnTitle, 0, 0, 2, 1);
    add(lipidSpectrumChart, 0, 1);
    add(metaDataScroll, 1, 1);

    getColumnConstraints().add(0, ccSpectrum);
    getColumnConstraints().add(1, ccMetadata);
  }

  private Pane createTitlePane() {
    String styleWhiteScoreSmall = "white-score-label-small";
    // create Top panel
    double msMsScore = matchedLipid.getMsMsScore();
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

    Label ion = new Label("Ion notation: " + matchedLipid.getIonizationType().getAdductName() + " "
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

    Label rawFile = new Label(
        "Raw data file: " + matchedLipid.getMatchedFragments().stream().findFirst().get()
            .getMsMsScan().getDataFile().getName());
    rawFile.setWrapText(true);
    panelOther.getChildren().addAll(rawFile);

    Label matchedScan = new Label(
        "Matched Scan number: " + matchedLipid.getMatchedFragments().stream().findFirst().get()
            .getMsMsScan().getScanNumber());
    matchedScan.setWrapText(true);
    panelOther.getChildren().addAll(matchedScan);

    Label otherInfo = new Label("Lipid information");
    otherInfo.getStyleClass().add("bold-title-label");
    BorderPane pn = new BorderPane(panelOther);
    pn.setTop(otherInfo);
    return pn;
  }

}
