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

package io.github.mzmine.modules.visualization.intensitymap.sampling;

import static io.github.mzmine.modules.visualization.intensitymap.IntensityMapDimensions.AUTOMATIC;
import static io.github.mzmine.modules.visualization.intensitymap.IntensityMapDimensions.IMAGING;
import static io.github.mzmine.modules.visualization.intensitymap.IntensityMapDimensions.LC_MS;
import static io.github.mzmine.modules.visualization.intensitymap.IntensityMapDimensions.MOBILITY_FRAME;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.preferences.ImageNormalization;
import io.github.mzmine.modules.io.import_rawdata_imzml.Coordinates;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters;
import io.github.mzmine.modules.visualization.intensitymap.IntensityMapDimensions;
import io.github.mzmine.modules.visualization.intensitymap.IntensityMapParameters;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapDetail;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapGrid;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapRegion;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

public class IntensityMapSamplerTest {

  private static final IntensityMapSampler.Progress PROGRESS = new IntensityMapSampler.Progress() {
    @Override
    public boolean canceled() {
      return false;
    }

    @Override
    public void advance(final int completed, final int total) {
    }
  };

  @Test
  void samplesLcMsInRetentionTimeAndMz() {
    final RawDataFile file = mock(RawDataFile.class);
    final Scan first = spectrum(mock(Scan.class), new double[]{100, 150}, new double[]{3, 7});
    final Scan second = spectrum(mock(Scan.class), new double[]{100, 150}, new double[]{4, 9});
    when(first.getRetentionTime()).thenReturn(1f);
    when(second.getRetentionTime()).thenReturn(2f);
    final ParameterSet parameters = parameters(file, LC_MS, new Scan[]{first, second});

    final IntensityMapGrid data = IntensityMapSampler.sample(file, parameters, PROGRESS);

    assertEquals(IntensityMapSampler.axisLabel("Retention time", "min"), data.xLabel());
    assertEquals(7, data.intensity(0, data.binY(150)));
    assertEquals(9, data.intensity(data.width() - 1, data.binY(150)));
    assertEquals(2, data.width());
    assertEquals(1, data.xValue(0));
    assertEquals(2, data.xValue(1));
  }

  @Test
  void doesNotOverwriteArraysOwnedByEarlierSpectra() {
    final double[] firstMz = {100, 150};
    final double[] firstIntensity = {3, 7};
    final RawDataFile file = mock(RawDataFile.class);
    final Scan first = spectrum(mock(Scan.class), firstMz, firstIntensity);
    final Scan second = spectrum(mock(Scan.class), new double[]{110, 160}, new double[]{4, 9});
    when(first.getRetentionTime()).thenReturn(1f);
    when(second.getRetentionTime()).thenReturn(2f);
    when(second.getMzValues(any(double[].class))).thenAnswer(call -> {
      final double[] buffer = call.getArgument(0);
      System.arraycopy(new double[]{110, 160}, 0, buffer, 0, 2);
      return buffer;
    });
    when(second.getIntensityValues(any(double[].class))).thenAnswer(call -> {
      final double[] buffer = call.getArgument(0);
      System.arraycopy(new double[]{4, 9}, 0, buffer, 0, 2);
      return buffer;
    });
    IntensityMapSampler.sample(file, parameters(file, LC_MS, new Scan[]{first, second}),
        PROGRESS);
    assertArrayEquals(new double[]{100, 150}, firstMz);
    assertArrayEquals(new double[]{3, 7}, firstIntensity);
  }

  @Test
  void samplesOneMobilityFrame() {
    final RawDataFile file = mock(io.github.mzmine.datamodel.IMSRawDataFile.class);
    final Frame frame = mock(Frame.class);
    final MobilityScan scan = spectrum(mock(MobilityScan.class), new double[]{150},
        new double[]{11});
    when(frame.getScanNumber()).thenReturn(42);
    when(frame.getMobilityRange()).thenReturn(Range.closed(1d, 2d));
    when(frame.getNumberOfMobilityScans()).thenReturn(1);
    when(frame.getSortedMobilityScans()).thenReturn(java.util.List.of(scan));
    when(scan.getMobility()).thenReturn(1.5);
    final ParameterSet parameters = parameters(file, MOBILITY_FRAME, new Scan[]{frame});

    final IntensityMapGrid data = IntensityMapSampler.sample(file, parameters, PROGRESS);

    // m/z horizontal, mobility in depth
    assertEquals("m/z", data.xLabel());
    assertTrue(data.yLabel().startsWith("Mobility"));
    assertEquals(11, data.maximum());
    assertEquals(11, data.intensity(data.binX(150), data.binY(1.5)));
    final ParameterSet noFrames = parameters(file, MOBILITY_FRAME,
        new Scan[]{mock(Scan.class)});
    assertThrows(IllegalArgumentException.class,
        () -> IntensityMapSampler.sample(file, noFrames, PROGRESS));
  }

  @Test
  void ms2ScansDoNotBreakMs1Traces() {
    // DDA: MS2 scans between the MS1 scans of a trace
    final Scan ms1a = mock(Scan.class);
    final Scan ms2 = mock(Scan.class);
    final Scan ms1b = mock(Scan.class);
    when(ms1a.getMSLevel()).thenReturn(1);
    when(ms2.getMSLevel()).thenReturn(2);
    when(ms1b.getMSLevel()).thenReturn(1);
    assertEquals(List.of(ms1a, ms1b),
        IntensityMapSampler.lowestMsLevel(new Scan[]{ms1a, ms2, ms1b}));
    // only MS2 selected: the MS2 scans
    assertEquals(List.of(ms2), IntensityMapSampler.lowestMsLevel(new Scan[]{ms2}));
  }

  @Test
  void framesAreSelectedByRetentionTimeOrIntensity() {
    final Frame early = mock(Frame.class);
    final Frame late = mock(Frame.class);
    when(early.getRetentionTime()).thenReturn(1f);
    when(early.getBasePeakIntensity()).thenReturn(5d);
    when(late.getRetentionTime()).thenReturn(2f);
    when(late.getBasePeakIntensity()).thenReturn(20d);
    final Scan[] scans = {early, mock(Scan.class), late};

    // without a retention time, the most intense frame
    final IntensityMapFrameCache frames = new IntensityMapFrameCache();
    assertSame(late, frames.frame(scans));
    assertSame(early, frames.select(Range.singleton(1.4f)).frame(scans));
    assertSame(late, frames.select(Range.singleton(1.6f)).frame(scans));
    assertSame(late, frames.select(Range.singleton(99f)).frame(scans));
    // a range with a single frame shows the frame closest to its center
    assertEquals(java.util.List.of(early), IntensityMapFrameCache.within(scans, Range.closed(0.5f, 1.2f)));
    assertSame(early, frames.select(Range.closed(0.5f, 1.2f)).frame(scans));
    assertThrows(IllegalArgumentException.class,
        () -> frames.frame(new Scan[]{mock(Scan.class)}));
  }

  @Test
  void imagingSumsOnlySelectedMzAndLeavesMissingPixels() {
    final ImagingRawDataFile file = mock(ImagingRawDataFile.class);
    when(file.getImagingParam()).thenReturn(new ImagingParameters(30, 30, 10, 10, 3, 3));
    final ImagingScan scan = spectrum(mock(ImagingScan.class), new double[]{100, 150, 250},
        new double[]{2, 3, 20});
    when(scan.getCoordinates()).thenReturn(new Coordinates(1, 1, 1));
    final ParameterSet parameters = parameters(file, IMAGING, new Scan[]{scan});

    final IntensityMapGrid data = IntensityMapSampler.sample(file, parameters, PROGRESS);

    assertEquals(IntensityMapSampler.axisLabel("X", "µm"), data.xLabel());
    assertEquals(5, data.maximum());
    assertTrue(data.isPresent(data.binX(10), data.binY(10)));
    assertEquals(1, data.width());
    assertEquals(10, data.xValue(0));
  }

  @Test
  void extractsSeveralMzRangesInOnePassAndAlignsThem() {
    final RawDataFile file = mock(RawDataFile.class);
    final Scan first = spectrum(mock(Scan.class), new double[]{100, 150.001, 200},
        new double[]{3, 7, 5});
    final Scan second = spectrum(mock(Scan.class), new double[]{100.001, 150, 200.002},
        new double[]{4, 9, 6});
    when(first.getRetentionTime()).thenReturn(1f);
    when(second.getRetentionTime()).thenReturn(2f);
    final ParameterSet parameters = parameters(file, LC_MS, new Scan[]{first, second});
    final var ranges = List.of(Range.closed(149.99, 150.01), Range.closed(199.99, 200.01));

    final IntensityMapGrid[] absolute = IntensityMapSampler.sample(file, parameters, ranges,
        IntensityMapRegion.FULL, IntensityMapDetail.DEFAULT, PROGRESS);
    assertEquals(2, absolute.length);
    assertEquals("m/z", absolute[0].yLabel());
    assertEquals(9, absolute[0].maximum());
    assertEquals(6, absolute[1].maximum());
    assertTrue(absolute[1].yMin() >= 199.99);
    // each spectrum is read once, even for several overlays
    verify(first, times(1)).getMzValues(any(double[].class));
  }

  @Test
  void averageTicNormalizationUsesTheImagingFactors() {
    final RawDataFile file = mock(RawDataFile.class);
    final Scan first = spectrum(mock(Scan.class), new double[]{150}, new double[]{10});
    final Scan second = spectrum(mock(Scan.class), new double[]{150}, new double[]{10});
    when(first.getRetentionTime()).thenReturn(1f);
    when(second.getRetentionTime()).thenReturn(2f);
    when(first.getTIC()).thenReturn(10d);
    when(second.getTIC()).thenReturn(30d);
    final ParameterSet parameters = parameters(file, LC_MS, new Scan[]{first, second});

    final IntensityMapGrid data = IntensityMapSampler.sample(file, parameters,
        List.of(Range.closed(100d, 200d)), IntensityMapRegion.FULL,
        ImageNormalization.TIC_AVG_NORMALIZATION, new IntensityMapFrameCache(), IntensityMapDetail.DEFAULT,
        PROGRESS)[0];

    // average TIC 20: factors 2 and 2/3
    assertEquals(20, data.intensity(0, data.binY(150)), 1e-4);
    assertEquals(20d / 3, data.intensity(1, data.binY(150)), 1e-4);
  }

  @Test
  void imagingFilesAlwaysShowImages() {
    final ImagingRawDataFile image = mock(ImagingRawDataFile.class);
    final RawDataFile lcms = mock(RawDataFile.class);
    // e.g. an LC-MS mode stored in the module settings
    assertEquals(IMAGING, IntensityMapSampler.resolveMode(image, LC_MS));
    assertEquals(IMAGING, IntensityMapSampler.resolveMode(image, AUTOMATIC));
    assertEquals(LC_MS, IntensityMapSampler.resolveMode(lcms, IMAGING));
    assertEquals(LC_MS, IntensityMapSampler.resolveMode(lcms, AUTOMATIC));
  }

  @Test
  void imagingIgnoresTheRetentionTimeFilter() {
    // imzML pixels have retention time 0, an LC-MS range would exclude all of them
    final ParameterSet parameters = mock(ParameterSet.class);
    final ScanSelection withRt = new ScanSelection(1, Range.closed(4.1f, 4.6f), PolarityType.ANY);
    when(parameters.getValue(IntensityMapParameters.scanSelection)).thenReturn(withRt);
    assertNull(IntensityMapSampler.scanSelection(parameters, IMAGING).getScanRTRange());
    assertEquals(withRt.getScanRTRange(),
        IntensityMapSampler.scanSelection(parameters, LC_MS).getScanRTRange());
  }

  @Test
  void featureRangesOfTheSameIonAreMerged() {
    // one ion in two samples and a second ion
    final var merged = IntensityMapLayer.mergeOverlapping(
        List.of(Range.closed(500.001, 500.004), Range.closed(300.1, 300.2),
            Range.closed(500.003, 500.006)));
    assertEquals(List.of(Range.closed(300.1, 300.2), Range.closed(500.001, 500.006)), merged);
  }

  @Test
  void regionRestrictsScansAndSkipsRangesOutside() {
    final RawDataFile file = mock(RawDataFile.class);
    final Scan first = spectrum(mock(Scan.class), new double[]{100, 150}, new double[]{3, 7});
    final Scan second = spectrum(mock(Scan.class), new double[]{100, 150}, new double[]{4, 9});
    when(first.getRetentionTime()).thenReturn(1f);
    when(second.getRetentionTime()).thenReturn(2f);
    final ParameterSet parameters = parameters(file, LC_MS, new Scan[]{first, second});
    final IntensityMapRegion region = new IntensityMapRegion(Range.closed(1.5, 3d),
        Range.closed(120d, 200d));

    final IntensityMapGrid[] data = IntensityMapSampler.sample(file, parameters,
        List.of(Range.closed(100d, 200d), Range.closed(90d, 110d)), region,
        IntensityMapDetail.DEFAULT, PROGRESS);

    assertEquals(1, data[0].width());
    assertEquals(9, data[0].maximum());
    assertTrue(data[0].yMin() >= 120);
    assertNull(data[1]);
  }

  private static <T extends Scan> @NotNull T spectrum(@NotNull final T scan,
      @NotNull final double[] mz, @NotNull final double[] intensity) {
    when(scan.getNumberOfDataPoints()).thenReturn(mz.length);
    when(scan.getMzValues(any(double[].class))).thenReturn(mz);
    when(scan.getIntensityValues(any(double[].class))).thenReturn(intensity);
    return scan;
  }

  private static @NotNull ParameterSet parameters(@NotNull final RawDataFile file,
      @NotNull final IntensityMapDimensions mode, @NotNull final Scan[] scans) {
    final ParameterSet parameters = mock(ParameterSet.class);
    final ScanSelection selection = mock(ScanSelection.class);
    when(selection.getMatchingScans(file)).thenReturn(scans);
    when(parameters.getValue(IntensityMapParameters.mode)).thenReturn(mode);
    when(parameters.getValue(IntensityMapParameters.mzRange)).thenReturn(
        Range.closed(100d, 200d));
    when(parameters.getValue(IntensityMapParameters.scanSelection)).thenReturn(selection);
    return parameters;
  }
}
