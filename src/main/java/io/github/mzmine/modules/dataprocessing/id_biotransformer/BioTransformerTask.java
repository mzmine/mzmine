package io.github.mzmine.modules.dataprocessing.id_biotransformer;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkLibrary;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BioTransformerTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(BioTransformerTask.class.getName());

  private final MZmineProject project;
  private final ParameterSet parameters;
  private final ModularFeatureList[] flists;
  private final File bioPath;
  private final String transformationType;
  private final Integer steps;
  private final MZTolerance mzTolerance;
  private String description;
  private final FeatureList flist;

  private final boolean useFilterParam;
  private final boolean eductMustHaveMsMs;
  private final boolean productMustHaveMsMs;
  private final boolean checkEductIntensity;
  private final boolean checkProdcutIntensity;
  private final double minEductIntensity;
  private final double minProductIntensity;

  private int numEducts = 0;
  private int predictions = 1;

  public BioTransformerTask(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      FeatureList flist, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.project = project;
    this.parameters = parameters;
    bioPath = parameters.getValue(BioTransformerParameters.bioPath);
//    final String cmdOptions = parameters.getValue(BioTransformerParameters.cmdOptions);
    transformationType = parameters.getValue(BioTransformerParameters.transformationType);
    steps = parameters.getValue(BioTransformerParameters.steps);
    flists = parameters.getValue(BioTransformerParameters.flists).getMatchingFeatureLists();
    mzTolerance = parameters.getValue(BioTransformerParameters.mzTol);

    useFilterParam = parameters.getValue(BioTransformerParameters.filterParam);
    var filterParam = parameters.getParameter(BioTransformerParameters.filterParam)
        .getEmbeddedParameters();
    eductMustHaveMsMs = filterParam.getValue(BioTransformerFilterParameters.eductMustHaveMsMs);
    productMustHaveMsMs = filterParam.getValue(BioTransformerFilterParameters.productMustHaveMsMs);
    checkEductIntensity = filterParam.getValue(BioTransformerFilterParameters.minEductHeight);
    checkProdcutIntensity = filterParam.getValue(BioTransformerFilterParameters.minProductHeight);
    minEductIntensity = filterParam.getParameter(BioTransformerFilterParameters.minEductHeight)
        .getEmbeddedParameter().getValue();
    minProductIntensity = filterParam.getParameter(BioTransformerFilterParameters.minProductHeight)
        .getEmbeddedParameter().getValue();

    description = "Biotransformer process";
    this.flist = flist;
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

      StringProperty prefix = new SimpleStringProperty();
      final FeatureAnnotation bestAnnotation = getBestAnnotation(row, prefix);
      if (bestAnnotation == null) {
        continue;
      }
      uniqueSmilesMap.put(bestAnnotation.getSmiles(), row);
    }

    numEducts = uniqueSmilesMap.size();

    for (Entry<String, FeatureListRow> entry : uniqueSmilesMap.entrySet()) {
      if (isCanceled()) {
        return;
      }

      final String bestSmiles = entry.getKey();
      final FeatureListRow row = entry.getValue();
      final FeatureAnnotation bestAnnotation = getBestAnnotation(row, null);

      if (bestAnnotation == null || !bestSmiles.equals(bestAnnotation.getSmiles())) {
        throw new ConcurrentModificationException(
            "Best smiles of row " + row.getID() + " was altered.");
      }

      description = String.format(
          "Biotransformer task for %s. Processing educt %d/%d\tName: %s SMILES: %s",
          flist.getName(), predictions, numEducts, bestAnnotation.getCompoundName(), bestSmiles);

      final List<CompoundDBAnnotation> bioTransformerAnnotations = singleRowPrediction(row,
          bestSmiles, bestAnnotation.getCompoundName(), bioPath, parameters);

      if (bioTransformerAnnotations.isEmpty()) {
        predictions++;
        continue;
      }

      AtomicInteger numAnnotations = new AtomicInteger(0);
      for (CompoundDBAnnotation annotation : bioTransformerAnnotations) {
        flist.stream().filter(this::filterProductRow).forEach(r -> {
          if (LocalCSVDatabaseSearchTask.checkMatchAnnotateRow(annotation, r, mzTolerance, null,
              null, null)) {
            numAnnotations.getAndIncrement();
          }
        });
      }
      description = "Annotated " + numAnnotations + " rows for as transformations of " + bestAnnotation.getCompoundName();
      predictions++;
    }

    flist.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(BioTransformerModule.class, parameters,
            getModuleCallDate()));

    setStatus(TaskStatus.FINISHED);
  }

  @NotNull
  public static List<CompoundDBAnnotation> singleRowPrediction(@NotNull FeatureListRow row,
      @NotNull String bestSmiles, @Nullable String prefix, @NotNull File bioTransformerPath,
      @NotNull ParameterSet parameters) {

    var ionLibraryParam = parameters.getParameter(BioTransformerParameters.ionLibrary).getValue();
    var ionLibrary = new IonNetworkLibrary((IonLibraryParameterSet) ionLibraryParam);

    final Integer id = row.getID();
    String filename = id + "_transformation";
    final File file;
    try {
      // will be cleaned by temp file cleanup (windows)
      file = File.createTempFile("mzmine_bio_" + filename, ".csv");
      file.deleteOnExit();
    } catch (IOException e) {
      logger.log(Level.WARNING, "Error creating temp file for bio transformer. " + e.getMessage(),
          e);
      return List.of();
    }
    final List<String> cmd = BioTransformerUtil.buildCommandLineArguments(bestSmiles, parameters,
        file);

    BioTransformerUtil.runCommandAndWait(bioTransformerPath.getParentFile(), cmd);
    final List<CompoundDBAnnotation> bioTransformerAnnotations;
    try {
      bioTransformerAnnotations = BioTransformerUtil.parseLibrary(file,
          ionLibrary.getAllAdducts().toArray(new IonType[0]), new AtomicBoolean(),
          new AtomicInteger());
    } catch (IOException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
      return List.of();
    }
    AtomicInteger counter = new AtomicInteger(1);
    bioTransformerAnnotations.forEach(a -> a.put(CompoundNameType.class,
        Objects.requireNonNullElse(prefix, "") + "_transformation " + counter.getAndIncrement()));
    return bioTransformerAnnotations;
  }

  /**
   * @param row          The row.
   * @param compoundName out - Will be set to the name of the compound if a smiles is found
   * @return The smiles of the first spectral library match or compound db match. (may be null)
   */
  @Nullable
  private FeatureAnnotation getBestAnnotation(@NotNull FeatureListRow row,
      @Nullable StringProperty compoundName) {
    final List<SpectralDBAnnotation> spectralLibraryMatches = row.getSpectralLibraryMatches();
    if (!spectralLibraryMatches.isEmpty()) {
      final String smiles = spectralLibraryMatches.get(0).getEntry()
          .getOrElse(DBEntryField.SMILES, null);
      return spectralLibraryMatches.get(0);
    }

    final List<CompoundDBAnnotation> compoundAnnotations = row.getCompoundAnnotations();
    if (!compoundAnnotations.isEmpty()) {
      return compoundAnnotations.get(0);
    }

    return null;
  }

  boolean filterProductRow(FeatureListRow row) {
    if (!useFilterParam) {
      return true;
    }
    if (checkProdcutIntensity && row.getBestFeature().getHeight() < minProductIntensity) {
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
