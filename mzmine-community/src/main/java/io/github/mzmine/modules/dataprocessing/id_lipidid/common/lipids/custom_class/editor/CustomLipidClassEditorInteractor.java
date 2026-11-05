package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.HashMap;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation.SpectrumTestResult;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation.SimulatedIon;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation.PeakListParser;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation.LipidSpectrumTester;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation.LipidRuleSimulator;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation.ExampleLipidChains;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationModule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationChainParameters;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.javafx.mvci.FxInteractor;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidMainClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClassJson;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.library.LocalLipidClassLibrary;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.util.FormulaUtils;
import io.mzio.general.Result;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Business logic of the custom lipid class editor: validation, loading and saving classes in the
 * local library, import and export.
 */
class CustomLipidClassEditorInteractor extends FxInteractor<CustomLipidClassEditorModel> {

  private static final Logger logger = Logger.getLogger(
      CustomLipidClassEditorInteractor.class.getName());

  private final LocalLipidClassLibrary library;
  // decision: field changes while loading a class do not mark the editor as modified
  private boolean loading = false;

  CustomLipidClassEditorInteractor(@NotNull CustomLipidClassEditorModel model,
      @NotNull LocalLipidClassLibrary library) {
    super(model);
    this.library = library;
  }

  /**
   * Validates the editor fields and updates the valid class, issues and warnings.
   */
  @Override
  public void updateModel() {
    final List<String> issues = new ArrayList<>();
    final List<String> warnings = new ArrayList<>();
    final String name = model.nameProperty().get().strip();
    final String abbr = model.abbrProperty().get().strip();
    final LipidCategories category = model.categoryProperty().get();
    final LipidMainClasses mainClass = model.mainClassProperty().get();
    final String backbone = model.backboneFormulaProperty().get().strip();
    final List<LipidChainType> chains = List.copyOf(model.getChains());

    if (name.isEmpty()) {
      issues.add("Enter a name");
    }
    if (abbr.isEmpty()) {
      issues.add("Enter an abbreviation");
    }
    if (category == null) {
      issues.add("Select a lipid category");
    }
    if (mainClass == null) {
      issues.add("Select a main class");
    } else if (category != null && mainClass.getLipidCategory() != category) {
      warnings.add("Main class %s belongs to %s, not %s".formatted(mainClass,
          mainClass.getLipidCategory(), category));
    }
    if (backbone.isEmpty()) {
      issues.add("Enter the backbone formula");
    } else if (FormulaUtils.createMajorIsotopeMolFormulaWithCharge(backbone) == null) {
      issues.add("Backbone formula %s cannot be read".formatted(backbone));
    }
    if (chains.isEmpty()) {
      issues.add("Add at least one chain");
    }
    final boolean sphingolipid = category == LipidCategories.SPHINGOLIPIDS;
    // decision: sphingolipid specific chains and rules are only allowed for Sphingolipids,
    // consistent with the module parameter check
    if (!sphingolipid && chains.stream().anyMatch(c -> c.name().startsWith("SPHINGOLIPID_"))) {
      issues.add("Sphingoid base chains need the category Sphingolipids");
    }

    final List<LipidFragmentationRule> rules = new ArrayList<>();
    if (model.getRules().isEmpty()) {
      issues.add("Add at least one ionization or fragmentation rule");
    }
    for (int i = 0; i < model.getRules().size(); i++) {
      final LipidRuleRow row = model.getRules().get(i);
      final Result check = row.validate();
      if (!check.isOk()) {
        issues.add("Rule %d: %s".formatted(i + 1, check.message()));
        continue;
      }
      final int ruleNumber = i + 1;
      if (!sphingolipid && row.getRuleType().map(LipidFragmentationRuleType::isSphingolipidSpecific)
          .orElse(false)) {
        issues.add("Rule %d is specific to sphingolipids and needs the category Sphingolipids"
            .formatted(ruleNumber));
        continue;
      }
      rules.add(row.toRule());
      row.getRuleType().ifPresent(type -> addRuleWarnings(ruleNumber, type, chains, warnings));
    }

    final CustomLipidClass built = issues.isEmpty() ? new CustomLipidClass(name, abbr, category,
        mainClass, backbone, chains.toArray(LipidChainType[]::new),
        rules.toArray(LipidFragmentationRule[]::new)) : null;
    model.getIssues().setAll(issues);
    model.getWarnings().setAll(warnings);
    model.validClassProperty().set(built);
    if (!loading) {
      model.modifiedProperty().set(true);
    }
  }

  private static void addRuleWarnings(int ruleNumber, @NotNull LipidFragmentationRuleType type,
      @NotNull List<LipidChainType> chains, @NotNull List<String> warnings) {
    final LipidChainType chain = type.getChainType();
    if (chain == LipidChainType.TWO_ACYL_CHAINS_COMBINED) {
      if (chains.stream().filter(c -> c == LipidChainType.ACYL_CHAIN).count() < 2) {
        warnings.add("Rule %d needs two acyl chains in the class".formatted(ruleNumber));
      }
    } else if (chain != null && !chains.contains(chain)) {
      final String target = type.getTarget().toString();
      warnings.add("Rule %d refers to a %s, which the class does not have".formatted(ruleNumber,
          Character.toLowerCase(target.charAt(0)) + target.substring(1)));
    }
  }

  /**
   * Shows a class in the editor.
   *
   * @param lipidClass the class to edit
   * @param saved      true if the class is in the library, false to edit it as a new class
   */
  void load(@NotNull ILipidClass lipidClass, boolean saved) {
    loading = true;
    try {
      final CustomLipidClass custom = CustomLipidClassJson.toCustom(lipidClass);
      model.originalProperty().set(saved ? custom : null);
      model.nameProperty().set(Objects.requireNonNullElse(custom.getName(), ""));
      model.abbrProperty().set(Objects.requireNonNullElse(custom.getAbbr(), ""));
      model.categoryProperty().set(custom.getCoreClass());
      model.mainClassProperty().set(custom.getMainClass());
      model.backboneFormulaProperty().set(
          Objects.requireNonNullElse(custom.getBackBoneFormula(), ""));
      model.getChains().setAll(
          custom.getChainTypes() == null ? List.of() : Arrays.asList(custom.getChainTypes()));
      model.getRules().setAll(custom.getFragmentationRules() == null ? List.of()
          : Arrays.stream(custom.getFragmentationRules()).filter(Objects::nonNull)
              .map(LipidRuleRow::of).toList());
      model.selectedRuleProperty().set(
          model.getRules().isEmpty() ? null : model.getRules().getFirst());
      model.exampleProperty().set(
          ExampleLipidChains.defaultsFor(model.getChains().toArray(LipidChainType[]::new)));
      model.getTestResults().clear();
      model.testStatusProperty().set("");
      model.editingProperty().set(true);
      updateModel();
    } finally {
      loading = false;
    }
    model.modifiedProperty().set(!saved);
  }

  /**
   * Starts a new empty class.
   */
  void newClass() {
    loading = true;
    try {
      model.originalProperty().set(null);
      model.nameProperty().set("");
      model.abbrProperty().set("");
      model.categoryProperty().set(null);
      model.mainClassProperty().set(null);
      model.backboneFormulaProperty().set("");
      model.getChains().clear();
      model.getRules().clear();
      model.selectedRuleProperty().set(null);
      model.getTestResults().clear();
      model.testStatusProperty().set("");
      model.editingProperty().set(true);
      updateModel();
    } finally {
      loading = false;
    }
    model.modifiedProperty().set(true);
  }

  /**
   * Turns the class in the editor into a new unsaved copy.
   */
  void duplicate() {
    model.originalProperty().set(null);
    model.nameProperty().set(model.nameProperty().get() + " (copy)");
    model.modifiedProperty().set(true);
  }

  /**
   * @return true if saving would replace another class with the same abbreviation and name
   */
  boolean wouldOverwriteOtherClass() {
    final CustomLipidClass valid = model.validClassProperty().get();
    if (valid == null) {
      return false;
    }
    final CustomLipidClass original = model.originalProperty().get();
    final boolean sameKey = original != null && original.getAbbr().equalsIgnoreCase(valid.getAbbr())
        && original.getName().equalsIgnoreCase(valid.getName());
    return !sameKey && library.contains(valid.getAbbr(), valid.getName());
  }

  /**
   * Saves the valid class to the library.
   *
   * @return the saved class or null if the class has issues
   */
  @Nullable CustomLipidClass save() {
    final CustomLipidClass valid = model.validClassProperty().get();
    if (valid == null) {
      return null;
    }
    final CustomLipidClass original = model.originalProperty().get();
    if (original == null) {
      library.save(valid);
    } else {
      library.replace(original, valid);
    }
    model.originalProperty().set(valid);
    model.modifiedProperty().set(false);
    return valid;
  }

  /**
   * Discards changes and shows the saved class again or clears the editor for new classes.
   */
  void revert() {
    final CustomLipidClass original = model.originalProperty().get();
    if (original != null) {
      load(original, true);
    } else {
      clear();
    }
  }

  /**
   * Closes the class in the editor.
   */
  void clear() {
    newClass();
    model.editingProperty().set(false);
    model.modifiedProperty().set(false);
  }

  /**
   * @param lipidClass removed from the library
   */
  void delete(@NotNull CustomLipidClass lipidClass) {
    library.delete(lipidClass);
    final CustomLipidClass original = model.originalProperty().get();
    if (original != null && original.getAbbr().equals(lipidClass.getAbbr())
        && original.getName().equals(lipidClass.getName())) {
      clear();
    }
  }

  /**
   * Reads classes from a file and saves them to the library. Existing classes with the same
   * abbreviation and name are replaced.
   *
   * @return the imported classes
   */
  @NotNull List<CustomLipidClass> importFile(@NotNull Path file) throws IOException {
    final List<CustomLipidClass> classes = CustomLipidClassJson.read(file);
    addToLibrary(classes);
    return classes;
  }

  /**
   * @param classes saved to the library, replacing classes with the same abbreviation and name
   */
  void addToLibrary(@NotNull Collection<CustomLipidClass> classes) {
    for (final CustomLipidClass lipidClass : classes) {
      library.save(lipidClass);
    }
  }

  /**
   * @return the number of classes that already exist in the library
   */
  long countExisting(@NotNull Collection<CustomLipidClass> classes) {
    return classes.stream().filter(c -> library.contains(c.getAbbr(), c.getName())).count();
  }

  /**
   * @param file    target file
   * @param classes written in the mzmine import format
   */
  void exportFile(@NotNull Path file, @NotNull Collection<CustomLipidClass> classes)
      throws IOException {
    CustomLipidClassJson.write(file, classes);
  }

  /**
   * Calculates the m/z of each valid rule and the precursor m/z of each adduct for the example
   * lipid. Works with a partially valid class, so the user sees m/z values while editing.
   */
  void simulate() {
    final List<LipidChainType> chains = List.copyOf(model.getChains());
    if (model.exampleProperty().get().size() != chains.size()) {
      model.exampleProperty().set(
          ExampleLipidChains.defaultsFor(chains.toArray(LipidChainType[]::new)));
    }
    final Map<LipidRuleRow, List<SimulatedIon>> ions = new HashMap<>();
    final Map<IonizationType, Double> precursors = new HashMap<>();
    String exampleName = "";

    final String backbone = model.backboneFormulaProperty().get().strip();
    final List<LipidRuleRow> validRows = model.getRules().stream()
        .filter(row -> row.validate().isOk()).toList();
    if (!chains.isEmpty() && !backbone.isEmpty()
        && FormulaUtils.createMajorIsotopeMolFormulaWithCharge(backbone) != null) {
      final String abbr = model.abbrProperty().get().isBlank() ? "?" : model.abbrProperty().get();
      final CustomLipidClass simulationClass = new CustomLipidClass("simulation", abbr.strip(),
          Objects.requireNonNullElse(model.categoryProperty().get(), LipidCategories.GLYCEROLIPIDS),
          Objects.requireNonNullElse(model.mainClassProperty().get(),
              LipidMainClasses.OTHERGLYCEROLIPIDS), backbone,
          chains.toArray(LipidChainType[]::new),
          validRows.stream().map(LipidRuleRow::toRule).toArray(LipidFragmentationRule[]::new));
      try {
        final ExampleLipidChains example = model.exampleProperty().get();
        final List<List<SimulatedIon>> simulated = LipidRuleSimulator.simulate(simulationClass,
            example);
        for (int i = 0; i < validRows.size(); i++) {
          ions.put(validRows.get(i), simulated.get(i));
        }
        final SpeciesLevelAnnotation lipid = LipidRuleSimulator.exampleLipid(simulationClass,
            example);
        if (lipid != null) {
          exampleName = lipid.getAnnotation();
          for (final LipidRuleRow row : model.getRules()) {
            final IonizationType ion = row.ionizationProperty().get();
            final Double mz = LipidRuleSimulator.precursorMz(lipid, ion);
            if (mz != null) {
              precursors.put(ion, mz);
            }
          }
        }
      } catch (RuntimeException e) {
        // decision: an unusual class while editing must not break the editor, show no m/z
        logger.log(Level.FINE, "Cannot simulate lipid class", e);
      }
    }
    model.getSimulatedIons().clear();
    model.getSimulatedIons().putAll(ions);
    model.getPrecursorMzs().clear();
    model.getPrecursorMzs().putAll(precursors);
    model.exampleLipidNameProperty().set(exampleName);
  }

  /**
   * Tests the class in the editor against the pasted spectrum. Runs on a task thread.
   *
   * @return results, best species first, or an error message
   */
  @NotNull TestOutcome testSpectrum(@Nullable CustomLipidClass lipidClass,
      @Nullable IonizationType ionization, @NotNull String precursorText,
      @NotNull String peakText, double ms1Ppm, double ms2Ppm) {
    if (lipidClass == null) {
      return TestOutcome.error("Fix the issues of the class first.");
    }
    if (ionization == null) {
      return TestOutcome.error("Select the adduct of the precursor.");
    }
    final double precursor;
    try {
      precursor = Double.parseDouble(precursorText.strip().replace(',', '.'));
    } catch (NumberFormatException e) {
      return TestOutcome.error("Enter the precursor m/z.");
    }
    final DataPoint[] peaks = PeakListParser.parse(peakText);
    if (peaks.length == 0) {
      return TestOutcome.error("Paste the MS2 peaks, one m/z and intensity per line.");
    }
    final LipidAnnotationChainParameters chains = ConfigService.getConfiguration()
        .getModuleParameters(LipidAnnotationModule.class)
        .getParameter(LipidAnnotationParameters.lipidChainParameters).getEmbeddedParameters();
    final List<SpectrumTestResult> results = LipidSpectrumTester.test(lipidClass, ionization,
        precursor, peaks, new MZTolerance(0.002, ms1Ppm), new MZTolerance(0.002, ms2Ppm),
        chains);
    if (results.isEmpty()) {
      return TestOutcome.error(
          "No %s species matches m/z %.4f as %s. Check the adduct, the backbone formula and the chain length range of the lipid annotation module."
              .formatted(lipidClass.getAbbr(), precursor, ionization.getAdductName()));
    }
    return new TestOutcome(results, "%d peaks tested".formatted(peaks.length));
  }

  /**
   * @param results results of a spectrum test or empty on error
   * @param message summary or error message
   */
  record TestOutcome(@NotNull List<SpectrumTestResult> results, @NotNull String message) {

    static @NotNull TestOutcome error(@NotNull String message) {
      return new TestOutcome(List.of(), message);
    }
  }
}
