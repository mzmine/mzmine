package io.github.mzmine.modules.dataanalysis.compounddashboard;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardColoring.ColorAssignment;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.data.xy.XYDataset;

/**
 * Off-FX-thread task that builds the mobilogram datasets for the dashboard. Mirrors
 * {@link CompoundDashboardEicTask} but uses {@link SummedMobilogramXYProvider} per member row that
 * has an {@link IonMobilogramTimeSeries} in the currently selected raw file. Only emits datasets
 * when the selected raw file is an {@link IMSRawDataFile}; the view binds visibility separately so
 * a non-IMS file simply yields no datasets.
 */
public class CompoundDashboardMobilogramTask extends FxUpdateTask<CompoundDashboardModel> {

  private final @NotNull CompoundRow compound;
  private final @Nullable RawDataFile file;
  private final @NotNull SimpleColorPalette palette;

  private List<DatasetAndRenderer> mobilogramOut = List.of();
  private final Map<FeatureListRow, XYDataset> mobilogramDatasetsOut = new IdentityHashMap<>();
  private @Nullable String domainAxisLabel = null;

  public CompoundDashboardMobilogramTask(@NotNull CompoundDashboardModel model,
      @NotNull CompoundRow compound, @Nullable RawDataFile file,
      @NotNull SimpleColorPalette palette) {
    super("Compound dashboard mobilograms", model);
    this.compound = compound;
    this.file = file;
    this.palette = palette;
  }

  @Override
  public String getTaskDescription() {
    return "Building compound dashboard mobilograms";
  }

  @Override
  public double getFinishedPercentage() {
    return 0d;
  }

  @Override
  protected void process() {
    if (!(file instanceof IMSRawDataFile imsFile)) {
      mobilogramOut = List.of();
      return;
    }
    final MobilityType mt = imsFile.getMobilityType();
    domainAxisLabel = mt == null ? "Mobility" : mt.getAxisLabel();

    final ColorAssignment colors = CompoundDashboardColoring.assign(compound, palette);
    final List<DatasetAndRenderer> result = new ArrayList<>();
    for (final FeatureListRow row : CompoundDashboardColoring.flattenAllMemberRows(compound)) {
      final Feature feat = row.getFeature(imsFile);
      if (feat == null || feat.getFeatureStatus() == FeatureStatus.UNKNOWN) {
        continue;
      }
      final IonTimeSeries<?> series = feat.getFeatureData();
      if (!(series instanceof IonMobilogramTimeSeries)) {
        continue;
      }
      final Color color = colors.colorFor(row);
      final SummedMobilogramXYProvider provider = new SummedMobilogramXYProvider(
          ((IonMobilogramTimeSeries) series).getSummedMobilogram(),
          new SimpleObjectProperty<>(color), CompoundDashboardColoring.shortIonLabel(row));
      final ColoredXYLineRenderer renderer = new ColoredXYLineRenderer();
      renderer.setDefaultItemLabelPaint(FxColorUtil.fxColorToAWT(color));
      final ColoredXYDataset dataset = new ColoredXYDataset(provider, RunOption.THIS_THREAD);
      mobilogramDatasetsOut.put(row, dataset);
      result.add(new DatasetAndRenderer(dataset, renderer));
    }
    mobilogramOut = result;
  }

  @Override
  protected void updateGuiModel() {
    if (domainAxisLabel != null) {
      // The mobilogram plot's domain axis label depends on the IMS file's mobility type; refresh it
      // every time so switching raw files updates the label.
      model.setMobilogramDomainAxisLabel(domainAxisLabel);
    }
    model.getMobilogramDatasetsByRow().clear();
    model.getMobilogramDatasetsByRow().putAll(mobilogramDatasetsOut);
    model.getMobilogramDatasets().setAll(mobilogramOut);
  }
}
