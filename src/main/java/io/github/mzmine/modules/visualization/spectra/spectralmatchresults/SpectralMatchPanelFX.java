package io.github.mzmine.modules.visualization.spectra.spectralmatchresults;

import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.chartbasics.listener.AxisRangeChangedListener;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra.MirrorScanWindowFX;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.util.color.ColorScaleUtil;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.javafx.FxColorUtil;
import io.github.mzmine.util.javafx.FxIconUtil;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralDBPeakIdentity;
import io.github.mzmine.util.swing.IconUtil;
import io.github.mzmine.util.swing.SwingExportUtil;
import it.unimi.dsi.fastutil.ints.Int2CharOpenHashMap;
import java.awt.Dimension;
import java.io.File;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
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

public class SpectralMatchPanelFX extends BorderPane {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private static final int ICON_WIDTH = 50;
  protected static final Image iconAll = FxIconUtil
      .loadImageFromResources("icons/exp_graph_all.png");
  protected static final Image iconPdf = FxIconUtil
      .loadImageFromResources("icons/exp_graph_pdf.png");
  protected static final Image iconEps = FxIconUtil
      .loadImageFromResources("icons/exp_graph_eps.png");
  protected static final Image iconEmf = FxIconUtil
      .loadImageFromResources("icons/exp_graph_emf.png");
  protected static final Image iconSvg = FxIconUtil
      .loadImageFromResources("icons/exp_graph_svg.png");

  private static final DecimalFormat COS_FORM = new DecimalFormat("0.000");

  private static Font font;

  public static final int META_WIDTH = 500;
  public static final int ENTRY_HEIGHT = 500;

  public static final double MIN_COS_COLOR_VALUE = 0.5;
  public static final double MAX_COS_COLOR_VALUE = 1.0;

  // min color is a darker red
  // max color is a darker green
  public static Color MAX_COS_COLOR = Color.web("0x388E3C");
  public static Color MIN_COS_COLOR = Color.web("0xE30B0B");

  private Font headerFont = new Font("Dialog", 16);
  private Font titleFont = new Font("Dialog", 18);
  private Font scoreFont = new Font("Dialog", 30);

  private EChartViewer mirrorChart;

  private boolean setCoupleZoomY;

  private XYPlot queryPlot;

  private XYPlot libraryPlot;

  private BorderPane pnMain;
  private VBox metaDataPanel;
  private GridPane boxTitlePanel;
  private GridPane pnExport;
  private BorderPane pnSpectrum;

  private Label lblScore;
  private Label lblHit;
  private java.awt.Font chartFont;

  public SpectralMatchPanelFX(SpectralDBPeakIdentity hit) {

    pnMain = this;

    metaDataPanel = new VBox();
    boxTitlePanel = new GridPane();
    boxTitlePanel.setAlignment(Pos.CENTER);

    // create Top panel
    double simScore = hit.getSimilarity().getScore();
    Color gradientCol = FxColorUtil.awtColorToFX(ColorScaleUtil
        .getColor(FxColorUtil.fxColorToAWT(MIN_COS_COLOR), FxColorUtil.fxColorToAWT(MAX_COS_COLOR),
            MIN_COS_COLOR_VALUE, MAX_COS_COLOR_VALUE, simScore));
    boxTitlePanel.setBackground(
        new Background(new BackgroundFill(gradientCol, CornerRadii.EMPTY, Insets.EMPTY)));

    lblHit = new Label(hit.getName());
    lblScore = new Label(COS_FORM.format(simScore));
    lblScore
        .setTooltip(new Tooltip("Cosine similarity of raw data scan (top, blue) and database scan: "
            + COS_FORM.format(simScore)));
    boxTitlePanel.add(lblHit, 0, 0);
    boxTitlePanel.add(lblScore, 1, 0);

    // preview panel
    IAtomContainer molecule;
    BorderPane pnPreview2D = new BorderPane();
    pnPreview2D.setPrefSize(META_WIDTH, 150);
    pnPreview2D.setMinSize(META_WIDTH, 150);
    pnPreview2D.setMaxSize(META_WIDTH, 150);

    pnExport = new GridPane(); // wrapped in additional pane before
    pnPreview2D.setRight(pnExport);

    // TODO add export Buttons

    Node newComponent = null;

    String inchiString = hit.getEntry().getField(DBEntryField.INCHI).orElse("N/A").toString();
    String smilesString = hit.getEntry().getField(DBEntryField.SMILES).orElse("N/A").toString();

    // check for INCHI
    if (inchiString != "N/A") {
      molecule = parseInChi(hit);
    }
    // check for smiles
    else if (smilesString != "N/A") {
      molecule = parseSmiles(hit);
    } else {
      molecule = null;
    }

    // try to draw the component
    if (molecule != null) {
      try {
        // newComponent = new Structure2DComponent(molecule, FONT);
      } catch (Exception e) {
        String errorMessage = "Could not load 2D structure\n" + "Exception: ";
        logger.log(Level.WARNING, errorMessage, e);
        newComponent = new Label(errorMessage);
        ((Label) newComponent).setWrapText(true);
      }
      pnPreview2D.setCenter(newComponent);

      metaDataPanel.getChildren().add(pnPreview2D);
    }

    ColumnConstraints columnConstraints1 = new ColumnConstraints();
    ColumnConstraints columnConstraints2 = new ColumnConstraints();
    columnConstraints1.setPercentWidth(0.5);
    columnConstraints2.setPercentWidth(0.5);

    GridPane g1 = new GridPane();
    BorderPane pnCompounds = extractMetaData("Compound information", hit.getEntry(),
        DBEntryField.COMPOUND_FIELDS);
    BorderPane panelInstrument =
        extractMetaData("Instrument information", hit.getEntry(), DBEntryField.INSTRUMENT_FIELDS);
    g1.add(pnCompounds, 0, 0);
    g1.add(panelInstrument, 1, 0);
//    g1.getColumnConstraints().add(0, columnConstraints1);
//    g1.getColumnConstraints().add(1, columnConstraints2);
    metaDataPanel.getChildren().add(g1); // TODO maybe put all info in one gridpane and add that?

    GridPane g2 = new GridPane();
    BorderPane pnDB =
        extractMetaData("Database links", hit.getEntry(), DBEntryField.DATABASE_FIELDS);
    BorderPane pnOther =
        extractMetaData("Other information", hit.getEntry(), DBEntryField.OTHER_FIELDS);
//    g2.add(pnDB, 0, 0);
//    g2.add(pnOther, 1, 0);
//    g2.getColumnConstraints().add(0, columnConstraints1);
//    g2.getColumnConstraints().add(1, columnConstraints2);
    g1.add(pnDB, 0, 1);
    g1.add(pnOther, 1, 1);

    metaDataPanel.getChildren().add(g2);

    MirrorScanWindowFX mirrorWindow = new MirrorScanWindowFX();
    mirrorWindow.setScans(hit);

    ScrollPane pnScroll = new ScrollPane(metaDataPanel);
    pnScroll.setFitToWidth(true);
    pnScroll.setHbarPolicy(ScrollBarPolicy.NEVER);
    pnScroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

    mirrorChart = mirrorWindow.getMirrorSpecrumPlot();
    mirrorChart = new EChartViewer(mirrorChart.getChart());

    pnSpectrum = new BorderPane();
    pnSpectrum.setCenter(mirrorChart);

    coupleZoomYListener();

    metaDataPanel.setPrefSize(META_WIDTH + 20, ENTRY_HEIGHT);

    // put into main
    pnMain.setTop(boxTitlePanel);
    pnMain.setCenter(pnSpectrum);
    pnMain.setRight(metaDataPanel);
    pnMain.setBorder(
        new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
            BorderWidths.DEFAULT)));

    applyTheme();

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
      if (!axis.getRange().equals(range)) {
        axis.setRange(range);
      }
      ValueAxis axisQuery = queryPlot.getRangeAxis();
      if (!axisQuery.getRange().equals(range)) {
        axisQuery.setRange(range);
      }
    }
  }

  public EChartViewer getMirrorChart() {
    return mirrorChart;
  }

  public void setCoupleZoomY(boolean selected) {
    setCoupleZoomY = selected;
  }

  public void applyTheme() {
    /*EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();

    this.chartFont = theme.getRegularFont();
    if (mirrorChart != null) {

      // add datasets and renderer
      // set up renderer
      CombinedDomainXYPlot domainPlot = (CombinedDomainXYPlot) mirrorChart.getChart().getXYPlot();
      NumberAxis axis = (NumberAxis) domainPlot.getDomainAxis();
      axis.setLabel("m/z");
      XYPlot queryPlot = (XYPlot) domainPlot.getSubplots().get(0);
      XYPlot libraryPlot = (XYPlot) domainPlot.getSubplots().get(1);
      domainPlot.getDomainAxis().setLabelFont(chartFont);
      domainPlot.getDomainAxis().setTickLabelFont(chartFont);
      queryPlot.getRangeAxis().setLabelFont(chartFont);
      queryPlot.getRangeAxis().setTickLabelFont(chartFont);
      libraryPlot.getRangeAxis().setLabelFont(chartFont);
      libraryPlot.getRangeAxis().setTickLabelFont(chartFont);
    }*/
  }

  private IAtomContainer parseInChi(SpectralDBPeakIdentity hit) {
    String inchiString = hit.getEntry().getField(DBEntryField.INCHI).orElse("N/A").toString();
    InChIGeneratorFactory factory;
    IAtomContainer molecule;
    if (inchiString != "N/A") {
      try {
        factory = InChIGeneratorFactory.getInstance();
        // Get InChIToStructure
        InChIToStructure inchiToStructure =
            factory.getInChIToStructure(inchiString, DefaultChemObjectBuilder.getInstance());
        molecule = inchiToStructure.getAtomContainer();
        return molecule;
      } catch (CDKException e) {
        String errorMessage = "Could not load 2D structure\n" + "Exception: ";
        logger.log(Level.WARNING, errorMessage, e);
        return null;
      }
    } else {
      return null;
    }
  }

  private IAtomContainer parseSmiles(SpectralDBPeakIdentity hit) {
    SmilesParser smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
    String smilesString = hit.getEntry().getField(DBEntryField.SMILES).orElse("N/A").toString();
    IAtomContainer molecule;
    if (smilesString != "N/A") {
      try {
        molecule = smilesParser.parseSmiles(smilesString);
        return molecule;
      } catch (InvalidSmilesException e1) {
        String errorMessage = "Could not load 2D structure\n" + "Exception: ";
        logger.log(Level.WARNING, errorMessage, e1);
        return null;
      }
    } else {
      return null;
    }
  }


  private BorderPane extractMetaData(String title, SpectralDBEntry entry, DBEntryField[] other) {
    VBox panelOther = new VBox();
    panelOther.setAlignment(Pos.TOP_LEFT);

    for (DBEntryField db : other) {
      Object o = entry.getField(db).orElse("N/A");
      if (!o.equals("N/A")) {
        Label text = new Label();
        text.setWrapText(true);
        text.setText(db.toString() + ": " + o.toString());
        panelOther.getChildren().add(text);
      }
    }

    Label otherInfo = new Label(title);
    otherInfo.setFont(headerFont);
    BorderPane pn = new BorderPane();
    pn.setTop(otherInfo);
    pn.setCenter(panelOther);
//    JPanel pn1 = new JPanel(new BorderLayout());
//    pn1.add(pn, BorderLayout.NORTH);
//    pn1.setBackground(java.awt.Color.WHITE);
//    return pn1;
    return pn;
  }

  public void applySettings(ParameterSet param) {
    pnExport.getChildren().removeAll();
    addExportButtons(param);
  }

  /**
   * @param param {@link SpectraIdentificationResultsParameters}
   */
  private void addExportButtons(ParameterSet param) {
    Button btnExport = null;

    if (param.getParameter(SpectraIdentificationResultsParameters.all).getValue()) {
      ImageView img = new ImageView(iconAll);
      img.setPreserveRatio(true);
      img.setFitWidth(ICON_WIDTH);
      btnExport = new Button(null, img);
      btnExport.setMaxSize(ICON_WIDTH + 6, ICON_WIDTH + 6);
      btnExport.setOnAction(e -> exportToGraphics("all"));
      pnExport.add(btnExport, 0, 0);
    }

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

    if (param.getParameter(SpectraIdentificationResultsParameters.svg).getValue()) {
      ImageView img = new ImageView(iconSvg);
      img.setPreserveRatio(true);
      img.setFitWidth(ICON_WIDTH);
      btnExport = new Button(null, img);
      btnExport.setMaxSize(ICON_WIDTH + 6, ICON_WIDTH + 6);
      btnExport.setOnAction(e -> exportToGraphics("svg"));
      pnExport.add(btnExport, 0, 4);
    }
  }

  public void exportToGraphics(String format) {
    // old path
    FileNameParameter param =
        MZmineCore.getConfiguration().getModuleParameters(SpectraIdentificationResultsModule.class)
            .getParameter(SpectraIdentificationResultsParameters.file);
    final FileChooser chooser;
    if (param.getValue() != null) {
      chooser = new FileChooser();
      chooser.setInitialDirectory(param.getValue().getParentFile());
    } else {
      chooser = new FileChooser();
    }
    // get file
    File f = chooser.showSaveDialog(null);
    if (f != null) {
      try {
        pnExport.setVisible(false);
//        pnExport.revalidate();
//        pnExport.getParent().revalidate();
//        pnExport.getParent().repaint();
        JFXPanel pn = new JFXPanel(); // I know this is dirty but I'm lazy
        pn.setScene(new Scene(this));
        SwingExportUtil.writeToGraphics(pn, f.getParentFile(), f.getName(), format);
        // save path
        param.setValue(FileAndPathUtil.eraseFormat(f));
      } catch (Exception ex) {
        logger.log(Level.WARNING, "Cannot export graphics of spectra match panel", ex);
      } finally {
        pnExport.setVisible(true);
//        pnExport.getParent().revalidate();
//        pnExport.getParent().repaint();
      }
    }
  }

}




































