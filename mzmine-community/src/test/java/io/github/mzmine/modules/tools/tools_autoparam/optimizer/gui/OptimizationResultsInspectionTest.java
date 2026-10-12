package io.github.mzmine.modules.tools.tools_autoparam.optimizer.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.ConfigService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.BorderPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class OptimizationResultsInspectionTest {
  @BeforeAll
  static void toolkit() {
    FxThread.initJavaFx();
    Platform.setImplicitExit(false);
    ConfigService.getPreferences();
  }

  @Test
  void inspectionRetainsNativeResultsWithoutOfferingApplyOrRun() throws Exception {
    final FutureTask<Void> test = new FutureTask<>(() -> {
      final var model = new OptimizationResultModel(false);
      model.getDisplayedSolutions().add(new org.moeaframework.core.Solution(0, 1));
      final BorderPane inspection = (BorderPane) new OptimizationResultsViewBuilder(model, null,
          null, () -> {}, null, null, null).build();
      final ButtonBar buttons = (ButtonBar) inspection.getBottom();
      assertEquals(java.util.List.of("Export to .csv"), buttons.getButtons().stream()
          .map(node -> ((Button) node).getText()).toList());
      assertTrue(inspection.getCenter() instanceof BorderPane);

      final BorderPane normal = (BorderPane) new OptimizationResultsViewBuilder(model, () -> {},
          () -> {}, () -> {}, () -> {}, null, null).build();
      assertEquals(4, ((ButtonBar) normal.getBottom()).getButtons().size());
      return null;
    });
    Platform.runLater(test);
    test.get(30, TimeUnit.SECONDS);
  }
}
