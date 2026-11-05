package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class;

import org.jfree.chart.axis.NumberAxis;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation.ExampleLipidChains;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
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
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationChainParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationMSMSParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationModule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.fragmentation.ILipidFragmentFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.fragmentation.LipidFragmentFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidFactory;
import io.github.mzmine.modules.visualization.spectra.matchedlipid.MatchedLipidLabelGenerator;
import io.github.mzmine.modules.visualization.spectra.matchedlipid.StyledLipidSpectrumBarRenderer;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.jetbrains.annotations.Nullable;

/**
 * Preview of a lipid class: example annotations, formula, ion m/z and simulated MS2 spectra for
 * each ionization of the fragmentation rules. The chain lengths of the example lipid can be
 * changed by the user.
 */
public class CustomLipidClassPreviewPane extends BorderPane {

  private static final Logger logger = Logger.getLogger(
      CustomLipidClassPreviewPane.class.getName());

  private final List<Integer> numberOfCAtomsList = new ArrayList<>();
  private final List<Integer> numberOfDbesList = new ArrayList<>();
  private final RawDataFile dummyFile = RawDataFile.createDummyFile();
  private final Scan simpleScan = new SimpleScan(dummyFile, -1, 2, 0.1F, null, new double[]{500},
      new double[]{100}, MassSpectrumType.ANY, PolarityType.ANY, "Pseudo", null);

  private static final MassList MASS_LIST = SimpleMassList.create(null,
      new SimpleDataPoint[]{new SimpleDataPoint(500, 100)});

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();
  private static final Random random = new Random();
  private BorderPane lipidPane;
  private GridPane lipidGridPane;
  private @Nullable ILipidClass lipidClass;
  private @Nullable Consumer<ExampleLipidChains> onExampleChanged;
  private final Map<IonizationType, SpectraPlot> plots = new HashMap<>();
  private @Nullable IonizationType highlightIonization;
  private List<Double> highlightMzs = List.of();
  private static final Color HIGHLIGHT_COLOR = new Color(255, 140, 0);

  /**
   * Shows the preview of a lipid class.
   *
   * @param lipidClass the class or null to clear the preview
   */
  public void setLipidClass(@Nullable ILipidClass lipidClass) {
    this.lipidClass = lipidClass;
    final LipidChainType[] chains = lipidClass == null ? null : lipidClass.getChainTypes();
    if (chains != null && chains.length != numberOfCAtomsList.size()) {
      adjustExampleCarbonAndDbeNumbers(chains);
      notifyExampleChanged();
    }
    try {
      updateExampleLipid();
    } catch (RuntimeException e) {
      // decision: an incomplete class while editing must not break the editor
      logger.log(Level.FINE, "Cannot preview lipid class", e);
      setCenter(new Label("Preview not available: " + e.getMessage()));
    }
  }

  private void adjustExampleCarbonAndDbeNumbers(LipidChainType[] lipidChainTypes) {
    final ExampleLipidChains example = ExampleLipidChains.defaultsFor(lipidChainTypes);
    numberOfCAtomsList.clear();
    numberOfDbesList.clear();
    for (int i = 0; i < example.size(); i++) {
      numberOfCAtomsList.add(example.carbons()[i]);
      numberOfDbesList.add(example.dbes()[i]);
    }
  }

  /**
   * @return the chains of the example lipid that the preview shows
   */
  public @NotNull ExampleLipidChains getExample() {
    return new ExampleLipidChains(numberOfCAtomsList.stream().mapToInt(Integer::intValue).toArray(),
        numberOfDbesList.stream().mapToInt(Integer::intValue).toArray());
  }

  /**
   * @param onExampleChanged called when the user edits the example chains or the chains change
   */
  public void setOnExampleChanged(@Nullable Consumer<ExampleLipidChains> onExampleChanged) {
    this.onExampleChanged = onExampleChanged;
  }

  private void notifyExampleChanged() {
    if (onExampleChanged != null) {
      onExampleChanged.accept(getExample());
    }
  }

  /**
   * Marks the m/z values of a rule in the simulated spectrum of its adduct.
   *
   * @param ionization adduct of the rule or null to remove the marks
   * @param mzs        m/z values to mark
   */
  public void highlight(@Nullable IonizationType ionization, @NotNull List<Double> mzs) {
    highlightIonization = ionization;
    highlightMzs = List.copyOf(mzs);
    applyHighlight();
  }

  private void applyHighlight() {
    // the renderer asks isHighlighted for every bar, a redraw is enough
    for (final SpectraPlot plot : plots.values()) {
      plot.getChart().fireChartChanged();
    }
  }

  private boolean isHighlighted(@NotNull IonizationType ionization, double mz) {
    if (ionization != highlightIonization) {
      return false;
    }
    for (final double highlighted : highlightMzs) {
      if (Math.abs(highlighted - mz) < 1e-4) {
        return true;
      }
    }
    return false;
  }


  private void updateExampleLipid() {
    final ILipidClass lipidClass = this.lipidClass;
    if (lipidClass == null || lipidClass.getChainTypes() == null) {
      setCenter(null);
      return;
    }
    final LipidChainType[] lipidChainTypes = lipidClass.getChainTypes();
    if (lipidChainTypes.length > 0 && !numberOfCAtomsList.isEmpty()
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
              notifyExampleChanged();
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
            notifyExampleChanged();
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
              notifyExampleChanged();
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
            notifyExampleChanged();
          } catch (NumberFormatException e) {
            e.printStackTrace();
          }
        });
        lipidGridPane.add(dbeTextField, 5, chainIndex + 1);
      }

      final ILipidClass newCustomLipidClass = lipidClass;
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
          FormulaUtils.getFormulaString(speciesLevelAnnotation.getMolecularFormula()));
      lipidGridPane.add(formulaLabel, 1, 3);

      Label mass = new Label("Neutral mass: ");
      lipidGridPane.add(mass, 0, 4);
      Label massLabel = new Label(ConfigService.getConfiguration().getMZFormat()
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
          final String ionMz = ionNotation.ionizeFormula(
              speciesLevelAnnotation.getMolecularFormula()).map(
              ionFormula -> MZmineCore.getConfiguration().getMZFormat()
                  .format(FormulaUtils.calculateMzRatio(ionFormula))).orElse("NA");
          lipidGridPane.add(new Label(ionMz), 2, ionNotationStartColumn);
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
      setCenter(lipidPane);
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
    plots.clear();

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
        final Color positiveColor = ConfigService.getConfiguration().getDefaultColorPalette()
            .getPositiveColorAWT();
        PlotXYDataProvider fragmentDataProvider = new LipidSpectrumProvider(lipidFragments,
            fragmentScanDps.stream().mapToDouble(DataPoint::getMZ).toArray(),
            fragmentScanDps.stream().mapToDouble(DataPoint::getIntensity).toArray(),
            "In-silico fragments", positiveColor);
        ColoredXYDataset fragmentDataSet = new ColoredXYDataset(fragmentDataProvider,
            RunOption.NEW_THREAD);
        MatchedLipidLabelGenerator matchedLipidLabelGenerator = new MatchedLipidLabelGenerator(
            spectraPlot, lipidFragments, true, false);
        final IonizationType plotIonization = ionizationType;
        final StyledLipidSpectrumBarRenderer renderer = new HighlightingLipidBarRenderer(true,
            positiveColor, mz -> isHighlighted(plotIonization, mz), HIGHLIGHT_COLOR);
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelGenerator(matchedLipidLabelGenerator);
        spectraPlot.addDataSet(fragmentDataSet, positiveColor, true, renderer,
            matchedLipidLabelGenerator, true, true);
        spectraPlot.getChart().setBackgroundPaint((new Color(0, 0, 0, 0)));
        spectraPlot.getXYPlot().setBackgroundPaint((new Color(0, 0, 0, 0)));
        spectraPlot.setTitle(ionizationType.getAdductName(), "");
        // a single fragment would otherwise give an axis with identical tick labels
        ((NumberAxis) spectraPlot.getXYPlot().getDomainAxis()).setAutoRangeMinimumSize(20d);
        spectraPlot.setMinSize(400, 200);
        spectraPlot.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        GridPane.setHgrow(spectraPlot, Priority.ALWAYS);
        GridPane.setVgrow(spectraPlot, Priority.ALWAYS);
        gridPane.add(spectraPlot, 0, i);
        plots.put(ionizationType, spectraPlot);
        i++;
      }
    }
    applyHighlight();
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
    var param = ConfigService.getConfiguration().getModuleParameters(LipidAnnotationModule.class)
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
    param.setParameter(LipidAnnotationParameters.minimumOverallQualityScore, 40.0);
    param.setParameter(LipidAnnotationParameters.searchForMSMSFragments, true);
    param.getParameter(LipidAnnotationParameters.searchForMSMSFragments).getEmbeddedParameters()
        .setParameter(LipidAnnotationMSMSParameters.keepUnconfirmedAnnotations, false);
    param.getParameter(LipidAnnotationParameters.searchForMSMSFragments).getEmbeddedParameters()
        .setParameter(LipidAnnotationMSMSParameters.mzToleranceMS2, new MZTolerance(1000, 1));
    return param;
  }
}
