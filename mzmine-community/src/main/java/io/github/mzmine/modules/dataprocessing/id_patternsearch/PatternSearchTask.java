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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_patternsearch;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.similarity.HandleUnmatchedSignalOptions;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import io.github.mzmine.util.scans.similarity.Weights;
import io.github.mzmine.util.scans.similarity.impl.cosine.WeightedCosineSpectralSimilarity;
import io.github.mzmine.util.scans.similarity.impl.cosine.WeightedCosineSpectralSimilarityParameters;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import io.github.mzmine.util.spectraldb.parser.AutoLibraryParser;
import io.github.mzmine.util.spectraldb.parser.UnsupportedFormatException;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PatternSearchTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(PatternSearchTask.class.getName());

  private final FeatureList flist;
  private final SpectralSimilarityFunction similarityFunction = new WeightedCosineSpectralSimilarity(
      WeightedCosineSpectralSimilarityParameters.of(Weights.INTENSITY, 0.7,
          HandleUnmatchedSignalOptions.KEEP_LIBRARY_SIGNALS));
//  private final @NotNull MZTolerance interScanMergingTolerance;
  private final @NotNull MZTolerance matchingTolerance;
  private SpectrumOption spectrumOption = SpectrumOption.MERGED_FWHM;
  private final File libraryFile;
  private AutoLibraryParser parser;

  /**
   * @param storage        The {@link MemoryMapStorage} used to store results of this task (e.g.
   *                       RawDataFiles, MassLists, FeatureLists). May be null if results shall be
   *                       stored in ram. For now, one storage should be created per module call in
   * @param moduleCallDate the call date of module to order execution order
   * @param parameters
   * @param moduleClass
   * @param project
   */
  protected PatternSearchTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull ParameterSet parameters, @NotNull Class<? extends MZmineModule> moduleClass,
      FeatureList flist, @NotNull MZmineProject project) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.flist = flist;
    totalItems = flist.getNumberOfRows();
    libraryFile = parameters.getValue(PatternSearchParameters.libraryFile);
//    interScanMergingTolerance = parameters.getValue(PatternSearchParameters.ms1MergingTolerance);
    matchingTolerance = parameters.getValue(PatternSearchParameters.isotopeMatchingTolerance);
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(flist);
  }

  @Override
  protected void process() {

    final SpectralLibrary library = importLibrary(libraryFile);
    final List<SpectralLibraryEntry> entries = library.getEntries();

    for (final FeatureListRow row : flist.getRowsCopy()) {

      final List<SpectralDBAnnotation> annotations = new ArrayList<>();

      for (ModularFeature feature : row.getFeatures()) {
//      final Scan featureSpectrum = extractSpectrumForFeature(feature, spectrumOption);
        final Scan featureSpectrum = feature.getMostIntenseFragmentScan();
        if (featureSpectrum == null) {
          continue;
        }

        for (SpectralLibraryEntry entry : entries) {
          final DataPoint[] shiftedDataPoints = new DataPoint[entry.getNumberOfDataPoints()];
          for (int i = 0; i < entry.getNumberOfDataPoints(); i++) {
            final double mzValue = entry.getMzValue(i);
            final double intensityValue = entry.getIntensityValue(i);
            shiftedDataPoints[i] = new SimpleDataPoint(mzValue + feature.getMZ(), intensityValue);
          }

          final SpectralSimilarity similarity = similarityFunction.getSimilarity(matchingTolerance,
              shiftedDataPoints.length, shiftedDataPoints,
              ScanUtils.extractDataPoints(featureSpectrum));

          if (similarity != null) {
            final SpectralDBEntry matchedEntry = new SpectralDBEntry(getMemoryMapStorage(),
                Arrays.stream(shiftedDataPoints).mapToDouble(DataPoint::getMZ).toArray(),
                Arrays.stream(shiftedDataPoints).mapToDouble(DataPoint::getIntensity).toArray(),
                entry.getFields(), entry.getLibrary());

            annotations.add(
                new SpectralDBAnnotation(matchedEntry, similarity, featureSpectrum, null, null,
                    null));
          }
        }
      }

      row.setSpectralLibraryMatch(annotations.stream()
          .sorted(Comparator.comparingDouble(SpectralDBAnnotation::getScore).reversed()).toList());
      incrementFinishedItems();
    }
  }

  /*@Nullable
  private Scan extractSpectrumForFeature(ModularFeature feature, SpectrumOption spectrumOption) {
    return switch (spectrumOption) {
      case MERGED_FWHM -> {
        final Range<Float> rtRange = RangeUtils.rangeAround(feature.getRT(),
            Objects.requireNonNullElse(feature.getFWHM(),
                RangeUtils.rangeLength(feature.getRawDataPointsRTRange())));
        final Range<Float> mobilityFWHM = Objects.requireNonNullElse(
            FeatureUtils.isImsFeature(feature) ? IonMobilityUtils.getMobilityFWHM(
                ((IonMobilogramTimeSeries) feature.getFeatureData()).getSummedMobilogram())
                : Range.all(), Range.all());
        yield SpectraMerging.extractMergedScan(feature, interScanMergingTolerance, mobilityFWHM,
            rtRange, null);
      }
      case SINGLE_BEST -> {
        if (FeatureUtils.isImsFeature(feature)) {
          yield IonMobilityUtils.getBestMobilityScan(feature);
        } else {
          yield feature.getRepresentativeScan();
        }
      }
    };
  }*/

  @Override
  public String getTaskDescription() {
    return "Searching for patterns";
  }

  private SpectralLibrary importLibrary(File file) {
    final SpectralLibrary library = new SpectralLibrary(MemoryMapStorage.forMassList(), file);
    parser = new AutoLibraryParser(1000, (list, alreadyProcessed) -> library.addEntries(list));
    // return tasks
    try {
      parser.parse(this, file, library);
    } catch (UnsupportedFormatException e) {
      logger.log(Level.SEVERE, "Unsupported format for spectral library: " + file.getAbsolutePath(),
          e);
      throw new RuntimeException(e);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error while reading spectral library: " + file.getAbsolutePath(),
          e);
    }
    return library;
  }

  @Override
  public double getFinishedPercentage() {
    return (totalItems != 0 ? finishedItems.get() / (double) totalItems : 0) * 0.5 + (0.5 * (
        parser != null ? parser.getProgress() : 0));
  }
}
