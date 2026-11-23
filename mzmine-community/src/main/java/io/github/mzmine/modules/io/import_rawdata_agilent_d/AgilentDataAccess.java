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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.IonMobilityMsMsInfo;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.gui.preferences.AgilentCentroidingOption;
import io.github.mzmine.gui.preferences.AgilentImportOptions;
import io.github.mzmine.gui.preferences.VendorImportParameters;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalibration;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.DriftTubeCCSCalibration;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.parameters.parametertypes.combowithinput.ComboWithComboInputValue;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.concurrent.NotThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Single touch point between mzmine and the AgilentBridge subprocess. Mirrors
 * {@code MassLynxDataAccess}: it owns the open file (here: the bridge process), exposes typed read
 * methods, and is {@link AutoCloseable}. The import task only calls these methods and never sees
 * the wire protocol. Frames and scans are built by {@link AgilentSpectraBuilder}, the non-MS data
 * by {@link AgilentOtherData}, shared with the other native Agilent import paths.
 * <p>
 * Frame numbers are 1-based (1..{@link #getFrameCount()}); scans are 0-based
 * (0..{@link #getScanCount()}-1), matching the bridge.
 * <p>
 * IMS spectrum storage in Agilent {@code .d} files (checked on 21 IMS files, 2026-11): mobility
 * scans are always stored as profile only. The frame spectrum (total frame spectrum, TFS) is always
 * stored as profile. Native acquisitions from MassHunter B.09 onward additionally store vendor
 * centroids of the frame spectrum (MSPeak.bin); B.07 files and PNNL PreProcessor output (DeMP,
 * _MA) store profile only. No file stored frame spectrum centroids only. The stored centroids can
 * be cut by the acquisition thresholds to only a few peaks per frame (e.g., 10-30 in
 * MSV000096020).
 * <p>
 * decision: the frame spectra are always recentroided from the profile, the stored frame spectrum
 * centroids are not used, also not with {@link AgilentCentroidingOption#PREFER_STORED}. Here, the
 * bridge centroids the profile frame spectrum with the vendor peak filter (MHDAC sidecar), see
 * {@link #readFrame(IMSRawDataFileImpl, int)}.
 */
@NotThreadSafe
public class AgilentDataAccess implements AgilentRawDataAccess {

  private static final Logger logger = Logger.getLogger(AgilentDataAccess.class.getName());

  private final AgilentReaderClient client;
  private final File rawFile;
  @Nullable
  private final MemoryMapStorage storage;
  private final AgilentSpectraBuilder builder;

  /**
   * if the import option centroids IMS data with mzmine
   * ({@link AgilentImportOptions#isMzmineImsCentroiding()}) and
   * {@link AgilentDataAccess#requestCentroid}, profile frames and all mobility scans are centroided
   * by mzmine
   */
  private final AgilentImportOptions importOption;

  // cached "open" response
  private final boolean isIms;
  private final boolean requestCentroid;
  private final long numScans;
  private final int numFrames;
  private final double[] driftTimes;
  private final boolean hasSingleFieldCcs;
  private final double tFix;
  private final double beta;
  private final boolean hasMrm;
  private final boolean hasMsScans;
  // cached "fileMetadata" response
  private @Nullable AgilentFileMetadata fileMetadata;

  public AgilentDataAccess(@NotNull File rawFile, @NotNull VendorImportParameters vendorParam,
      @Nullable MemoryMapStorage storage, @Nullable ScanImportProcessorConfig processor) {
    this.rawFile = rawFile;
    this.storage = storage;
    this.builder = new AgilentSpectraBuilder(rawFile.getName(), processor);
    this.requestCentroid = vendorParam.getValue(VendorImportParameters.applyVendorCentroiding);
    final ComboWithComboInputValue<AgilentImportOptions, AgilentCentroidingOption> agilentSettings = vendorParam.getValue(
        VendorImportParameters.agilentImportChoice);
    this.importOption = agilentSettings.getSelectedOption();

    // The general profile-vs-centroid decision stays with "Try vendor centroiding": when off we
    // request "none" (profile); when on, the centroid source selects stored vs. recentroided.
    final AgilentCentroidingOption centroiding =
        agilentSettings.getEmbeddedValue() != null ? agilentSettings.getEmbeddedValue()
            : AgilentCentroidingOption.PREFER_STORED;
    final String centroidMode = requestCentroid ? centroiding.getWireCentroidMode() : "none";

    this.client = new AgilentReaderClient();
    final JsonNode open;
    try {
      open = client.send(
          Map.of("op", "open", "path", rawFile.getAbsolutePath(), "centroidMode", centroidMode));
    } catch (RuntimeException e) {
      // the import task never receives this instance to close, so stop the bridge process here
      client.close();
      throw e;
    }

    this.isIms = open.path("isIms").asBoolean(false);
    this.numScans = open.path("numScans").asLong(0);
    this.numFrames = open.path("numFrames").asInt(0);
    this.driftTimes = toDoubleArray(open.get("driftTimes"));
    this.hasSingleFieldCcs = open.path("hasSingleFieldCcs").asBoolean(false);
    this.tFix = open.path("tFix").asDouble(Double.NaN);
    this.beta = open.path("beta").asDouble(Double.NaN);
    this.hasMrm = open.path("hasMrm").asBoolean(false);
    this.hasMsScans = open.path("hasMsScans").asBoolean(true);

    logger.finest(
        "AgilentReader opened %s: ims=%b, frames=%d, scans=%d, requestCentroid=%b".formatted(
            rawFile.getName(), isIms, numFrames, numScans, requestCentroid));
  }

  @Override
  public boolean isIms() {
    return isIms;
  }

  @Override
  public int getFrameCount() {
    return numFrames;
  }

  @Override
  public long getScanCount() {
    return numScans;
  }

  /**
   * Whether the file has any non-MRM MS scans to import as spectra.
   */
  @Override
  public boolean hasMsScans() {
    return hasMsScans;
  }

  /**
   * Whether the file has MRM/SRM transitions (imported as OtherTimeSeries, not spectra).
   */
  @Override
  public boolean hasMrm() {
    return hasMrm;
  }

  @Override
  public @NotNull RawDataFileImpl createDataFile() {
    if (isIms) {
      return new IMSRawDataFileImpl(rawFile.getName(), rawFile.getAbsolutePath(), storage);
    }
    return new RawDataFileImpl(rawFile.getName(), rawFile.getAbsolutePath(), storage);
  }

  /**
   * Single-field drift-tube CCS calibration from the bridge, or null if unavailable.
   */
  @Override
  public @Nullable CCSCalibration getCcsCalibration() {
    if (hasSingleFieldCcs && !Double.isNaN(beta) && !Double.isNaN(tFix)) {
      return new DriftTubeCCSCalibration(beta, tFix, -1, -1);
    }
    return null;
  }

  /**
   * @return the file level metadata from the bridge, requested once and cached
   */
  private @NotNull AgilentFileMetadata fileMetadata() {
    if (fileMetadata == null) {
      final JsonNode node = client.send(Map.of("op", "fileMetadata"));
      fileMetadata = new AgilentFileMetadata(text(node, "sampleName"), text(node, "sampleId"),
          text(node, "sampleDescription"), text(node, "operatorName"),
          text(node, "instrumentModel"), text(node, "instrumentSerialNumber"),
          text(node, "instrumentVendor"), text(node, "acquisitionSoftware"),
          text(node, "acquisitionSoftwareVersion"), text(node, "acquisitionDateTime"),
          text(node, "acquisitionMethod"), list(node.get("ionSources")),
          list(node.get("massAnalyzers")));
    }
    return fileMetadata;
  }

  /**
   * @return the acquisition date and time, null if not available
   */
  @Override
  public @Nullable LocalDateTime getAcqDate() {
    return fileMetadata().acquisitionDate(rawFile.getName());
  }

  /**
   * Sets the file level metadata of the bridge (sample, operator, instrument, acquisition) to the
   * raw file metadata.
   */
  @Override
  public void applyToFileMetadata(@NotNull final ModularDataModel metadata) {
    fileMetadata().applyTo(metadata);
  }

  private static @Nullable String text(@NotNull JsonNode node, @NotNull String field) {
    final JsonNode value = node.get(field);
    if (value == null || !value.isTextual() || value.asText().isBlank()) {
      return null;
    }
    return value.asText().strip();
  }

  private static @NotNull List<String> list(@Nullable JsonNode array) {
    if (array == null || !array.isArray()) {
      return List.of();
    }
    final List<String> values = new ArrayList<>(array.size());
    for (final JsonNode value : array) {
      if (value.isTextual()) {
        values.add(value.asText());
      }
    }
    return values;
  }

  /**
   * Reads one IMS frame: the Total Frame Spectrum becomes the {@link SimpleFrame}, the per-bin
   * mobility scans its mobility scans. Returns null if filtered out by the scan selection.
   * <p>
   * With vendor centroiding, the bridge always recentroids the profile frame spectrum with the
   * vendor peak filter (MHDAC), even if the file stores frame spectrum centroids. This is a
   * conscious decision, see the class documentation. Mobility scans are always stored and returned
   * as profile; they are only centroided by mzmine if
   * {@link AgilentImportOptions#isMzmineImsCentroiding()}.
   */
  @Override
  public @Nullable SimpleFrame readFrame(@NotNull IMSRawDataFileImpl file, int frameId) {
    // --- metadata (mtd) ---
    final JsonNode mtd = client.send(Map.of("op", "mtd", "id", frameId));
    final float rt = (float) mtd.path("rtMin").asDouble(0);
    final PolarityType polarity = parsePolarity(mtd.path("polarity").asText(null));
    // MS level comes from the frame's fragmentation class (All-Ions high-energy -> MS2)
    final int msLevel = mtd.path("msLevel").asInt(1);
    // max possible abundance of a mobility scan point (detector saturation level, not for the
    // TFS); NaN when not stored
    final double mobilityScanAbundLimit = abundanceLimit(mtd, "mobilityScanAbundLimit");

    if (builder.isFrameFilteredOut(frameId, msLevel, polarity, rt, driftTimes)) {
      // peak ops (tfs / mobilityScans) are never requested for a filtered frame
      return null;
    }

    final IonMobilityMsMsInfo msMsInfo =
        msLevel > 1 ? AgilentSpectraBuilder.imsMsMsInfo(mtd.path("collisionEnergy").asDouble(0),
            mtd.path("hasPrecursor").asBoolean(false), mtd.path("precursorMz").asDouble(0),
            mtd.path("isolationWidth").asDouble(0), driftTimes.length) : null;
    final AgilentFrameInfo info = new AgilentFrameInfo(rt, polarity, msLevel,
        mobilityScanAbundLimit, msMsInfo);

    // --- Total Frame Spectrum (peak data) -> the Frame ---
    final JsonNode tfs = client.send(Map.of("op", "tfs", "frameId", frameId));
    final int numPoints = tfs.path("numPoints").asInt(0);
    final boolean centroided = tfs.path("centroided").asBoolean(false);
    final double[] mzs = client.readBlob(numPoints);
    final double[] intensities = client.readBlob(numPoints);

    // --- mobility scans, always profile from the API ---
    final JsonNode mob = client.send(Map.of("op", "mobilityScans", "frameId", frameId));
    final int numMobScans = mob.path("numScans").asInt(0);
    final JsonNode pointsPerScan = mob.get("pointsPerScan");
    final List<SimpleSpectralArrays> mobilityScans = new ArrayList<>(numMobScans);
    for (int i = 0; i < numMobScans; i++) {
      final int pts = pointsPerScan.get(i).asInt(0);
      mobilityScans.add(new SimpleSpectralArrays(client.readBlob(pts), client.readBlob(pts)));
    }

    // the mzio reader option uses this path as fallback if the reader is not available
    final boolean mzmineCentroiding = requestCentroid && importOption.isMzmineImsCentroiding();
    return builder.buildFrame(file, frameId, info, mzs, intensities, centroided, mobilityScans,
        mzmineCentroiding, driftTimes);
  }

  /**
   * Reads one non-IMS scan, or null if filtered out by the scan selection.
   */
  @Override
  public @Nullable SimpleScan readScan(@NotNull RawDataFileImpl file, int id) {
    // --- metadata (mtd) ---
    final JsonNode mtd = client.send(Map.of("op", "mtd", "id", id));

    // MRM/SRM scans are imported as OtherTimeSeries (see readMrms), never as spectra
    if ("MultipleReaction".equals(mtd.path("msScanType").asText(""))) {
      return null;
    }

    final int msLevel = mtd.path("msLevel").asInt(1);
    final float rt = (float) mtd.path("rtMin").asDouble(0);
    final PolarityType polarity = parsePolarity(mtd.path("polarity").asText(null));
    final double precursorMz = mtd.path("precursorMz").asDouble(0);

    if (builder.isScanFilteredOut(id, msLevel, polarity, rt, precursorMz)) {
      // the peak "scan" op is never requested for a filtered scan
      return null;
    }

    // --- peak data ---
    final JsonNode peak = client.send(Map.of("op", "scan", "id", id));
    final int numPoints = peak.path("numPoints").asInt(0);
    final boolean centroided = peak.path("centroided").asBoolean(false);
    final double[] mzs = client.readBlob(numPoints);
    final double[] intensities = client.readBlob(numPoints);

    final MsMsInfo msMsInfo = AgilentSpectraBuilder.scanMsMsInfo(msLevel,
        mtd.path("hasPrecursor").asBoolean(false), precursorMz, precursorCharge(mtd),
        mtd.path("collisionEnergy").asDouble(0), activationMethod(mtd), isolationWindow(mtd));
    return builder.buildScan(file, id, msLevel, rt, polarity, precursorMz, msMsInfo, mzs,
        intensities, centroided);
  }

  /**
   * Reads the file's MRM/SRM transitions and attaches them to {@code dataFile}. No-op for non-MRM
   * files.
   */
  @Override
  public void readMrms(@NotNull RawDataFileImpl dataFile) {
    final JsonNode h = client.send(Map.of("op", "mrm"));
    final int count = h.path("count").asInt(0);
    final JsonNode transitions = h.get("transitions");
    final List<AgilentMrmTrace> traces = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      final JsonNode t = transitions.get(i);
      final int n = t.path("numPoints").asInt(0);
      traces.add(new AgilentMrmTrace(t.path("q1").asDouble(0), t.path("q3").asDouble(0),
          toFloats(client.readBlob(n)), client.readBlob(n)));
    }
    AgilentOtherData.addMrms(dataFile, storage, traces);
  }

  /**
   * Reads non-MS detector signals (DAD/UV, instrument curves) and attaches them to
   * {@code dataFile}.
   */
  @Override
  public void readAnalogChannels(@NotNull RawDataFileImpl dataFile) {
    final JsonNode h = client.send(Map.of("op", "analog"));
    final int count = h.path("count").asInt(0);
    final JsonNode channels = h.get("channels");
    final List<AgilentAnalogTrace> traces = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      final JsonNode c = channels.get(i);
      final int n = c.path("numPoints").asInt(0);
      traces.add(
          new AgilentAnalogTrace(c.path("deviceType").asText(""), c.path("signalDesc").asText(""),
              toFloats(client.readBlob(n)), client.readBlob(n)));
    }
    AgilentOtherData.addAnalogChannels(dataFile, storage, traces);
  }

  private static float[] toFloats(double[] values) {
    final float[] out = new float[values.length];
    for (int i = 0; i < values.length; i++) {
      out[i] = (float) values[i];
    }
    return out;
  }

  /**
   * Precursor charge: the spectrum-derived charge if present, else the low-level scan record's
   * charge state. 0 when neither is set (typical for non-DDA acquisitions).
   */
  private static int precursorCharge(JsonNode header) {
    final int charge = header.path("precursorCharge").asInt(0);
    return charge > 0 ? charge : header.path("chargeState").asInt(0);
  }

  /**
   * Dissociation/activation method from the bridge's low-level scan record. Agilent reports
   * {@code Unspecified} for the default workflow, which we treat as CID (its usual mode); only an
   * explicit {@code ETD} flag changes the mapping.
   */
  private static ActivationMethod activationMethod(JsonNode header) {
    final String mode = header.path("fragmentationMode").asText(null);
    if (mode == null) {
      return ActivationMethod.CID;
    }
    return switch (mode) {
      case "ETD" -> ActivationMethod.ETD;
      default -> ActivationMethod.CID; // CID / Unspecified
    };
  }

  /**
   * Quad isolation window from the bridge: the explicit MZOfInterest range when present, else the
   * precursor m/z widened by the configured quad isolation width. Null when neither is available.
   */
  private static Range<Double> isolationWindow(JsonNode header) {
    final double low = header.path("isolationLowMz").asDouble(0);
    final double high = header.path("isolationHighMz").asDouble(0);
    if (high > low) {
      return Range.closed(low, high);
    }
    final double mz = header.path("precursorMz").asDouble(0);
    final double width = header.path("isolationWidth").asDouble(0);
    return mz > 0 && width > 0 ? Range.closed(mz - width / 2, mz + width / 2) : null;
  }

  /**
   * Abundance limit from the bridge mtd. The SDK reports 0 or NaN when the file does not store it,
   * both mapped to NaN.
   */
  private static double abundanceLimit(@NotNull JsonNode mtd, @NotNull String field) {
    final double limit = mtd.path(field).asDouble(Double.NaN);
    return limit > 0 ? limit : Double.NaN;
  }

  private static PolarityType parsePolarity(@Nullable String polarity) {
    if (polarity == null) {
      return PolarityType.UNKNOWN;
    }
    return switch (polarity) {
      case "Positive" -> PolarityType.POSITIVE;
      case "Negative" -> PolarityType.NEGATIVE;
      default -> PolarityType.UNKNOWN;
    };
  }

  private static double[] toDoubleArray(@Nullable JsonNode array) {
    if (array == null || !array.isArray()) {
      return new double[0];
    }
    final double[] out = new double[array.size()];
    for (int i = 0; i < out.length; i++) {
      out[i] = array.get(i).asDouble();
    }
    return out;
  }

  @Override
  public void close() {
    try {
      client.send(Map.of("op", "close"));
    } catch (RuntimeException e) {
      logger.finest("AgilentBridge close op failed (ignored): " + e.getMessage());
    }
    client.close();
  }
}
