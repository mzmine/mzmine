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

package io.github.mzmine.modules.dataprocessing.id_precursordbsearch;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelectionException;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.IndexRange;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Search for possible precursor m/z . All rows average m/z against local spectral libraries
 */
class PrecursorDBSearchTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(PrecursorDBSearchTask.class.getName());

  private final List<@NotNull FeatureList> featureLists;
  private final MZTolerance mzTol;
  private final RTTolerance rtTol;
  private final MobilityTolerance mobTol;
  private final Double ccsTol;
  private int libraryEntries;

  public PrecursorDBSearchTask(final ParameterSet parameters, final Instant moduleCallDate) {
    super(null, moduleCallDate, parameters, PrecursorDBSearchModule.class);
    this.featureLists = List.of(
        parameters.getValue(PrecursorDBSearchParameters.featureLists).getMatchingFeatureLists());
    mzTol = parameters.getValue(PrecursorDBSearchParameters.mzTolerancePrecursor);
    rtTol = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        PrecursorDBSearchParameters.rtTolerance, null);
    mobTol = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        PrecursorDBSearchParameters.mobTolerance, null);
    ccsTol = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        PrecursorDBSearchParameters.ccsTolerance, null);

  }

  @Override
  public String getTaskDescription() {
    if (libraryEntries == 0) {
      return "Identify possible precursor m/z in %d feature lists".formatted(featureLists.size());
    }

    return "Identify possible precursor m/z in %d feature lists using %d spectral library entries".formatted(
        featureLists.size(), libraryEntries);
  }

  @Override
  protected void process() {
    List<CompoundDBAnnotation> entries = combineLibrariesSortedByMz();
    if (isCanceled()) {
      return;
    }
    totalItems = featureLists.stream().mapToLong(FeatureList::getNumberOfRows).sum();
    finishedItems.set(0); // reset

    long matches = featureLists.parallelStream()
        .mapToLong(flist -> annotateFeatureList(flist, entries)).sum();

    if (featureLists.size() > 1) {
      logger.info("Added " + matches + " precursor m/z matches to all feature lists.");
    }
  }

  private long annotateFeatureList(final FeatureList featureList,
      final List<CompoundDBAnnotation> mzSortedEntries) {
    AtomicLong matches = new AtomicLong(0);

    // sort by mz like the entries
    List<FeatureListRow> rows = featureList.getRows().stream()
        .sorted(Comparator.comparingDouble(FeatureListRow::getAverageMZ)).toList();
    int entryIndexStart = 0;
    for (final FeatureListRow row : rows) {
      Range<Double> mzRange = mzTol.getToleranceRange(row.getAverageMZ());
      // precursor mz is already checked
      @SuppressWarnings("DataFlowIssue") IndexRange indexRange = BinarySearch.indexRange(
          mzRange.lowerEndpoint(), mzRange.upperEndpoint(), mzSortedEntries, entryIndexStart,
          mzSortedEntries.size(), CompoundDBAnnotation::getPrecursorMZ);
      if (indexRange.isEmpty()) {
        continue;
      }

      entryIndexStart = indexRange.min(); // skip a few entries next time

      indexRange.forEach(index -> {
        CompoundDBAnnotation db = mzSortedEntries.get(index);
        var match = db.checkMatchAndCalculateDeviation(row, mzTol, rtTol, mobTol, ccsTol);
        if (match != null) {
          row.addCompoundAnnotation(match);
          matches.incrementAndGet();
        }
      });

      finishedItems.incrementAndGet();
    }

    logger.info("Added %d precursor m/z matches to feature list %s.".formatted(matches.get(),
        featureList.getName()));
    return matches.get();
  }

  @NotNull
  private List<CompoundDBAnnotation> combineLibrariesSortedByMz() {
    // combine libraries
    try {
      final List<SpectralLibraryEntry> entries = parameters.getValue(
          PrecursorDBSearchParameters.libraries).getMatchingLibraryEntriesAndCheckAvailability();

      totalItems = entries.size();
      finishedItems.set(0);

      @SuppressWarnings("DataFlowIssue") List<CompoundDBAnnotation> dbEntries = entries.stream()
          .map(spec -> {
            finishedItems.incrementAndGet();
            return CompoundAnnotationUtils.convertSpectralToCompoundDb(spec);
          }).filter(db -> db.getPrecursorMZ() != null)
          .sorted(Comparator.comparingDouble(CompoundDBAnnotation::getPrecursorMZ)).toList();
      libraryEntries = dbEntries.size();
      if (dbEntries.isEmpty()) {
        error("No library entries with precursor mz found.");
      }
      logger.fine("Checking for precursors against %d entries.".formatted(libraryEntries));
      return dbEntries;
    } catch (SpectralLibrarySelectionException e) {
      error("Error in spectral library search.", e);
      return List.of();
    }
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return featureLists;
  }

}
