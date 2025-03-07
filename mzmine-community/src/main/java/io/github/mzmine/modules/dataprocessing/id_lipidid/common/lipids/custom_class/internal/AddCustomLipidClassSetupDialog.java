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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.internal;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.LipidSpectrumProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationChainParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationMSMSParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationModule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.fragmentation.ILipidFragmentFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.fragmentation.LipidFragmentFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.molecular_species.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidMainClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidFactory;
import io.github.mzmine.modules.visualization.spectra.matchedlipid.MatchedLipidLabelGenerator;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

class AddCustomLipidClassSetupDialog extends ParameterSetupDialog {

  private final List<Integer> numberOfCAtomsList = new ArrayList<>();
  private final List<Integer> numberOfDbesList = new ArrayList<>();
  private final ReentrantLock lock = new ReentrantLock();
  private boolean isUpdateFromPreset = false;

  private final RawDataFile dummyFile = RawDataFile.createDummyFile();
  private final Scan simpleScan = new SimpleScan(dummyFile, -1, 2, 0.1F, null, new double[]{500},
      new double[]{100}, MassSpectrumType.ANY, PolarityType.ANY, "Pseudo", null);

  private static final MassList MASS_LIST = SimpleMassList.create(null,
      new SimpleDataPoint[]{new SimpleDataPoint(500, 100)});

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();
  private static final Random random = new Random();
  private final SplitPane paramPreviewSplit;
  private final BorderPane previewWrapperPane;
  private BorderPane lipidPane;
  private GridPane lipidGridPane;
  private String name;
  private String abbr;
  private LipidCategories lipidCategory;
  private LipidMainClasses lipidMainClass;
  private String backboneFormula;
  private LipidChainType[] lipidChainTypes;
  private LipidFragmentationRule[] customLipidClassFragmentationRules;

  public AddCustomLipidClassSetupDialog(boolean valueCheckRequired, ParameterSet parameters) {
    super(valueCheckRequired, parameters);
    this.name = parameters.getParameter(AddCustomLipidClassParameters.name).getValue();
    this.abbr = parameters.getParameter(AddCustomLipidClassParameters.abbr).getValue();
    this.lipidCategory = parameters.getParameter(AddCustomLipidClassParameters.lipidCategory)
        .getValue();
    this.lipidMainClass = parameters.getParameter(AddCustomLipidClassParameters.lipidMainClass)
        .getValue();
    this.backboneFormula = parameters.getParameter(AddCustomLipidClassParameters.backBoneFormula)
        .getValue();
    this.lipidChainTypes = parameters.getParameter(AddCustomLipidClassParameters.lipidChainTypes)
        .getValue();
    this.customLipidClassFragmentationRules = parameters.getParameter(
        AddCustomLipidClassParameters.customLipidClassFragmentationRules).getValue();
    paramPreviewSplit = new SplitPane();
    paramPreviewSplit.getItems().add(getParamPane());
    paramPreviewSplit.setOrientation(Orientation.HORIZONTAL);
    mainPane.setCenter(paramPreviewSplit);
    previewWrapperPane = new BorderPane();
    paramPreviewSplit.getItems().add(previewWrapperPane);
    paramPreviewSplit.setDividerPosition(0, 0.5);
    paramsPane.setHgap(7d);
    paramsPane.setVgap(1d);
    initDummyChains();
    updateExampleLipid();

    ObservableList<LipidClasses> observableList = FXCollections.observableArrayList();
    observableList.addAll(Arrays.asList(LipidClasses.values()));
    ComboBox<LipidClasses> loadPresetLipid = new ComboBox<>(observableList);
    loadPresetLipid.setPromptText("Preset lipid class");
    Tooltip loadPresetLipidTooltip = new Tooltip("Load a preset lipid class as a starting point");
    loadPresetLipid.setTooltip(loadPresetLipidTooltip);
    loadPresetLipid.setOnAction(event -> {
      LipidClasses selectedLipidClass = loadPresetLipid.getSelectionModel().getSelectedItem();
      updateWithPresetLipidClass(selectedLipidClass);
    });
    getButtonBar().getButtons().add(loadPresetLipid);
  }

  private void initDummyChains() {
    numberOfCAtomsList.clear();
    numberOfDbesList.clear();
    for (LipidChainType lipidChain : lipidChainTypes) {
      int lowerBound = 14;
      int upperBound = 21;
      int carbons =
          2 * random.nextInt((upperBound / 2) - (lowerBound / 2) + 1) + (lowerBound / 2) * 2;
      numberOfCAtomsList.add(carbons);
      numberOfDbesList.add(random.nextInt(4));
    }
  }

  private void updateWithPresetLipidClass(LipidClasses selectedLipidClass) {
    try {
      lock.lock();
      isUpdateFromPreset = true;
      updateParametersFromPreset(selectedLipidClass);
    } finally {
      lock.unlock();
    }
  }

  private void updateParametersFromPreset(LipidClasses selectedLipidClass) {
    this.parameterSet.setParameter(AddCustomLipidClassParameters.name,
        selectedLipidClass.getName());
    this.parameterSet.setParameter(AddCustomLipidClassParameters.abbr,
        selectedLipidClass.getAbbr());
    this.parameterSet.setParameter(AddCustomLipidClassParameters.lipidCategory,
        selectedLipidClass.getCoreClass());
    this.parameterSet.setParameter(AddCustomLipidClassParameters.lipidMainClass,
        selectedLipidClass.getMainClass());
    this.parameterSet.setParameter(AddCustomLipidClassParameters.lipidChainTypes,
        selectedLipidClass.getChainTypes());
    this.parameterSet.setParameter(AddCustomLipidClassParameters.backBoneFormula,
        selectedLipidClass.getBackBoneFormula());
    this.parameterSet.setParameter(AddCustomLipidClassParameters.customLipidClassFragmentationRules,
        selectedLipidClass.getFragmentationRules());
    setParameterValuesToComponents();
    updateExampleLipid();

    isUpdateFromPreset = true;
    parametersChanged();
    isUpdateFromPreset = false;
  }

  @Override
  protected void parametersChanged() {
    try {
      lock.lock();
      if (!isUpdateFromPreset) {
        super.updateParameterSetFromComponents();
      }
      name = parameterSet.getParameter(AddCustomLipidClassParameters.name).getValue();
      abbr = parameterSet.getParameter(AddCustomLipidClassParameters.abbr).getValue();
      lipidCategory = parameterSet.getParameter(AddCustomLipidClassParameters.lipidCategory)
          .getValue();
      lipidMainClass = parameterSet.getParameter(AddCustomLipidClassParameters.lipidMainClass)
          .getValue();
      backboneFormula = parameterSet.getParameter(AddCustomLipidClassParameters.backBoneFormula)
          .getValue();
      lipidChainTypes = parameterSet.getParameter(AddCustomLipidClassParameters.lipidChainTypes)
          .getValue();
      customLipidClassFragmentationRules = parameterSet.getParameter(
          AddCustomLipidClassParameters.customLipidClassFragmentationRules).getValue();
      if (lipidChainTypes.length != numberOfCAtomsList.size()) {
        adjustExampleCarbonAndDbeNumbers(lipidChainTypes);
      }
      if(parameterSet.checkParameterValues(new ArrayList<>(), true)) {
        updateExampleLipid();
      }
    } finally {
      lock.unlock();
    }
  }

  private void adjustExampleCarbonAndDbeNumbers(LipidChainType[] lipidChainTypes) {
    numberOfCAtomsList.clear();
    numberOfDbesList.clear();
    for (int i = 0; i < lipidChainTypes.length; i++) {
      int lowerBound = 14;
      int upperBound = 21;
      int carbons =
          2 * random.nextInt((upperBound / 2) - (lowerBound / 2) + 1) + (lowerBound / 2) * 2;
      numberOfCAtomsList.add(carbons);
      numberOfDbesList.add(random.nextInt(4));
    }
  }

  private void updateExampleLipid() {
    if (name != null && abbr != null && lipidMainClass != null && lipidCategory != null
        && lipidChainTypes != null && lipidChainTypes.length > 0 && !numberOfCAtomsList.isEmpty()
        && !numberOfDbesList.isEmpty()) {
      lipidPane = new BorderPane();
      ScrollPane scrollPane = new ScrollPane();
      scrollPane.setFitToHeight(true);
      scrollPane.setFitToWidth(true);
      lipidGridPane = new GridPane();
      GridPane.setHgrow(lipidGridPane, Priority.ALWAYS);
      GridPane.setVgrow(lipidGridPane, Priority.ALWAYS);
      lipidGridPane.setHgap(10);
      lipidGridPane.setVgap(10);
      lipidGridPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
      lipidGridPane.setPadding(new Insets(10));

      Label lipidClassSummary = new Label("Lipid class summary");
      lipidClassSummary.getStyleClass().add("bold-title-label");
      lipidGridPane.add(lipidClassSummary, 0, 0, 2, 1);
      Label simulateLipidFragments = new Label("Simulate lipid fragments");
      simulateLipidFragments.getStyleClass().add("bold-title-label");
      lipidGridPane.add(simulateLipidFragments, 3, 0, 3, 1);

      Label chains = new Label("Chains");
      lipidGridPane.add(chains, 3, 1);
      Label carbons = new Label("Carbons");
      lipidGridPane.add(carbons, 4, 1);
      Label dbes = new Label("DBEs");
      lipidGridPane.add(dbes, 5, 1);
      for (int i = 0; i < lipidChainTypes.length; i++) {
        int chainIndex = i + 1;
        lipidGridPane.add(new Label("#" + chainIndex), 3, chainIndex + 1);
        TextField carbonTextField = new TextField(String.valueOf(numberOfCAtomsList.get(i)));
        carbonTextField.setMaxWidth(50);
        int finalI = i;

        carbonTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
          if (!newValue) {
            try {
              int intValue = Integer.parseInt(carbonTextField.getText());
              numberOfCAtomsList.set(finalI, intValue);
              updateExampleLipid();
            } catch (NumberFormatException e) {
              e.printStackTrace();
            }
          }
        });

        carbonTextField.setOnAction(event -> {
          try {
            int intValue = Integer.parseInt(carbonTextField.getText());
            numberOfCAtomsList.set(finalI, intValue);
            updateExampleLipid();
          } catch (NumberFormatException e) {
            e.printStackTrace();
          }
        });
        lipidGridPane.add(carbonTextField, 4, chainIndex + 1);

        TextField dbeTextField = new TextField(String.valueOf(numberOfDbesList.get(i)));
        dbeTextField.setMaxWidth(50);
        dbeTextField.focusedProperty().addListener((_, _, newValue) -> {
          if (!newValue) {
            try {
              int intValue = Integer.parseInt(dbeTextField.getText());
              numberOfDbesList.set(finalI, intValue);
              updateExampleLipid();
            } catch (NumberFormatException e) {
              e.printStackTrace();
            }
          }
        });

        dbeTextField.setOnAction(event -> {
          try {
            int intValue = Integer.parseInt(dbeTextField.getText());
            numberOfDbesList.set(finalI, intValue);
            updateExampleLipid();
          } catch (NumberFormatException e) {
            e.printStackTrace();
          }
        });
        lipidGridPane.add(dbeTextField, 5, chainIndex + 1);
      }

      ILipidClass newCustomLipidClass = new CustomLipidClass(name, abbr, lipidCategory,
          lipidMainClass, backboneFormula, lipidChainTypes, customLipidClassFragmentationRules);
      int totalNumberOfCarbons = numberOfCAtomsList.stream().mapToInt(Integer::intValue).sum();
      int totalNumberOfDBEs = numberOfDbesList.stream().mapToInt(Integer::intValue).sum();
      SpeciesLevelAnnotation speciesLevelAnnotation = LIPID_FACTORY.buildSpeciesLevelLipid(
          newCustomLipidClass, totalNumberOfCarbons, totalNumberOfDBEs, 0);

      Label speciesLevel = new Label("Species level: ");
      lipidGridPane.add(speciesLevel, 0, 1);
      Label annotationLabel = new Label(speciesLevelAnnotation.getAnnotation());
      lipidGridPane.add(annotationLabel, 1, 1, 2, 1);

      int[] carbonsInChains = numberOfCAtomsList.stream().mapToInt(Integer::intValue).toArray();
      int[] dbesInChains = numberOfDbesList.stream().mapToInt(Integer::intValue).toArray();
      int[] additionalOxygens = new int[numberOfDbesList.size()];

      Label molecularSpeciesLevel = new Label("Molecular species level: ");
      lipidGridPane.add(molecularSpeciesLevel, 0, 2);
      MolecularSpeciesLevelAnnotation molecularSpeciesLevelAnnotation = LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
          newCustomLipidClass, carbonsInChains, dbesInChains, additionalOxygens);
      Label molecularSpeciesAnnotationLabel = new Label(
          molecularSpeciesLevelAnnotation.getAnnotation());
      lipidGridPane.add(molecularSpeciesAnnotationLabel, 1, 2, 2, 1);

      Label formula = new Label("Molecular formula: ");
      lipidGridPane.add(formula, 0, 3);
      Label formulaLabel = new Label(
          MolecularFormulaManipulator.getString(speciesLevelAnnotation.getMolecularFormula()));
      lipidGridPane.add(formulaLabel, 1, 3);

      Label mass = new Label("Neutral mass: ");
      lipidGridPane.add(mass, 0, 4);
      Label massLabel = new Label(MZmineCore.getConfiguration().getMZFormat()
          .format(FormulaUtils.calculateMzRatio(speciesLevelAnnotation.getMolecularFormula())));
      lipidGridPane.add(massLabel, 1, 4);

      Label numberOfChains = new Label("Number of chains: ");
      lipidGridPane.add(numberOfChains, 0, 5);
      Label chainsLabel = new Label(
          String.valueOf(speciesLevelAnnotation.getLipidClass().getChainTypes().length));
      lipidGridPane.add(chainsLabel, 1, 5);

      int ionNotationStartColumn = 6;
      Label ionNotations = new Label("Ion notations: ");
      lipidGridPane.add(ionNotations, 0, ionNotationStartColumn);
      if (speciesLevelAnnotation.getLipidClass().getFragmentationRules() != null
          && speciesLevelAnnotation.getLipidClass().getFragmentationRules().length > 0) {
        Set<IonizationType> ionizationTypeList = new HashSet<>();
        for (LipidFragmentationRule fragmentationRule : speciesLevelAnnotation.getLipidClass()
            .getFragmentationRules()) {
          ionizationTypeList.add(fragmentationRule.getIonizationType());
        }
        for (IonizationType ionNotation : ionizationTypeList) {
          lipidGridPane.add(new Label(ionNotation.getAdductName()), 1, ionNotationStartColumn);
          lipidGridPane.add(new Label(MZmineCore.getConfiguration().getMZFormat().format(
                  FormulaUtils.calculateMzRatio(FormulaUtils.ionizeFormula(
                      MolecularFormulaManipulator.getString(
                          speciesLevelAnnotation.getMolecularFormula()), ionNotation)))), 2,
              ionNotationStartColumn);
          ionNotationStartColumn++;
        }
      } else {
        Label ionNotationsLabel = new Label("No ion notations found");
        ionNotationsLabel.setTooltip(
            new Tooltip("No ion notations found. Setup fragmentation rule"));
        lipidGridPane.add(ionNotationsLabel, 1, ionNotationStartColumn);
      }
      lipidPane.setTop(lipidGridPane);
      if (speciesLevelAnnotation.getLipidClass().getFragmentationRules() != null
          && speciesLevelAnnotation.getLipidClass().getFragmentationRules().length > 0) {
        GridPane lipidPlots = calcualteSpectraPlotGridPane(
            speciesLevelAnnotation.getLipidClass().getFragmentationRules(), speciesLevelAnnotation);
        scrollPane.setContent(lipidPlots);
        lipidPane.setCenter(scrollPane);
      }
      previewWrapperPane.setCenter(lipidPane);
    }
  }

  private GridPane calcualteSpectraPlotGridPane(LipidFragmentationRule[] lipidFragmentationRules,
      SpeciesLevelAnnotation speciesLevelAnnotation) {
    GridPane gridPane = new GridPane();
    GridPane.setHgrow(gridPane, Priority.ALWAYS);
    GridPane.setVgrow(gridPane, Priority.ALWAYS);
    gridPane.setHgap(10);
    gridPane.setVgap(10);
    gridPane.setPadding(new Insets(10));
    Label inSilicoFragments = new Label("In-silico fragments (random intensities)");
    inSilicoFragments.getStyleClass().add("bold-title-label");
    gridPane.add(inSilicoFragments, 0, 0);
    simpleScan.addMassList(MASS_LIST);
    Set<IonizationType> ionizationTypeList = new HashSet<>();

    for (LipidFragmentationRule fragmentationRule : lipidFragmentationRules) {
      ionizationTypeList.add(fragmentationRule.getIonizationType());
    }
    int i = 1;
    for (IonizationType ionizationType : ionizationTypeList) {
      List<LipidFragment> lipidFragments = new ArrayList<>();
      SpectraPlot spectraPlot = new SpectraPlot();
      for (LipidFragmentationRule fragmentationRule : lipidFragmentationRules) {
        if (fragmentationRule.getLipidFragmentInformationLevelType() != null
            && fragmentationRule.getLipidFragmentInformationLevelType()
            .equals(LipidAnnotationLevel.SPECIES_LEVEL)) {
          ILipidFragmentFactory lipidFragmentFactory = new LipidFragmentFactory(
              new MZTolerance(10000, 1), speciesLevelAnnotation, ionizationType,
              new LipidFragmentationRule[]{fragmentationRule}, simpleScan,
              makeLipidAnnotationParameterSet(speciesLevelAnnotation, numberOfCAtomsList.get(0),
                  numberOfDbesList.get(0)).getParameter(
                  LipidAnnotationParameters.lipidChainParameters).getEmbeddedParameters());
          addFragments(lipidFragments, lipidFragmentFactory);
        } else {
          for (int j = 0; j < numberOfCAtomsList.size(); j++) {
            ILipidFragmentFactory lipidFragmentFactory = new LipidFragmentFactory(
                new MZTolerance(10000, 1), speciesLevelAnnotation, ionizationType,
                new LipidFragmentationRule[]{fragmentationRule}, simpleScan,
                makeLipidAnnotationParameterSet(speciesLevelAnnotation, numberOfCAtomsList.get(j),
                    numberOfDbesList.get(j)).getParameter(
                    LipidAnnotationParameters.lipidChainParameters).getEmbeddedParameters());
            addFragments(lipidFragments, lipidFragmentFactory);
          }
        }
      }
      List<DataPoint> fragmentScanDps = lipidFragments.stream().map(
              lipidFragment -> new SimpleDataPoint(lipidFragment.getMzExact(), random.nextInt(81) + 20))
          .collect(Collectors.toList());
      if (!fragmentScanDps.isEmpty()) {
        PlotXYDataProvider fragmentDataProvider = new LipidSpectrumProvider(lipidFragments,
            fragmentScanDps.stream().mapToDouble(DataPoint::getMZ).toArray(),
            fragmentScanDps.stream().mapToDouble(DataPoint::getIntensity).toArray(),
            "In-silico fragments",
            MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT());
        ColoredXYDataset fragmentDataSet = new ColoredXYDataset(fragmentDataProvider,
            RunOption.NEW_THREAD);
        MatchedLipidLabelGenerator matchedLipidLabelGenerator = new MatchedLipidLabelGenerator(
            spectraPlot, lipidFragments);
        spectraPlot.getXYPlot().getRenderer().setDefaultItemLabelsVisible(true);
        spectraPlot.getXYPlot().getRenderer()
            .setSeriesItemLabelGenerator(1, matchedLipidLabelGenerator);
        spectraPlot.addDataSet(fragmentDataSet,
            MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT(), true,
            matchedLipidLabelGenerator, true);
        spectraPlot.getChart().setBackgroundPaint((new Color(0, 0, 0, 0)));
        spectraPlot.getXYPlot().setBackgroundPaint((new Color(0, 0, 0, 0)));
        spectraPlot.setTitle(ionizationType.getAdductName(), ionizationType.getAdductName());
        spectraPlot.setMinSize(400, 200);
        spectraPlot.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        GridPane.setHgrow(spectraPlot, Priority.ALWAYS);
        GridPane.setVgrow(spectraPlot, Priority.ALWAYS);
        gridPane.add(spectraPlot, 0, i);
        i++;
      }
    }
    return gridPane;
  }

  private void addFragments(List<LipidFragment> lipidFragments,
      ILipidFragmentFactory lipidFragmentFactory) {
    List<LipidFragment> newLipidFragments = lipidFragmentFactory.findLipidFragments();
    List<LipidFragment> lipidFragmentsToAdd = new ArrayList<>();
    for (LipidFragment newLipidFragment : newLipidFragments) {
      if ((newLipidFragment.getLipidFragmentInformationLevelType()
          .equals(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)
          && newLipidFragment.getChainLength() != null
          && newLipidFragment.getNumberOfDBEs() != null)
          || newLipidFragment.getLipidFragmentInformationLevelType()
          .equals(LipidAnnotationLevel.SPECIES_LEVEL)) {
        boolean addLipidFragment = true;
        for (LipidFragment lipidFragment : lipidFragments) {
          if (lipidFragment.getChainLength() != null && lipidFragment.getNumberOfDBEs() != null) {
            if ((lipidFragment.getLipidFragmentInformationLevelType()
                .equals(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL) && lipidFragment.getMzExact()
                .equals(newLipidFragment.getMzExact()) && lipidFragment.getChainLength()
                .equals(newLipidFragment.getChainLength())) || (
                lipidFragment.getLipidFragmentInformationLevelType()
                    .equals(LipidAnnotationLevel.SPECIES_LEVEL) && lipidFragment.getMzExact()
                    .equals(newLipidFragment.getMzExact()))) {
              addLipidFragment = false;
              break;
            }
          }
        }
        if (addLipidFragment) {
          lipidFragmentsToAdd.add(newLipidFragment);
        }
      }
    }
    lipidFragments.addAll(lipidFragmentsToAdd);
  }

  private ParameterSet makeLipidAnnotationParameterSet(
      SpeciesLevelAnnotation speciesLevelAnnotation, int numberOfCarbons, int numberofDbes) {
    var param = MZmineCore.getConfiguration().getModuleParameters(LipidAnnotationModule.class)
        .cloneParameterSet();
    var chainParam = new LipidAnnotationChainParameters().cloneParameterSet();
    chainParam.setParameter(LipidAnnotationChainParameters.maxChainLength, numberOfCarbons);
    chainParam.setParameter(LipidAnnotationChainParameters.minChainLength, numberOfCarbons);
    chainParam.setParameter(LipidAnnotationChainParameters.maxDBEs, numberofDbes);
    chainParam.setParameter(LipidAnnotationChainParameters.minDBEs, numberofDbes);
    chainParam.setParameter(LipidAnnotationChainParameters.onlySearchForEvenChainLength, false);
    param.setParameter(LipidAnnotationParameters.lipidClasses,
        new Object[]{speciesLevelAnnotation.getLipidClass()});
    param.setParameter(LipidAnnotationParameters.lipidChainParameters,
        (LipidAnnotationChainParameters) chainParam);
    param.setParameter(LipidAnnotationParameters.mzTolerance, new MZTolerance(1000, 1));
    param.setParameter(LipidAnnotationParameters.searchForMSMSFragments, true);
    param.getParameter(LipidAnnotationParameters.searchForMSMSFragments).getEmbeddedParameters()
        .setParameter(LipidAnnotationMSMSParameters.keepUnconfirmedAnnotations, false);
    param.getParameter(LipidAnnotationParameters.searchForMSMSFragments).getEmbeddedParameters()
        .setParameter(LipidAnnotationMSMSParameters.minimumMsMsScore, 0.0);
    param.getParameter(LipidAnnotationParameters.searchForMSMSFragments).getEmbeddedParameters()
        .setParameter(LipidAnnotationMSMSParameters.mzToleranceMS2, new MZTolerance(1000, 1));
    return param;
  }
}
