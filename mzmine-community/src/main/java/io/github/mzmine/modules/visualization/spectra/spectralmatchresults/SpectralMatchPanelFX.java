/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.spectra.spectralmatchresults;

import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.chartbasics.listener.AxisRangeChangedListener;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.molstructure.Structure2DComponent;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.util.MirrorChartFactory;
import io.github.mzmine.util.color.ColorScaleUtil;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.io.ClipboardWriter;
import io.github.mzmine.util.javafx.FxColorUtil;
import io.github.mzmine.util.javafx.FxIconUtil;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.DataPointsTag;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.awt.Dimension;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.controlsfx.control.Notifications;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;

public class SpectralMatchPanelFX extends GridPane {

  public static final int META_WIDTH = 500;
  public static final int ENTRY_HEIGHT = 400;
  public static final int STRUCTURE_HEIGHT = 150;
  public static final double MIN_COS_COLOR_VALUE = 0.5;
  public static final double MAX_COS_COLOR_VALUE = 1.0;
  protected static final Image iconAll = FxIconUtil.loadImageFromResources(
      "icons/exp_graph_all.png");
  protected static final Image iconPdf = FxIconUtil.loadImageFromResources(
      "icons/exp_graph_pdf.png");
  protected static final Image iconEps = FxIconUtil.loadImageFromResources(
      "icons/exp_graph_eps.png");
  protected static final Image iconEmf = FxIconUtil.loadImageFromResources(
      "icons/exp_graph_emf.png");
  protected static final Image iconSvg = FxIconUtil.loadImageFromResources(
      "icons/exp_graph_svg.png");
  private static final int ICON_WIDTH = 50;
  private static final DecimalFormat COS_FORM = new DecimalFormat("0.000");
  // min color is a darker red
  // max color is a darker green
  public static Color MAX_COS_COLOR = Color.web("0x388E3C");
  public static Color MIN_COS_COLOR = Color.web("0xE30B0B");
  private static Font font;
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final EChartViewer mirrorChart;
  private final SpectralDBAnnotation hit;
  private final BorderPane mirrorChartWrapper;
  private final EStandardChartTheme theme;
  private boolean setCoupleZoomY;
  private XYPlot queryPlot;
  private XYPlot libraryPlot;
  private VBox metaDataPanel;
  private ScrollPane metaDataScroll;
  private GridPane pnExport;
  private Label lblScore;
  private Label lblHit;

  public SpectralMatchPanelFX(SpectralDBAnnotation hit) {
    super();

    this.hit = hit;

    setMinSize(750, 200);

    theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();

    MAX_COS_COLOR = palette.getPositiveColor();
    MIN_COS_COLOR = palette.getNegativeColor();

    var pnTitle = createTitlePane();

    metaDataScroll = createMetaDataPane();

    mirrorChart = MirrorChartFactory.createMirrorPlotFromSpectralDBPeakIdentity(hit);
    MZmineCore.getConfiguration().getDefaultChartTheme().apply(mirrorChart.getChart());
    mirrorChartWrapper = new BorderPane();
    mirrorChartWrapper.setCenter(mirrorChart);

    coupleZoomYListener();

    // put into main
    ColumnConstraints ccSpectrum = new ColumnConstraints(400, -1, Region.USE_COMPUTED_SIZE,
        Priority.ALWAYS, HPos.CENTER, true);
    ColumnConstraints ccMetadata = new ColumnConstraints(META_WIDTH + 30, META_WIDTH + 30,
        Region.USE_COMPUTED_SIZE, Priority.NEVER, HPos.LEFT, false);

    add(pnTitle, 0, 0, 2, 1);
    add(mirrorChartWrapper, 0, 1);
    add(metaDataScroll, 1, 1);

    getColumnConstraints().add(0, ccSpectrum);
    getColumnConstraints().add(1, ccMetadata);

//    setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
//        BorderWidths.DEFAULT)));
  }

  private Pane createTitlePane() {
    String explIntTooltip = "Explained library intensity for matched library signals divided by all library signal intensities";
    String matchedSignalsTooltip = "Matched library signals divided by all library signals";

    String styleWhiteScoreSmall = "white-score-label-small";
    // create Top panel
    double simScore = hit.getSimilarity().getScore();
    Color gradientCol = FxColorUtil.awtColorToFX(
        ColorScaleUtil.getColor(FxColorUtil.fxColorToAWT(MIN_COS_COLOR),
            FxColorUtil.fxColorToAWT(MAX_COS_COLOR), MIN_COS_COLOR_VALUE, MAX_COS_COLOR_VALUE,
            simScore));

    lblHit = createLabel(hit.getCompoundName(), "white-larger-label");

    String simScoreTooltip =
        "Cosine similarity of raw data scan (top, blue) and database scan: " + COS_FORM.format(
            simScore);
    lblScore = createLabel(COS_FORM.format(simScore), simScoreTooltip, "white-score-label");

    var totalSignals = hit.getLibraryDataPoints(DataPointsTag.FILTERED).length;
    var overlap = hit.getSimilarity().getOverlap();
    var lblMatched = createLabel("%d / %d".formatted(overlap, totalSignals), matchedSignalsTooltip,
        styleWhiteScoreSmall);

    var intensity = hit.getSimilarity().getExplainedLibraryIntensity();
    var lblExplained = createLabel(COS_FORM.format(intensity), explIntTooltip,
        styleWhiteScoreSmall);

    lblExplained.getStyleClass().add(styleWhiteScoreSmall);

    var leftScores = new VBox(0, lblMatched, lblExplained);
    leftScores.setAlignment(Pos.CENTER);

    var scoreDef = new VBox(0,
        createLabel("Matched signals:", matchedSignalsTooltip, styleWhiteScoreSmall),
        createLabel("Expl. intensity:", explIntTooltip, styleWhiteScoreSmall));
    scoreDef.setAlignment(Pos.CENTER_RIGHT);

    var scoreBox = new HBox(5, scoreDef, leftScores, lblScore);
    scoreBox.setPadding(new Insets(0, 5, 0, 10));
    scoreBox.setAlignment(Pos.CENTER);

    var titlePane = new BorderPane(lblHit);
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

    // preview panel
    BorderPane pnPreview2D = new BorderPane();
    pnPreview2D.getStyleClass().add("region");
    pnPreview2D.setPrefSize(META_WIDTH, STRUCTURE_HEIGHT);
    pnPreview2D.setMinSize(META_WIDTH, STRUCTURE_HEIGHT);
    pnPreview2D.setMaxSize(META_WIDTH, STRUCTURE_HEIGHT);

    // TODO! - Export functionality for Java FX nodes
    pnExport = new GridPane(); // wrapped in additional pane before
    pnExport.getStyleClass().add("region");

    pnPreview2D.setRight(pnExport);
    addExportButtons(MZmineCore.getConfiguration()
        .getModuleParameters(SpectraIdentificationResultsModule.class));

    Node newComponent = null;

    // check for INCHI
    IAtomContainer molecule = parseStructure(hit);

    // try to draw the component
    if (molecule != null) {
      try {
        newComponent = new Structure2DComponent(molecule, theme.getRegularFont());
      } catch (Exception e) {
        String errorMessage = "Could not load 2D structure\n" + "Exception: ";
        logger.log(Level.WARNING, errorMessage, e);
        newComponent = new Label(errorMessage);
        ((Label) newComponent).setWrapText(true);
      }
      pnPreview2D.setCenter(newComponent);

      metaDataPanel.getChildren().add(pnPreview2D);
    }

    ColumnConstraints ccMetadata1 = new ColumnConstraints(META_WIDTH / 2d, -1, Double.MAX_VALUE,
        Priority.ALWAYS, HPos.LEFT, false);
    ColumnConstraints ccMetadata2 = new ColumnConstraints(META_WIDTH / 2d, -1, Double.MAX_VALUE,
        Priority.ALWAYS, HPos.LEFT, false);
    ccMetadata1.setPercentWidth(50);
    ccMetadata2.setPercentWidth(50);

    GridPane g1 = new GridPane();
//    g1.getStyleClass().add("region");

    BorderPane pnCompounds = extractMetaData("Compound information", hit.getEntry(),
        DBEntryField.COMPOUND_FIELDS);
    BorderPane panelInstrument = extractMetaData("Instrument information", hit.getEntry(),
        DBEntryField.INSTRUMENT_FIELDS);

    BorderPane pnDB = extractMetaData("Database links", hit.getEntry(),
        DBEntryField.DATABASE_FIELDS);
    BorderPane pnOther = extractMetaData("Other information", hit.getEntry(),
        DBEntryField.OTHER_FIELDS);
    BorderPane pnLib = extractLibraryPanel(hit.getEntry());

    var leftBox = new VBox(4, pnCompounds);
    leftBox.setPadding(Insets.EMPTY);
    var rightBox = new VBox(4, panelInstrument, pnOther, pnDB, pnLib);
    rightBox.setPadding(new Insets(0, 0, 0, 15));
    g1.getColumnConstraints().add(0, ccMetadata1);
    g1.getColumnConstraints().add(1, ccMetadata2);
    g1.add(leftBox, 0, 0);
    g1.add(rightBox, 1, 0);

    metaDataPanel.getChildren().add(g1);
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

  private BorderPane extractLibraryPanel(SpectralLibraryEntry entry) {
    final Label library = new Label("Spectral library: " + entry.getLibraryName());
    return new BorderPane(library);
  }

  private IAtomContainer parseStructure(final SpectralDBAnnotation hit) {
    String inchiString = hit.getEntry().getField(DBEntryField.INCHI).orElse("n/a").toString();
    String smilesString = hit.getEntry().getField(DBEntryField.SMILES).orElse("n/a").toString();
    if (!inchiString.equalsIgnoreCase("n/a") && !inchiString.isBlank()) {
      var molecule = parseInChi(hit);
      if (molecule != null) {
        return molecule;
      }
    }
    // check for smiles
    if (!smilesString.equalsIgnoreCase("n/a") && !smilesString.isBlank()) {
      var molecule = parseSmiles(hit);
      if (molecule != null) {
        return molecule;
      }
    }
    return null;
  }

  private void coupleZoomYListener() {
    CombinedDomainXYPlot domainPlot = (CombinedDomainXYPlot) mirrorChart.getChart().getXYPlot();
    NumberAxis axis = (NumberAxis) domainPlot.getDomainAxis();
    axis.setLabel("m/z");
    queryPlot = (XYPlot) domainPlot.getSubplots().get(0);
    libraryPlot = (XYPlot) domainPlot.getSubplots().get(1);
    queryPlot.getRangeAxis().addChangeListener(new AxisRangeChangedListener(null) {
      @Override
      public void axisRangeChanged(ChartViewWrapper chart, ValueAxis axis, Range lastR,
          Range newR) {
        rangeHasChanged(newR);
      }
    });
    libraryPlot.getRangeAxis().addChangeListener(new AxisRangeChangedListener(null) {
      @Override
      public void axisRangeChanged(ChartViewWrapper chart, ValueAxis axis, Range lastR,
          Range newR) {
        rangeHasChanged(newR);
      }
    });
  }

  /**
   * Apply changes to all other charts
   *
   * @param range
   */
  private void rangeHasChanged(Range range) {
    if (setCoupleZoomY) {
      ValueAxis axis = libraryPlot.getRangeAxis();
      ValueAxis axisQuery = queryPlot.getRangeAxis();
      // is this range still active or was it changed again?
      final Range axisRange = axis.getRange();
      final Range queryRange = axisQuery.getRange();
      if (axisRange.equals(range) ^ queryRange.equals(range)) {
        if (!axisRange.equals(range)) {
          axis.setRange(range);
        }
        if (!queryRange.equals(range)) {
          axisQuery.setRange(range);
        }
      }
    }
  }

  public EChartViewer getMirrorChart() {
    return mirrorChart;
  }

  public void setCoupleZoomY(boolean selected) {
    setCoupleZoomY = selected;
  }

  private IAtomContainer parseInChi(SpectralDBAnnotation hit) {
    String inchiString = hit.getEntry().getField(DBEntryField.INCHI).orElse("n/a").toString();
    InChIGeneratorFactory factory;
    IAtomContainer molecule;
    if (inchiString.equalsIgnoreCase("n/a") || inchiString.isBlank()) {
      return null;
    }
    try {
      factory = InChIGeneratorFactory.getInstance();
      // Get InChIToStructure
      InChIToStructure inchiToStructure = factory.getInChIToStructure(inchiString,
          DefaultChemObjectBuilder.getInstance());
      molecule = inchiToStructure.getAtomContainer();
      return molecule;
    } catch (CDKException e) {
      String errorMessage = "Could not load 2D structure\n" + "Exception: ";
      logger.log(Level.WARNING, errorMessage, e);
      return null;
    }
  }

  private IAtomContainer parseSmiles(SpectralDBAnnotation hit) {
    SmilesParser smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
    String smilesString = hit.getEntry().getField(DBEntryField.SMILES).orElse("n/a").toString();
    IAtomContainer molecule;
    if (smilesString.equalsIgnoreCase("n/a") || smilesString.isBlank()) {
      return null;
    }
    try {
      molecule = smilesParser.parseSmiles(smilesString);
      return molecule;
    } catch (InvalidSmilesException e1) {
      String errorMessage = "Could not load 2D structure\n" + "Exception: ";
      logger.log(Level.WARNING, errorMessage, e1);
      return null;
    }
  }


  private BorderPane extractMetaData(String title, SpectralLibraryEntry entry,
      DBEntryField[] other) {
    VBox panelOther = new VBox();
    panelOther.setAlignment(Pos.TOP_LEFT);

    for (DBEntryField db : other) {
      String o = entry.getField(db).orElse("n/a").toString();
      if (!o.equalsIgnoreCase("n/a")) {
        Label text = new Label(db.toString() + ": " + o);
        text.setWrapText(true);
        text.setOnMouseClicked(event -> {
          ClipboardWriter.writeToClipBoard(o);
          Notifications.create().title("Copied to clipboard").hideAfter(new Duration(2500))
              .owner(MZmineCore.getDesktop().getMainWindow()).show();

          // Other option for overlay
//          var popOver = new PopOver();
//          popOver.setContentNode(new Label("Copied to clipboard"));
//          popOver.setAutoHide(true);
//          popOver.setAutoFix(true);
//          popOver.setHideOnEscape(true);
//          popOver.setDetachable(true);
//          popOver.setDetached(false);
//          popOver.setArrowLocation(ArrowLocation.LEFT_BOTTOM);
//
//          PauseTransition pause = new PauseTransition(Duration.seconds(2.5));
//          pause.setOnFinished(e -> popOver.hide());
//          pause.play();
//
//          popOver.show(text);

        });

        panelOther.getChildren().addAll(text);
      }
    }

    Label otherInfo = new Label(title);
    otherInfo.getStyleClass().add("bold-title-label");
    BorderPane pn = new BorderPane(panelOther);
    pn.setTop(otherInfo);
    return pn;
  }

  public void applySettings(@Nullable ParameterSet param) {
    pnExport.getChildren().clear();
    if (param == null) {
      return;
    }
    addExportButtons(param);
  }

  /**
   * @param param {@link SpectraIdentificationResultsParameters}
   */
  private void addExportButtons(ParameterSet param) {
    if (param == null) {
      return;
    }
    Button btnExport = null;

    // TODO does not work - so remove
    //    if (true) {
    //      return;
    //    }

    //    if (param.getParameter(SpectraIdentificationResultsParameters.all).getValue()) {
    //      ImageView img = new ImageView(iconAll);
    //      img.setPreserveRatio(true);
    //      img.setFitWidth(ICON_WIDTH);
    //      btnExport = new Button(null, img);
    //      btnExport.setMaxSize(ICON_WIDTH + 6, ICON_WIDTH + 6);
    //      btnExport.setOnAction(e -> exportToGraphics("all"));
    //      pnExport.add(btnExport, 0, 0);
    //    }

    if (param.getParameter(SpectraIdentificationResultsParameters.pdf).getValue()) {
      ImageView img = new ImageView(iconPdf);
      img.setPreserveRatio(true);
      img.setFitWidth(ICON_WIDTH);
      btnExport = new Button(null, img);
      btnExport.setMaxSize(ICON_WIDTH + 6, ICON_WIDTH + 6);
      btnExport.setOnAction(e -> exportToGraphics("pdf"));
      pnExport.add(btnExport, 0, 1);
    }

    if (param.getParameter(SpectraIdentificationResultsParameters.emf).getValue()) {
      ImageView img = new ImageView(iconEmf);
      img.setPreserveRatio(true);
      img.setFitWidth(ICON_WIDTH);
      btnExport = new Button(null, img);
      btnExport.setMaxSize(ICON_WIDTH + 6, ICON_WIDTH + 6);
      btnExport.setOnAction(e -> exportToGraphics("emf"));
      pnExport.add(btnExport, 0, 2);
    }

    if (param.getParameter(SpectraIdentificationResultsParameters.eps).getValue()) {
      ImageView img = new ImageView(iconEps);
      img.setPreserveRatio(true);
      img.setFitWidth(ICON_WIDTH);
      btnExport = new Button(null, img);
      btnExport.setMaxSize(ICON_WIDTH + 6, ICON_WIDTH + 6);
      btnExport.setOnAction(e -> exportToGraphics("eps"));
      pnExport.add(btnExport, 0, 3);
    }

    //TODO SVG broken somehow
    //    if (param.getParameter(SpectraIdentificationResultsParameters.svg).getValue()) {
    //      ImageView img = new ImageView(iconSvg);
    //      img.setPreserveRatio(true);
    //      img.setFitWidth(ICON_WIDTH);
    //      btnExport = new Button(null, img);
    //      btnExport.setMaxSize(ICON_WIDTH + 6, ICON_WIDTH + 6);
    //      btnExport.setOnAction(e -> exportToGraphics("svg"));
    //      pnExport.add(btnExport, 0, 4);
    //    }
  }

  /**
   * Please don't look into this method.
   *
   * @param format The format specifier to export this node to.
   */
  public void exportToGraphics(String format) {

    // old path
    FileNameParameter param = MZmineCore.getConfiguration()
        .getModuleParameters(SpectraIdentificationResultsModule.class)
        .getParameter(SpectraIdentificationResultsParameters.file);
    final FileChooser chooser;
    if (param.getValue() != null) {
      chooser = new FileChooser();
      chooser.setInitialDirectory(param.getValue().getParentFile());
    } else {
      chooser = new FileChooser();
    }

    // this is so unbelievably dirty
    // i'm so sorry ~SteffenHeu
    final JFrame[] frame = new JFrame[1];
    logger.info("Creating dummy window for spectral match export...");
    SpectralMatchPanel[] swingPanel = new SpectralMatchPanel[1];
    SwingUtilities.invokeLater(() -> {
      frame[0] = new JFrame();
      swingPanel[0] = new SpectralMatchPanel(hit);
      frame[0].setContentPane(swingPanel[0]);
      frame[0].revalidate();
      frame[0].setVisible(true);
      frame[0].toBack();
      swingPanel[0].calculateAndSetSize();
    });

    // get file
    File file = chooser.showSaveDialog(null);
    if (file != null) {
      swingPanel[0].exportToGraphics(format, file);
    }

    logger.info("Disposing dummy window for spectral match export...");
    SwingUtilities.invokeLater(() -> frame[0].dispose());

    // it works though, until we figure something out
  }

  /**
   * Please don't look into this method. Used for batch exporting
   *
   * @param format The format specifier to export this node to.
   */
  public Dimension exportToGraphics(String format, File path)
      throws InterruptedException, InvocationTargetException {

    // this is so unbelievably dirty
    // i'm so sorry ~SteffenHeu
    AtomicReference<Dimension> dim = new AtomicReference<>();
    logger.info("Creating dummy window for spectral match export...");
    SwingUtilities.invokeAndWait(() -> {
      final var frame = new JFrame();
      var swingPanel = new SpectralMatchPanel(hit);
      frame.setContentPane(swingPanel);
      frame.revalidate();
      frame.setVisible(true);
      frame.toBack();
      swingPanel.calculateAndSetSize();
      swingPanel.exportToGraphics(format, path);
      dim.set(swingPanel.getSize());
      logger.info("Disposing dummy window for spectral match export...");
      frame.dispose();
    });

    // it works though, until we figure something out
    return dim.get();
  }

  public SpectralDBAnnotation getHit() {
    return hit;
  }

}




































