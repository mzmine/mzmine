package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor;

import javafx.collections.ObservableMap;
import java.util.List;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation.SpectrumTestResult;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation.SimulatedIon;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation.ExampleLipidChains;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidMainClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.library.CustomLipidClassPreset;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import java.util.function.Consumer;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * State of the custom lipid class editor: the local library and the class being edited.
 */
public class CustomLipidClassEditorModel {

  // library
  private final ObservableList<CustomLipidClassPreset> libraryPresets;
  private final StringProperty librarySearch = new SimpleStringProperty("");
  private final ObjectProperty<@Nullable CustomLipidClassPreset> selectedPreset = new SimpleObjectProperty<>();

  // class being edited, original is null for a new class that was not saved yet
  private final ObjectProperty<@Nullable CustomLipidClass> original = new SimpleObjectProperty<>();
  private final StringProperty name = new SimpleStringProperty("");
  private final StringProperty abbr = new SimpleStringProperty("");
  private final ObjectProperty<@Nullable LipidCategories> category = new SimpleObjectProperty<>();
  private final ObjectProperty<@Nullable LipidMainClasses> mainClass = new SimpleObjectProperty<>();
  private final StringProperty backboneFormula = new SimpleStringProperty("");
  private final ObservableList<LipidChainType> chains = FXCollections.observableArrayList();
  // extractor: edits inside a row are list changes, so validation runs on every edit
  private final ObservableList<LipidRuleRow> rules = FXCollections.observableArrayList(
      row -> new Observable[]{row.ionizationProperty(), row.targetProperty(),
          row.observationProperty(), row.formulaProperty(), row.plusFormulaProperty(),
          row.minusFormulaProperty(),
          row.levelProperty(), row.ratingProperty(), row.relativeIntensityProperty()});

  // derived state, updated by the interactor
  private final ObservableList<String> issues = FXCollections.observableArrayList();
  private final ObservableList<String> warnings = FXCollections.observableArrayList();
  private final ObjectProperty<@Nullable CustomLipidClass> validClass = new SimpleObjectProperty<>();
  private final BooleanProperty modified = new SimpleBooleanProperty(false);
  private final BooleanProperty editing = new SimpleBooleanProperty(false);
  private @NotNull Consumer<CustomLipidClassEditorEvent> eventHandler = _ -> {
  };

  // rule editing and simulation
  private final ObjectProperty<@Nullable LipidRuleRow> selectedRule = new SimpleObjectProperty<>();
  private final ObjectProperty<@NotNull ExampleLipidChains> example = new SimpleObjectProperty<>(
      new ExampleLipidChains(new int[0], new int[0]));
  private final ObservableMap<LipidRuleRow, List<SimulatedIon>> simulatedIons = FXCollections.observableHashMap();
  private final ObservableMap<IonizationType, Double> precursorMzs = FXCollections.observableHashMap();
  private final StringProperty exampleLipidName = new SimpleStringProperty("");

  // test with a measured spectrum
  private final ObjectProperty<@Nullable IonizationType> testIonization = new SimpleObjectProperty<>();
  private final StringProperty testPrecursorMz = new SimpleStringProperty("");
  private final StringProperty testPeaks = new SimpleStringProperty("");
  private final ObjectProperty<@NotNull Double> testMs1Ppm = new SimpleObjectProperty<>(5d);
  private final ObjectProperty<@NotNull Double> testMs2Ppm = new SimpleObjectProperty<>(10d);
  private final ObservableList<SpectrumTestResult> testResults = FXCollections.observableArrayList();
  private final StringProperty testStatus = new SimpleStringProperty("");

  /**
   * @param libraryPresets live list of the local library
   */
  public CustomLipidClassEditorModel(@NotNull ObservableList<CustomLipidClassPreset> libraryPresets) {
    this.libraryPresets = libraryPresets;
  }

  public ObservableList<CustomLipidClassPreset> getLibraryPresets() {
    return libraryPresets;
  }

  public StringProperty librarySearchProperty() {
    return librarySearch;
  }

  public ObjectProperty<@Nullable CustomLipidClassPreset> selectedPresetProperty() {
    return selectedPreset;
  }

  public ObjectProperty<@Nullable CustomLipidClass> originalProperty() {
    return original;
  }

  public StringProperty nameProperty() {
    return name;
  }

  public StringProperty abbrProperty() {
    return abbr;
  }

  public ObjectProperty<@Nullable LipidCategories> categoryProperty() {
    return category;
  }

  public ObjectProperty<@Nullable LipidMainClasses> mainClassProperty() {
    return mainClass;
  }

  public StringProperty backboneFormulaProperty() {
    return backboneFormula;
  }

  public ObservableList<LipidChainType> getChains() {
    return chains;
  }

  public ObservableList<LipidRuleRow> getRules() {
    return rules;
  }

  /**
   * @return errors that prevent saving the class
   */
  public ObservableList<String> getIssues() {
    return issues;
  }

  /**
   * @return hints that do not prevent saving
   */
  public ObservableList<String> getWarnings() {
    return warnings;
  }

  /**
   * @return the class built from the editor fields or null if there are issues
   */
  public ObjectProperty<@Nullable CustomLipidClass> validClassProperty() {
    return validClass;
  }

  /**
   * @return true if the editor fields differ from the saved class
   */
  public BooleanProperty modifiedProperty() {
    return modified;
  }

  /**
   * @return the rule shown in the rule form
   */
  public ObjectProperty<@Nullable LipidRuleRow> selectedRuleProperty() {
    return selectedRule;
  }

  /**
   * @return chains of the example lipid used for the preview and the m/z of each rule
   */
  public ObjectProperty<@NotNull ExampleLipidChains> exampleProperty() {
    return example;
  }

  /**
   * @return the ions each valid rule produces for the example lipid
   */
  public ObservableMap<LipidRuleRow, List<SimulatedIon>> getSimulatedIons() {
    return simulatedIons;
  }

  /**
   * @return the precursor m/z of the example lipid for each adduct of the class
   */
  public ObservableMap<IonizationType, Double> getPrecursorMzs() {
    return precursorMzs;
  }

  /**
   * @return species name of the example lipid, e.g., PE 34:1
   */
  public StringProperty exampleLipidNameProperty() {
    return exampleLipidName;
  }

  public ObjectProperty<@Nullable IonizationType> testIonizationProperty() {
    return testIonization;
  }

  public StringProperty testPrecursorMzProperty() {
    return testPrecursorMz;
  }

  /**
   * @return the pasted peak list
   */
  public StringProperty testPeaksProperty() {
    return testPeaks;
  }

  public ObjectProperty<@NotNull Double> testMs1PpmProperty() {
    return testMs1Ppm;
  }

  public ObjectProperty<@NotNull Double> testMs2PpmProperty() {
    return testMs2Ppm;
  }

  /**
   * @return results for the species that match the precursor, best first
   */
  public ObservableList<SpectrumTestResult> getTestResults() {
    return testResults;
  }

  public StringProperty testStatusProperty() {
    return testStatus;
  }

  /**
   * @param event user action, handled by the controller
   */
  public void sendEvent(@NotNull CustomLipidClassEditorEvent event) {
    eventHandler.accept(event);
  }

  void setEventHandler(@NotNull Consumer<CustomLipidClassEditorEvent> eventHandler) {
    this.eventHandler = eventHandler;
  }

  /**
   * @return true if a class is open in the editor
   */
  public BooleanProperty editingProperty() {
    return editing;
  }
}
