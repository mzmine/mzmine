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

package io.github.mzmine.modules.dataprocessing.id_biotransformer;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkLibrary;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class BioTransformerTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(BioTransformerTask.class.getName());
  // biotransformer does not allow molecular weights > 1000
  private static final double molecularMassCutoff = 1000;

  private final ParameterSet parameters;
  private final File bioPath;
  private final MZTolerance mzTolerance;
  private final SmilesSource smilesSource;
  private final IonNetworkLibrary ionLibrary;
  private final FeatureList flist;
  private final boolean useFilterParam;
  private final boolean eductMustHaveMsMs;
  private final boolean productMustHaveMsMs;
  private final boolean checkEductIntensity;
  private final boolean checkProductIntensity;
  private final double minEductIntensity;
  private final double minProductIntensity;
  private final boolean rowCorrelationFilter;

  /**
   * Null if no filter is applied
   */
  @Nullable
  private final RTTolerance rtTolerance;
  private String description;
  private int numEducts = 0;
  private int predictions = 1;

  public BioTransformerTask(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      FeatureList flist, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.parameters = parameters;
    bioPath = parameters.getValue(BioTransformerParameters.bioPath);
    mzTolerance = parameters.getValue(BioTransformerParameters.mzTol);

    useFilterParam = parameters.getValue(BioTransformerParameters.filterParam);
    var filterParam = parameters.getParameter(BioTransformerParameters.filterParam)
        .getEmbeddedParameters();
    eductMustHaveMsMs = filterParam.getValue(BioTransformerFilterParameters.eductMustHaveMsMs);
    productMustHaveMsMs = filterParam.getValue(BioTransformerFilterParameters.productMustHaveMsMs);
    checkEductIntensity = filterParam.getValue(BioTransformerFilterParameters.minEductHeight);
    checkProductIntensity = filterParam.getValue(BioTransformerFilterParameters.minProductHeight);
    minEductIntensity = filterParam.getParameter(BioTransformerFilterParameters.minEductHeight)
        .getEmbeddedParameter().getValue();
    minProductIntensity = filterParam.getParameter(BioTransformerFilterParameters.minProductHeight)
        .getEmbeddedParameter().getValue();
    smilesSource = parameters.getValue(BioTransformerParameters.smilesSource);

    final boolean enableAdvancedFilters = parameters.getValue(BioTransformerParameters.advanced);
    final ParameterSet filterParams = parameters.getEmbeddedParameterValue(
        BioTransformerParameters.advanced);
    rowCorrelationFilter = enableAdvancedFilters && filterParams.getValue(
        RtClusterFilterParameters.rowCorrelationFilter);
    rtTolerance = enableAdvancedFilters ? filterParams.getEmbeddedParameterValueIfSelectedOrElse(
        RtClusterFilterParameters.rtTolerance, null) : null;

    var ionLibraryParam = parameters.getParameter(BioTransformerParameters.ionLibrary).getValue();
    ionLibrary = new IonNetworkLibrary((IonLibraryParameterSet) ionLibraryParam);

    description = "Biotransformer process";
    this.flist = flist;
  }

  /**
   * @param id                 A unique id for the transformation (e.g. row id)
   * @param prefix             Prefix for the transformation names. (e.g. Valsartan for
   *                           Valsartan_transformation_1)
   * @param bioTransformerPath The path to bio transformer
   * @param parameters         A {@link BioTransformerParameters} set to read the parameters for the
   *                           transformation from.
   * @return A list of transformation products, already ionized with the given ion library in
   * {@link BioTransformerParameters#ionLibrary}.
   */
  public static List<CompoundDBAnnotation> singleRowPrediction(final int id,
      @NotNull String bestSmiles, @Nullable String prefix, @NotNull File bioTransformerPath,
      @NotNull ParameterSet parameters) throws IOException {
    var ionLibraryParam = parameters.getParameter(BioTransformerParameters.ionLibrary).getValue();
    var ionLibrary = new IonNetworkLibrary((IonLibraryParameterSet) ionLibraryParam);

    return singleRowPrediction(id, bestSmiles, prefix, bioTransformerPath, parameters, ionLibrary);
  }

  /**
   * @param id                 A unique id for the transformation (e.g. row id)
   * @param prefix             Prefix for the transformation names. (e.g. Valsartan for
   *                           Valsartan_transformation_1)
   * @param bioTransformerPath The path to bio transformer
   * @param parameters         A {@link BioTransformerParameters} set to read the parameters for the
   *                           transformation from.
   * @param ionLibrary         The ion library to use.
   * @return A list of transformation products, already ionized with the given ion library.
   */
  @NotNull
  public static List<CompoundDBAnnotation> singleRowPrediction(final int id,
      @NotNull String bestSmiles, @Nullable String prefix, @NotNull File bioTransformerPath,
      @NotNull ParameterSet parameters, @NotNull IonNetworkLibrary ionLibrary) throws IOException {

    final IMolecularFormula fomulaFromSmiles = FormulaUtils.getFormulaFromSmiles(bestSmiles);
    if (FormulaUtils.getMonoisotopicMass(fomulaFromSmiles) > molecularMassCutoff) {
      return List.of();
    }

    String filename = id + "_transformation";
    // will be cleaned by temp file cleanup (windows)
    final File outputFile = FileAndPathUtil.createTempFile("mzmine_bio_" + filename, ".csv");
    outputFile.deleteOnExit();

    final List<String> cmd = BioTransformerUtil.buildCommandLineArguments(bestSmiles, parameters,
        outputFile);

    BioTransformerUtil.runCommandAndWait(bioTransformerPath.getParentFile(), cmd);

    final List<CompoundDBAnnotation> bioTransformerAnnotations = BioTransformerUtil.parseLibrary(
        outputFile, ionLibrary);

    bioTransformerAnnotations.forEach(a -> a.put(CompoundNameType.class,
        Objects.requireNonNullElse(prefix, "") + "_" + a.get(CompoundNameType.class)));

    return bioTransformerAnnotations;
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return numEducts == 0 ? 0d : (predictions - 1d) / numEducts;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // make a map of all unique annotations (by smiles code)
    final Map<String, FeatureListRow> uniqueSmilesMap = new HashMap<>();
    for (FeatureListRow row : flist.getRows()) {
      if (isCanceled()) {
        return;
      }

      if (!filterEductRow(row)) {
        continue;
      }

      final FeatureAnnotation bestAnnotation = getBestAnnotation(row);
      if (bestAnnotation == null) {
        continue;
      }
      uniqueSmilesMap.put(bestAnnotation.getSmiles(), row);
    }

    numEducts = uniqueSmilesMap.size();

    try {
      for (Entry<String, FeatureListRow> entry : uniqueSmilesMap.entrySet()) {
        if (isCanceled()) {
          return;
        }

        final String bestSmiles = entry.getKey();
        final FeatureListRow row = entry.getValue();
        final FeatureAnnotation bestAnnotation = getBestAnnotation(row);

        if (bestAnnotation == null || !bestSmiles.equals(bestAnnotation.getSmiles())) {
          throw new ConcurrentModificationException(
              "Best smiles of row " + row.getID() + " was altered.");
        }

        description = String.format(
            "Biotransformer task for %s. Processing educt %d/%d\tName: %s SMILES: %s",
            flist.getName(), predictions, numEducts, bestAnnotation.getCompoundName(), bestSmiles);

        final List<CompoundDBAnnotation> bioTransformerAnnotations = singleRowPrediction(
            row.getID(), bestSmiles, bestAnnotation.getCompoundName(), bioPath, parameters,
            ionLibrary);

        // rtTolerance filtering enabled -> we need to set the rt of the main compound to the
        // annotation for the filtering to work. Otherwise we don't set an rt to avoid confusion
        if (rtTolerance != null) {
          bioTransformerAnnotations.forEach(a -> a.put(RTType.class, row.getAverageRT()));
        }

        if (bioTransformerAnnotations.isEmpty()) {
          predictions++;
          continue;
        }

        final var ms1Groups = flist.getMs1CorrelationMap();
        AtomicInteger numAnnotations = new AtomicInteger(0);
        for (CompoundDBAnnotation annotation : bioTransformerAnnotations) {
          flist.stream().filter(this::filterProductRow).forEach(r -> {
            final CompoundDBAnnotation clone = annotation.checkMatchAndCalculateDeviation(r,
                mzTolerance, rtTolerance, null, null);
            if (clone != null) {

              final RowsRelationship correlation = ms1Groups.map(map -> map.get(row, r))
                  .orElse(null);
              if (rowCorrelationFilter && correlation == null) {
                return;
              }

              r.addCompoundAnnotation(clone);
              final List<CompoundDBAnnotation> annotations = new ArrayList<>(
                  row.getCompoundAnnotations());
              annotations.sort(CompoundAnnotationUtils.getSorterMaxScoreFirst());
              row.setCompoundAnnotations(annotations);
              numAnnotations.getAndIncrement();
            }
          });
        }
        description = "Annotated " + numAnnotations + " rows for as transformations of "
            + bestAnnotation.getCompoundName();
        predictions++;
      }

      flist.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(BioTransformerModule.class, parameters,
              getModuleCallDate()));

      setStatus(TaskStatus.FINISHED);
    } catch (IOException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
      setErrorMessage("Error reading/writing temporary files during BioTransformer prediction.\n"
          + e.getMessage());
      setStatus(TaskStatus.ERROR);
    }
  }

  /**
   * @param row The row.
   * @return The smiles of the first spectral library match or compound db match. (may be null)
   */
  @Nullable
  private FeatureAnnotation getBestAnnotation(@NotNull FeatureListRow row) {

    final List<SpectralDBAnnotation> spectralLibraryMatches = row.getSpectralLibraryMatches();
    if (!spectralLibraryMatches.isEmpty() && (smilesSource == SmilesSource.ALL
        || smilesSource == SmilesSource.SPECTRAL_LIBRARY)) {
      return spectralLibraryMatches.get(0);
    }

    final List<CompoundDBAnnotation> compoundAnnotations = row.getCompoundAnnotations();
    if (!compoundAnnotations.isEmpty() && (smilesSource == SmilesSource.ALL
        || smilesSource == SmilesSource.COMPOUND_DB)) {
      return compoundAnnotations.get(0);
    }

    return null;
  }

  boolean filterProductRow(FeatureListRow row) {
    if (!useFilterParam) {
      return true;
    }
    if (checkProductIntensity && row.getBestFeature().getHeight() < minProductIntensity) {
      return false;
    }
    if (productMustHaveMsMs && row.getMostIntenseFragmentScan() == null) {
      return false;
    }
    return true;
  }

  boolean filterEductRow(FeatureListRow row) {
    if (!useFilterParam) {
      return true;
    }
    if (checkEductIntensity && row.getBestFeature().getHeight() < minEductIntensity) {
      return false;
    }
    if (eductMustHaveMsMs && row.getMostIntenseFragmentScan() == null) {
      return false;
    }
    return true;
  }
}
