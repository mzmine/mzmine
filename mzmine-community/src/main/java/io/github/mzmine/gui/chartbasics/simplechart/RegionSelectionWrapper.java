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

package io.github.mzmine.gui.chartbasics.simplechart;

import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.RegionSelectionListener;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.parametertypes.RegionsParameter;
import io.github.mzmine.util.XMLUtils;
import java.awt.BasicStroke;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * Wraps around a {@link AllowsRegionSelection} and adds controls to select regions of interest.
 *
 * @param <T>
 * @author https://github.com/SteffenHeu
 */
public class RegionSelectionWrapper<T extends EChartViewer & AllowsRegionSelection> extends
    BorderPane {

  private static final String REGION_FILE_EXTENSION = "*.mzmineregionxml";

  final GridPane selectionControls;
  final FlowPane importExportControls;
  final VBox bottomWrap;
  private final T node;
  private final ObservableList<RegionSelectionListener> finishedRegionSelectionListeners;
  private final Stroke roiStroke = new BasicStroke(1f);
  private final Paint roiPaint = MZmineCore.getConfiguration().getDefaultColorPalette()
      .getNegativeColorAWT();

  public RegionSelectionWrapper(T node) {
    this.node = node;
    setCenter(node);
    selectionControls = new GridPane();
    importExportControls = new FlowPane();

    final Button btnSaveToFile = new Button("Save regions");
    btnSaveToFile.setOnAction(e -> saveRegionsToFile());
    final Button btnLoadFromFile = new Button("Load regions");
    btnLoadFromFile.setOnAction(e -> loadRegionsFromFile());

    importExportControls.getChildren().addAll(btnSaveToFile, btnLoadFromFile);
    importExportControls.setHgap(5);
    importExportControls.setAlignment(Pos.TOP_CENTER);

    bottomWrap = new VBox();
    bottomWrap.getChildren().addAll(selectionControls, importExportControls);
    setBottom(bottomWrap);
    bottomWrap.setAlignment(Pos.TOP_CENTER);

    finishedRegionSelectionListeners = FXCollections.observableArrayList();
    initSelectionPane();
  }

  public ObservableList<RegionSelectionListener> getFinishedRegionListeners() {
    return finishedRegionSelectionListeners;
  }

  public List<List<Point2D>> getFinishedRegionsAsListOfPointLists() {
    return finishedRegionSelectionListeners.stream().map(l -> l.buildingPointsProperty().get())
        .collect(Collectors.toList());
  }

  public List<Path2D> getFinishedRegionsAsListOfPaths() {
    return finishedRegionSelectionListeners.stream().map(l -> l.buildingPathProperty().get())
        .collect(Collectors.toList());
  }

  private void initSelectionPane() {
    final Button btnStartRegion = new Button("Start region");
    final Button btnFinishRegion = new Button("Finish region");
    final Button btnCancelRegion = new Button("Cancel region");
    final Button btnClearRegions = new Button("Clear regions");

    btnStartRegion.setOnAction(e -> {
      node.startRegion();
      btnFinishRegion.setDisable(false);
    });

    btnFinishRegion.setOnAction(e -> {
      RegionSelectionListener selection = node.finishPath();
      if (selection.buildingPointsProperty().getSize() > 3) {
        finishedRegionSelectionListeners.add(selection);
      }
      btnFinishRegion.setDisable(true);
    });
    btnFinishRegion.setDisable(true);

    btnCancelRegion.setOnAction(e -> {
      node.finishPath();
      btnFinishRegion.setDisable(true);
    });

    btnClearRegions.setDisable(true);
    btnClearRegions.setOnAction(e -> {
      final List<XYAnnotation> annotations = node.getChart().getXYPlot().getAnnotations();
      new ArrayList<>(annotations).forEach(
          a -> node.getChart().getXYPlot().removeAnnotation(a, true));
      finishedRegionSelectionListeners.clear();
      btnFinishRegion.setDisable(true);
    });

    finishedRegionSelectionListeners.addListener(
        (ListChangeListener<RegionSelectionListener>) c -> {
          c.next();
          if (c.wasRemoved()) {
            boolean disable = c.getList().isEmpty();
            btnClearRegions.setDisable(disable);
          }
          if (c.wasAdded()) {
            node.getChart().getXYPlot().addAnnotation(
                new XYShapeAnnotation(c.getAddedSubList().get(0).buildingPathProperty().get(),
                    roiStroke, roiPaint));
            btnClearRegions.setDisable(false);
          }
        });

    selectionControls.add(btnStartRegion, 0, 0);
    selectionControls.add(btnFinishRegion, 1, 0);
    selectionControls.add(btnCancelRegion, 2, 0);
    selectionControls.add(btnClearRegions, 3, 0);
    selectionControls.setHgap(5);
    selectionControls.getStyleClass().add(".region-match-chart-bg");
    selectionControls.setAlignment(Pos.TOP_CENTER);
  }

  public GridPane getSelectionControls() {
    return selectionControls;
  }

  private void loadRegionsFromFile() {
    MZmineCore.runLater(() -> {
      final FileChooser chooser = new FileChooser();
      chooser.getExtensionFilters()
          .add(new ExtensionFilter("MZmine regions file", REGION_FILE_EXTENSION));
      final File file = chooser.showOpenDialog(MZmineCore.getDesktop().getMainWindow());
      if (file == null) {
        return;
      }
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

          for (List<Point2D> point2DS : param.getValue()) {
            final RegionSelectionListener l = new RegionSelectionListener(node);
            l.buildingPointsProperty().setValue(FXCollections.observableArrayList(point2DS));
            finishedRegionSelectionListeners.add(l);
          }
        }
      } catch (ParserConfigurationException | IOException | SAXException |
               XPathExpressionException e) {
        e.printStackTrace();
      }
    });
  }

  private void saveRegionsToFile() {
    final RegionsParameter parameter = new RegionsParameter("Regions", "User defined regions");
    finishedRegionSelectionListeners.forEach(
        l -> parameter.getValue().add(l.buildingPointsProperty().getValue()));
    MZmineCore.runLater(() -> {
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
}
