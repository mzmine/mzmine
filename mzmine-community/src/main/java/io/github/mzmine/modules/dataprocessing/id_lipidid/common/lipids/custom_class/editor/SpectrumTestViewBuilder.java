package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor.CustomLipidClassEditorEvent.TestSpectrum;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation.AnnotationTestResult;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation.MatchedRuleFragment;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation.RuleTestResult;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation.SimulatedIon;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation.SpectrumTestResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tests the class against a pasted MS2 spectrum: which rules match which peaks, which expected
 * fragments are missing and which annotation the lipid annotation module would report.
 */
class SpectrumTestViewBuilder {

  private static final String ISSUE_STYLE = "-fx-text-fill: #d9534f;";
  private static final String OK_STYLE = "-fx-text-fill: -fx-accent;";

  private final CustomLipidClassEditorModel model;
  private final ObjectProperty<@Nullable SpectrumTestResult> shown = new SimpleObjectProperty<>();

  SpectrumTestViewBuilder(@NotNull CustomLipidClassEditorModel model) {
    this.model = model;
  }

  @NotNull Region build() {
    // adducts of the class, the test adduct follows the rules
    final ObservableList<IonizationType> adducts = FXCollections.observableArrayList();
    final Runnable updateAdducts = () -> {
      adducts.setAll(LipidRulesViewBuilder.adductsOf(model.getRules()));
      final IonizationType current = model.testIonizationProperty().get();
      if (current == null || !adducts.contains(current)) {
        model.testIonizationProperty().set(adducts.isEmpty() ? null : adducts.getFirst());
      }
    };
    model.getRules().addListener((ListChangeListener<LipidRuleRow>) _ -> updateAdducts.run());
    updateAdducts.run();
    final ComboBox<IonizationType> adduct = new ComboBox<>(adducts);
    adduct.valueProperty().bindBidirectional(model.testIonizationProperty());
    adduct.setConverter(new StringConverter<>() {
      @Override
      public String toString(IonizationType ion) {
        return ion == null ? "" : ion.getAdductName();
      }

      @Override
      public IonizationType fromString(String s) {
        return null;
      }
    });

    final TextField precursor = FxTextFields.newTextField(model.testPrecursorMzProperty(),
        "Measured m/z of the precursor ion");
    precursor.setPromptText("e.g. 716.5236");
    final TextField ms1 = numberField(model.testMs1PpmProperty(), "MS1 tolerance in ppm");
    final TextField ms2 = numberField(model.testMs2PpmProperty(), "MS2 tolerance in ppm");

    final TextArea peaks = new TextArea();
    peaks.textProperty().bindBidirectional(model.testPeaksProperty());
    peaks.setPromptText("""
        Paste the MS2 peaks, one m/z and intensity per line, e.g., copied from mzmine, \
        a spreadsheet or an MGF file:
        196.0380  1200
        255.2330  800
        281.2486  1000""");
    peaks.setWrapText(false);
    peaks.setPrefRowCount(8);

    final Button test = FxButtons.createButton("Test", "Test the class with this spectrum",
        () -> model.sendEvent(new TestSpectrum()));
    FxButtons.disableIf(test, model.validClassProperty().isNull());
    final Button example = FxButtons.createButton("Use example lipid",
        "Fill in the simulated spectrum of the example lipid to see how a test works",
        this::fillExample);
    final Label status = FxLabels.newLabel(model.testStatusProperty());
    status.setWrapText(true);

    final VBox input = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true,
        FxLayout.newGrid2Col(FxLabels.newLabel("Adduct"), adduct, FxLabels.newLabel("Precursor m/z"),
            precursor, FxLabels.newLabel("MS1 / MS2 ppm"),
            FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, ms1, ms2)), peaks,
        FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, test, example), status);

    final VBox content = FxLayout.newVBox(Pos.TOP_LEFT, FxLayout.DEFAULT_PADDING_INSETS, true,
        FxLabels.newBoldTitle("Test with a measured spectrum"), input, buildResults());
    final ScrollPane scroll = new ScrollPane(content);
    scroll.setFitToWidth(true);
    return scroll;
  }

  private static @NotNull TextField numberField(@NotNull ObjectProperty<Double> property,
      @NotNull String tooltip) {
    final TextField field = new TextField();
    field.setPrefColumnCount(4);
    field.setTooltip(new Tooltip(tooltip));
    final var formatter = new TextFormatter<>(new DoubleStringConverter(),
        property.get());
    formatter.valueProperty().bindBidirectional(property);
    field.setTextFormatter(formatter);
    return field;
  }

  /**
   * Fills precursor and peaks with the simulated spectrum of the example lipid.
   */
  private void fillExample() {
    final IonizationType ion = model.testIonizationProperty().get();
    if (ion == null) {
      return;
    }
    final Double precursor = model.getPrecursorMzs().get(ion);
    if (precursor != null) {
      model.testPrecursorMzProperty().set("%.4f".formatted(precursor));
    }
    final List<String> lines = new ArrayList<>();
    for (final LipidRuleRow row : model.getRules()) {
      if (row.ionizationProperty().get() != ion) {
        continue;
      }
      final List<SimulatedIon> ions = model.getSimulatedIons().get(row);
      if (ions != null) {
        ions.forEach(i -> lines.add("%.4f\t1000".formatted(i.mz())));
      }
    }
    model.testPeaksProperty().set(String.join("\n", lines));
  }

  private @NotNull Node buildResults() {
    final ComboBox<SpectrumTestResult> species = new ComboBox<>(model.getTestResults());
    species.setConverter(new StringConverter<>() {
      @Override
      public String toString(SpectrumTestResult r) {
        return r == null ? "" : "%s  (precursor Δ %.1f ppm)".formatted(r.species(), r.precursorPpm());
      }

      @Override
      public SpectrumTestResult fromString(String s) {
        return null;
      }
    });
    species.valueProperty().bindBidirectional(shown);
    model.getTestResults().addListener((ListChangeListener<SpectrumTestResult>) _ -> shown.set(
        model.getTestResults().isEmpty() ? null : model.getTestResults().getFirst()));

    final Label summary = FxLabels.newBoldLabel("");
    final VBox annotations = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true);

    final TableView<RuleTestResult> rules = new TableView<>();
    rules.setPrefHeight(240);
    rules.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    rules.setPlaceholder(FxLabels.newLabel("No rules for this adduct."));
    final TableColumn<RuleTestResult, String> status = column("", r -> r.isMatched() ? "✓" : "✗");
    status.setMaxWidth(30);
    status.setMinWidth(30);
    status.setCellFactory(_ -> new TableCell<>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty ? null : item);
        setStyle(empty ? null : "✓".equals(item) ? OK_STYLE : ISSUE_STYLE);
      }
    });
    rules.getColumns().addAll(List.of(status,
        column("Rule", r -> LipidRuleRow.of(r.rule()).describe()),
        column("Matched peaks", r -> r.matches().stream().limit(3)
            .map(m -> "%.4f".formatted(m.observedMz())).collect(Collectors.joining(", "))
            + (r.matches().size() > 3 ? ", …" : "")),
        column("Δ ppm", r -> r.matches().isEmpty() ? ""
            : "%.1f".formatted(r.matches().getFirst().ppm())),
        column("Chains", r -> r.matches().stream().map(MatchedRuleFragment::chain)
            .filter(Objects::nonNull).distinct().collect(Collectors.joining(", ")))));
    // decision: selecting a result opens the rule in the form, so the user can fix it directly
    rules.getSelectionModel().selectedItemProperty().addListener((_, _, result) -> {
      if (result != null && result.ruleIndex() < model.getRules().size()) {
        model.selectedRuleProperty().set(model.getRules().get(result.ruleIndex()));
      }
    });

    shown.addListener((_, _, result) -> {
      if (result == null) {
        summary.setText("");
        annotations.getChildren().clear();
        rules.getItems().clear();
        return;
      }
      summary.setText("%d of %d rules matched · %d of %d peaks explained".formatted(
          result.matchedRules(), result.rules().size(), result.explainedPeaks(),
          result.totalPeaks()));
      annotations.getChildren().setAll(annotationLabels(result.annotations()));
      rules.getItems().setAll(result.rules());
    });

    final VBox results = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true,
        FxLabels.newBoldLabel("Results"), species, summary, annotations, rules);
    results.visibleProperty().bind(shown.isNotNull());
    results.managedProperty().bind(results.visibleProperty());
    VBox.setVgrow(rules, Priority.ALWAYS);
    return results;
  }

  private static @NotNull List<Node> annotationLabels(
      @NotNull List<AnnotationTestResult> annotations) {
    if (annotations.isEmpty()) {
      final Label none = FxLabels.newLabel(
          "No annotation: the matched fragments are not enough for the lipid annotation module. "
              + "Check the major rules of this adduct.");
      none.setWrapText(true);
      none.setStyle(ISSUE_STYLE);
      return List.of(none);
    }
    return annotations.stream().map(a -> (Node) FxLabels.newLabel(
        "%s  ·  %s  ·  MS2 score %.0f %%".formatted(a.annotation(),
            a.molecularSpecies() ? "molecular species" : "species level", a.msmsScore() * 100)))
        .toList();
  }

  private static @NotNull TableColumn<RuleTestResult, String> column(@NotNull String title,
      @NotNull Function<RuleTestResult, String> value) {
    final TableColumn<RuleTestResult, String> column = new TableColumn<>(title);
    column.setCellValueFactory(cd -> new ReadOnlyStringWrapper(value.apply(cd.getValue())));
    return column;
  }
}
