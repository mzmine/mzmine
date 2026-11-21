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

package io.github.mzmine.modules.io.import_rawdata_agilent_d;

import io.github.mzmine.datamodel.featuredata.OtherFeatureUtils;
import io.github.mzmine.datamodel.features.types.otherdectectors.ChromatogramTypeType;
import io.github.mzmine.datamodel.features.types.otherdectectors.WavelengthType;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFileImpl;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureImpl;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesDataImpl;
import io.github.mzmine.datamodel.otherdetectors.SimpleOtherTimeSeries;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Converts the non-MS data of an Agilent .d file (MRM transitions, analog detector signals) into
 * other data files of a raw data file. Shared by all Agilent import paths.
 */
public final class AgilentOtherData {

  /**
   * Agilent writes "Sig=wavelength,bandwidth", e.g. "Sig=272,16 Ref=360,100" or "Sig=220.0,4.0
   * Ref=off" depending on the acquisition software. The comma separates the bandwidth.
   */
  private static final Pattern wavelengthPattern = Pattern.compile("Sig=(\\d+(?:\\.\\d+)?)");

  private AgilentOtherData() {
  }

  /**
   * Attaches the MRM/SRM transitions to {@code dataFile} as one {@link ChromatogramType#MRM_SRM}
   * other data file. No-op for an empty list.
   */
  public static void addMrms(@NotNull RawDataFileImpl dataFile, @Nullable MemoryMapStorage storage,
      @NotNull List<AgilentMrmTrace> transitions) {
    if (transitions.isEmpty()) {
      return;
    }
    final OtherDataFileImpl mrmFile = new OtherDataFileImpl(dataFile);
    mrmFile.setDescription("MRM/SRM");
    final OtherTimeSeriesDataImpl mrmData = new OtherTimeSeriesDataImpl(mrmFile);
    mrmData.setChromatogramType(ChromatogramType.MRM_SRM);
    mrmData.setTimeSeriesRangeLabel("Intensity");
    mrmData.setTimeSeriesRangeUnit("cts");
    mrmData.setTimeSeriesDomainLabel("RT");
    mrmData.setTimeSeriesDomainUnit("min");
    mrmFile.setOtherTimeSeriesData(mrmData);

    for (final AgilentMrmTrace t : transitions) {
      final SimpleOtherTimeSeries series = new SimpleOtherTimeSeries(storage, t.rt(), t.intensity(),
          "%.2f -> %.2f".formatted(t.q1(), t.q3()), mrmData);
      final OtherFeature feature = new OtherFeatureImpl(series);
      OtherFeatureUtils.applyMrmInfo(t.q1(), t.q3(), ActivationMethod.CID, null, feature);
      mrmData.addRawTrace(feature);
    }
    if (mrmFile.hasTimeSeries()) {
      dataFile.addOtherDataFiles(List.of(mrmFile));
    }
  }

  /**
   * Attaches non-MS detector signals (DAD/UV, instrument curves) to {@code dataFile}, grouped by
   * device and unit into one other data file each. Signals of unknown device types are skipped.
   */
  public static void addAnalogChannels(@NotNull RawDataFileImpl dataFile,
      @Nullable MemoryMapStorage storage, @NotNull List<AgilentAnalogTrace> traces) {
    if (traces.isEmpty()) {
      return;
    }
    final Map<DeviceWithUnit, OtherTimeSeriesDataImpl> unitToData = new HashMap<>();
    for (final AgilentAnalogTrace t : traces) {
      final String channelDescription = t.description();

      final AgilentAnalogDevice device = AgilentAnalogDevice.fromTitle(t.deviceType());
      AgilentAnalogUnit unit = AgilentAnalogUnit.fromDescription(channelDescription);
      if (device == null) {
        continue;
      }
      if (unit == AgilentAnalogUnit.UNKNOWN) {
        unit = AgilentAnalogUnit.fromDevice(device);
      }

      final OtherTimeSeriesDataImpl tsd = unitToData.computeIfAbsent(
          new DeviceWithUnit(device, unit), dwu -> {
            final OtherDataFileImpl otherFile = new OtherDataFileImpl(dataFile);
            final OtherTimeSeriesDataImpl data = new OtherTimeSeriesDataImpl(otherFile);
            otherFile.setOtherTimeSeriesData(data);
            otherFile.setDescription(device.title + " " + dwu.unit().description);
            data.setTimeSeriesRangeUnit(dwu.unit().unit.getSign());
            data.setTimeSeriesRangeLabel(dwu.unit().unit.getLabel());
            data.setChromatogramType(dwu.unit().type);
            return data;
          });

      double[] values = t.values();
      if (unit.getMapper() != null) {
        final DoubleUnaryOperator mapper = unit.getMapper();
        values = Arrays.stream(values).map(mapper).toArray();
      }
      final SimpleOtherTimeSeries trace = new SimpleOtherTimeSeries(storage, t.rt(), values,
          channelDescription, tsd);

      final OtherFeatureImpl feature = new OtherFeatureImpl(trace);
      feature.set(ChromatogramTypeType.class, unit.type);
      setWavelength(unit, channelDescription, feature);

      tsd.addRawTrace(feature);
    }
    dataFile.addOtherDataFiles(
        unitToData.values().stream().map(OtherTimeSeriesDataImpl::getOtherDataFile).toList());
  }

  private static void setWavelength(@NotNull AgilentAnalogUnit unit,
      @NotNull String channelDescription, @NotNull OtherFeatureImpl feature) {
    if (unit.type != ChromatogramType.ABSORPTION) {
      return;
    }
    final Matcher matcher = wavelengthPattern.matcher(channelDescription);
    if (!matcher.find()) {
      return;
    }

    feature.set(WavelengthType.class, Double.parseDouble(matcher.group(1)));
  }

  private record DeviceWithUnit(AgilentAnalogDevice device, AgilentAnalogUnit unit) {

  }
}
