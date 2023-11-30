package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.customlipidclass;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.LipidSpectrumProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation.ILipidFragmentFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.molecularspecieslevelidentities.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.specieslevellipidmatches.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidMainClasses;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.customlipidclass.CustomLipidClassChoiceComponent.AddCustomLipidClassParameters;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipidutils.LipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.AdvancedLipidAnnotationParameters;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.LipidAnnotationChainParameters;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.LipidAnnotationMSMSParameters;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.LipidAnnotationModule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.LipidAnnotationParameters;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.LipidAnnotationUtils;
import io.github.mzmine.modules.visualization.spectra.matchedlipid.MatchedLipidLabelGenerator;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class CustomLipidClassSetupDialog extends ParameterSetupDialog {

  private final List<Integer> numberOfCAtomsList = new ArrayList<>();
  private final List<Integer> numberOfDbesList = new ArrayList<>();
  private final ReentrantLock lock = new ReentrantLock();
  private boolean isUpdateFromPreset = false;

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();
  private BorderPane lipidPane;
  private GridPane lipidGridPane;
  private String name;
  private String abbr;
  private LipidCategories lipidCategory;
  private LipidMainClasses lipidMainClass;
  private String backboneFormula;
  private LipidChainType[] lipidChainTypes;
  private LipidFragmentationRule[] customLipidClassFragmentationRules;

  public CustomLipidClassSetupDialog(boolean valueCheckRequired, ParameterSet parameters) {
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
    numberOfCAtomsList.add(16);
    numberOfCAtomsList.add(18);
    numberOfDbesList.add(0);
    numberOfDbesList.add(1);
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
      updateExampleLipid();
    } finally {
      lock.unlock();
    }
  }

  private void adjustExampleCarbonAndDbeNumbers(LipidChainType[] lipidChainTypes) {
    numberOfCAtomsList.clear();
    numberOfDbesList.clear();
    for (int i = 0; i < lipidChainTypes.length; i++) {
      numberOfCAtomsList.add(16);
      numberOfDbesList.add(0);
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

      Label chains = new Label("Chains");
      lipidGridPane.add(chains, 3, 0);
      Label carbons = new Label("Carbons");
      lipidGridPane.add(carbons, 4, 0);
      Label dbes = new Label("DBEs");
      lipidGridPane.add(dbes, 5, 0);
      for (int i = 0; i < lipidChainTypes.length; i++) {
        int chainNumber = i + 1;
        lipidGridPane.add(new Label("#" + chainNumber), 3, i + 1);
        TextField carbonTextField = new TextField(String.valueOf(numberOfCAtomsList.get(i)));
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
        lipidGridPane.add(carbonTextField, 4, i + 1);

        TextField dbeTextField = new TextField(String.valueOf(numberOfDbesList.get(i)));

        dbeTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
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
        lipidGridPane.add(dbeTextField, 5, i + 1);
      }

      ILipidClass newCustomLipidClass = new CustomLipidClass(name, abbr, lipidCategory,
          lipidMainClass, backboneFormula, lipidChainTypes, customLipidClassFragmentationRules);
      int totalNumberOfCarbons = numberOfCAtomsList.stream().mapToInt(Integer::intValue).sum();
      int totalNumberOfDBEs = numberOfDbesList.stream().mapToInt(Integer::intValue).sum();
      SpeciesLevelAnnotation speciesLevelAnnotation = LIPID_FACTORY.buildSpeciesLevelLipid(
          newCustomLipidClass, totalNumberOfCarbons, totalNumberOfDBEs, 0);

      Label speciesLevel = new Label("Species level: ");
      lipidGridPane.add(speciesLevel, 0, 0);
      Label annotationLabel = new Label(speciesLevelAnnotation.getAnnotation());
      lipidGridPane.add(annotationLabel, 1, 0, 2, 1);

      int[] carbonsInChains = numberOfCAtomsList.stream().mapToInt(Integer::intValue).toArray();
      int[] dbesInChains = numberOfDbesList.stream().mapToInt(Integer::intValue).toArray();
      int[] additionalOxygens = new int[numberOfDbesList.size()];

      Label molecularSpeciesLevel = new Label("Molecular species level: ");
      lipidGridPane.add(molecularSpeciesLevel, 0, 1);
      MolecularSpeciesLevelAnnotation molecularSpeciesLevelAnnotation = LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
          newCustomLipidClass, carbonsInChains, dbesInChains, additionalOxygens);
      Label molecularSpeciesAnnotationLabel = new Label(
          molecularSpeciesLevelAnnotation.getAnnotation());
      lipidGridPane.add(molecularSpeciesAnnotationLabel, 1, 1, 2, 1);

      Label formula = new Label("Molecular formula: ");
      lipidGridPane.add(formula, 0, 2);
      Label formulaLabel = new Label(
          MolecularFormulaManipulator.getString(speciesLevelAnnotation.getMolecularFormula()));
      lipidGridPane.add(formulaLabel, 1, 2);

      Label mass = new Label("Neutral mass: ");
      lipidGridPane.add(mass, 0, 3);
      Label massLabel = new Label(MZmineCore.getConfiguration().getMZFormat()
          .format(FormulaUtils.calculateMzRatio(speciesLevelAnnotation.getMolecularFormula())));
      lipidGridPane.add(massLabel, 1, 3);

      Label numberOfChains = new Label("Number of chains: ");
      lipidGridPane.add(numberOfChains, 0, 4);
      Label chainsLabel = new Label(
          String.valueOf(speciesLevelAnnotation.getLipidClass().getChainTypes().length));
      lipidGridPane.add(chainsLabel, 1, 4);

      Label ionNotations = new Label("Ion notations: ");
      lipidGridPane.add(ionNotations, 0, 5);
      StringBuilder sb = new StringBuilder();
      Label ionNotationsLabel = new Label();
      if (speciesLevelAnnotation.getLipidClass().getFragmentationRules() != null
          && speciesLevelAnnotation.getLipidClass().getFragmentationRules().length > 0) {
        Set<IonizationType> ionizationTypeList = new HashSet<>();
        for (LipidFragmentationRule fragmentationRule : speciesLevelAnnotation.getLipidClass()
            .getFragmentationRules()) {
          ionizationTypeList.add(fragmentationRule.getIonizationType());
        }
        for (IonizationType ionNotation : ionizationTypeList) {
          sb.append(ionNotation.getAdductName()).append(" ");
        }
      } else {
        sb.append("No ion notations found");
        ionNotationsLabel.setTooltip(
            new Tooltip("No ion notations found. Setup fragmentation rule"));
        ionNotationsLabel.setTextFill(
            MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColor());
      }
      ionNotationsLabel.setText(sb.toString());
      lipidGridPane.add(ionNotationsLabel, 1, 5);
      lipidPane.setTop(lipidGridPane);
      if (speciesLevelAnnotation.getLipidClass().getFragmentationRules() != null
          && speciesLevelAnnotation.getLipidClass().getFragmentationRules().length > 0) {
        GridPane lipidPlots = calcualteSpectraPlotGridPane(
            speciesLevelAnnotation.getLipidClass().getFragmentationRules(), speciesLevelAnnotation);
        lipidPane.setCenter(lipidPlots);
      }
      scrollPane.setContent(lipidPane);
      getParamPane().getCenterPane().setRight(scrollPane);
    }
  }

  private GridPane calcualteSpectraPlotGridPane(LipidFragmentationRule[] lipidFragmentationRules,
      SpeciesLevelAnnotation speciesLevelAnnotation) {
    GridPane gridPane = new GridPane();
    GridPane.setHgrow(gridPane, Priority.ALWAYS);
    GridPane.setVgrow(gridPane, Priority.ALWAYS);
    gridPane.setHgap(10);
    gridPane.setVgap(10);
    gridPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    Set<IonizationType> ionizationTypeList = new HashSet<>();
    Random random = new Random();
    for (LipidFragmentationRule fragmentationRule : lipidFragmentationRules) {
      ionizationTypeList.add(fragmentationRule.getIonizationType());
    }
    int i = 0;
    for (IonizationType ionizationType : ionizationTypeList) {
      List<LipidFragment> lipidFragments = new ArrayList<>();
      SpectraPlot spectraPlot = new SpectraPlot();
      for (LipidFragmentationRule fragmentationRule : lipidFragmentationRules) {
        if (fragmentationRule.getLipidFragmentInformationLevelType() != null
            && fragmentationRule.getLipidFragmentInformationLevelType()
            .equals(LipidAnnotationLevel.SPECIES_LEVEL)) {
          ILipidFragmentFactory lipidFragmentFactory = LipidAnnotationUtils.getLipidFragmentFactory(
              ionizationType,//
              speciesLevelAnnotation,//
              makeLipidAnnotationParameterSet(speciesLevelAnnotation, 16, 1),//
              null,//
              new LipidFragmentationRule[]{fragmentationRule},//
              new SimpleDataPoint(500, 100),//
              new MZTolerance(10000, 1).getToleranceRange(500),//
              speciesLevelAnnotation.getLipidClass().getCoreClass());
          addFragments(lipidFragments, lipidFragmentFactory);
        } else {
          for (int j = 0; j < numberOfCAtomsList.size(); j++) {
            ILipidFragmentFactory lipidFragmentFactory = LipidAnnotationUtils.getLipidFragmentFactory(
                ionizationType,//
                speciesLevelAnnotation,//
                makeLipidAnnotationParameterSet(speciesLevelAnnotation, numberOfCAtomsList.get(j),
                    numberOfDbesList.get(j)),//
                null,//
                new LipidFragmentationRule[]{fragmentationRule},//
                new SimpleDataPoint(500, 100),//
                new MZTolerance(10000, 1).getToleranceRange(500),//
                speciesLevelAnnotation.getLipidClass().getCoreClass());
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
      boolean addLipidFragment = true;
      for (LipidFragment lipidFragment : lipidFragments) {
        if (lipidFragment.getMzExact().equals(newLipidFragment.getMzExact())
            && lipidFragment.getChainLength().equals(newLipidFragment.getChainLength())) {
          addLipidFragment = false;
          break;
        }
      }
      if (addLipidFragment) {
        lipidFragmentsToAdd.add(newLipidFragment);
      }
    }
    lipidFragments.addAll(lipidFragmentsToAdd);
  }

  private ParameterSet makeLipidAnnotationParameterSet(
      SpeciesLevelAnnotation speciesLevelAnnotation, int numberOfCarbons, int numberofDbes) {
    var param = MZmineCore.getConfiguration().getModuleParameters(LipidAnnotationModule.class)
        .cloneParameterSet();
    //TODO dont do this, make a clone!
    var chainParam = new LipidAnnotationChainParameters();
    chainParam.setParameter(LipidAnnotationChainParameters.maxChainLength, numberOfCarbons);
    chainParam.setParameter(LipidAnnotationChainParameters.minChainLength, numberOfCarbons);
    chainParam.setParameter(LipidAnnotationChainParameters.maxDBEs, numberofDbes);
    chainParam.setParameter(LipidAnnotationChainParameters.minDBEs, numberofDbes);
    chainParam.setParameter(LipidAnnotationChainParameters.onlySearchForEvenChainLength, false);
    param.setParameter(LipidAnnotationParameters.lipidClasses,
        new Object[]{speciesLevelAnnotation.getLipidClass()});
    param.setParameter(LipidAnnotationParameters.lipidChainParameters, chainParam);
    param.setParameter(LipidAnnotationParameters.mzTolerance, new MZTolerance(1000, 1));
    param.setParameter(LipidAnnotationParameters.searchForMSMSFragments, true);
    param.getParameter(LipidAnnotationParameters.searchForMSMSFragments).getEmbeddedParameters()
        .setParameter(LipidAnnotationMSMSParameters.keepUnconfirmedAnnotations, false);
    param.getParameter(LipidAnnotationParameters.searchForMSMSFragments).getEmbeddedParameters()
        .setParameter(LipidAnnotationMSMSParameters.minimumMsMsScore, 0.0);
    param.getParameter(LipidAnnotationParameters.searchForMSMSFragments).getEmbeddedParameters()
        .setParameter(LipidAnnotationMSMSParameters.mzToleranceMS2, new MZTolerance(1000, 1));
    param.setParameter(LipidAnnotationParameters.advanced, false);
    var advanced = param.getEmbeddedParameterValue(LipidAnnotationParameters.advanced);
    advanced.setParameter(AdvancedLipidAnnotationParameters.IONS_TO_IGNORE,
        IonizationType.values());
    return param;
  }
}
