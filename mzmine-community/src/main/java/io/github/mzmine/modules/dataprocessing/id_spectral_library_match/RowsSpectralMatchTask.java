/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_spectral_library_match;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.PseudoSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.modules.dataprocessing.id_ccscalc.CCSUtils;
import io.github.mzmine.modules.dataprocessing.id_spectral_match_sort.SortSpectralMatchesTask;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoper;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoperParameters;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase.SingleSpectrumLibrarySearchParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelectionException;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.PercentTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.ScanAlignment;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunctions;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RowsSpectralMatchTask extends AbstractTask {

  // isotopes to check number of signals matching
  public final static double[] DELTA_ISOTOPES = new double[]{1.0034, 1.0078, 2.0157, 1.9970};

  private static final Logger logger = Logger.getLogger(RowsSpectralMatchTask.class.getName());
  protected final List<FeatureListRow> rows;
  protected final AtomicInteger finishedRows = new AtomicInteger(0);
  protected final ParameterSet parameters;
  protected String librariesJoined = "";
  // remove +- 4 Da around the precursor - including the precursor signal
  // this signal does not matter for matching
  protected final MZTolerance mzToleranceRemovePrecursor = new MZTolerance(4d, 0d);
  // in some cases this task is only going to run on one scan
  protected final Scan scan;
  protected final AtomicInteger matches = new AtomicInteger(0);
  protected final MZTolerance mzToleranceSpectra;
  protected final MZTolerance mzTolerancePrecursor;
  // scan merging and ms levels
  private final MsLevelFilter msLevelFilter;
  private final AtomicInteger errorCounter = new AtomicInteger(0);
  private final int totalRows;
  private final int minMatch;
  private final boolean removePrecursor;
  private String description = "Spectral library search";
  private final SpectralSimilarityFunction simFunction;
  private final FragmentScanSelection fragmentScanSelection;
  protected RTTolerance rtTolerance;
  protected PercentTolerance ccsTolerance;
  private boolean useRT;
  private boolean cropSpectraToOverlap;
  // remove 13C isotopes
  private boolean removeIsotopes;
  private MassListDeisotoperParameters deisotopeParam;
  // needs any signals within mzToleranceSpectra for
  // 13C, H, 2H or Cl
  private boolean needsIsotopePattern;
  private int minMatchedIsoSignals;
  private double scanPrecursorMZ;

  public RowsSpectralMatchTask(ParameterSet parameters, @NotNull Scan scan,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.parameters = parameters;
    this.scan = scan;
    this.rows = null;

    mzToleranceSpectra = parameters.getValue(SpectralLibrarySearchParameters.mzTolerance);

    msLevelFilter = MsLevelFilter.of(scan.getMSLevel());

    // use precursor mz provided by user
    scanPrecursorMZ = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        SingleSpectrumLibrarySearchParameters.usePrecursorMZ, scan.getPrecursorMz());

    useRT = false;
    rtTolerance = null;

    minMatch = parameters.getValue(SpectralLibrarySearchParameters.minMatch);
    var simfuncParams = parameters.getParameter(SpectralLibrarySearchParameters.similarityFunction)
        .getValueWithParameters();
    simFunction = SpectralSimilarityFunctions.createOption(simfuncParams);
    removePrecursor = parameters.getValue(SpectralLibrarySearchParameters.removePrecursor);
    mzTolerancePrecursor = scan.getMSLevel() <= 1 ? null
        : parameters.getValue(SpectralLibrarySearchParameters.mzTolerancePrecursor);

    var useAdvanced = parameters.getValue(SpectralLibrarySearchParameters.advanced);
    if (useAdvanced) {
      AdvancedSpectralLibrarySearchParameters advanced = parameters.getParameter(
          SpectralLibrarySearchParameters.advanced).getEmbeddedParameters();

      needsIsotopePattern = advanced.getValue(
          AdvancedSpectralLibrarySearchParameters.needsIsotopePattern);
      minMatchedIsoSignals = !needsIsotopePattern ? 0
          : advanced.getParameter(AdvancedSpectralLibrarySearchParameters.needsIsotopePattern)
              .getEmbeddedParameter().getValue();
      removeIsotopes = advanced.getValue(AdvancedSpectralLibrarySearchParameters.deisotoping);
      deisotopeParam = advanced.getParameter(AdvancedSpectralLibrarySearchParameters.deisotoping)
          .getEmbeddedParameters();
      cropSpectraToOverlap = advanced.getValue(
          AdvancedSpectralLibrarySearchParameters.cropSpectraToOverlap);

      ccsTolerance = advanced.getValue(AdvancedSpectralLibrarySearchParameters.ccsTolerance)
          ? new PercentTolerance(
          advanced.getParameter(AdvancedSpectralLibrarySearchParameters.ccsTolerance)
              .getEmbeddedParameter().getValue()) : null;
    }

    // parameter for scan selection and merging
    var mergeSelect = parameters.getParameter(SpectralLibrarySearchParameters.spectraMergeSelect)
        .getValueWithParameters();

    fragmentScanSelection = mergeSelect.value()
        .createFragmentScanSelection(getMemoryMapStorage(), mergeSelect.parameters());

    totalRows = 1;
  }

  public RowsSpectralMatchTask(ParameterSet parameters, @NotNull List<FeatureListRow> rows,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.parameters = parameters;
    this.rows = rows;
    this.scan = null;

    mzToleranceSpectra = parameters.getValue(SpectralLibrarySearchParameters.mzTolerance);
    minMatch = parameters.getValue(SpectralLibrarySearchParameters.minMatch);
    var simfuncParams = parameters.getParameter(SpectralLibrarySearchParameters.similarityFunction)
        .getValueWithParameters();
    simFunction = SpectralSimilarityFunctions.createOption(simfuncParams);
    removePrecursor = parameters.getValue(SpectralLibrarySearchParameters.removePrecursor);

    // parameter for scan selection and merging
    var mergeSelect = parameters.getParameter(SpectralLibrarySearchParameters.spectraMergeSelect)
        .getValueWithParameters();

    fragmentScanSelection = mergeSelect.value()
        .createFragmentScanSelection(getMemoryMapStorage(), mergeSelect.parameters());

    msLevelFilter = parameters.getValue(SpectralLibrarySearchParameters.msLevelFilter);

    mzTolerancePrecursor = parameters.getValue(
        SpectralLibrarySearchParameters.mzTolerancePrecursor);

    var useAdvanced = parameters.getValue(SpectralLibrarySearchParameters.advanced);
    if (useAdvanced) {
      AdvancedSpectralLibrarySearchParameters advanced = parameters.getParameter(
          SpectralLibrarySearchParameters.advanced).getEmbeddedParameters();
      useRT = advanced.getValue(AdvancedSpectralLibrarySearchParameters.rtTolerance);
      rtTolerance = advanced.getParameter(AdvancedSpectralLibrarySearchParameters.rtTolerance)
          .getEmbeddedParameter().getValue();

      needsIsotopePattern = advanced.getValue(
          AdvancedSpectralLibrarySearchParameters.needsIsotopePattern);
      minMatchedIsoSignals = !needsIsotopePattern ? 0
          : advanced.getParameter(AdvancedSpectralLibrarySearchParameters.needsIsotopePattern)
              .getEmbeddedParameter().getValue();
      removeIsotopes = advanced.getValue(AdvancedSpectralLibrarySearchParameters.deisotoping);
      deisotopeParam = advanced.getParameter(AdvancedSpectralLibrarySearchParameters.deisotoping)
          .getEmbeddedParameters();
      cropSpectraToOverlap = advanced.getValue(
          AdvancedSpectralLibrarySearchParameters.cropSpectraToOverlap);

      ccsTolerance = advanced.getValue(AdvancedSpectralLibrarySearchParameters.ccsTolerance)
          ? new PercentTolerance(
          advanced.getParameter(AdvancedSpectralLibrarySearchParameters.ccsTolerance)
              .getEmbeddedParameter().getValue()) : null;
    }

    totalRows = rows.size();
  }

  /**
   * Checks for isotope pattern in matched signals within mzToleranceSpectra
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
    final List<SpectralLibraryEntry> entries;
    try {
      entries = getSortedSpectralLibraryEntries();
      if (isCanceled()) {
        return;
      }
    } catch (SpectralLibrarySelectionException e) {
      error("Error in spectral library search.", e);
      return;
    }

    // run on spectra
    if (scan != null) {
      description = """
          Spectral library matching of a selected scan against %d spectral library entries""".formatted(
          entries.size());
      logger.info(
          () -> String.format("Comparing %d library spectra to scan: %s", entries.size(), scan));

      matchScan(entries, scan);

      logger.info(
          () -> String.format("library matches=%d (Errors:%d); library entries=%d; for scan: %s",
              getCount(), getErrorCount(), entries.size(), scan));
    }

    // run in parallel
    if (rows != null) {
      description = """
          Spectral library matching of %d feature rows against %d spectral library entries""".formatted(
          rows.size(), entries.size());

      logger.info(() -> String.format("Comparing %d library spectra to %d feature list rows",
          entries.size(), totalRows));
      // cannot use parallel.forEach with side effects - this thread will continue without waiting for
      // stream to finish
      var totalMatches = rows.stream().filter(FeatureListRow::hasMs2Fragmentation).parallel()
          .mapToInt(row -> {
            if (!isCanceled()) {
              int matches = matchRowToLibraries(entries, row);
              finishedRows.incrementAndGet();
              return matches;
            }
            return 0;
          }).sum();
      logger.info("Total spectral library matches " + totalMatches);
      logger.info(() -> String.format("library matches=%d (Errors:%d); rows=%d; library entries=%d",
          getCount(), getErrorCount(), totalRows, entries.size()));
    }
  }

  private @NotNull List<SpectralLibraryEntry> getSortedSpectralLibraryEntries() {
    final List<SpectralLibraryEntry> entries = parameters.getValue(
        SpectralLibrarySearchParameters.libraries).getMatchingLibraryEntriesAndCheckAvailability();
    var stream = entries.stream().filter(entry -> entry.getNumberOfDataPoints() >= minMatch);

    if (msLevelFilter.isFragmentationNoMS1()) {
      // remove scans without precursor mz if its MS2
      // sort by mz for binary search
      stream = stream.filter(entry -> entry.getPrecursorMZ() != null)
          .sorted(Comparator.comparing(SpectralLibraryEntry::getPrecursorMZ));
    }
    return stream.toList();
  }

  /**
   * Match row against all entries, add matches, sort them by score
   *
   * @param entries combined library entries
   * @param scan    target scan
   */
  public void matchScan(List<SpectralLibraryEntry> entries, Scan scan) {
    try {
      // filter entries first if MS2
      // MS1 is never filtered because of missing precursor mz
      entries = binaryFindCandidateEntries(entries, ScanUtils.getPrecursorMz(scan));
      if (entries.isEmpty()) {
        return;
      }

      // get mass list and perform deisotoping if active
      DataPoint[] masses = getDataPoints(scan, scan.getPrecursorMz());

      // get a ccs for the precursor of this scan
      final Float precursorCCS = getPrecursorCCSFromMsMs(scan);

      for (var entry : entries) {
        float rt = scan.getRetentionTime();
        final SpectralSimilarity sim = matchSpectrum(rt, scanPrecursorMZ, precursorCCS, masses,
            entry);
        if (sim != null) {
          Float ccsError = PercentTolerance.getPercentError(entry.getOrElse(DBEntryField.CCS, null),
              precursorCCS);

          matches.incrementAndGet();
          addIdentities(null,
              List.of(new SpectralDBAnnotation(entry, sim, scan, ccsError, scanPrecursorMZ, rt)));
        }
      }
    } catch (MissingMassListException e) {
      logger.log(Level.WARNING, "No mass list in spectrum:" + scan, e);
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
            MobilityType.TIMS, ddaInfo.getPrecursorCharge(),
            (IMSRawDataFile) mobScan.getDataFile());
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
            MobilityType.TIMS, ddaInfo.getPrecursorCharge(),
            (IMSRawDataFile) mobScan.getDataFile());
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
  public int matchRowToLibraries(List<SpectralLibraryEntry> entries, FeatureListRow row) {
    try {
      // filter entries first if not MS1
      entries = binaryFindCandidateEntries(entries, row.getAverageMZ());
      if (entries.isEmpty()) {
        return 0;
      }

      // All MS2 or only best MS2 scan
      // best MS1 scan
      // check for MS1 or MSMS scan
      List<Scan> scans = getScans(row);
      if (scans.isEmpty()) {
        return 0;
      }

      // prepare data
      List<DataPoint[]> rowMassLists = new ArrayList<>();
      for (Scan scan : scans) {
        // get mass list and perform deisotoping if active
        DataPoint[] rowMassList = getDataPoints(scan, row.getAverageMZ());
        rowMassLists.add(rowMassList);
      }

      final Float rowCCS = row.getAverageCCS();
      List<SpectralDBAnnotation> ids = null;
      // match against all library entries
      for (SpectralLibraryEntry ident : entries) {
        if (isCanceled()) {
          return 0;
        }

        final String entryPolarity = ident.getOrElse(DBEntryField.POLARITY, null);

        final Float libCCS = ident.getOrElse(DBEntryField.CCS, null);
        SpectralDBAnnotation best = null;
        // match all scans against this ident to find best match
        for (int i = 0; i < scans.size(); i++) {
          final PolarityType scanPolarity = scans.get(i).getPolarity();
          if (!weakPolarityCheck(entryPolarity, scanPolarity)) {
            // check each ms2 scan individually, maybe we have grouped pos/neg rows in the future.
            continue;
          }

          SpectralSimilarity sim = matchSpectrum(row.getAverageRT(), row.getAverageMZ(), rowCCS,
              rowMassLists.get(i), ident);
          if (sim != null && (!needsIsotopePattern || checkForIsotopePattern(sim,
              mzToleranceSpectra, minMatchedIsoSignals)) && (best == null
                                                             || best.getSimilarity().getScore()
                                                                < sim.getScore())) {

            Float ccsRelativeError = PercentTolerance.getPercentError(rowCCS, libCCS);

            best = new SpectralDBAnnotation(ident, sim, scans.get(i), ccsRelativeError,
                row.getAverageMZ(), row.getAverageRT());
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
        return ids.size();

      }
    } catch (MissingMassListException e) {
      logger.log(Level.WARNING, "No mass list in spectrum for rowID=" + row.getID(), e);
      errorCounter.getAndIncrement();
    }
    return 0;
  }

  /**
   * Does no filtering in MS1 but for all other filters by precursor mz using binary search.
   *
   * @param entries         original entries will not be changed
   * @param scanPrecursorMZ if null returns the initial entries - otherwise used as filter if not
   *                        MS1
   * @return either filtered sublist or the original list if no filters applicable
   */
  private List<SpectralLibraryEntry> binaryFindCandidateEntries(List<SpectralLibraryEntry> entries,
      @Nullable final Double scanPrecursorMZ) {
    if (scanPrecursorMZ == null || msLevelFilter.isMs1Only()) {
      return entries;
    }
    var indexRange = BinarySearch.indexRange(
        mzTolerancePrecursor.getToleranceRange(scanPrecursorMZ), entries,
        SpectralLibraryEntry::getPrecursorMZ);
    // filter
    entries = indexRange.sublist(entries);
    return entries;
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
      DataPoint[] rowMassList, SpectralLibraryEntry ident) {
    // prefilters
    if (!checkRT(rowRT, ident) // retention time optional
        // mz only for MS2 not for MS1
        || (msLevelFilter.isFragmentationNoMS1() && !checkPrecursorMZ(rowMZ, ident))
        // CCS/ion mobility optional
        || !checkCCS(rowCCS, ident)) {
      return null;
    }
    // prefilters matched - score spectrum
    DataPoint[] library = ident.getDataPoints();
    if (removeIsotopes) {
      library = removeIsotopes(library);
    }

    // crop the spectra to their overlapping mz range
    // helpful when comparing spectra, acquired with different
    // fragmentation energy
    DataPoint[] query = rowMassList;
    if (cropSpectraToOverlap) {
      DataPoint[][] cropped = ScanAlignment.cropToOverlap(mzToleranceSpectra, library, query,
          ident.getPrecursorMZ(), rowMZ);
      library = cropped[0];
      query = cropped[1];
    }

    // remove precursor signals
    if (!msLevelFilter.isMs1Only() && removePrecursor && ident.getPrecursorMZ() != null) {
      // precursor mz from library entry for signal filtering
      double precursorMZ = ident.getPrecursorMZ();
      // remove from both spectra
      library = removePrecursor(library, precursorMZ);
    }

    // check spectra similarity
    return createSimilarity(library, query);
  }

  private boolean checkCCS(Float rowCCS, SpectralLibraryEntry ident) {
    return ccsTolerance == null || ccsTolerance.matches(rowCCS,
        ident.getOrElse(DBEntryField.CCS, null));
  }


  /**
   * Remove precursor +- mzToleranceRemovePrecursor
   */
  private DataPoint[] removePrecursor(DataPoint[] masslist, double precursorMZ) {
    var removeMzRange = mzToleranceRemovePrecursor.getToleranceRange(precursorMZ);
    List<DataPoint> filtered = new ArrayList<>();
    for (DataPoint dp : masslist) {
      double mz = dp.getMZ();
      // skip precursor mz +- 4
      if (!removeMzRange.contains(mz)) {
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
    return simFunction.getSimilarity(mzToleranceSpectra, minMatch, library, query);
  }

  private boolean checkPrecursorMZ(double rowMZ, SpectralLibraryEntry ident) {
    if (ident.getPrecursorMZ() == null) {
      return false;
    } else {
      return mzTolerancePrecursor.checkWithinTolerance(ident.getPrecursorMZ(), rowMZ);
    }
  }

  private boolean checkRT(Float retentionTime, SpectralLibraryEntry ident) {
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
  protected DataPoint[] getDataPoints(Scan scan, Double precursorMz)
      throws MissingMassListException {
    if (scan == null || scan.getMassList() == null) {
      return new DataPoint[0];
    }

    MassList masses = scan.getMassList();
    DataPoint[] dps = masses.getDataPoints();
    if (removeIsotopes) {
      dps = removeIsotopes(dps);
    }
    // remove precursor signals
    if (!msLevelFilter.isMs1Only() && removePrecursor && precursorMz != null) {
      // remove from both spectra
      dps = removePrecursor(dps, precursorMz);
    }
    return dps;
  }

  public List<Scan> getScans(FeatureListRow row) throws MissingMassListException {
    var allFragmentScans = fragmentScanSelection.getAllFragmentSpectra(row);
    if (msLevelFilter.isMs1Only()) {
      List<Scan> pseudoSpectra = allFragmentScans.stream()
          .filter(scan -> scan instanceof PseudoSpectrum).toList();
      if (!pseudoSpectra.isEmpty()) {
        return pseudoSpectra;
      } else {
        var scan = row.getBestFeature().getRepresentativeScan();
        return scan == null ? List.of() : List.of(scan);
      }
    } else {
      // merge spectra by energy and total - or just use all scans
      // depending on selected option
      return allFragmentScans.stream().filter(scan -> scan.getNumberOfDataPoints() >= minMatch)
          .toList();
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

  /**
   * Weak polarity check. If in doubt (e.g. either the entryPolarityString or scanPolarity is null
   * or unknown) this returns true.
   *
   * @param entryPolarityString the scanPolarity string
   * @param scanPolarity        The spectrum scanPolarity
   * @return false if both polarities are defined and do not match, true otherwise.
   */
  public boolean weakPolarityCheck(String entryPolarityString, PolarityType scanPolarity) {
    if (scanPolarity == null || scanPolarity == PolarityType.UNKNOWN) {
      return true;
    }
    final PolarityType entryPolarity = PolarityType.parseFromString(entryPolarityString);
    if (entryPolarity != PolarityType.UNKNOWN) {
      return entryPolarity == scanPolarity;
    }
    return true;
  }
}
