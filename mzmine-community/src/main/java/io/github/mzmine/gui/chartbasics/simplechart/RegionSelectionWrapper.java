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

package io.github.mzmine.gui.chartbasics.simplechart;

import static io.github.mzmine.javafx.components.factories.FxButtons.createButton;
import static io.github.mzmine.javafx.components.factories.FxButtons.createCancelButton;
import static io.github.mzmine.javafx.components.factories.FxButtons.createLoadButton;
import static io.github.mzmine.javafx.components.factories.FxButtons.createSaveButton;
import static io.github.mzmine.javafx.components.util.FxLayout.newAccordion;
import static io.github.mzmine.javafx.components.util.FxLayout.newFlowPane;
import static io.github.mzmine.javafx.components.util.FxLayout.newTitledPane;

import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.RegionSelectionListener;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYItemObjectProvider;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.parametertypes.RegionsParameter;
import io.github.mzmine.util.XMLUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javax.validation.constraints.NotNull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.data.xy.XYDataset;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * Wraps around a {@link EChartViewer} and adds controls to select regions of interest.
 *
 * @param <T>
 * @author https://github.com/SteffenHeu
 */
public class RegionSelectionWrapper<T extends EChartViewer> extends BorderPane {

  private static final Logger logger = Logger.getLogger(RegionSelectionWrapper.class.getName());
  public static final String REGION_FILE_EXTENSION = "*.mzmineregionxml";

  private final T node;
  private final ListProperty<RegionSelectionListener> finishedRegionSelectionListenersProperty = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final Stroke roiStroke = new BasicStroke(1f);
  private final Paint roiPaint = MZmineCore.getConfiguration().getDefaultColorPalette()
      .getNegativeColorAWT();
  private FlowPane controlPane;

  private RegionSelectionListener currentRegionListener = null;
  private XYShapeAnnotation currentRegionAnnotation;
  private final BooleanProperty isDrawingRegion = new SimpleBooleanProperty(false);

  public RegionSelectionWrapper(@NotNull final T node) {
    this(node, null);
  }

  /**
   * @param node The chart
   */
  public RegionSelectionWrapper(T node, Consumer<List<List<Point2D>>> onExtractPressed) {
    this.node = node;
    setCenter(node);

    final Button btnSaveToFile = createSaveButton(this::saveRegionsToFile);
    final Button btnLoadFromFile = createLoadButton(this::loadRegionsFromFile);
    var regionDrawingButtons = createSelectionButtons();

    controlPane = newFlowPane(Pos.TOP_CENTER);
    controlPane.getChildren()
        .addAll(btnSaveToFile, btnLoadFromFile, new Separator(Orientation.VERTICAL));
    controlPane.getChildren().addAll(regionDrawingButtons);

    setBottom(
        newAccordion(false, newTitledPane("Regions of interest (ROI) selection", controlPane)));

    if (onExtractPressed != null) {
      final Button extractButton = FxButtons.createButton("Extract to feature list", FxIcons.FILTER,
          null, () -> onExtractPressed.accept(getFinishedRegionsAsListOfPointLists()));
      extractButton.disableProperty()
          .bind(finishedRegionSelectionListenersProperty().emptyProperty());
      controlPane.getChildren().add(extractButton);
    }
  }

  /**
   * Initializes a {@link RegionSelectionListener} and adds it to the plot. Following clicks will be
   * added to a region. Region selection can be finished by
   * {@link SimpleXYZScatterPlot#finishPath()}.
   */
  public void startRegion() {
    isDrawingRegion.set(true);

    if (currentRegionListener != null) {
      node.removeChartMouseListener(currentRegionListener);
    }
    currentRegionListener = new RegionSelectionListener(node);
    currentRegionListener.pathProperty().addListener(((observable, oldValue, newValue) -> {
      if (currentRegionAnnotation != null) {
        node.getChart().getXYPlot().removeAnnotation(currentRegionAnnotation, false);
      }
      Color regionColor = new Color(0.6f, 0.6f, 0.6f, 0.4f);
      currentRegionAnnotation = new XYShapeAnnotation(newValue, new BasicStroke(1f), regionColor,
          regionColor);
      node.getChart().getXYPlot().addAnnotation(currentRegionAnnotation, true);
    }));
    node.addChartMouseListener(currentRegionListener);
  }

  /**
   * The {@link RegionSelectionListener} of the current selection. The path/points can be retrieved
   * from the listener object.
   *
   * @return The finished listener
   */
  public RegionSelectionListener finishPath() {
    if (!isDrawingRegion.get()) {
      return null;
    }
    if (currentRegionAnnotation != null) {
      node.getChart().getXYPlot().removeAnnotation(currentRegionAnnotation);
    }
    isDrawingRegion.set(false);
    node.removeChartMouseListener(currentRegionListener);
    RegionSelectionListener tempRegionListener = currentRegionListener;
    currentRegionListener = null;
    return tempRegionListener;
  }

  public ListProperty<RegionSelectionListener> finishedRegionSelectionListenersProperty() {
    return finishedRegionSelectionListenersProperty;
  }

  public ObservableList<RegionSelectionListener> getFinishedRegionSelectionListeners() {
    return finishedRegionSelectionListenersProperty.get();
  }

  public List<List<Point2D>> getFinishedRegionsAsListOfPointLists() {
    return finishedRegionSelectionListenersProperty.stream()
        .map(l -> l.buildingPointsProperty().get()).collect(Collectors.toList());
  }

  public List<Path2D> getFinishedRegionsAsListOfPaths() {
    return finishedRegionSelectionListenersProperty.stream().map(l -> l.pathProperty().get())
        .collect(Collectors.toList());
  }

  private List<Button> createSelectionButtons() {
    final SimpleBooleanProperty disableFinish = new SimpleBooleanProperty(true);
    final Button btnFinishRegion = createButton("Finish", FxIcons.CHECK_CIRCLE, null, () -> {
      RegionSelectionListener selection = finishPath();
      if (selection.buildingPointsProperty().getSize() > 3) {
        finishedRegionSelectionListenersProperty.add(selection);
      }
      disableFinish.set(true);
    });
    btnFinishRegion.disableProperty().bind(disableFinish);

    final Button btnStartRegion = createButton("Start", FxIcons.DRAW_REGION, null, () -> {
      startRegion();
      disableFinish.set(false);
    });

    final Button btnCancelRegion = createCancelButton(() -> {
      finishPath();
      disableFinish.set(true);
    });

    final Button btnClearRegions = createButton("Clear", FxIcons.CLEAR, null, () -> {
      final List<XYAnnotation> annotations = node.getChart().getXYPlot().getAnnotations();
      new ArrayList<>(annotations).forEach(
          a -> node.getChart().getXYPlot().removeAnnotation(a, true));
      finishedRegionSelectionListenersProperty.clear();
      disableFinish.set(true);
    });
    btnClearRegions.setDisable(true);

    finishedRegionSelectionListenersProperty.addListener(
        (ListChangeListener<RegionSelectionListener>) c -> {
          c.next();
          if (c.wasRemoved()) {
            boolean disable = c.getList().isEmpty();
            btnClearRegions.setDisable(disable);
          }
          if (c.wasAdded()) {
            node.getChart().getXYPlot().addAnnotation(
                new XYShapeAnnotation(c.getAddedSubList().get(0).pathProperty().get(), roiStroke,
                    roiPaint));
            btnClearRegions.setDisable(false);
          }
        });

    return List.of(btnStartRegion, btnFinishRegion, btnCancelRegion, btnClearRegions);
  }

  public FlowPane getControlPane() {
    return controlPane;
  }

  private void loadRegionsFromFile() {
    FxThread.runLater(() -> {
      final FileChooser chooser = new FileChooser();
      chooser.getExtensionFilters()
          .add(new ExtensionFilter("mzmine regions file", REGION_FILE_EXTENSION));
      final File file = chooser.showOpenDialog(MZmineCore.getDesktop().getMainWindow());
      var regions = loadRegionsFromFile(file);

      for (List<Point2D> point2DS : regions) {
        final RegionSelectionListener l = new RegionSelectionListener(node);
        l.buildingPointsProperty().setValue(FXCollections.observableArrayList(point2DS));
        finishedRegionSelectionListenersProperty.add(l);
      }
    });
  }

  @NotNull
  public static List<List<Point2D>> loadRegionsFromFile(File file) {
    if (file == null || !file.exists() || !file.canRead()) {
      return List.of();
    }
    List<List<Point2D>> regions = new ArrayList<>();

    try {
      final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      final Document regionsFile = dBuilder.parse(file);
      final XPathFactory factory = XPathFactory.newInstance();
      final XPath xpath = factory.newXPath();

      final XPathExpression expr = xpath.compile("//root");
      final NodeList nodes = (NodeList) expr.evaluate(regionsFile, XPathConstants.NODESET);

      for (int i = 0; i < nodes.getLength(); i++) {
        final RegionsParameter param = new RegionsParameter("regions", "User defined regions");
        param.loadValueFromXML((Element) nodes.item(i));

        regions.addAll(param.getValue());
      }
    } catch (ParserConfigurationException | IOException | SAXException |
             XPathExpressionException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }
    return regions;
  }

  private void saveRegionsToFile() {
    final RegionsParameter parameter = new RegionsParameter("Regions", "User defined regions");
    finishedRegionSelectionListenersProperty.forEach(
        l -> parameter.getValue().add(l.buildingPointsProperty().getValue()));
    FxThread.runLater(() -> {
      final FileChooser chooser = new FileChooser();
      chooser.getExtensionFilters()
          .add(new ExtensionFilter("MZmine regions file", REGION_FILE_EXTENSION));
      final File file = chooser.showSaveDialog(MZmineCore.getDesktop().getMainWindow());
      if (file == null) {
        return;
      }
      try {
        final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document doc = builder.newDocument();
        final Element root = doc.createElement("root");
        doc.appendChild(root);
        final Element element = doc.createElement(parameter.getName().toLowerCase());
        parameter.saveValueToXML(element);
        root.appendChild(element);

        XMLUtils.saveToFile(file, doc);
      } catch (ParserConfigurationException | IOException | TransformerException e) {
        e.printStackTrace();
      }
    });
  }

  public static <V, D extends XYDataset & XYItemObjectProvider<V>> boolean isItemInRegion(int item,
      int series, @NotNull D dataset, @NotNull RegionSelectionListener region) {
    if (item > dataset.getItemCount(series)) {
      return false;
    }

    final double x = dataset.getXValue(series, item);
    final double y = dataset.getYValue(series, item);

    final Path2D path = region.pathProperty().get();
    if (path == null) {
      return false;
    }
    return path.contains(x, y);
  }
}
