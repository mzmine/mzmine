package io.github.mzmine.modules.dataprocessing.featdet_imsexpander;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.modules.dataprocessing.norm_intensity.FactorNormalizationFunction;
import io.github.mzmine.modules.dataprocessing.norm_intensity.IntensityNormalizationSummary;
import io.github.mzmine.modules.dataprocessing.norm_intensity.IntensityNormalizerModule;
import io.github.mzmine.modules.dataprocessing.norm_intensity.RawFileNormalizationFunction;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class ImsExpanderTaskNormalizationTest {

  private static final double[] MOBILITIES = {0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4,
      1.5, 1.6};

  /**
   * Expanded features have new intensities, so the expanded list has no normalization. The
   * original list keeps it.
   */
  @Test
  void expandedListDropsNormalization() {
    final IMSRawDataFileImpl raw = new IMSRawDataFileImpl("ims sample", null, null, Color.BLACK);
    final List<Frame> frames = new ArrayList<>();
    final double[] frameIntensities = new double[10];
    for (int frameIndex = 0; frameIndex < frameIntensities.length; frameIndex++) {
      frameIntensities[frameIndex] = 1E4 * (1 + Math.min(frameIndex, 9 - frameIndex));
      final SimpleFrame frame = new SimpleFrame(raw, frameIndex + 1, 1, frameIndex * 0.05f,
          new double[]{100d}, new double[]{frameIntensities[frameIndex]},
          MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "", Range.closed(50d, 1000d),
          MobilityType.DRIFT_TUBE, null, null);
      // mobilities first, so the mobility scan count is validated against them
      frame.setMobilities(MOBILITIES);
      final List<BuildingMobilityScan> mobilityScans = new ArrayList<>();
      for (int i = 0; i < MOBILITIES.length; i++) {
        mobilityScans.add(new BuildingMobilityScan(i, new double[]{100d, 200d},
            new double[]{1E3 * (1 + Math.min(i, 11 - i)), 1E3}, MassSpectrumType.CENTROIDED));
      }
      frame.setMobilityScans(mobilityScans, false);
      raw.addScan(frame);
      frames.add(frame);
    }

    final ModularFeatureList flist = new ModularFeatureList("chromatograms", null, raw);
    flist.setSelectedScans(raw, frames);
    flist.addFeatureType(DataTypes.get(NormalizedAreaType.class));
    flist.addFeatureType(DataTypes.get(NormalizedHeightType.class));
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, 1);
    final double[] mzs = new double[frames.size()];
    Arrays.fill(mzs, 100d);
    final ModularFeature feature = new ModularFeature(flist, raw,
        new SimpleIonTimeSeries(null, mzs, frameIntensities, frames), FeatureStatus.DETECTED);
    feature.set(NormalizedAreaType.class, 99f);
    feature.set(NormalizedHeightType.class, 99f);
    row.addFeature(raw, feature);
    flist.addRow(row);
    flist.setSettings(flist.getSettings().withNormalization(new IntensityNormalizationSummary(
        List.of(new RawFileNormalizationFunction(raw, new FactorNormalizationFunction(2d))))));

    final MZmineProjectImpl project = new MZmineProjectImpl();
    project.addFile(raw);
    project.addFeatureList(flist);
    final ParameterSet parameters = new ImsExpanderParameters().cloneParameterSet();
    parameters.setParameter(ImsExpanderParameters.useRawData, true, 0d);
    parameters.setParameter(ImsExpanderParameters.mobilogramBinWidth, false);
    parameters.setParameter(ImsExpanderParameters.maxNumTraces, false);
    parameters.setParameter(ImsExpanderParameters.handleOriginal, OriginalFeatureListOption.KEEP);

    final ImsExpanderTask task = new ImsExpanderTask(null, parameters, flist, project,
        Instant.now());
    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus(), task.getErrorMessage());
    final List<FeatureList> expanded = project.getCurrentFeatureLists().stream()
        .filter(list -> list != flist).toList();
    assertEquals(1, expanded.size());
    assertTrue(expanded.getFirst().getNumberOfRows() > 0);
    assertNull(expanded.getFirst().getSettings().normalization());
    expanded.getFirst().streamFeatures().forEach(f -> {
      assertNull(f.get(NormalizedAreaType.class));
      assertNull(f.get(NormalizedHeightType.class));
    });
    // the original list keeps its normalization
    assertTrue(IntensityNormalizerModule.getNormalizationFunctionsOfLatestCallForFile(flist, raw)
        .isPresent());
    assertEquals(99f, feature.get(NormalizedAreaType.class));
  }
}
