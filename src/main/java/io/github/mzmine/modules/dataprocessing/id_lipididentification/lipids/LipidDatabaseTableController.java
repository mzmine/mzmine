/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.LipidSearchParameters;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.customlipidclass.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidmodifications.LipidModification;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidFactory;
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
  private TableColumn<LipidClassDescription, String> lipidCoreClassColumn;

  @FXML
  private TableColumn<LipidClassDescription, String> lipidMainClassColumn;

  @FXML
  private TableColumn<LipidClassDescription, String> lipidClassColumn;

  @FXML
  private TableColumn<LipidClassDescription, String> formulaColumn;

  @FXML
  private TableColumn<LipidClassDescription, String> abbreviationColumn;

  @FXML
  private TableColumn<LipidClassDescription, String> ionizationColumn;

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
  private IonizationType ionizationType;
  private boolean useModification;
  private LipidModification[] lipidModification;
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
    this.ionizationType =
        parameters.getParameter(LipidSearchParameters.ionizationMethod).getValue();
    this.useModification =
        parameters.getParameter(LipidSearchParameters.searchForModifications).getValue();
    if (useModification) {
      this.lipidModification = parameters.getParameter(LipidSearchParameters.searchForModifications)
          .getEmbeddedParameter().getValue();
    }
    this.searchForCustomLipidClasses =
        parameters.getParameter(LipidSearchParameters.customLipidClasses).getValue();
    if (searchForCustomLipidClasses) {
      this.customLipidClasses = parameters.getParameter(LipidSearchParameters.customLipidClasses)
          .getEmbeddedParameter().getChoices();
    }

    this.mzTolerance = parameters.getParameter(LipidSearchParameters.mzTolerance).getValue();

    NumberFormat numberFormat = MZmineCore.getConfiguration().getMZFormat();
    int id = 1;

    for (int i = 0; i < selectedLipids.length; i++) {
      for (int chainLength = minChainLength; chainLength <= maxChainLength; chainLength++) {
        for (int chainDoubleBonds =
            minDoubleBonds; chainDoubleBonds <= maxDoubleBonds; chainDoubleBonds++) {

          // If we have non-zero fatty acid, which is shorter
          // than minimal length, skip this lipid
          if (((chainLength > 0) && (chainLength < minChainLength))) {
            continue;
          }

          // If we have more double bonds than carbons, it
          // doesn't make sense, so let's skip such lipids
          if (((chainDoubleBonds > 0) && (chainDoubleBonds > chainLength - 1))) {
            continue;
          }
          // Prepare a lipid instance
          SpeciesLevelAnnotation lipid = LIPID_FACTORY.buildSpeciesLevelLipid(selectedLipids[i],
              chainLength, chainDoubleBonds);
          List<LipidFragmentationRule> fragmentationRules =
              Arrays.asList(selectedLipids[i].getFragmentationRules());
          StringBuilder fragmentationRuleSB = new StringBuilder();
          fragmentationRules.stream().forEach(rule -> {
            fragmentationRuleSB.append(rule.toString());
          });
          double lipidIonMass = MolecularFormulaManipulator.getMass(lipid.getMolecularFormula(),
              AtomContainerManipulator.MonoIsotopic) + ionizationType.getAddedMass();
          tableData.add(new LipidClassDescription(String.valueOf(id), // id
              selectedLipids[i].getCoreClass().getName(), // core class
              selectedLipids[i].getMainClass().getName(), // main class
              selectedLipids[i].getName(), // lipid class
              MolecularFormulaManipulator.getString(lipid.getMolecularFormula()), // molecular
                                                                                  // formula
              lipid.getAnnotation(),
              // abbr
              ionizationType.toString(), // ionization type
              numberFormat.format(lipidIonMass), // exact
              // mass
              "", // info
              "", // status
              fragmentationRuleSB.toString())); // msms fragments
          id++;
          // if (useModification) {
          // for (int j = 0; j < lipidModification.length; j++) {
          // tableData.add(new LipidClassDescription(String.valueOf(id), // id
          // selectedLipids[i].getCoreClass().getName(), // core class
          // selectedLipids[i].getMainClass().getName(), // main class
          // selectedLipids[i].getName() + " " + lipidModification[j].toString(), // lipid
          // // class
          // lipidChain.getFormula() + lipidModification[j].getLipidModificatio(), // sum
          // // formula
          // selectedLipids[i].getAbbr() + " (" + chainLength + ":" + chainDoubleBonds + ")"
          // + lipidModification[j].getLipidModificatio(),
          // ionizationType.toString(), // ionization type
          // numberFormat.format(lipidChain.getMass() + ionizationType.getAddedMass() // exact
          // // mass
          // + lipidModification[j].getModificationMass()),
          // "", // info
          // "", // status
          // "" // msms fragments postive
          // ));
          // id++;
          // }
          // }
        }
      }
    }
    // for (int i = 0; i < customLipidClasses.length; i++) {
    // LipidChainType[] chainTypes = customLipidClasses[i].getChainTypes();
    // for (int chainLength = minChainLength; chainLength <= maxChainLength; chainLength++) {
    // for (int chainDoubleBonds =
    // minDoubleBonds; chainDoubleBonds <= maxDoubleBonds; chainDoubleBonds++) {
    // // If we have non-zero fatty acid, which is shorter
    // // than minimal length, skip this lipid
    // if (((chainLength > 0) && (chainLength < minChainLength))) {
    // continue;
    // }
    //
    // // If we have more double bonds than carbons, it
    // // doesn't make sense, so let's skip such lipids
    // if (((chainDoubleBonds > 0) && (chainDoubleBonds > chainLength - 1))) {
    // continue;
    // }
    // // Prepare a lipid instance
    // LipidFeatureIdentity lipidChain = new LipidFeatureIdentity(customLipidClasses[i],
    // chainLength, chainDoubleBonds, null, chainTypes, LipidClassType.CUSTOM_LIPID_CLASS);
    // List<LipidFragmentationRule> fragmentationRules =
    // Arrays.asList(customLipidClasses[i].getFragmentationRules());
    // StringBuilder fragmentationRuleSB = new StringBuilder();
    // fragmentationRules.stream().forEach(rule -> {
    // fragmentationRuleSB.append(rule.toString());
    // });
    // tableData.add(new LipidClassDescription(String.valueOf(id), // id
    // "", // core class
    // "", // main class
    // customLipidClasses[i].getName(), // lipid class
    // lipidChain.getFormula(), // molecular formula
    // customLipidClasses[i].getAbbr() + " (" + chainLength + ":" + chainDoubleBonds + ")", // abbr
    // ionizationType.toString(), // ionization type
    // numberFormat.format(lipidChain.getMass() + ionizationType.getAddedMass()), // exact
    // // mass
    // "", // info
    // "", // status
    // fragmentationRuleSB.toString())); // msms fragments
    // id++;
    // if (useModification) {
    // for (int j = 0; j < lipidModification.length; j++) {
    // tableData.add(new LipidClassDescription(String.valueOf(id), // id
    // "", // core class
    // "", // main class
    // customLipidClasses[i].getName() + " " + lipidModification[j].toString(), // lipid
    // // class
    // lipidChain.getFormula() + lipidModification[j].getLipidModificatio(), // sum
    // // formula
    // customLipidClasses[i].getAbbr() + " (" + chainLength + ":" + chainDoubleBonds
    // + ")"// abbr
    // + lipidModification[j].getLipidModificatio(),
    // ionizationType.toString(), // ionization type
    // numberFormat.format(lipidChain.getMass() + ionizationType.getAddedMass() // exact
    // // mass
    // + lipidModification[j].getModificationMass()),
    // "", // info
    // "", // status
    // "" // msms fragments postive
    // ));
    // id++;
    // }
    // }
    // }
    // }
    // }

    idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
    lipidCoreClassColumn.setCellValueFactory(new PropertyValueFactory<>("lipidCoreClass"));
    lipidMainClassColumn.setCellValueFactory(new PropertyValueFactory<>("lipidMainClass"));
    lipidClassColumn.setCellValueFactory(new PropertyValueFactory<>("lipidClass"));
    formulaColumn.setCellValueFactory(new PropertyValueFactory<>("molecularFormula"));
    abbreviationColumn.setCellValueFactory(new PropertyValueFactory<>("abbreviation"));
    ionizationColumn.setCellValueFactory(new PropertyValueFactory<>("ionization"));
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

    // awt/swing colors or jfreechart
    noInterSwing = palette.getPositiveColorAWT();
    possibleInterSwing = palette.getNeutralColorAWT();
    interSwing = palette.getNegativeColorAWT();

    // create cell factory
    statusColumn.setCellFactory(e -> new TableCell<LipidClassDescription, String>() {
      @Override
      public void updateItem(String item, boolean empty) {
        // Always invoke super constructor.
        super.updateItem(item, empty);
        if (getIndex() >= 0) {
          if (tableData.get(getIndex()).getInfo().toString().contains("Possible interference")) {
            this.setStyle("-fx-background-color:#" + ColorsFX.toHexString(possibleInterFX));
          } else if (tableData.get(getIndex()).getInfo().contains("Interference")) {
            this.setStyle("-fx-background-color:#" + ColorsFX.toHexString(interFX));
          } else {
            this.setStyle("-fx-background-color:#" + ColorsFX.toHexString(noInterFX));
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
    noInterLabel.setStyle("-fx-background-color:#" + ColorsFX.toHexString(noInterFX));
    possibleInterLabel.setStyle("-fx-background-color:#" + ColorsFX.toHexString(possibleInterFX));
    interLabel.setStyle("-fx-background-color:#" + ColorsFX.toHexString(interFX));
  }

  /**
   * This method checks for m/z interferences in the generated database table using the user set m/z
   * window
   */
  private void checkInterferences() {
    for (int i = 0; i < tableData.size(); i++) {
      for (int j = 0; j < tableData.size(); j++) {
        double valueOne = Double.parseDouble(tableData.get(j).getExactMass());
        double valueTwo = Double.parseDouble(tableData.get(i).getExactMass());
        if (valueOne == valueTwo && j != i) {
          tableData.get(j).setInfo("Interference with: " + tableData.get(i).getAbbreviation());
        } else if (mzTolerance.checkWithinTolerance(valueOne, valueTwo) && j != i) {
          tableData.get(j)
              .setInfo("Possible interference with: " + tableData.get(i).getAbbreviation());
        }
      }
    }
  }

  /**
   * This method creates Kendrick database plots to visualize the database and possible
   * interferences
   */
  private JFreeChart create2DKendrickMassDatabasePlot(String base) {

    XYSeriesCollection datasetCollection = new XYSeriesCollection();
    XYSeries noInterferenceSeries = new XYSeries("No interference");
    XYSeries possibleInterferenceSeries = new XYSeries("Possible interference");
    XYSeries interferenceSeries = new XYSeries("Isomeric interference");

    // add data to all series
    double yValue = 0;
    double xValue = 0;

    for (int i = 0; i < tableData.size(); i++) {

      // calc y value depending on KMD base
      if (base.equals("CH2")) {
        double exactMassFormula = FormulaUtils.calculateExactMass("CH2");
        yValue =
            ((int) (Double.parseDouble(tableData.get(i).getExactMass()) * (14 / exactMassFormula)
                + 1))
                - Double.parseDouble(tableData.get(i).getExactMass()) * (14 / exactMassFormula);
      } else if (base.equals("H")) {
        double exactMassFormula = FormulaUtils.calculateExactMass("H");
        yValue =
            ((int) (Double.parseDouble(tableData.get(i).getExactMass()) * (1 / exactMassFormula)
                + 1))
                - Double.parseDouble(tableData.get(i).getExactMass()) * (1 / exactMassFormula);
      } else {
        yValue = 0;
      }

      // get x value from table
      xValue = Double.parseDouble(tableData.get(i).getExactMass());

      // add xy values to series based on interference status
      if (tableData.get(i).getInfo().toString().contains("Possible interference")) {
        possibleInterferenceSeries.add(xValue, yValue);
      } else if (tableData.get(i).getInfo().toString().contains("Interference")) {
        interferenceSeries.add(xValue, yValue);
      } else {
        noInterferenceSeries.add(xValue, yValue);
      }
    }

    datasetCollection.addSeries(noInterferenceSeries);
    datasetCollection.addSeries(possibleInterferenceSeries);
    datasetCollection.addSeries(interferenceSeries);

    // create chart
    JFreeChart chart = ChartFactory.createScatterPlot("Database plot KMD base " + base, "m/z",
        "KMD (" + base + ")", datasetCollection, PlotOrientation.VERTICAL, true, true, false);

    chart.setBackgroundPaint(null);
    chart.getLegend().setBackgroundPaint(null);
    // create plot
    XYPlot plot = (XYPlot) chart.getPlot();
    plot.setBackgroundPaint(java.awt.Color.BLACK);
    plot.setRangeGridlinesVisible(false);
    plot.setDomainGridlinesVisible(false);

    // set axis
    NumberAxis range = (NumberAxis) plot.getRangeAxis();
    range.setRange(0, 1);

    // set renderer
    XYDotRenderer renderer = new XYDotRenderer();
    renderer.setSeriesPaint(0, noInterSwing);
    renderer.setSeriesPaint(1, possibleInterSwing);
    renderer.setSeriesPaint(2, interSwing);
    renderer.setDotHeight(3);
    renderer.setDotWidth(3);
    plot.setRenderer(renderer);

    return chart;
  }
}
