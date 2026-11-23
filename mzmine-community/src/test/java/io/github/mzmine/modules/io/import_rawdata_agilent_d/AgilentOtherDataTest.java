package io.github.mzmine.modules.io.import_rawdata_agilent_d;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesData;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import testutils.MZmineTestUtil;

@DisabledOnOs(OS.MAC)
class AgilentOtherDataTest {

  /**
   * DAD signal descriptions as written by Agilent, taken from an MSConvert mzML of an LC-UV-MS
   * run: "Sig=220.0,4.0  Ref=off" is the wavelength followed by the bandwidth.
   */
  private static final String UV_TRACES = "rawdatafiles/additional/agilent_uv_traces.mzML";

  /**
   * Signal description of DAD1 A in the mzio test file
   * TOFsulfasMS4GHzDualMode+DADSpectra+UVSignal272.d, as returned by both native readers: 272 nm
   * with 16 nm bandwidth.
   */
  private static final String NATIVE_DAD_DESCRIPTION = "Sig=272,16 Ref=360,100";

  /**
   * The wavelength parsed from a DAD signal description is the signal wavelength, not the
   * bandwidth that follows it.
   */
  @Test
  void dadWavelengthFromSignalDescription() throws InterruptedException {
    MZmineTestUtil.startMzmineCore();
    MZmineTestUtil.cleanProject();
    MZmineTestUtil.importFiles(List.of(UV_TRACES), 60);
    final RawDataFile mzml = ProjectService.getProject().getDataFiles()[0];
    final List<String> descriptions = mzml.getOtherDataFiles().stream()
        .map(OtherDataFile::getOtherTimeSeriesData).filter(Objects::nonNull)
        .map(OtherTimeSeriesData::getRawTraces).flatMap(List::stream)
        .map(f -> f.getFeatureData().getName()).toList();
    Assertions.assertEquals(
        List.of("DAD1 A: Sig=220.0,4.0  Ref=off", "DAD1 B: Sig=254.0,4.0  Ref=off"), descriptions);
    MZmineTestUtil.cleanProject();

    final RawDataFileImpl file = new RawDataFileImpl("test.d", null, null, Color.BLACK);
    final List<String> allDescriptions = new ArrayList<>(descriptions);
    allDescriptions.add(NATIVE_DAD_DESCRIPTION);
    AgilentOtherData.addAnalogChannels(file, null, allDescriptions.stream()
        .map(d -> new AgilentAnalogTrace("DiodeArrayDetector", d, new float[]{0f},
            new double[]{0d})).toList());

    final List<Double> wavelengths = file.getOtherDataFiles().stream()
        .map(OtherDataFile::getOtherTimeSeriesData).filter(Objects::nonNull)
        .map(OtherTimeSeriesData::getRawTraces).flatMap(List::stream)
        .map(OtherFeature::getWavelength).toList();
    Assertions.assertEquals(List.of(220.0, 254.0, 272.0), wavelengths);
  }
}
