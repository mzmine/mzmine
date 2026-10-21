/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.batch_resource_estimation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.HeadLessDesktop;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.batchmode.timing.HeapMeasurementQuality;
import io.github.mzmine.modules.batchmode.timing.StepMeasurement;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerComponent;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javax.imageio.ImageIO;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.fx.FXGraphics2D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisabledOnOs(OS.LINUX)
class ResourceEstimationJavaFxTest {

  private static SubsetRun run(final int files) {
    return new SubsetRun(files, files, 0.3, 0, 1000, List.of(
        new StepMeasurement(1, "Compound grouping (beta)", files, "0.5", files * 0.1, files * 0.05,
            0, 0, 0, 0, files * 0.1, HeapMeasurementQuality.PARTIAL_GC)));
  }

  private static BatchResourceModel model() {
    return BatchResourceModel.fit(List.of(run(6), run(12), run(24)), run(48),
        List.of(StepScalingMode.SCALES_WITH_FILES), 640, 16);
  }

  private static <T> T onFxThread(final Callable<T> action) throws Exception {
    FxThread.initJavaFx();
    final FutureTask<T> task = new FutureTask<>(() -> {
      final var desktop = DesktopService.getDesktop();
      DesktopService.setDesktop(new HeadLessDesktop());
      try {
        return action.call();
      } finally {
        DesktopService.setDesktop(desktop);
      }
    });
    Platform.runLater(task);
    return task.get(30, TimeUnit.SECONDS);
  }

  private static void savePreview(final WritableImage image, final String name) throws Exception {
    final Path file = Path.of("build", "reports", "resource-estimation", name);
    Files.createDirectories(file.getParent());
    ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file.toFile());
  }

  private static BatchResourceEstimationResultTab createTab(final EstimationSession session,
      final BatchResourceModel model) throws Exception {
    final var constructor = BatchResourceEstimationResultTab.class.getDeclaredConstructor(
        EstimationSession.class, BatchResourceModel.class, ParameterSet.class);
    constructor.setAccessible(true);
    return constructor.newInstance(session, model, new BatchResourceEstimationParameters());
  }

  @Test
  void additionalRunsValidateTheirSizeAndAdvanceAfterCompletion() throws Exception {
    onFxThread(() -> {
      final List<SubsetRun> runs = List.of(run(6), run(12), run(24));
      final List<StepScalingMode> modes = List.of(StepScalingMode.SCALES_WITH_FILES);
      final SubsetRunner runner = new SubsetRunner(new BatchQueue(), List.of(), null, 1,
          new GlobalProjectProvider());
      final EstimationSession session = new EstimationSession(runner, modes, 16, runs, run(48),
          List.of());
      final BatchResourceEstimationResultTab tab = createTab(session, model());
      new TabPane(tab);
      final BorderPane root = (BorderPane) tab.getContent();
      final HBox input = (HBox) ((VBox) root.getTop()).getChildren().get(1);
      final IntegerComponent estimateFiles = (IntegerComponent) input.getChildren().get(1);
      final IntegerComponent runFiles = (IntegerComponent) input.getChildren().get(4);
      final Button runButton = (Button) input.getChildren().get(5);
      assertEquals("96", runFiles.getText());
      ((EstimationOutcomePane) ((VBox) root.getTop()).getChildren().getFirst()).setOutcome(
          new EstimationOutcome(EstimationVerdict.ENOUGH_RESOURCES, List.of(), null));
      assertFalse(runButton.isDisabled());
      for (final String invalid : List.of("", "0", "48", "641")) {
        runFiles.setText(invalid);
        assertTrue(runButton.isDisabled(), invalid);
      }
      runFiles.setText("100");
      assertFalse(runButton.isDisabled());
      estimateFiles.valueProperty().setValue(200);
      assertEquals("100", runFiles.getText(), "Changing the prediction must preserve run input");

      final var runningField = BatchResourceEstimationResultTab.class.getDeclaredField(
          "largerRunRunning");
      runningField.setAccessible(true);
      final BooleanProperty running = (BooleanProperty) runningField.get(tab);
      running.set(true);
      assertTrue(runButton.isDisabled());
      assertTrue(runFiles.isDisabled());
      running.set(false);
      assertFalse(runButton.isDisabled());

      final var setResult = BatchResourceEstimationResultTab.class.getDeclaredMethod("setResult",
          EstimationSession.class, BatchResourceModel.class);
      setResult.setAccessible(true);
      final EstimationSession extended = session.withExtension(run(400));
      setResult.invoke(tab, extended,
          BatchResourceModel.fit(runs, run(48), extended.extensionRuns(), modes, 640, 16));
      assertEquals("640", runFiles.getText(), "Default is capped at the batch size");
      assertFalse(runButton.isDisabled());
      assertEquals(200, estimateFiles.valueProperty().getValue());
      final EstimationSession complete = extended.withExtension(run(640));
      setResult.invoke(tab, complete,
          BatchResourceModel.fit(runs, run(48), complete.extensionRuns(), modes, 640, 16));
      assertTrue(runButton.isDisabled());
      assertTrue(runFiles.isDisabled());
      return null;
    });
  }

  @Test
  void hatchingDoesNotClipLaterDrawingOnTheJavaFxCanvas() throws Exception {
    onFxThread(() -> {
      final BatchResourceModel model = model();
      final RamReliabilityBarRenderer renderer = new RamReliabilityBarRenderer();
      renderer.setRuns(model.measured().subList(0, 2));
      renderer.setSeriesPaint(0, Color.BLUE);
      renderer.setSeriesPaint(1, Color.ORANGE);
      final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
      dataset.addValue(1, "6 files", "Grouping");
      dataset.addValue(2, "12 files", "Grouping");
      final CategoryPlot plot = new CategoryPlot(dataset, new CategoryAxis("Step"),
          new NumberAxis("GB"), renderer);
      final JFreeChart chart = new JFreeChart("Peak RAM", JFreeChart.DEFAULT_TITLE_FONT, plot,
          true);
      final Canvas canvas = new Canvas(900, 500);
      final FXGraphics2D graphics = new FXGraphics2D(canvas.getGraphicsContext2D());
      chart.draw(graphics, new Rectangle2D.Double(0, 0, 900, 500));
      // This is outside all bars. A leaked bar clip makes this marker disappear, even though
      // the same renderer still works with AWT's independent child graphics contexts.
      graphics.setPaint(Color.RED);
      graphics.fill(new Rectangle2D.Double(880, 480, 10, 10));
      final WritableImage image = canvas.snapshot(null, null);
      assertEquals(Color.RED.getRGB(), image.getPixelReader().getArgb(885, 485));
      savePreview(image, "ram-reliability-javafx.png");
      return null;
    });
  }

  @Test
  void resultsUseAvailableWidthAndKeepChartsWithinTheirCells() throws Exception {
    onFxThread(() -> {
      final List<SubsetRun> runs = List.of(run(6), run(12), run(24));
      final List<StepScalingMode> modes = List.of(StepScalingMode.SCALES_WITH_FILES);
      final SubsetRunner runner = new SubsetRunner(new BatchQueue(), List.of(), null, 1,
          new GlobalProjectProvider());
      final EstimationSession session = new EstimationSession(runner, modes, 16, runs, run(48),
          List.of());
      final BatchResourceEstimationResultTab tab = createTab(session, model());
      final BorderPane root = (BorderPane) tab.getContent();
      new Scene(root, 1400, 1000);
      root.applyCss();
      for (final int width : new int[]{1400, 900}) {
        root.resize(width, 1000);
        root.layout();
        final VBox top = (VBox) root.getTop();
        final HBox input = (HBox) top.getChildren().get(1);
        final GridPane summary = (GridPane) top.getChildren().get(2);
        assertTrue(summary.getWidth() > width - 60, "summary must fill available width");
        final Label time = (Label) summary.getChildren().get(3);
        assertTrue(time.getWidth() > (summary.getWidth() - 24) / 2 - 2);
        assertTrue(input.getChildren().get(2).getBoundsInParent().getMinX()
                < input.getChildren().get(1).getBoundsInParent().getMaxX() + 30,
            "thread count must stay beside the input");
        final GridPane charts = (GridPane) ((SplitPane) root.getCenter()).getItems().getFirst();
        for (final var node : charts.getChildren()) {
          assertTrue(node.getBoundsInParent().getMaxX() <= charts.getWidth() + 1,
              "charts must not overflow their grid");
          ((EChartViewer) node).getCanvas().draw();
        }
        savePreview(root.snapshot(null, null), "results-javafx-" + width + ".png");
      }
      return null;
    });
  }
}
