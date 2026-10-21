package io.github.mzmine.modules.tools.tools_autoparam.optimizer.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.ConfigService;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisabledOnOs(value = OS.LINUX, disabledReason = "JavaFX needs a display; Linux CI is headless.")
class OptimizationResultsInspectionTest {
  @BeforeAll
  static void toolkit() {
    FxThread.initJavaFx();
    Platform.setImplicitExit(false);
    ConfigService.getPreferences();
  }

  @Test
  void externalApplicationRetainsNativeResultsWithOnlyApplyAndExport() throws Exception {
    final FutureTask<Void> test = new FutureTask<>(() -> {
      final var model = new OptimizationResultModel(false);
      final var solution = new org.moeaframework.core.Solution(0, 1);
      model.getDisplayedSolutions().add(solution);
      model.optimizationRunningProperty().set(false);
      final AtomicReference<org.moeaframework.core.Solution> applied = new AtomicReference<>();
      final BorderPane inspection = (BorderPane) new OptimizationResultsViewBuilder(model,
          () -> applied.set(model.getSelectedSolution()), null, () -> {}, null, null, null).build();
      final ButtonBar buttons = (ButtonBar) inspection.getBottom();
      assertEquals(java.util.List.of("Export to .csv", "Apply"), buttons.getButtons().stream()
          .map(node -> ((Button) node).getText()).toList());
      final BorderPane content = (BorderPane) inspection.getCenter();
      final TableView<?> solutionTable = (TableView<?>) content.getBottom();
      solutionTable.getSelectionModel().select(0);
      final Button apply = (Button) buttons.getButtons().get(1);
      assertFalse(apply.isDisable());
      apply.fire();
      assertTrue(applied.get() == solution);
      assertTrue(content.getCenter() != null);

      final BorderPane normal = (BorderPane) new OptimizationResultsViewBuilder(model, () -> {},
          () -> {}, () -> {}, () -> {}, null, null).build();
      assertEquals(4, ((ButtonBar) normal.getBottom()).getButtons().size());
      return null;
    });
    Platform.runLater(test);
    test.get(30, TimeUnit.SECONDS);
  }

  @Test
  void initialUserSettingsRemainVisibleButCannotBeAppliedAsAnOptimizerResult() throws Exception {
    final FutureTask<Void> test = new FutureTask<>(() -> {
      final var model = new OptimizationResultModel(false);
      final var initialUserSettings = new org.moeaframework.core.Solution(0, 1);
      model.initialUserSettingsSolutionProperty().set(initialUserSettings);
      model.getDisplayedSolutions().add(initialUserSettings);
      model.selectedSolutionProperty().set(initialUserSettings);
      model.optimizationRunningProperty().set(false);
      model.terminalResultDescriptionProperty().set("Canceled");

      final BorderPane view = (BorderPane) new OptimizationResultsViewBuilder(model, () -> {},
          () -> {}, () -> {}, () -> {}, null, null).build();
      final List<Button> buttons = ((ButtonBar) view.getBottom()).getButtons().stream()
          .map(node -> (Button) node).toList();

      assertEquals("Canceled", model.getTerminalResultDescription());
      assertFalse(buttons.getFirst().isDisable());
      assertTrue(buttons.subList(1, buttons.size()).stream().allMatch(Button::isDisable));
      return null;
    });
    Platform.runLater(test);
    test.get(30, TimeUnit.SECONDS);
  }
}
