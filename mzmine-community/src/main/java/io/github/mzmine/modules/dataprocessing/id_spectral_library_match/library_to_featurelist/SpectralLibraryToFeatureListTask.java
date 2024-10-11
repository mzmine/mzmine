/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_spectral_library_match.library_to_featurelist;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.FeatureShapeType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.similarity.HandleUnmatchedSignalOptions;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.scans.similarity.Weights;
import io.github.mzmine.util.scans.similarity.impl.cosine.WeightedCosineSpectralSimilarity;
import io.github.mzmine.util.scans.similarity.impl.cosine.WeightedCosineSpectralSimilarityParameters;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.LibraryEntryWrappedScan;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryDataFile;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class SpectralLibraryToFeatureListTask extends AbstractTask {


  private final ParameterSet parameters;
  private final MZmineProject project;
  private final SpectralLibrary library;
  private final ParameterSet scoringParameters;
  private final MZTolerance mzTol;
  private int finished;

  protected SpectralLibraryToFeatureListTask(final @NotNull ParameterSet parameters,
      final @NotNull MZmineProject project, final SpectralLibrary library,
      @NotNull final Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.parameters = parameters;
    this.project = project;
    this.library = library;

    scoringParameters = MZmineCore.getConfiguration()
        .getModuleParameters(WeightedCosineSpectralSimilarity.class).cloneParameterSet();
    scoringParameters.setParameter(WeightedCosineSpectralSimilarityParameters.weight, Weights.SQRT);
    scoringParameters.setParameter(WeightedCosineSpectralSimilarityParameters.minCosine, 0d);
    scoringParameters.setParameter(WeightedCosineSpectralSimilarityParameters.handleUnmatched,
        HandleUnmatchedSignalOptions.KEEP_ALL_AND_MATCH_TO_ZERO);
    mzTol = new MZTolerance(0.005, 5);
  }

  @Override
  public String getTaskDescription() {
    return "Converting library to feature list: " + library.getName();
  }

  @Override
  public double getFinishedPercentage() {
    // two steps
    return finished / (double) library.size() / 2d;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    var libRaw = new SpectralLibraryDataFile(library);
    project.addFile(libRaw);

    var flist = addFeatureListWithEachEntry(libRaw);
    addFeatureListWithGroupedEntries(flist, libRaw);

    setStatus(TaskStatus.FINISHED);
  }

  @NotNull
  private static ModularFeature createFeature(final ModularFeatureList flist,
      final SpectralLibraryDataFile libRaw, final LibraryEntryWrappedScan scan) {
    var feature = new ModularFeature(flist, libRaw, FeatureStatus.DETECTED);
    feature.setAllMS2FragmentScans(List.of(scan));
    feature.setRepresentativeScan(scan);
    double mz = requireNonNullElse(scan.getPrecursorMz(),
        requireNonNullElse(scan.getBasePeakMz(), -1d));
    if (mz > 0) {
      feature.setMZ(mz);
    }
    var rt = scan.getRetentionTime();
    if (rt >= 0) {
      feature.setRT(rt);
    }
    var charge = scan.getPrecursorCharge();
    if (charge != null) {
      feature.setCharge(charge);
    }

    float tic = (float) ScanUtils.getTIC(scan);
    feature.setHeight(tic);
    return feature;
  }

  private void addFeatureListWithGroupedEntries(final FeatureList singleEntryFeatureList,
      final SpectralLibraryDataFile libRaw) {

    if (isCanceled()) {
      return;
    }

    var flist = new ModularFeatureList(library.getName(), null, libRaw);

    Map<LibraryEntryEqualityTester, List<LibraryEntryWrappedScan>> compoundMap = new HashMap<>();
    libRaw.streamLibraryScan().forEach(scan -> {
      var entry = scan.getEntry();
      var tester = new LibraryEntryEqualityTester(entry.getOrElse(DBEntryField.NAME, ""),
          entry.getOrElse(DBEntryField.FORMULA, ""), entry.getOrElse(DBEntryField.SMILES, ""),
          entry.getOrElse(DBEntryField.INCHI, ""), entry.getOrElse(DBEntryField.INCHIKEY, ""),
          entry.getOrElse(DBEntryField.ION_TYPE, ""));
      var list = compoundMap.computeIfAbsent(tester, t -> new ArrayList<>());
      list.add(scan);
    });

    // largest first
    var compounds = compoundMap.values().stream()
        .sorted(Comparator.comparingInt(value -> -value.size())).toList();
    int counter = 0;
    for (final List<LibraryEntryWrappedScan> compound : compounds) {
      if (isCanceled()) {
        return;
      }

      var bestScan = compound.get(0);

      var feature = createFeature(flist, libRaw, bestScan);
      feature.setAllMS2FragmentScans(compound.stream().map(s -> (Scan) s).toList());

      var row = new ModularFeatureListRow(flist, counter, feature);

      // add all scans as matches
      List<SpectralDBAnnotation> matches = new ArrayList<>();
      for (final LibraryEntryWrappedScan scan : compound) {
        var similarity = SpectralSimilarity.ofMatchIdentity(scan.getEntry());
        // add spectral lib match
        var match = new SpectralDBAnnotation(scan.getEntry(), similarity, scan, null, null, null);
        matches.add(match);
      }
      row.addSpectralLibraryMatches(matches);

      row.set(FeatureShapeType.class, false);
      flist.addRow(row);
      finished++;
      counter++;
    }

    if (singleEntryFeatureList.getNumberOfRows() != flist.getNumberOfRows()) {
      flist.setName("%s grouped %d compounds".formatted(flist.getName(), compounds.size()));
      flist.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(SpectralLibraryToFeatureListModule.class, parameters,
              moduleCallDate));
      project.addFeatureList(flist);
    }
  }

  private FeatureList addFeatureListWithEachEntry(final SpectralLibraryDataFile libRaw) {
    // add feature list
    var flist = new ModularFeatureList(library.getName() + " single scans", null, libRaw);

    var libScans = libRaw.streamLibraryScan().toList();
    for (final LibraryEntryWrappedScan scan : libScans) {

      if (isCanceled()) {
        return flist;
      }

      var feature = createFeature(flist, libRaw, scan);

      var row = new ModularFeatureListRow(flist, scan.getScanNumber(), feature);
      var similarity = SpectralSimilarity.ofMatchIdentity(scan.getEntry());
      // add spectral lib match
      var match = new SpectralDBAnnotation(scan.getEntry(), similarity, scan, null, null, null);
      row.addSpectralLibraryMatch(match);

      row.set(FeatureShapeType.class, false);
      flist.addRow(row);
      finished++;
    }

    flist.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(SpectralLibraryToFeatureListModule.class, parameters,
            moduleCallDate));
    project.addFeatureList(flist);
    return flist;
  }
}
