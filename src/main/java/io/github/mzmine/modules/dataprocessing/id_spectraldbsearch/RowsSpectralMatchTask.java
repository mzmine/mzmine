/*
 *
 *  * Copyright 2006-2021 The MZmine Development Team
 *  *
 *  * This file is part of MZmine.
 *  *
 *  * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  * General Public License as published by the Free Software Foundation; either version 2 of the
 *  * License, or (at your option) any later version.
 *  *
 *  * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *  *
 *
 */

package io.github.mzmine.modules.dataprocessing.id_spectraldbsearch;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.id_spectraldbsearch.sort.SortSpectralMatchesTask;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoper;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoperParameters;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase.SpectralMatchTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanAlignment;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import io.github.mzmine.util.scans.sorting.ScanSortMode;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class RowsSpectralMatchTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(RowsSpectralMatchTask.class.getName());
  private static final String METHOD = "Spectral library search";
  protected final List<FeatureListRow> rows;
  protected final AtomicInteger finishedRows = new AtomicInteger(0);
  protected final ParameterSet parameters;
  protected final List<SpectralLibrary> libraries;
  private final AtomicInteger errorCounter = new AtomicInteger(0);
  private final AtomicInteger matches = new AtomicInteger(0);
  private final MZTolerance mzToleranceSpectra;
  private final MZTolerance mzTolerancePrecursor;
  private final RTTolerance rtTolerance;
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

  public RowsSpectralMatchTask(ParameterSet parameters, @NotNull List<FeatureListRow> rows,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.parameters = parameters;
    this.rows = rows;
    this.libraries = parameters.getValue(SpectralLibrarySearchParameters.libraries);
    this.description = String
        .format("Spectral library matching for %d rows in %d libraries: %s", rows.size(),
            libraries.size(), libraries.stream().map(SpectralLibrary::getName).collect(
                Collectors.joining(", ")));

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
    removeIsotopes =
        parameters.getValue(SpectralLibrarySearchParameters.deisotoping);
    deisotopeParam = parameters.getParameter(SpectralLibrarySearchParameters.deisotoping)
        .getEmbeddedParameters();

    removePrecursor =
        parameters.getValue(SpectralLibrarySearchParameters.removePrecursor);

    cropSpectraToOverlap =
        parameters.getValue(SpectralLibrarySearchParameters.cropSpectraToOverlap);
    if (msLevel > 1) {
      mzTolerancePrecursor =
          parameters.getValue(SpectralLibrarySearchParameters.mzTolerancePrecursor);
    } else {
      mzTolerancePrecursor = null;
    }

    allMS2Scans = parameters.getValue(SpectralLibrarySearchParameters.allMS2Spectra);

    totalRows = rows.size();
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

    logger.info(() -> String
        .format("Comparing %d library spectra to %d feature list rows", entries.size(),
            rows.size()));

    // run in parallel
    rows.stream().parallel().forEach(row -> {
      if (!isCanceled()) {
        matchRowToLibraries(entries, row);
        finishedRows.incrementAndGet();
      }
    });

    logger.info(() -> String.format("library matches=%d (Errors:%d); rows=%d; library entries=%d",
        getCount(), getErrorCount(), rows.size(), entries.size()));

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
        if (removeIsotopes) {
          rowMassList = removeIsotopes(rowMassList);
        }
        rowMassLists.add(rowMassList);
      }

      List<SpectralDBFeatureIdentity> ids = null;
      // match against all library entries
      for (SpectralDBEntry ident : entries) {
        SpectralDBFeatureIdentity best = null;
        // match all scans against this ident to find best match
        for (int i = 0; i < scans.size(); i++) {
          SpectralSimilarity sim = spectraDBMatch(row, rowMassLists.get(i), ident);
          if (sim != null
              && (!needsIsotopePattern || SpectralMatchTask.checkForIsotopePattern(sim,
              mzToleranceSpectra, minMatchedIsoSignals))
              && (best == null || best.getSimilarity().getScore() < sim.getScore())) {
            best = new SpectralDBFeatureIdentity(scans.get(i), ident, sim, METHOD);
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
      addIdentities(row, ids);
      SortSpectralMatchesTask.sortIdentities(row);
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
   * @param row         target row
   * @param rowMassList mass list (data points) for row
   * @param ident       library entry
   * @return spectral similarity or null if no match
   */
  private SpectralSimilarity spectraDBMatch(FeatureListRow row, DataPoint[] rowMassList,
      SpectralDBEntry ident) {
    // retention time
    // MS level 1 or check precursorMZ
    if (checkRT(row, ident) && (msLevel == 1 || checkPrecursorMZ(row, ident))) {
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
    return simFunction.getModule().getSimilarity(simFunction.getParameterSet(), mzToleranceSpectra,
        minMatch, library, query);
  }

  private boolean checkPrecursorMZ(FeatureListRow row, SpectralDBEntry ident) {
    if (ident.getPrecursorMZ() == null) {
      return false;
    } else {
      return mzTolerancePrecursor.checkWithinTolerance(ident.getPrecursorMZ(), row.getAverageMZ());
    }
  }

  private boolean checkRT(FeatureListRow row, SpectralDBEntry ident) {
    if (!useRT) {
      return true;
    }
    Float rt = (Float) ident.getField(DBEntryField.RT).orElse(null);
    return (rt == null || rtTolerance.checkWithinTolerance(rt, row.getAverageRT()));
  }

  /**
   * Thresholded masslist
   *
   * @return the mass list data points from scan
   * @throws MissingMassListException if no mass list available
   */
  private DataPoint[] getDataPoints(Scan scan, boolean noiseFilter)
      throws MissingMassListException {
    if (scan == null || scan.getMassList() == null) {
      return new DataPoint[0];
    }

    MassList masses = scan.getMassList();
    DataPoint[] dps = masses.getDataPoints();
    return noiseFilter ? ScanUtils.getFiltered(dps, noiseLevel) : dps;
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

  protected void addIdentities(FeatureListRow row, List<SpectralDBFeatureIdentity> matches) {
    // add new identity to the row
    row.addSpectralLibraryMatches(matches);
  }

  public int getCount() {
    return matches.get();
  }

  public int getErrorCount() {
    return errorCounter.get();
  }
}
