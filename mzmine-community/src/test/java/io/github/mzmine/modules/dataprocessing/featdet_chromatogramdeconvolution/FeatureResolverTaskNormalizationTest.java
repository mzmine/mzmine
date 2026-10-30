package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2SubParameters;
import io.github.mzmine.modules.dataprocessing.norm_intensity.FactorNormalizationFunction;
import io.github.mzmine.modules.dataprocessing.norm_intensity.IntensityNormalizationSummary;
import io.github.mzmine.modules.dataprocessing.norm_intensity.IntensityNormalizerModule;
import io.github.mzmine.modules.dataprocessing.norm_intensity.RawFileNormalizationFunction;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class FeatureResolverTaskNormalizationTest {

  /**
   * Resolved features have new intensities and new row IDs, so the resolved list has no
   * normalization. The original list keeps it.
   */
  @Test
  void resolvedListDropsNormalization() {
    final RawDataFileImpl raw = new RawDataFileImpl("sample", null, null);
    final List<Scan> scans = new ArrayList<>();
    final double[] intensities = new double[30];
    for (int i = 0; i < intensities.length; i++) {
      // a single gaussian peak
      intensities[i] = 1E5 * Math.exp(-Math.pow(i - 15, 2) / 8d);
      final SimpleScan scan = new SimpleScan(raw, i + 1, 1, i * 0.05f, null, new double[]{100d},
          new double[]{intensities[i]}, MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "",
          Range.closed(0d, 2000d));
      raw.addScan(scan);
      scans.add(scan);
    }

    final ModularFeatureList flist = new ModularFeatureList("chromatograms", null, raw);
    flist.setSelectedScans(raw, scans);
    flist.addFeatureType(DataTypes.get(NormalizedAreaType.class));
    flist.addFeatureType(DataTypes.get(NormalizedHeightType.class));
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, 1);
    final double[] mzs = new double[intensities.length];
    Arrays.fill(mzs, 100d);
    final ModularFeature feature = new ModularFeature(flist, raw,
        new SimpleIonTimeSeries(null, mzs, intensities, scans), FeatureStatus.DETECTED);
    feature.set(NormalizedAreaType.class, 99f);
    feature.set(NormalizedHeightType.class, 99f);
    row.addFeature(raw, feature);
    flist.addRow(row);
    flist.setSettings(flist.getSettings().withNormalization(new IntensityNormalizationSummary(
        List.of(new RawFileNormalizationFunction(raw, new FactorNormalizationFunction(2d))))));

    final MZmineProjectImpl project = new MZmineProjectImpl();
    project.addFile(raw);
    project.addFeatureList(flist);
    final MinimumSearchFeatureResolverParameters parameters = MinimumSearchFeatureResolverParameters.create(
        new FeatureListsSelection(flist), "r", OriginalFeatureListOption.KEEP, false,
        GroupMS2SubParameters.createDefault(), ResolvingDimension.RETENTION_TIME, 0d, 0.05, 0d, 1d,
        1.5, Range.closed(0d, 10d), 3);

    final FeatureResolverTask task = new FeatureResolverTask(project, null, flist, parameters,
        FeatureDataUtils.DEFAULT_CENTER_FUNCTION, Instant.now());
    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus(), task.getErrorMessage());
    final List<FeatureList> resolved = project.getCurrentFeatureLists().stream()
        .filter(list -> list != flist).toList();
    assertEquals(1, resolved.size());
    assertTrue(resolved.getFirst().getNumberOfRows() > 0);
    assertNull(resolved.getFirst().getSettings().normalization());
    resolved.getFirst().streamFeatures().forEach(f -> {
      assertNull(f.get(NormalizedAreaType.class));
      assertNull(f.get(NormalizedHeightType.class));
    });
    // the original list keeps its normalization
    assertTrue(IntensityNormalizerModule.getNormalizationFunctionsOfLatestCallForFile(flist, raw)
        .isPresent());
    assertEquals(99f, feature.get(NormalizedAreaType.class));
  }
}
