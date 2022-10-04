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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.gui.chartbasics.chartutils.XYCirclePixelSizeRenderer;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.LipidSearchParameters;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.customlipidclass.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidFactory;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotXYDataset;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.color.ColorsFX;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.color.Vision;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

public class LipidDatabaseTableController {

  @FXML
  private TableView<LipidClassDescription> lipidDatabaseTableView;

  @FXML
  private TableColumn<LipidClassDescription, String> idColumn;

  @FXML
  private TableColumn<LipidClassDescription, String> lipidClassColumn;

  @FXML
  private TableColumn<LipidClassDescription, String> formulaColumn;

  @FXML
  private TableColumn<LipidClassDescription, String> abbreviationColumn;

  @FXML
  private TableColumn<LipidClassDescription, String> exactMassColumn;

  @FXML
  private TableColumn<LipidClassDescription, String> infoColumn;

  @FXML
  private TableColumn<LipidClassDescription, String> statusColumn;

  @FXML
  private TableColumn<LipidClassDescription, String> fragmentsPosColumn;

  @FXML
  private BorderPane kendrickPlotPanelCH2;

  @FXML
  private BorderPane kendrickPlotPanelH;

  @FXML
  private Label statusLabel;

  @FXML
  private Label noInterLabel;

  @FXML
  private Label possibleInterLabel;

  @FXML
  private Label interLabel;

  ObservableList<LipidClassDescription> tableData = FXCollections.observableArrayList();

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();

  private int minChainLength;
  private int maxChainLength;
  private int minDoubleBonds;
  private int maxDoubleBonds;
  private MZTolerance mzTolerance;
  private boolean searchForCustomLipidClasses;
  private CustomLipidClass[] customLipidClasses;

  private Color noInterFX;
  private Color possibleInterFX;
  private Color interFX;
  private java.awt.Color noInterSwing;
  private java.awt.Color possibleInterSwing;
  private java.awt.Color interSwing;

  public void initialize(ParameterSet parameters, LipidClasses[] selectedLipids) {

    this.minChainLength =
        parameters.getParameter(LipidSearchParameters.chainLength).getValue().lowerEndpoint();
    this.maxChainLength =
        parameters.getParameter(LipidSearchParameters.chainLength).getValue().upperEndpoint();
    this.minDoubleBonds =
        parameters.getParameter(LipidSearchParameters.doubleBonds).getValue().lowerEndpoint();
    this.maxDoubleBonds =
        parameters.getParameter(LipidSearchParameters.doubleBonds).getValue().upperEndpoint();
    this.searchForCustomLipidClasses =
        parameters.getParameter(LipidSearchParameters.customLipidClasses).getValue();
    if (searchForCustomLipidClasses) {
      this.customLipidClasses = parameters.getParameter(LipidSearchParameters.customLipidClasses)
          .getEmbeddedParameter().getChoices();
    }

    this.mzTolerance = parameters.getParameter(LipidSearchParameters.mzTolerance).getValue();

    int id = 1;
    addLipidsToTable(selectedLipids, id);
    if (customLipidClasses != null && customLipidClasses.length > 0) {
      id = id + selectedLipids.length;
      addLipidsToTable(customLipidClasses, id);
    }

    idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
    lipidClassColumn.setCellValueFactory(new PropertyValueFactory<>("lipidClass"));
    formulaColumn.setCellValueFactory(new PropertyValueFactory<>("molecularFormula"));
    abbreviationColumn.setCellValueFactory(new PropertyValueFactory<>("abbreviation"));
    exactMassColumn.setCellValueFactory(new PropertyValueFactory<>("exactMass"));
    infoColumn.setCellValueFactory(new PropertyValueFactory<>("info"));
    statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
    fragmentsPosColumn.setCellValueFactory(new PropertyValueFactory<>("msmsFragmentsPos"));

    // check for interferences
    checkInterferences();

    // set colors depending on colors
    SimpleColorPalette palette = (MZmineCore.getConfiguration().getDefaultColorPalette() != null)
        ? MZmineCore.getConfiguration().getDefaultColorPalette()
        : SimpleColorPalette.DEFAULT.get(Vision.DEUTERANOPIA);

    // fx colors
    noInterFX = palette.getPositiveColor();
    possibleInterFX = palette.getNeutralColor();
    interFX = palette.getNegativeColor();

    // awt/swing colors for jfreechart
    noInterSwing = palette.getPositiveColorAWT();
    possibleInterSwing = palette.getNeutralColorAWT();
    interSwing = palette.getNegativeColorAWT();

    // create cell factory
    statusColumn.setCellFactory(e -> new TableCell<LipidClassDescription, String>() {
      @Override
      public void updateItem(String item, boolean empty) {
        // Always invoke super constructor.
        super.updateItem(item, empty);
        if (getIndex() >= 0 && item != null) {
          if (tableData.get(getIndex()).getInfo().contains("interference")) {
            this.setStyle("-fx-background-color:" + ColorsFX.toHexString(interFX));
          } else if (tableData.get(getIndex()).getInfo().contains("possible interference")) {
            this.setStyle("-fx-background-color:" + ColorsFX.toHexString(possibleInterFX));
          } else {
            this.setStyle("-fx-background-color:" + ColorsFX.toHexString(noInterFX));
          }
        }
      }
    });

    lipidDatabaseTableView.setItems(tableData);

    // add plots
    EChartViewer kendrickChartCH2 =
        new EChartViewer(create2DKendrickMassDatabasePlot("CH2"), true, true, true, true, false);
    kendrickPlotPanelCH2.setCenter(kendrickChartCH2);
    EChartViewer kendrickChartH =
        new EChartViewer(create2DKendrickMassDatabasePlot("H"), true, true, true, true, false);
    kendrickPlotPanelH.setCenter(kendrickChartH);

    // legend
    statusLabel.setStyle("-fx-font-weight: bold");
    noInterLabel.setStyle("-fx-background-color:" + ColorsFX.toHexString(noInterFX));
    possibleInterLabel.setStyle("-fx-background-color:" + ColorsFX.toHexString(possibleInterFX));
    interLabel.setStyle("-fx-background-color:" + ColorsFX.toHexString(interFX));
  }

  private void addLipidsToTable(ILipidClass[] selectedLipids, int id) {
    for (int i = 0; i < selectedLipids.length; i++) {
      for (int chainLength = minChainLength; chainLength <= maxChainLength; chainLength++) {
        for (int chainDoubleBonds =
            minDoubleBonds; chainDoubleBonds <= maxDoubleBonds; chainDoubleBonds++) {

          if (chainLength / 2 < chainDoubleBonds || chainLength == 0) {
            continue;
          }
          // Prepare a lipid instance
          SpeciesLevelAnnotation lipid = LIPID_FACTORY.buildSpeciesLevelLipid(selectedLipids[i],
              chainLength, chainDoubleBonds);
          if (lipid == null) {
            continue;
          }
          List<LipidFragmentationRule> fragmentationRules =
              Arrays.asList(selectedLipids[i].getFragmentationRules());
          StringBuilder fragmentationRuleSB = new StringBuilder();
          fragmentationRules.stream().forEach(rule -> {
            fragmentationRuleSB.append(rule.toString()).append("\n");
          });
          StringBuilder exactMassSB = new StringBuilder();
          Set<IonizationType> ionizationTypes = fragmentationRules.stream()
              .map(LipidFragmentationRule::getIonizationType).collect(Collectors.toSet());
          for (IonizationType ionizationType : ionizationTypes) {
            double mz = MolecularFormulaManipulator.getMass(lipid.getMolecularFormula(),
                AtomContainerManipulator.MonoIsotopic) + ionizationType.getAddedMass();
            exactMassSB.append(ionizationType.getAdductName()).append(" ")
                .append(MZmineCore.getConfiguration().getMZFormat().format(mz)).append("\n");
          }
          tableData.add(new LipidClassDescription(String.valueOf(id), // id
              selectedLipids[i].getName(), // lipid class
              MolecularFormulaManipulator.getString(lipid.getMolecularFormula()), // molecular
              // formula
              lipid.getAnnotation(),
              // abbr
              exactMassSB.toString(), // exact mass
              // mass
              "", // info
              "", // status
              fragmentationRuleSB.toString())); // msms fragments
          id++;
        }
      }
    }
  }

  /**
   * This method checks for m/z interferences in the generated database table using the user set m/z
   * window
   */
  private void checkInterferences() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < tableData.size(); i++) {
      Map<String, Double> ionSpecificMzValues =
          extractIonNotationMzValuesFromTable(tableData.get(i));
      for (Entry<String, Double> entry : ionSpecificMzValues.entrySet()) {
        for (int j = 0; j < tableData.size(); j++) {
          sb.setLength(0);
          Map<String, Double> ionSpecificMzValuesCompare =
              extractIonNotationMzValuesFromTable(tableData.get(j));
          for (Entry<String, Double> entryCompare : ionSpecificMzValuesCompare.entrySet()) {
            double valueOne = entry.getValue();
            double valueTwo = entryCompare.getValue();
            if (valueOne == valueTwo && j != i
                && isSamePolarity(entry.getKey(), entryCompare.getKey())) {
              if (!sb.isEmpty()) {
                sb.append("\n");
              }
              sb.append(entryCompare.getKey()).append(" interference with ")
                  .append(tableData.get(i).getAbbreviation()).append(" ").append(entry.getKey());
            } else if (mzTolerance.checkWithinTolerance(valueOne, valueTwo) && j != i
                && isSamePolarity(entry.getKey(), entryCompare.getKey())) {
              double delta = valueOne - valueTwo;
              if (!sb.isEmpty()) {
                sb.append("\n");
              }
              sb.append(entryCompare.getKey()).append(" possible interference with ")
                  .append(tableData.get(i).getAbbreviation()).append(" ").append(entry.getKey())
                  .append(" \u0394 ")
                  .append(MZmineCore.getConfiguration().getMZFormat().format(delta));
            }
          }
          if (!sb.isEmpty()) {
            tableData.get(j).setInfo(tableData.get(j).getInfo() + "\n" + sb.toString());
          }
        }
      }
    }
  }

  private boolean isSamePolarity(String key, String key2) {
    return ((key.contains("]+") && key2.contains("]+"))
        || (key.contains("]-") && key2.contains("]-")));
  }

  /**
   * This method creates Kendrick database plots to visualize the database and possible
   * interferences
   */
  private JFreeChart create2DKendrickMassDatabasePlot(String base) {

    List<DataPointXYZ> noInterferenceDps = new ArrayList<>();
    List<DataPointXYZ> possibleInterferenceDps = new ArrayList<>();
    List<DataPointXYZ> interferenceDps = new ArrayList<>();

    // add data to all series
    double yValue = 0;
    double xValue = 0;

    for (int i = 0; i < tableData.size(); i++) {
      Map<String, Double> ionSpecificMzValues =
          extractIonNotationMzValuesFromTable(tableData.get(i));
      for (Entry<String, Double> entry : ionSpecificMzValues.entrySet()) {

        // calc y value depending on KMD base
        if (base.equals("CH2")) {
          double exactMassFormula = FormulaUtils.calculateExactMass("CH2");
          yValue = ((int) (entry.getValue() * (14 / exactMassFormula) + 1))
              - entry.getValue() * (14 / exactMassFormula);
        } else if (base.equals("H")) {
          double exactMassFormula = FormulaUtils.calculateExactMass("H");
          yValue = ((int) (entry.getValue() * (1 / exactMassFormula) + 1))
              - entry.getValue() * (1 / exactMassFormula);
        } else {
          yValue = 0;
        }

        // get x value from table
        xValue = entry.getValue();

        // add xy values to series based on interference status
        if (tableData.get(i).getInfo().contains(entry.getKey() + " possible interference")) {
          possibleInterferenceDps
              .add(new DataPointXYZ(xValue, yValue, mzTolerance.getMzToleranceForMass(xValue) * 2));
        } else if (tableData.get(i).getInfo().contains(entry.getKey() + " interference")) {
          interferenceDps
              .add(new DataPointXYZ(xValue, yValue, mzTolerance.getMzToleranceForMass(xValue) * 2));
        } else {
          noInterferenceDps
              .add(new DataPointXYZ(xValue, yValue, mzTolerance.getMzToleranceForMass(xValue) * 2));
        }
      }

    }

    // create chart
    JFreeChart chart = ChartFactory.createScatterPlot("Database plot KMD base " + base, "m/z",
        "KMD (" + base + ")", null, PlotOrientation.VERTICAL, false, true, false);
    KendrickMassPlotXYDataset noInterferenceDataset = new KendrickMassPlotXYDataset(
        noInterferenceDps.stream().mapToDouble(DataPointXYZ::getX).toArray(),
        noInterferenceDps.stream().mapToDouble(DataPointXYZ::getY).toArray(),
        noInterferenceDps.stream().mapToDouble(DataPointXYZ::getZ).toArray(), "No Interference",
        noInterSwing);
    KendrickMassPlotXYDataset possibleInterferenceDataset = new KendrickMassPlotXYDataset(
        possibleInterferenceDps.stream().mapToDouble(DataPointXYZ::getX).toArray(),
        possibleInterferenceDps.stream().mapToDouble(DataPointXYZ::getY).toArray(),
        possibleInterferenceDps.stream().mapToDouble(DataPointXYZ::getZ).toArray(),
        "Possible Interference", possibleInterSwing);
    KendrickMassPlotXYDataset interferenceDataset = new KendrickMassPlotXYDataset(
        interferenceDps.stream().mapToDouble(DataPointXYZ::getX).toArray(),
        interferenceDps.stream().mapToDouble(DataPointXYZ::getY).toArray(),
        interferenceDps.stream().mapToDouble(DataPointXYZ::getZ).toArray(), "Interference",
        interSwing);
    chart.getXYPlot().setDataset(0, noInterferenceDataset);
    chart.getXYPlot().setDataset(1, possibleInterferenceDataset);
    chart.getXYPlot().setDataset(2, interferenceDataset);

    chart.setBackgroundPaint(
        MZmineCore.getConfiguration().getDefaultChartTheme().getChartBackgroundPaint());

    // create plot
    XYPlot plot = (XYPlot) chart.getPlot();
    plot.setRangeGridlinesVisible(false);
    plot.setDomainGridlinesVisible(false);

    // set axis
    NumberAxis range = (NumberAxis) plot.getRangeAxis();
    range.setRange(0, 1);

    // set renderer
    XYCirclePixelSizeRenderer renderer = new XYCirclePixelSizeRenderer();
    plot.setRenderer(renderer);

    return chart;
  }

  private Map<String, Double> extractIonNotationMzValuesFromTable(
      LipidClassDescription lipidClassDescription) {
    Map<String, Double> ionSpecificMzValues = new HashMap<>();
    String allPairs = lipidClassDescription.getExactMass();
    String[] pairs = allPairs.split("\n");
    for (int i = 0; i < pairs.length; i++) {
      String[] pair = pairs[i].split(" ");
      if (pair != null && pair.length > 1) {
        ionSpecificMzValues.put(pair[0], Double.parseDouble(pair[1]));
      }
    }
    return ionSpecificMzValues;
  }

  static class DataPointXYZ {

    private double x;
    private double y;
    private double z;

    public DataPointXYZ(double x, double y, double z) {
      super();
      this.x = x;
      this.y = y;
      this.z = z;
    }

    public double getX() {
      return x;
    }

    public void setX(double x) {
      this.x = x;
    }

    public double getY() {
      return y;
    }

    public void setY(double y) {
      this.y = y;
    }

    public double getZ() {
      return z;
    }

    public void setZ(double z) {
      this.z = z;
    }
  }

}
