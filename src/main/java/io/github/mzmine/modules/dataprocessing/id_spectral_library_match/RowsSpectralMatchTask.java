/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.id_spectral_library_match;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.id_ccscalc.CCSUtils;
import io.github.mzmine.modules.dataprocessing.id_spectral_match_sort.SortSpectralMatchesTask;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoper;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoperParameters;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase.SingleSpectrumLibrarySearchParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.PercentTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanAlignment;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import io.github.mzmine.util.scans.sorting.ScanSortMode;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class RowsSpectralMatchTask extends AbstractTask {

  // isotopes to check number of signals matching
  public final static double[] DELTA_ISOTOPES = new double[]{1.0034, 1.0078, 2.0157, 1.9970};

  private static final Logger logger = Logger.getLogger(RowsSpectralMatchTask.class.getName());
  private static final String METHOD = "Spectral library search";
  protected final List<FeatureListRow> rows;
  protected final AtomicInteger finishedRows = new AtomicInteger(0);
  protected final ParameterSet parameters;
  protected final List<SpectralLibrary> libraries;
  protected final String librariesJoined;
  // in some cases this task is only going to run on one scan
  protected final Scan scan;
  protected final AtomicInteger matches = new AtomicInteger(0);
  protected final MZTolerance mzToleranceSpectra;
  protected final MZTolerance mzTolerancePrecursor;
  protected final RTTolerance rtTolerance;
  protected final PercentTolerance ccsTolerance;
  private final AtomicInteger errorCounter = new AtomicInteger(0);
  private final boolean useRT;
  private final int totalRows;
  private final int msLevel;
  private final double noiseLevel;
  private final int minMatch;
  private final boolean removePrecursor;
  private final boolean cropSpectraToOverlap;
  private final String description;
  private final MZmineProcessingStep<SpectralSimilarityFunction> simFunction;
  private final boolean allMS2Scans;
  // remove 13C isotopes
  private final boolean removeIsotopes;
  private final MassListDeisotoperParameters deisotopeParam;
  // needs any signals within mzToleranceSpectra for
  // 13C, H, 2H or Cl
  private final boolean needsIsotopePattern;
  private final int minMatchedIsoSignals;
  // use precursor mz provided by user
  private boolean useScanPrecursorMZ;
  private double scanPrecursorMZ;

  public RowsSpectralMatchTask(ParameterSet parameters, @NotNull Scan scan,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.parameters = parameters;
    this.scan = scan;
    this.rows = null;
    this.libraries = parameters.getValue(SingleSpectrumLibrarySearchParameters.libraries)
        .getMatchingLibraries();
    this.librariesJoined = libraries.stream().map(SpectralLibrary::getName)
        .collect(Collectors.joining(", "));
    this.description = String.format("Spectral library matching for Scan %s in %d libraries: %s",
        scan, libraries.size(), librariesJoined);

    mzToleranceSpectra = parameters.getValue(SingleSpectrumLibrarySearchParameters.mzTolerance);
    msLevel = scan.getMSLevel();
    noiseLevel = parameters.getValue(SingleSpectrumLibrarySearchParameters.noiseLevel);

    useScanPrecursorMZ = parameters.getValue(SingleSpectrumLibrarySearchParameters.usePrecursorMZ);
    scanPrecursorMZ = !useScanPrecursorMZ ? scan.getPrecursorMz()
        : parameters.getParameter(SingleSpectrumLibrarySearchParameters.usePrecursorMZ)
            .getEmbeddedParameter().getValue();

    useRT = false;
    rtTolerance = null;

    minMatch = parameters.getValue(SingleSpectrumLibrarySearchParameters.minMatch);
    simFunction = parameters.getValue(SingleSpectrumLibrarySearchParameters.similarityFunction);
    needsIsotopePattern = parameters.getValue(
        SingleSpectrumLibrarySearchParameters.needsIsotopePattern);
    minMatchedIsoSignals = !needsIsotopePattern ? 0
        : parameters.getParameter(SingleSpectrumLibrarySearchParameters.needsIsotopePattern)
            .getEmbeddedParameter().getValue();
    removeIsotopes = parameters.getValue(SingleSpectrumLibrarySearchParameters.deisotoping);
    deisotopeParam = parameters.getParameter(SingleSpectrumLibrarySearchParameters.deisotoping)
        .getEmbeddedParameters();

    removePrecursor = parameters.getValue(SingleSpectrumLibrarySearchParameters.removePrecursor);

    cropSpectraToOverlap = parameters.getValue(
        SingleSpectrumLibrarySearchParameters.cropSpectraToOverlap);
    mzTolerancePrecursor = msLevel <= 1 ? null
        : parameters.getValue(SingleSpectrumLibrarySearchParameters.mzTolerancePrecursor);
    ccsTolerance = parameters.getValue(SingleSpectrumLibrarySearchParameters.ccsTolerance)
        ? new PercentTolerance(
        parameters.getParameter(SingleSpectrumLibrarySearchParameters.ccsTolerance)
            .getEmbeddedParameter().getValue()) : null;
    allMS2Scans = false;
    totalRows = 1;
  }

  public RowsSpectralMatchTask(ParameterSet parameters, @NotNull List<FeatureListRow> rows,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.parameters = parameters;
    this.rows = rows;
    this.scan = null;
    this.libraries = parameters.getValue(SpectralLibrarySearchParameters.libraries)
        .getMatchingLibraries();
    this.librariesJoined = libraries.stream().map(SpectralLibrary::getName)
        .collect(Collectors.joining(", "));
    this.description = String.format("Spectral library matching for %d rows in %d libraries: %s",
        rows.size(), libraries.size(), librariesJoined);

    mzToleranceSpectra = parameters.getValue(SpectralLibrarySearchParameters.mzTolerance);
    msLevel = parameters.getValue(SpectralLibrarySearchParameters.msLevel);
    noiseLevel = parameters.getValue(SpectralLibrarySearchParameters.noiseLevel);

    useRT = parameters.getValue(SpectralLibrarySearchParameters.rtTolerance);
    rtTolerance = parameters.getParameter(SpectralLibrarySearchParameters.rtTolerance)
        .getEmbeddedParameter().getValue();

    minMatch = parameters.getValue(SpectralLibrarySearchParameters.minMatch);
    simFunction = parameters.getValue(SpectralLibrarySearchParameters.similarityFunction);
    needsIsotopePattern = parameters.getValue(SpectralLibrarySearchParameters.needsIsotopePattern);
    minMatchedIsoSignals = !needsIsotopePattern ? 0
        : parameters.getParameter(SpectralLibrarySearchParameters.needsIsotopePattern)
            .getEmbeddedParameter().getValue();
    removeIsotopes = parameters.getValue(SpectralLibrarySearchParameters.deisotoping);
    deisotopeParam = parameters.getParameter(SpectralLibrarySearchParameters.deisotoping)
        .getEmbeddedParameters();

    removePrecursor = parameters.getValue(SpectralLibrarySearchParameters.removePrecursor);

    cropSpectraToOverlap = parameters.getValue(
        SpectralLibrarySearchParameters.cropSpectraToOverlap);
    if (msLevel > 1) {
      mzTolerancePrecursor = parameters.getValue(
          SpectralLibrarySearchParameters.mzTolerancePrecursor);
    } else {
      mzTolerancePrecursor = null;
    }

    allMS2Scans = parameters.getValue(SpectralLibrarySearchParameters.allMS2Spectra);

    ccsTolerance = parameters.getValue(SingleSpectrumLibrarySearchParameters.ccsTolerance)
        ? new PercentTolerance(
        parameters.getParameter(SingleSpectrumLibrarySearchParameters.ccsTolerance)
            .getEmbeddedParameter().getValue()) : null;

    totalRows = rows.size();
  }

  /**
   * Checks for isotope pattern in matched signals within mzToleranceSpectra
   *
   * @param sim
   * @return
   */
  public static boolean checkForIsotopePattern(SpectralSimilarity sim,
      MZTolerance mzToleranceSpectra, int minMatchedIsoSignals) {
    // use mzToleranceSpectra
    DataPoint[][] aligned = sim.getAlignedDataPoints();
    aligned = ScanAlignment.removeUnaligned(aligned);

    // find something in range of:
    // 13C 1.0034
    // H ( for M+ and M+H or -H -H2)
    // 2H 1.0078 2.0157
    // Cl 1.9970
    // just check one

    int matches = 0;
    DataPoint[] lib = aligned[0];
    for (int i = 0; i < lib.length - 1; i++) {
      double a = lib[i].getMZ();
      // each lib[i] can only have one match to each isotope dist
      for (double dIso : DELTA_ISOTOPES) {
        boolean matchedIso = false;
        for (int k = i + 1; k < lib.length && !matchedIso; k++) {
          double dmz = Math.abs(a - lib[k].getMZ());
          // any match?
          if (mzToleranceSpectra.checkWithinTolerance(dIso, dmz)) {
            matchedIso = true;
            matches++;
            if (matches >= minMatchedIsoSignals) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : finishedRows.get() / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public void run() {

    // combine libraries
    List<SpectralDBEntry> entries = new ArrayList<>();
    for (var lib : libraries) {
      entries.addAll(lib.getEntries());
    }

    // run on spectra
    if (scan != null) {
      logger.info(
          () -> String.format("Comparing %d library spectra to scan: %s", entries.size(), scan));

      matchScan(entries, scan);

      logger.info(
          () -> String.format("library matches=%d (Errors:%d); library entries=%d; for scan: %s",
              getCount(), getErrorCount(), entries.size(), scan));
    }

    // run in parallel
    if (rows != null) {
      logger.info(() -> String.format("Comparing %d library spectra to %d feature list rows",
          entries.size(), totalRows));
      rows.stream().parallel().forEach(row -> {
        if (!isCanceled()) {
          matchRowToLibraries(entries, row);
          finishedRows.incrementAndGet();
        }
      });

      logger.info(() -> String.format("library matches=%d (Errors:%d); rows=%d; library entries=%d",
          getCount(), getErrorCount(), totalRows, entries.size()));
    }

  }

  /**
   * Match row against all entries, add matches, sort them by score
   *
   * @param entries combined library entries
   * @param scan    target scan
   */
  public void matchScan(List<SpectralDBEntry> entries, Scan scan) {
    try {
      // get mass list and perform deisotoping if active
      DataPoint[] masses = getDataPoints(scan, true);

      // get a ccs for the precursor of this scan
      final Float precursorCCS = getPrecursorCCSFromMsMs(scan);

      for (var entry : entries) {
        final SpectralSimilarity sim = matchSpectrum(scan.getRetentionTime(), scanPrecursorMZ,
            precursorCCS, masses, entry);
        if (sim != null) {
          Float ccsError = PercentTolerance.getPercentError(entry.getOrElse(DBEntryField.CCS, null),
              precursorCCS);

          matches.incrementAndGet();
          addIdentities(null, List.of(new SpectralDBAnnotation(entry, sim, scan, ccsError)));
        }
      }
    } catch (MissingMassListException e) {
      logger.log(Level.WARNING, "No mass list in spectrum:" + scan.toString(), e);
    }
  }

  private Float getPrecursorCCSFromMsMs(Scan scan) {
    if (ccsTolerance == null) {
      return null;
    }

    Float precursorCCS = null;
    if (scan instanceof MobilityScan mobScan
        && mobScan.getMsMsInfo() instanceof DDAMsMsInfo ddaInfo) {
      if (ddaInfo.getPrecursorCharge() != null && (/*
          mobScan.getDataFile().getCCSCalibration() != null // enable after ccs calibration pr is merged
              ||*/ ((IMSRawDataFile) mobScan.getDataFile()).getMobilityType()
                   == MobilityType.TIMS)) {
        precursorCCS = CCSUtils.calcCCS(ddaInfo.getIsolationMz(), (float) mobScan.getMobility(),
            MobilityType.TIMS, ddaInfo.getPrecursorCharge(), (IMSRawDataFile) mobScan.getDataFile());
      }
    } else if (scan instanceof MergedMsMsSpectrum merged
               && merged.getMsMsInfo() instanceof DDAMsMsInfo ddaInfo) {
      MobilityScan mobScan = (MobilityScan) merged.getSourceSpectra().stream()
          .filter(MobilityScan.class::isInstance).max(Comparator.comparingDouble(
              s -> Objects.requireNonNullElse(((MobilityScan) s).getMobility(), 0d))).orElse(null);

      if (ddaInfo.getPrecursorCharge() != null && mobScan != null && (/*
          mobScan.getDataFile().getCCSCalibration() != null // enable after ccs calibration pr is merged
              ||*/ ((IMSRawDataFile) mobScan.getDataFile()).getMobilityType()
                   == MobilityType.TIMS)) {
        precursorCCS = CCSUtils.calcCCS(ddaInfo.getIsolationMz(), (float) mobScan.getMobility(),
            MobilityType.TIMS, ddaInfo.getPrecursorCharge(), (IMSRawDataFile) mobScan.getDataFile());
      }
    }
    return precursorCCS;
  }

  /**
   * Match row against all entries, add matches, sort them by score
   *
   * @param entries combined library entries
   * @param row     target row
   */
  public void matchRowToLibraries(List<SpectralDBEntry> entries, FeatureListRow row) {
    try {
      // All MS2 or only best MS2 scan
      // best MS1 scan
      // check for MS1 or MSMS scan
      List<Scan> scans = getScans(row);
      List<DataPoint[]> rowMassLists = new ArrayList<>();
      for (Scan scan : scans) {
        // get mass list and perform deisotoping if active
        DataPoint[] rowMassList = getDataPoints(scan, true);
        rowMassLists.add(rowMassList);
      }

      final Float rowCCS = row.getAverageCCS();
      List<SpectralDBAnnotation> ids = null;
      // match against all library entries
      for (SpectralDBEntry ident : entries) {
        final Float libCCS = ident.getOrElse(DBEntryField.CCS, null);
        SpectralDBAnnotation best = null;
        // match all scans against this ident to find best match
        for (int i = 0; i < scans.size(); i++) {
          SpectralSimilarity sim = matchSpectrum(row.getAverageRT(), row.getAverageMZ(), rowCCS,
              rowMassLists.get(i), ident);
          if (sim != null && (!needsIsotopePattern || checkForIsotopePattern(sim,
              mzToleranceSpectra, minMatchedIsoSignals)) && (best == null
                                                             || best.getSimilarity().getScore()
                                                                < sim.getScore())) {

            Float ccsRelativeError = PercentTolerance.getPercentError(rowCCS, libCCS);

            best = new SpectralDBAnnotation(ident, sim, scans.get(i), ccsRelativeError);
          }
        }
        // has match?
        if (best != null) {
          if (ids == null) {
            ids = new ArrayList<>();
          }
          ids.add(best);
          matches.getAndIncrement();
        }
      }

      // add and sort identities based on similarity score
      if (ids != null) {
        addIdentities(row, ids);
        SortSpectralMatchesTask.sortIdentities(row);
      }
    } catch (MissingMassListException e) {
      logger.log(Level.WARNING, "No mass list in spectrum for rowID=" + row.getID(), e);
      errorCounter.getAndIncrement();
    }
  }

  /**
   * Remove 13C isotopes from masslist
   */
  private DataPoint[] removeIsotopes(DataPoint[] a) {
    return MassListDeisotoper.filterIsotopes(a, deisotopeParam);
  }

  /**
   * match row against library entry
   *
   * @param rowRT       retention time of query row
   * @param rowMZ       m/z of query row
   * @param rowMassList mass list (data points) for row
   * @param ident       library entry
   * @return spectral similarity or null if no match
   */
  private SpectralSimilarity matchSpectrum(Float rowRT, double rowMZ, Float rowCCS,
      DataPoint[] rowMassList, SpectralDBEntry ident) {
    // retention time
    // MS level 1 or check precursorMZ
    if (checkRT(rowRT, ident) && (msLevel == 1 || checkPrecursorMZ(rowMZ, ident)) && checkCCS(
        rowCCS, ident)) {
      DataPoint[] library = ident.getDataPoints();
      if (removeIsotopes) {
        library = removeIsotopes(library);
      }

      // crop the spectra to their overlapping mz range
      // helpful when comparing spectra, acquired with different
      // fragmentation energy
      DataPoint[] query = rowMassList;
      if (cropSpectraToOverlap) {
        DataPoint[][] cropped = ScanAlignment.cropToOverlap(mzToleranceSpectra, library, query);
        library = cropped[0];
        query = cropped[1];
      }

      // remove precursor signals
      if (msLevel > 1 && removePrecursor && ident.getPrecursorMZ() != null) {
        // precursor mz from library entry for signal filtering
        double precursorMZ = ident.getPrecursorMZ();
        // remove from both spectra
        library = removePrecursor(library, precursorMZ);
        query = removePrecursor(query, precursorMZ);
      }

      // check spectra similarity
      return createSimilarity(library, query);
    }
    return null;
  }

  private boolean checkCCS(Float rowCCS, SpectralDBEntry ident) {
    return ccsTolerance == null || ccsTolerance.matches(rowCCS,
        ident.getOrElse(DBEntryField.CCS, null));
  }


  private DataPoint[] removePrecursor(DataPoint[] masslist, double precursorMZ) {
    List<DataPoint> filtered = new ArrayList<>();
    for (DataPoint dp : masslist) {
      double mz = dp.getMZ();
      // skip precursor mz
      if (!mzTolerancePrecursor.checkWithinTolerance(mz, precursorMZ)) {
        filtered.add(dp);
      }
    }
    return filtered.toArray(new DataPoint[0]);
  }

  /**
   * Uses the similarity function and filter to create similarity.
   *
   * @return positive match with similarity or null if criteria was not met
   */
  private SpectralSimilarity createSimilarity(DataPoint[] library, DataPoint[] query) {
    return simFunction.getModule()
        .getSimilarity(simFunction.getParameterSet(), mzToleranceSpectra, minMatch, library, query);
  }

  private boolean checkPrecursorMZ(double rowMZ, SpectralDBEntry ident) {
    if (ident.getPrecursorMZ() == null) {
      return false;
    } else {
      return mzTolerancePrecursor.checkWithinTolerance(ident.getPrecursorMZ(), rowMZ);
    }
  }

  private boolean checkRT(Float retentionTime, SpectralDBEntry ident) {
    if (!useRT || retentionTime == null) {
      return true;
    }
    Float rt = (Float) ident.getField(DBEntryField.RT).orElse(null);
    return (rt == null || rtTolerance.checkWithinTolerance(rt, retentionTime));
  }

  /**
   * Thresholded masslist
   *
   * @return the mass list data points from scan
   * @throws MissingMassListException if no mass list available
   */
  protected DataPoint[] getDataPoints(Scan scan, boolean noiseFilter)
      throws MissingMassListException {
    if (scan == null || scan.getMassList() == null) {
      return new DataPoint[0];
    }

    MassList masses = scan.getMassList();
    DataPoint[] dps = masses.getDataPoints();
    if (noiseFilter) {
      dps = ScanUtils.getFiltered(dps, noiseLevel);
    }
    if (removeIsotopes) {
      dps = removeIsotopes(dps);
    }
    return dps;
  }

  public List<Scan> getScans(FeatureListRow row) throws MissingMassListException {
    if (msLevel == 1) {
      List<Scan> scans = new ArrayList<>();
      scans.add(row.getBestFeature().getRepresentativeScan());
      return scans;
    } else {
      // first entry is the best scan
      List<Scan> scans = ScanUtils.listAllFragmentScans(row, noiseLevel, minMatch,
          ScanSortMode.MAX_TIC);
      if (allMS2Scans) {
        return scans;
      } else {
        // only keep first (with highest TIC)
        while (scans.size() > 1) {
          scans.remove(1);
        }
        return scans;
      }
    }
  }

  protected void addIdentities(FeatureListRow row, List<SpectralDBAnnotation> matches) {
    // add new identity to the row
    if (row != null) {
      row.addSpectralLibraryMatches(matches);
    }
  }

  public int getCount() {
    return matches.get();
  }

  public int getErrorCount() {
    return errorCounter.get();
  }
}
