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

package import_data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.gui.preferences.AgilentCentroidingOption;
import io.github.mzmine.gui.preferences.AgilentImportOptions;
import io.github.mzmine.gui.preferences.MassLynxImportOptions;
import io.github.mzmine.gui.preferences.VendorImportParameters;
import io.github.mzmine.gui.preferences.WatersLockmassParameters;
import io.github.mzmine.modules.io.import_rawdata_agilent_d.AgilentDataAccess;
import io.github.mzmine.parameters.parametertypes.combowithinput.ComboWithComboInputValue;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import testutils.MZmineTestUtil;

/**
 * Tests the native Agilent {@code .d} reader bridge ({@link AgilentDataAccess}) against the three
 * committed example files: an IMS All-Ions dataset, a non-IMS LC-MS dataset and an MRM dataset. The
 * tests exercise scan/frame counts, data-point counts of individual scans, both native import
 * options ({@link AgilentImportOptions#AGILENT_READER} vs
 * {@link AgilentImportOptions#AGILENT_READER_AUTO_CENTROID}) and both centroid sources
 * ({@link AgilentCentroidingOption}).
 * <p>
 * Each {@link AgilentDataAccess} launches a separate bridge subprocess (the expensive part), so —
 * unlike the in-process Waters reader — we must not open a fresh access per assertion. Instead a
 * {@link AgilentDataAccess} is opened once per distinct reader configuration (file + centroiding
 * toggle + import option + centroid source), cached in {@link #accessCache}, shared across the test
 * methods, and closed in {@link #closeAccesses()}. Reading scans/frames from an already-launched
 * process is cheap, so tests read what they need from the shared instances.
 * <p>
 * Windows-only: the bridge relies on the Windows-only Agilent MassHunter SDK and needs the bridge
 * executable resolvable under {@code external_tools/agilent_bridge}. Each test skips (rather than
 * fails) when its data file is absent, so it is a no-op on machines that do not carry the example
 * data.
 * <p>
 * The {@code MSCONVERT} import option is intentionally not covered here: it does not go through
 * {@link AgilentDataAccess} but through {@code MSConvertImportTask}, which requires an installed
 * ProteoWizard.
 */
@TestInstance(Lifecycle.PER_CLASS)
@DisabledOnOs({OS.MAC, OS.LINUX})
public class AgilentImportTest {

  private static final String DIR = "src/test/resources/rawdatafiles/additional/agilent/";
  private static final File IMS_FILE = new File(DIR + "ImsSynth_AllIons.d");
  private static final File LCMS_FILE = new File(DIR + "pepmix-with-variable-transients.d");
  private static final File MRM_FILE = new File(DIR + "TMRM_Mix_092110_50ppb_02.d");

  // A frame in the middle of the IMS elution that carries real signal in both the total frame
  // spectrum and a good number of mobility bins (verified during test authoring).
  private static final int IMS_SIGNAL_FRAME = 252;
  // A non-IMS LC-MS scan with the most data points (a full MS1 spectrum), used to read a rich
  // spectrum rather than a near-empty one.
  private static final int LCMS_RICH_SCAN = 940;

  // One open bridge subprocess per distinct reader configuration, shared across all test methods.
  private final Map<String, AgilentDataAccess> accessCache = new LinkedHashMap<>();

  @BeforeAll
  void init() {
    MZmineTestUtil.startMzmineCore();
  }

  @AfterAll
  void closeAccesses() {
    accessCache.values().forEach(AgilentDataAccess::close);
    accessCache.clear();
  }

  /**
   * Returns the shared {@link AgilentDataAccess} for the given reader configuration, opening (and
   * launching the bridge subprocess) only on the first request for that configuration.
   *
   * @param file              the .d file
   * @param vendorCentroiding the "Try vendor centroiding" toggle: when off the reader returns
   *                          profile data, when on it returns centroids
   * @param importOption      which native reader path to use
   * @param centroidSource    the centroid source (only relevant when {@code vendorCentroiding})
   */
  private AgilentDataAccess access(File file, boolean vendorCentroiding,
      AgilentImportOptions importOption, AgilentCentroidingOption centroidSource) {
    final String key =
        file.getName() + '|' + vendorCentroiding + '|' + importOption + '|' + centroidSource;
    return accessCache.computeIfAbsent(key, k -> new AgilentDataAccess(file,
        agilentParams(vendorCentroiding, importOption, centroidSource), null, null));
  }

  private static VendorImportParameters agilentParams(boolean vendorCentroiding,
      AgilentImportOptions importOption, AgilentCentroidingOption centroidSource) {
    return VendorImportParameters.create(vendorCentroiding,
        MassLynxImportOptions.NATIVE_MZMINE_CENTROIDING, true,
        WatersLockmassParameters.createDefault(), true,
        new ComboWithComboInputValue<>(importOption, centroidSource),
        VendorImportParameters.DEFAULT_PRESSURE_COMPENSATION);
  }

  // ------------------------------------------------------------------------------------- IMS ---

  @Test
  @DisplayName("IMS: file is recognized as IMS with the expected frame / mobility-bin structure")
  void imsFileStructure() {
    assumeTrue(IMS_FILE.exists(), "IMS example file missing");
    final AgilentDataAccess access = access(IMS_FILE, true,
        AgilentImportOptions.AGILENT_READER_AUTO_CENTROID, AgilentCentroidingOption.PREFER_STORED);

    assertTrue(access.isIms(), "should be detected as IMS");
    assertEquals(880, access.getFrameCount(), "frame count");
    // IMS data is frame-based; there are no flat (non-IMS) scans.
    assertEquals(0, access.getScanCount(), "no flat scans for an IMS file");
    assertFalse(access.hasMrm(), "no MRM in this IMS file");
    assertTrue(access.hasMsScans(), "has MS scans");
    assertTrue(access.createDataFile() instanceof IMSRawDataFileImpl,
        "IMS file -> IMSRawDataFileImpl");

    final var file = (IMSRawDataFileImpl) access.createDataFile();
    final SimpleFrame frame = access.readFrame(file, IMS_SIGNAL_FRAME);
    assertNotNull(frame);
    assertEquals(354, frame.getNumberOfMobilityScans(), "mobility bins per frame");
    assertEquals(MobilityType.DRIFT_TUBE, frame.getMobilityType());

    // All-Ions alternates: odd frames are MS1 (low energy), even frames are MS2 (high energy).
    assertEquals(1, access.readFrame(file, 251).getMSLevel(), "odd frame is MS1");
    assertEquals(2, frame.getMSLevel(), "even frame is MS2 (All-Ions)");
  }

  @Test
  @DisplayName("IMS: 'Try vendor centroiding' switches the frame between profile and centroid")
  void imsProfileVsCentroid() {
    assumeTrue(IMS_FILE.exists(), "IMS example file missing");

    // centroiding off -> profile total frame spectrum + profile mobility scans (vendor data)
    final AgilentDataAccess profile = access(IMS_FILE, false,
        AgilentImportOptions.AGILENT_READER_AUTO_CENTROID, AgilentCentroidingOption.PREFER_STORED);
    final SimpleFrame profileFrame = profile.readFrame(
        (IMSRawDataFileImpl) profile.createDataFile(), IMS_SIGNAL_FRAME);
    assertEquals(MassSpectrumType.PROFILE, profileFrame.getSpectrumType());
    assertEquals(1278, profileFrame.getNumberOfDataPoints(), "profile TFS data points");
    assertEquals(MassSpectrumType.PROFILE, profileFrame.getMobilityScan(0).getSpectrumType());
    assertEquals(16, countNonEmptyMobilityScans(profileFrame), "non-empty profile mobility bins");
    assertEquals(6212, totalMobilityPoints(profileFrame), "total profile mobility points");

    // centroiding on (auto-centroid path) -> centroided frame; mzmine centroids the mobility scans
    final AgilentDataAccess centroid = access(IMS_FILE, true,
        AgilentImportOptions.AGILENT_READER_AUTO_CENTROID, AgilentCentroidingOption.PREFER_STORED);
    final SimpleFrame centroidFrame = centroid.readFrame(
        (IMSRawDataFileImpl) centroid.createDataFile(), IMS_SIGNAL_FRAME);
    assertEquals(MassSpectrumType.CENTROIDED, centroidFrame.getSpectrumType());
    assertEquals(80, centroidFrame.getNumberOfDataPoints(), "vendor-centroided TFS data points");
    assertEquals(MassSpectrumType.CENTROIDED, centroidFrame.getMobilityScan(0).getSpectrumType(),
        "auto-centroid centroids the mobility scans");
    final int centroidedPoints = totalMobilityPoints(centroidFrame);
    assertTrue(centroidedPoints > 0, "centroided mobility scans keep data");
    assertTrue(centroidedPoints < 6212,
        "centroiding reduces the mobility point count (was " + centroidedPoints + ")");
  }

  @Test
  @DisplayName("IMS: plain AgilentReader does not centroid the mobility scans, auto-centroid does")
  void imsImportOptionMobilityCentroiding() {
    assumeTrue(IMS_FILE.exists(), "IMS example file missing");

    // Plain native reader: the total frame spectrum is vendor-centroided (MHDAC), but the mobility
    // scans are left as profile (the reader has no IMS centroiding of its own).
    final AgilentDataAccess access = access(IMS_FILE, true, AgilentImportOptions.AGILENT_READER,
        AgilentCentroidingOption.PREFER_STORED);
    final SimpleFrame frame = access.readFrame((IMSRawDataFileImpl) access.createDataFile(),
        IMS_SIGNAL_FRAME);
    assertEquals(MassSpectrumType.CENTROIDED, frame.getSpectrumType(), "TFS is vendor-centroided");
    assertEquals(80, frame.getNumberOfDataPoints());
    assertEquals(MassSpectrumType.PROFILE, frame.getMobilityScan(0).getSpectrumType(),
        "plain reader leaves mobility scans as profile");
    assertEquals(6212, totalMobilityPoints(frame), "mobility scans are untouched profile data");
  }

  @Test
  @DisplayName("IMS: stored vs recentroided centroid source produce the same TFS for this file")
  void imsCentroidSourceEquivalence() {
    assumeTrue(IMS_FILE.exists(), "IMS example file missing");
    // The IMS frame spectra are always recentroided from the profile (MHDAC), so the stored-vs-
    // recentroided choice does not change the result for this file. (The distinction matters for
    // non-IMS files that store both a profile and a vendor-centroid representation.)
    final AgilentDataAccess stored = access(IMS_FILE, true,
        AgilentImportOptions.AGILENT_READER_AUTO_CENTROID, AgilentCentroidingOption.PREFER_STORED);
    final AgilentDataAccess recentroided = access(IMS_FILE, true,
        AgilentImportOptions.AGILENT_READER_AUTO_CENTROID,
        AgilentCentroidingOption.PREFER_RECENTROIDED);

    final int storedPoints = stored.readFrame((IMSRawDataFileImpl) stored.createDataFile(),
        IMS_SIGNAL_FRAME).getNumberOfDataPoints();
    final int recentroidedPoints = recentroided.readFrame(
            (IMSRawDataFileImpl) recentroided.createDataFile(), IMS_SIGNAL_FRAME)
        .getNumberOfDataPoints();
    assertEquals(storedPoints, recentroidedPoints, "stored and recentroided TFS point counts");
  }

  // ------------------------------------------------------------------------------------ LCMS ---

  @Test
  @DisplayName("LC-MS: non-IMS file with the expected scan count and MS-level split")
  void lcmsFileStructure() {
    assumeTrue(LCMS_FILE.exists(), "LC-MS example file missing");
    final AgilentDataAccess access = access(LCMS_FILE, true, AgilentImportOptions.AGILENT_READER,
        AgilentCentroidingOption.PREFER_STORED);

    assertFalse(access.isIms(), "not IMS");
    assertEquals(0, access.getFrameCount(), "no frames for a non-IMS file");
    assertEquals(1561, access.getScanCount(), "scan count");
    assertFalse(access.hasMrm(), "no MRM");
    assertTrue(access.hasMsScans(), "has MS scans");
    assertTrue(access.createDataFile() instanceof RawDataFileImpl);
    assertFalse(access.createDataFile() instanceof IMSRawDataFileImpl);

    final var file = (RawDataFileImpl) access.createDataFile();
    int ms1 = 0;
    int ms2 = 0;
    for (int id = 0; id < access.getScanCount(); id++) {
      final SimpleScan scan = access.readScan(file, id);
      assertNotNull(scan, "scan " + id);
      if (scan.getMSLevel() == 1) {
        ms1++;
      } else {
        ms2++;
      }
    }
    assertEquals(853, ms1, "MS1 scans");
    assertEquals(708, ms2, "MS2 scans");
    assertEquals(1561, ms1 + ms2);
  }

  @Test
  @DisplayName("LC-MS: a rich scan has the expected data-point count, base peak and centroid type")
  void lcmsRichScan() {
    assumeTrue(LCMS_FILE.exists(), "LC-MS example file missing");
    final AgilentDataAccess access = access(LCMS_FILE, true, AgilentImportOptions.AGILENT_READER,
        AgilentCentroidingOption.PREFER_STORED);
    final SimpleScan scan = access.readScan((RawDataFileImpl) access.createDataFile(),
        LCMS_RICH_SCAN);
    assertNotNull(scan);
    assertEquals(6000, scan.getNumberOfDataPoints(), "data points of the rich scan");
    assertEquals(MassSpectrumType.CENTROIDED, scan.getSpectrumType());
    assertEquals(225.0799, scan.getBasePeakMz(), 1e-3, "base peak m/z");
    // m/z values are sorted ascending.
    assertTrue(scan.getMzValue(0) < scan.getMzValue(scan.getNumberOfDataPoints() - 1));
  }

  @Test
  @DisplayName("LC-MS: this file is centroid-only, so all import/centroid options agree")
  void lcmsOptionsEquivalence() {
    assumeTrue(LCMS_FILE.exists(), "LC-MS example file missing");

    // The two native import options differ only in IMS handling; for a non-IMS file they are
    // identical.
    final double readerBp = richScanBasePeak(
        access(LCMS_FILE, true, AgilentImportOptions.AGILENT_READER,
            AgilentCentroidingOption.PREFER_STORED));
    final double autoBp = richScanBasePeak(
        access(LCMS_FILE, true, AgilentImportOptions.AGILENT_READER_AUTO_CENTROID,
            AgilentCentroidingOption.PREFER_STORED));
    assertEquals(readerBp, autoBp, "import option must not change non-IMS data");

    // The file stores only centroids (no profile representation), so:
    //  - requesting profile (centroiding off) still yields centroided spectra, and
    //  - 'prefer recentroided' falls back to the stored centroids -> same as 'prefer stored'.
    final double recentroidedBp = richScanBasePeak(
        access(LCMS_FILE, true, AgilentImportOptions.AGILENT_READER,
            AgilentCentroidingOption.PREFER_RECENTROIDED));
    assertEquals(readerBp, recentroidedBp, "centroid-only file: recentroid falls back to stored");

    final AgilentDataAccess profile = access(LCMS_FILE, false, AgilentImportOptions.AGILENT_READER,
        AgilentCentroidingOption.PREFER_STORED);
    assertEquals(MassSpectrumType.CENTROIDED,
        profile.readScan((RawDataFileImpl) profile.createDataFile(), LCMS_RICH_SCAN)
            .getSpectrumType(),
        "centroid-only file stays centroided even when profile is requested");
  }

  private static double richScanBasePeak(AgilentDataAccess access) {
    return access.readScan((RawDataFileImpl) access.createDataFile(), LCMS_RICH_SCAN)
        .getBasePeakMz();
  }

  // ------------------------------------------------------------------------------------- MRM ---

  @Test
  @DisplayName("MRM: transitions are imported as time series, not as spectra")
  void mrmTransitions() {
    assumeTrue(MRM_FILE.exists(), "MRM example file missing");
    final AgilentDataAccess access = access(MRM_FILE, true, AgilentImportOptions.AGILENT_READER,
        AgilentCentroidingOption.PREFER_STORED);

    assertFalse(access.isIms(), "not IMS");
    assertTrue(access.hasMrm(), "has MRM transitions");
    assertFalse(access.hasMsScans(), "MRM-only file has no MS spectra to import");
    assertEquals(13868, access.getScanCount(), "raw MRM scan count");

    final var file = (RawDataFileImpl) access.createDataFile();
    // MRM scans are imported as chromatograms, not spectra: readScan returns null for them.
    assertNull(access.readScan(file, 0), "MRM scan must not be imported as a spectrum");

    access.readMrms(file);
    assertEquals(1, file.getOtherDataFiles().size(), "one MRM/SRM other-data file");
    final var otherFile = file.getOtherDataFiles().get(0);
    assertEquals("MRM/SRM", otherFile.getDescription());
    assertNotNull(otherFile.getOtherTimeSeriesData());
    assertEquals(197, otherFile.getOtherTimeSeriesData().getRawTraces().size(),
        "number of MRM transitions");
  }

  // ---------------------------------------------------------------------------------- helpers ---

  private static int countNonEmptyMobilityScans(SimpleFrame frame) {
    int count = 0;
    for (int i = 0; i < frame.getNumberOfMobilityScans(); i++) {
      if (frame.getMobilityScan(i).getNumberOfDataPoints() > 0) {
        count++;
      }
    }
    return count;
  }

  private static int totalMobilityPoints(SimpleFrame frame) {
    int total = 0;
    for (int i = 0; i < frame.getNumberOfMobilityScans(); i++) {
      total += frame.getMobilityScan(i).getNumberOfDataPoints();
    }
    return total;
  }
}
