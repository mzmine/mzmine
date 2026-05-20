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

package io.github.mzmine.modules.io.import_rawdata_shimadzu;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.impl.builders.SimpleBuildingScan;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.gui.preferences.VendorImportParameters;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Imports a Shimadzu LabSolutions {@code .lcd} (LC-MS) or {@code .qgd} (GC-MS) file via the
 * external {@code ShimadzuBridge.exe} child process.
 */
public class ShimadzuImportTask extends AbstractTask implements RawDataImportTask {

  private static final Logger logger = Logger.getLogger(ShimadzuImportTask.class.getName());

  private final File file;
  @NotNull
  private final Class<? extends MZmineModule> module;
  @NotNull
  private final ParameterSet parameters;
  @NotNull
  private final MZmineProject project;
  @NotNull
  private final ScanImportProcessorConfig processor;
  private final boolean centroid;

  private int totalScans = 0;
  private int loadedScans = 0;
  private RawDataFileImpl dataFile;

  public ShimadzuImportTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      File file, @NotNull Class<? extends MZmineModule> module, @NotNull ParameterSet parameters,
      @NotNull MZmineProject project, @NotNull ScanImportProcessorConfig processor) {
    super(storage, moduleCallDate);
    this.file = file;
    this.module = module;
    this.parameters = parameters;
    this.project = project;
    this.processor = processor;
    VendorImportParameters vendorParam = parameters.getParameter(
        AllSpectralDataImportParameters.vendorOptions).getValue();
    centroid = vendorParam.getValue(VendorImportParameters.applyVendorCentroiding);
  }

  @Override
  public String getTaskDescription() {
    return "Importing Shimadzu raw data file %s. Scan %d/%d".formatted(file.getName(), loadedScans,
        totalScans);
  }

  @Override
  public double getFinishedPercentage() {
    return totalScans == 0 ? 0d : loadedScans / (double) totalScans;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    try (ShimadzuBridgeProcess bridge = new ShimadzuBridgeProcess()) {
      final ShimadzuProtocol p = bridge.protocol();

      // open
      p.send(ShimadzuProtocol.openRequest(file.getAbsolutePath()));
      final JsonNode openResp = p.readHeader();
      if (!openResp.path("ok").asBoolean(false)) {
        error("ShimadzuBridge open failed: " + openResp.path("error").asText("unknown"));
        return;
      }
      totalScans = openResp.path("scanCount").asInt(0);
      if (totalScans <= 0) {
        // No scans in the file; finish with an empty data file rather than
        // failing — matches the behaviour of the other vendor importers.
        logger.warning("Shimadzu file has zero scans: " + file.getAbsolutePath());
      }

      dataFile = new RawDataFileImpl(file.getName(), file.getAbsolutePath(), storage);

      if (totalScans > 0) {
        readAllScans(p, dataFile, !centroid);
      }

      // close + shutdown handled by try-with-resources -> bridge.close()
      try {
        p.send(ShimadzuProtocol.closeRequest());
        p.readHeader();
      } catch (IOException ignored) {
        // bridge will be torn down anyway
      }

      if (isCanceled()) {
        return;
      }

      final var appliedMethod = new SimpleFeatureListAppliedMethod(module, parameters,
          getModuleCallDate());
      dataFile.getAppliedMethods().add(appliedMethod);

      project.addFile(dataFile);
      setStatus(TaskStatus.FINISHED);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error reading Shimadzu file " + file.getAbsolutePath(), e);
      error(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
    }
  }

  private void readAllScans(ShimadzuProtocol p, RawDataFileImpl out, boolean profile)
      throws IOException {
    // SDK scan numbers are 1-based; the wire mirrors that.
    p.send(ShimadzuProtocol.scanRangeRequest(1, totalScans, profile));
    final JsonNode outer = p.readHeader();
    if (!outer.path("ok").asBoolean(false)) {
      throw new IOException("scanRange failed: " + outer.path("error").asText("unknown"));
    }
    final int count = outer.path("count").asInt(0);
    if (count != totalScans) {
      // Defensive — should never happen, but if it does we want to know rather
      // than silently truncate or stall reading blobs.
      logger.warning(
          "ShimadzuBridge scanRange count (" + count + ") differs from open's scanCount ("
              + totalScans + ")");
    }

    int skippedFailedScans = 0;

    for (int i = 0; i < count; i++) {
      if (isCanceled()) {
        return;
      }

      final JsonNode hdr = p.readHeader();
      loadedScans++;

      if (!hdr.path("ok").asBoolean(false)) {
        // The bridge emits an {ok:false} header and NO binary blobs for an
        // individual scan that failed to read (e.g. SDK returned E_FAIL for
        // a corrupt frame). Skip it and keep importing — a single bad scan
        // shouldn't kill a 10k-scan file.
        skippedFailedScans++;
        if (skippedFailedScans <= 5) {
          logger.log(Level.WARNING, "Skipping scan #{0}: {1} ({2})",
              new Object[]{i + 1, hdr.path("error").asText("unknown"),
                  hdr.path("code").asText("?")});
        }
        continue;
      }

      final int nPeaks = hdr.path("nPeaks").asInt(0);
      final double[] mz = p.readDoubles(nPeaks);
      final double[] intensity = p.readDoubles(nPeaks);

      // 'scanNo' on the wire is the SDK's 1-based scan number — same
      // convention mzmine's SimpleScan uses, so pass through directly.
      final int scanNumber = hdr.path("scanNo").asInt(0);
      final int msLevel = hdr.path("msLevel").asInt(1);
      final float rt = (float) hdr.path("rt").asDouble(0d);
      final PolarityType polarity = parsePolarity(hdr.path("polarity").asText(""));
      final boolean isProfile = hdr.path("profile").asBoolean(false);
      final MassSpectrumType type =
          isProfile ? MassSpectrumType.PROFILE : MassSpectrumType.CENTROIDED;

      // The bridge zero-filters PrecursorMzList, so [0] is always a real
      // precursor (or the array is omitted entirely for MS1).
      final JsonNode precursors = hdr.path("precursorMz");
      final double precursorMz =
          (msLevel >= 2 && precursors.isArray() && precursors.size() > 0) ? precursors.get(0)
                                                                            .asDouble(0d) : 0d;
      final int charge = hdr.path("charge").asInt(0);

      final SimpleBuildingScan metadataScan = new SimpleBuildingScan(scanNumber, msLevel, polarity,
          type, rt, precursorMz, charge);

      if (!processor.scanFilter().matches(metadataScan)) {
        continue;
      }

      final SimpleSpectralArrays processed = processor.processor()
          .processScan(metadataScan, new SimpleSpectralArrays(mz, intensity));

      final MassSpectrumType finalType =
          type == MassSpectrumType.CENTROIDED || processor.isMassDetectActive(msLevel)
              ? MassSpectrumType.CENTROIDED : MassSpectrumType.PROFILE;

      final DDAMsMsInfo msMs;
      if (msLevel >= 2 && precursorMz > 0) {
        // Bridge always emits collisionEnergy (primitive int on SDK side).
        final float ce = (float) hdr.path("collisionEnergy").asDouble(0d);
        msMs = new DDAMsMsInfoImpl(precursorMz, charge > 0 ? charge : null, ce, null, null, msLevel,
            ActivationMethod.UNKNOWN, null);
      } else {
        msMs = null;
      }

      final String scanDefinition = buildScanDefinition(hdr);
      final Scan scan = new SimpleScan(out, scanNumber, msLevel, rt, msMs, processed.mzs(),
          processed.intensities(), finalType, polarity, scanDefinition, null);
      out.addScan(scan);
    }

    if (skippedFailedScans > 0) {
      logger.warning("Shimadzu import: skipped " + skippedFailedScans
          + " scan(s) the SDK could not read (first 5 logged above)");
    }
  }

  /**
   * Build a useful {@code scanDefinition} string for {@link SimpleScan} from the bridge's per-scan
   * header. mzmine uses scan definitions for sorting and display; combining
   * segment/event/event-mode gives something descriptive for multi-segment Shimadzu acquisitions
   * where many scans share the same retention time across different functions.
   */
  private static String buildScanDefinition(JsonNode hdr) {
    final int seg = hdr.path("segmentNo").asInt(-1);
    final int evt = hdr.path("eventNo").asInt(-1);
    final String mode = hdr.path("eventMode").asText("");
    final StringBuilder sb = new StringBuilder(32);
    if (seg >= 0) {
      sb.append("seg=").append(seg);
    }
    if (evt >= 0) {
      if (sb.length() > 0) {
        sb.append(' ');
      }
      sb.append("evt=").append(evt);
    }
    if (!mode.isEmpty()) {
      if (sb.length() > 0) {
        sb.append(' ');
      }
      sb.append("mode=").append(mode);
    }
    return sb.toString();
  }

  private static PolarityType parsePolarity(String s) {
    if (s == null || s.isEmpty()) {
      return PolarityType.UNKNOWN;
    }
    final String u = s.toUpperCase();
    if (u.contains("POSITIVE") || u.equals("POS") || u.equals("+")) {
      return PolarityType.POSITIVE;
    }
    if (u.contains("NEGATIVE") || u.equals("NEG") || u.equals("-")) {
      return PolarityType.NEGATIVE;
    }
    return PolarityType.UNKNOWN;
  }

  @Override
  public @NotNull List<RawDataFile> getImportedRawDataFiles() {
    return isFinished() && dataFile != null ? List.of(dataFile) : List.of();
  }
}
