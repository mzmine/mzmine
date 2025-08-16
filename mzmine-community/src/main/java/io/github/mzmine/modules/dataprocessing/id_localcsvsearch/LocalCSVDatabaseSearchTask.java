/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_localcsvsearch;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseNameType;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkLibrary;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.FeatureListUtils;
import java.io.File;
import java.nio.file.NoSuchFileException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.transformation.SortedList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LocalCSVDatabaseSearchTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(LocalCSVDatabaseSearchTask.class.getName());

  private final DatabaseNameType databaseType = DataTypes.get(DatabaseNameType.class);

  // vars
  private final FeatureList[] featureLists;
  private final @Nullable MobilityTolerance mobTolerance;
  private final @Nullable Double ccsTolerance;
  private final File dataBaseFile;
  private final String fieldSeparator;
  private final @Nullable MZTolerance mzTolerance;
  private final @Nullable RTTolerance rtTolerance;
  private final IsotopePatternMatcherParameters isotopePatternMatcherParameters;
  private final MZTolerance isotopeMzTolerance;
  private final double minRelativeIsotopeIntensity;
  private final double minIsotopeScore;
  private final ParameterSet parameters;
  private final List<ImportType<?>> importTypes;
  private final IonLibraryParameterSet ionLibraryParameterSet;
  private final Boolean filterSamples;
  private final String sampleHeader;
  private final List<RawDataFile> allRawDataFiles;
  private final ExtraColumnHandler extraColumnHandler;
  private IonNetworkLibrary ionNetworkLibrary;

  private List<String[]> databaseValues;
  private int finishedLines = 0;
  private int sampleColIndex = -1;
  private boolean importOtherColumns = true;

  LocalCSVDatabaseSearchTask(FeatureList[] featureLists, ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    this.featureLists = featureLists;
    this.parameters = parameters;

    dataBaseFile = parameters.getParameter(LocalCSVDatabaseSearchParameters.dataBaseFile)
        .getValue();
    fieldSeparator = parameters.getParameter(LocalCSVDatabaseSearchParameters.fieldSeparator)
        .getValue();
    importTypes = parameters.getParameter(LocalCSVDatabaseSearchParameters.columns).getValue();
    mzTolerance = parameters.getValue(LocalCSVDatabaseSearchParameters.mzTolerance);
    rtTolerance = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        LocalCSVDatabaseSearchParameters.rtTolerance, null);
    mobTolerance = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        LocalCSVDatabaseSearchParameters.mobTolerance, null);
    ccsTolerance = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        LocalCSVDatabaseSearchParameters.ccsTolerance, null);

    Boolean calcMz = parameters.getValue(LocalCSVDatabaseSearchParameters.ionLibrary);
    ionLibraryParameterSet = calcMz != null && calcMz ? parameters.getParameter(
        LocalCSVDatabaseSearchParameters.ionLibrary).getEmbeddedParameters() : null;
    filterSamples = parameters.getValue(LocalCSVDatabaseSearchParameters.filterSamples);

    // all raw data files for a name check if selected
    allRawDataFiles = Arrays.stream(featureLists).map(FeatureList::getRawDataFiles)
        .flatMap(Collection::stream).distinct().toList();
    sampleHeader = parameters.getParameter(LocalCSVDatabaseSearchParameters.filterSamples)
        .getEmbeddedParameter().getValue();

    extraColumnHandler = new ExtraColumnHandler(
        parameters.getValue(LocalCSVDatabaseSearchParameters.extraColumns));

    final boolean isotopePatternMatcher = parameters.getValue(
        LocalCSVDatabaseSearchParameters.isotopePatternMatcher);
    if (isotopePatternMatcher) {
      isotopePatternMatcherParameters = parameters.getParameter(
          LocalCSVDatabaseSearchParameters.isotopePatternMatcher).getEmbeddedParameters();
      isotopeMzTolerance = isotopePatternMatcherParameters.getParameter(
          IsotopePatternMatcherParameters.isotopeMzTolerance).getValue();
      minRelativeIsotopeIntensity = isotopePatternMatcherParameters.getParameter(
          IsotopePatternMatcherParameters.minIntensity).getValue();
      minIsotopeScore = isotopePatternMatcherParameters.getParameter(
          IsotopePatternMatcherParameters.minIsotopeScore).getValue();
    } else {
      isotopePatternMatcherParameters = null;
      isotopeMzTolerance = null;
      minRelativeIsotopeIntensity = 0d;
      minIsotopeScore = 0d;
    }
  }

  @Override
  public double getFinishedPercentage() {
    if (databaseValues == null) {
      return 0;
    }
    return ((double) finishedLines) / databaseValues.size();
  }

  @Override
  public String getTaskDescription() {
    return "Local CSV identification using database " + dataBaseFile;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    try {
      // read database contents in memory
      databaseValues = CSVParsingUtils.readData(dataBaseFile, fieldSeparator);
    } catch (NoSuchFileException e) {
      error("File %s does not exist.".formatted(
          Objects.requireNonNullElse(dataBaseFile, "File does not exist.")));
      return;
    } catch (Exception e) {
      logger.log(Level.WARNING, "Could not read file " + dataBaseFile, e);
      error(e.getMessage(), e);
      return;
    }

    try {
      ionNetworkLibrary =
          ionLibraryParameterSet != null ? new IonNetworkLibrary(ionLibraryParameterSet,
              mzTolerance) : null;

      final StringProperty error = new SimpleStringProperty();
      final List<ImportType<?>> lineIds = CSVParsingUtils.findLineIds(importTypes,
          databaseValues.getFirst(), error, true);
      if (lineIds == null) {
        this.error(error.get());
        return;
      }

      // sample header index
      if (filterSamples) {
        sampleColIndex = getHeaderColumnIndex(databaseValues.getFirst(), sampleHeader);
        if (sampleColIndex == -1) {
          error("Sample header " + sampleHeader + " not found");
          return;
        }
      }

      // extract rows and sort by mz for binary search
      var mzSortedRows = Arrays.stream(featureLists)
          .map(flist -> flist.getRows().sorted(FeatureListRowSorter.MZ_ASCENDING)).toList();

      for (String[] currentLine : databaseValues) {
        if (finishedLines == 0) {
          finishedLines++;
          continue; // skip header
        }
        if (isCanceled()) {
          return;
        }
        try {
          // check already once for all raw data files
          if (filterSamples && !matchSample(allRawDataFiles, currentLine[sampleColIndex])) {
            // sample mismatch for this line
            continue;
          }

          processOneLine(mzSortedRows, currentLine, lineIds, databaseValues.getFirst());
        } catch (Exception e) {
          logger.log(Level.FINE, "Exception while processing csv line " + finishedLines, e);
        }
        finishedLines++;
      }

      for (final SortedList<FeatureListRow> flist : mzSortedRows) {
        for (final FeatureListRow row : flist) {
          var matches = row.getCompoundAnnotations().stream().sorted()
              .collect(Collectors.toCollection(ArrayList::new));
          if (matches.isEmpty()) {
            continue;
          }
          row.setCompoundAnnotations(matches);
        }
      }
      if (isotopePatternMatcherParameters != null) {
        for (FeatureList flist : featureLists) {
          refineAnnotationsByIsotopes(flist);
        }
      }


    } catch (Exception e) {
      error(e.getMessage(), e);
      return;
    }

    // Add task description to peakList
    for (var flist : featureLists) {
      flist.addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod("Peak identification using database " + dataBaseFile,
              LocalCSVDatabaseSearchModule.class, parameters, getModuleCallDate()));
    }

    setStatus(TaskStatus.FINISHED);

  }

  private void refineAnnotationsByIsotopes(FeatureList flist) {
    DatabaseIsotopeRefinerScanBased.refineAnnotationsByIsotopesDifferentResolutions(flist.getRows(),
        isotopeMzTolerance, minRelativeIsotopeIntensity, minIsotopeScore);
  }

  private boolean matchSample(final List<RawDataFile> raws, final String sample) {
    return raws.stream()
        .anyMatch(raw -> raw.getName().toLowerCase().contains(sample.toLowerCase()));
  }

  /**
   * @param mzSortedRows     rows per feature list sorted by mz
   * @param values           csv values to be parsed into annotation
   * @param linesWithIndices columns
   * @param csvHeaders
   */
  @SuppressWarnings("ResultOfMethodCallIgnored")
  private void processOneLine(final List<SortedList<FeatureListRow>> mzSortedRows,
      @NotNull String[] values, @NotNull List<ImportType<?>> linesWithIndices,
      @NotNull String[] csvHeaders) {

    final List<CompoundDBAnnotation> annotations = getCompoundDBAnnotations(values,
        linesWithIndices, csvHeaders, extraColumnHandler, ionNetworkLibrary);

    IntStream indexStream = IntStream.range(0, featureLists.length);
    if (featureLists.length > 1000) {
      indexStream.parallel();
    }
    // not all feature lists have all samples
    indexStream.forEach(i -> {
      var rawFiles = featureLists[i].getRawDataFiles();
      //  if active, check sample name contains id - this time for the feature list
      if (!filterSamples || matchSample(rawFiles, values[sampleColIndex])) {
        var sortedRows = mzSortedRows.get(i);

        for (CompoundDBAnnotation annotation : annotations) {
          List<FeatureListRow> candidates = binarySearchCandidates(sortedRows, annotation);

          for (FeatureListRow row : candidates) {
            checkMatchAndAnnotate(annotation, row, mzTolerance, rtTolerance, mobTolerance,
                ccsTolerance);
          }
        }
      }
    });
  }

  @NotNull
  private List<FeatureListRow> binarySearchCandidates(final SortedList<FeatureListRow> mzSortedRows,
      final CompoundDBAnnotation annotation) {
    // ranges are build with prechecks - so if there is no mobility use Range.all() to deactivate the filter
    Double mz = annotation.getPrecursorMZ();
    assert mz != null;
    final Float rt = annotation.getRT();
    final Float mobility = annotation.getMobility();
    // we need a range with boundaries for binary search. Range.all does not have boundaries
    final Range<Double> mzRange = mzTolerance != null ? mzTolerance.getToleranceRange(mz)
        : Range.closed(0d, Double.MAX_VALUE);
    final Range<Float> rtRange =
        rtTolerance != null && rt != null ? rtTolerance.getToleranceRange(rt) : Range.all();
    final Range<Float> mobilityRange =
        mobTolerance != null && mobility != null ? mobTolerance.getToleranceRange(mobility)
            : Range.all();

    // get all canditates with binary search
    // CCS is still missing here but will be tested later
    return FeatureListUtils.getCandidatesWithinRanges(mzRange, rtRange, mobilityRange, mzSortedRows,
        true);
  }

  @NotNull
  private List<CompoundDBAnnotation> getCompoundDBAnnotations(final @NotNull String[] values,
      final @NotNull List<ImportType<?>> linesWithIndices, @NotNull String[] headerValues,
      @NotNull final ExtraColumnHandler extraColumnHandler,
      @Nullable final IonNetworkLibrary ionLibrary) {
    final CompoundDBAnnotation baseAnnotation = CSVParsingUtils.getCompoundFromLine(headerValues,
        values, linesWithIndices, extraColumnHandler);
    baseAnnotation.put(databaseType, dataBaseFile.getName());
    final List<CompoundDBAnnotation> annotations = new ArrayList<>();
    if (ionLibrary != null) {
      annotations.addAll(
          CompoundDBAnnotation.buildCompoundsWithAdducts(baseAnnotation, ionLibrary));
    } else {
      annotations.add(baseAnnotation);
    }
    return annotations;
  }

  private void checkMatchAndAnnotate(@NotNull CompoundDBAnnotation annotation,
      @NotNull FeatureListRow row, @Nullable MZTolerance mzTolerance,
      @Nullable RTTolerance rtTolerance, @Nullable MobilityTolerance mobTolerance,
      @Nullable Double percCcsTolerance) {

    final CompoundDBAnnotation clone = annotation.checkMatchAndCalculateDeviation(row, mzTolerance,
        rtTolerance, mobTolerance, percCcsTolerance);
    if (clone != null) {
      row.addCompoundAnnotation(clone);
    }
  }

  private int getHeaderColumnIndex(final String[] firstLine, final String colHeader) {
    int colIndex = -1;
    for (int i = 0; i < firstLine.length; i++) {
      String columnName = firstLine[i];
      if (columnName.trim().equalsIgnoreCase(colHeader.trim())) {
        if (colIndex != -1) {
          return -1;
        }
        colIndex = i;
      }
    }
    return colIndex;
  }

  private void doIfNotNull(Object something, Runnable r) {
    if (something != null) {
      r.run();
    }
  }
}
