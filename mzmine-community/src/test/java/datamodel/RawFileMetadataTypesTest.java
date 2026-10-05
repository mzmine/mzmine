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

package datamodel;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.rawfiletypes.AcquisitionMzRangeType;
import io.github.mzmine.datamodel.features.rawfiletypes.AcquisitionRtRangeType;
import io.github.mzmine.datamodel.features.rawfiletypes.AcquisitionSoftwareType;
import io.github.mzmine.datamodel.features.rawfiletypes.AcquisitionSoftwareVersionType;
import io.github.mzmine.datamodel.features.rawfiletypes.CalibrationDateTimeType;
import io.github.mzmine.datamodel.features.rawfiletypes.CalibrationUserType;
import io.github.mzmine.datamodel.features.rawfiletypes.DetectorsType;
import io.github.mzmine.datamodel.features.rawfiletypes.InstrumentModelType;
import io.github.mzmine.datamodel.features.rawfiletypes.InstrumentSerialNumberType;
import io.github.mzmine.datamodel.features.rawfiletypes.InstrumentVendorType;
import io.github.mzmine.datamodel.features.rawfiletypes.IonSourcesType;
import io.github.mzmine.datamodel.features.rawfiletypes.LcMethodNameType;
import io.github.mzmine.datamodel.features.rawfiletypes.MassAnalyzersType;
import io.github.mzmine.datamodel.features.rawfiletypes.MobilityCalibrationDateTimeType;
import io.github.mzmine.datamodel.features.rawfiletypes.MobilityCalibrationReferencePressureType;
import io.github.mzmine.datamodel.features.rawfiletypes.MobilityCalibrationStdDevPercentType;
import io.github.mzmine.datamodel.features.rawfiletypes.MobilityCalibrationUserType;
import io.github.mzmine.datamodel.features.rawfiletypes.MzCalibrationModeType;
import io.github.mzmine.datamodel.features.rawfiletypes.MzCalibrationStdDevPpmType;
import io.github.mzmine.datamodel.features.rawfiletypes.OperatorNameType;
import io.github.mzmine.datamodel.features.rawfiletypes.RawDataFileFormatType;
import io.github.mzmine.datamodel.features.rawfiletypes.SampleDescriptionType;
import io.github.mzmine.datamodel.features.rawfiletypes.SampleNameType;
import io.github.mzmine.datamodel.features.rawfiletypes.SourceFileSha1Type;
import io.github.mzmine.datamodel.features.rawfiletypes.TuneMethodNameType;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.util.RawDataFileType;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Context-free save/load of all data types in the raw file metadata package, the same way as
 * {@link io.github.mzmine.modules.io.projectsave.RawFileMetadataProjectIO} saves them. New types
 * need a sample value in {@link #SAMPLES}.
 */
class RawFileMetadataTypesTest {

  private static final String RAW_FILE_TYPES_PACKAGE = InstrumentModelType.class.getPackageName();

  // contains xml special characters to test escaping
  private static final String TEXT = "Q Exactive <HF> & \"plus\" 'slot #1'";
  private static final List<String> TEXT_LIST = List.of("quadrupole", "time-of-flight");

  private static final Map<Class<? extends DataType<?>>, Object> SAMPLES = Map.ofEntries(
      Map.entry(AcquisitionSoftwareType.class, TEXT),
      Map.entry(AcquisitionSoftwareVersionType.class, "2.9-290204/2.9.2.2947"),
      Map.entry(InstrumentModelType.class, TEXT), Map.entry(InstrumentSerialNumberType.class, TEXT),
      Map.entry(InstrumentVendorType.class, TEXT), Map.entry(OperatorNameType.class, TEXT),
      Map.entry(SampleNameType.class, TEXT),
      Map.entry(SourceFileSha1Type.class, "a7bf52e6b4cc2739fb0b692fef6a27bb90c4f2ac"),
      Map.entry(IonSourcesType.class, List.of("electrospray ionization", "electrospray inlet")),
      Map.entry(MassAnalyzersType.class, TEXT_LIST),
      Map.entry(DetectorsType.class, List.of("microchannel plate detector", "photomultiplier")),
      Map.entry(SampleDescriptionType.class, TEXT), Map.entry(LcMethodNameType.class, TEXT),
      Map.entry(TuneMethodNameType.class, "D:\\Projects\\CJH Feb25.PRO\\ACQUDB\\tune.ipr"),
      Map.entry(CalibrationDateTimeType.class, LocalDateTime.of(2025, 3, 5, 15, 43, 52)),
      Map.entry(CalibrationUserType.class, TEXT),
      Map.entry(MzCalibrationStdDevPpmType.class, 0.755987),
      Map.entry(MzCalibrationModeType.class, 7),
      Map.entry(MobilityCalibrationStdDevPercentType.class, 674.585316),
      Map.entry(MobilityCalibrationUserType.class, TEXT),
      Map.entry(MobilityCalibrationDateTimeType.class, LocalDateTime.of(2024, 1, 16, 16, 17, 53)),
      Map.entry(MobilityCalibrationReferencePressureType.class, 2.52874),
      Map.entry(AcquisitionMzRangeType.class, Range.closed(50d, 1500d)),
      Map.entry(AcquisitionRtRangeType.class, Range.closed(0.05f, 15.3f)),
      Map.entry(RawDataFileFormatType.class, RawDataFileType.BRUKER_TDF));

  static @NotNull Stream<DataType<?>> rawFileMetadataTypes() {
    return DataTypes.getInstances().stream()
        .filter(type -> type.getClass().getPackageName().equals(RAW_FILE_TYPES_PACKAGE))
        .<DataType<?>>map(type -> (DataType<?>) type)
        .sorted(Comparator.comparing(DataType::getUniqueID));
  }

  @SuppressWarnings("unchecked")
  private static <T> void saveLoad(@NotNull final DataType<T> type, @NotNull final Object sample) {
    final T value = (T) sample;
    DataTypeTestUtils.contextFreeSaveLoadTest(type, value);
    DataTypeTestUtils.testStringConversion(type, value);
  }

  @Test
  void allTypesHaveSampleValues() {
    final List<DataType<?>> types = rawFileMetadataTypes().toList();
    Assertions.assertFalse(types.isEmpty(), "No raw file metadata types registered");
    for (final DataType<?> type : types) {
      Assertions.assertTrue(SAMPLES.containsKey(type.getClass()),
          () -> "Add a sample value for " + type.getClass().getName() + " to "
              + RawFileMetadataTypesTest.class.getSimpleName());
    }
  }

  @ParameterizedTest
  @MethodSource("rawFileMetadataTypes")
  void saveLoadWithoutFeatureList(@NotNull final DataType<?> type) {
    Assertions.assertFalse(type.requiresFeatureListContext(),
        () -> type.getClass().getName() + " is in the raw file metadata package but requires a "
            + "feature list context. It cannot be saved as raw file metadata.");
    final Object sample = SAMPLES.get(type.getClass());
    Assertions.assertNotNull(sample, () -> "No sample value for " + type.getClass().getName());
    saveLoad(type, sample);
  }
}
