package io.github.mzmine.modules.dataanalysis.compounddashboard;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardColoring.ColorAssignment;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.data.xy.XYDataset;

/**
 * Off-FX-thread task that builds the EIC datasets for the dashboard. Extracts one dataset per
 * member row that has a feature in the current raw file, colored according to the shared
 * {@link CompoundDashboardColoring} rules.
 */
public class CompoundDashboardEicTask extends FxUpdateTask<CompoundDashboardModel> {

  private final @NotNull CompoundRow compound;
  private final @Nullable RawDataFile file;
  private final @NotNull SimpleColorPalette palette;

  private List<DatasetAndRenderer> eicOut = List.of();
  private final Map<FeatureListRow, ColoredXYLineRenderer> eicRenderersOut = new IdentityHashMap<>();
  private final Map<FeatureListRow, XYDataset> eicDatasetsOut = new IdentityHashMap<>();

  public CompoundDashboardEicTask(@NotNull CompoundDashboardModel model,
      @NotNull CompoundRow compound, @Nullable RawDataFile file,
      @NotNull SimpleColorPalette palette) {
    super("Compound dashboard EIC", model);
    this.compound = compound;
    this.file = file;
    this.palette = palette;
  }

  @Override
  public String getTaskDescription() {
    return "Building compound dashboard chromatograms";
  }

  @Override
  public double getFinishedPercentage() {
    return 0d;
  }

  @Override
  protected void process() {
    if (file == null) {
      eicOut = List.of();
      return;
    }
    final ColorAssignment colors = CompoundDashboardColoring.assign(compound, palette);
    final List<DatasetAndRenderer> result = new ArrayList<>();
    for (final FeatureListRow row : CompoundDashboardColoring.flattenAllMemberRows(compound)) {
      final Feature feat = row.getFeature(file);
      if (feat == null || feat.getFeatureStatus() == FeatureStatus.UNKNOWN
          || feat.getFeatureData() == null) {
        continue;
      }
      final Color color = colors.colorFor(row);
      final IonTimeSeriesToXYProvider provider = new IonTimeSeriesToXYProvider(
          feat.getFeatureData(), CompoundDashboardColoring.shortIonLabel(row), color,
          CompoundDashboardColoring.longIonLabel(row));
      final ColoredXYLineRenderer renderer = new ColoredXYLineRenderer();
      // match the on-plot series label color to the dataset color
      renderer.setDefaultItemLabelPaint(FxColorUtil.fxColorToAWT(color));
      // remember the renderer + dataset that draws this row's EIC so the controller can highlight
      // this row by setting it as the chromatogram plot's selectedDataset.
      eicRenderersOut.put(row, renderer);
      final ColoredXYDataset dataset = new ColoredXYDataset(provider);
      eicDatasetsOut.put(row, dataset);
      result.add(new DatasetAndRenderer(dataset, renderer));
    }
    eicOut = result;
  }

  @Override
  protected void updateGuiModel() {
    // update the reverse maps BEFORE the list setAll so list-change subscribers see a consistent
    // row -> renderer / row -> dataset mapping for the new datasets
    model.getEicRenderersByRow().clear();
    model.getEicRenderersByRow().putAll(eicRenderersOut);
    model.getEicDatasetsByRow().clear();
    model.getEicDatasetsByRow().putAll(eicDatasetsOut);
    model.getEicDatasets().setAll(eicOut);
  }
}
