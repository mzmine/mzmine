package io.github.mzmine.modules.dataprocessing.id_biotransformer;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  private int numEducts;
  private int processing = 1;

  public BioTransformerTask(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.project = project;
    this.parameters = parameters;
    bioPath = parameters.getValue(BioTransformerParameters.bioPath);
//    final String cmdOptions = parameters.getValue(BioTransformerParameters.cmdOptions);
    transformationType = parameters.getValue(BioTransformerParameters.transformationType);
    steps = parameters.getValue(BioTransformerParameters.steps);
    flists = parameters.getValue(BioTransformerParameters.flists).getMatchingFeatureLists();
    mzTolerance = parameters.getValue(BioTransformerParameters.mzTol);

    numEducts = Arrays.stream(flists).mapToInt(this::getNumEducts).sum();
    description = "Biotransformer process";
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return (processing - 1d) / numEducts;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (numEducts == 0) {
      setStatus(TaskStatus.FINISHED);
      return;
    }

    // make a map of all unique annotations (by smiles code)
    int numEducts = 0;
    Map<String, IonType> uniqueSmilesMap = new HashMap<>();
    for (ModularFeatureList flist : flists) {
      for (FeatureListRow row : flist.getRows()) {
        if (isCanceled()) {
          return;
        }

        StringProperty prefix = new SimpleStringProperty();
        final String bestSmiles = getBestSmiles(row, prefix);
        if (bestSmiles == null) {
          processing++;
          continue;
        }

      }
    }

    for (ModularFeatureList flist : flists) {
      for (FeatureListRow row : flist.getRows()) {
        if (isCanceled()) {
          return;
        }

        StringProperty prefix = new SimpleStringProperty();
        final String bestSmiles = getBestSmiles(row, prefix);
        if (bestSmiles == null) {
          processing++;
          continue;
        }
        description =
            "Biotransformer task " + processing + "/" + numEducts + " SMILES: " + bestSmiles;

        final List<BioTransformerAnnotation> bioTransformerAnnotations = singleRowPrediction(row,
            bestSmiles, prefix.getValue(), bioPath, parameters);

        if (bioTransformerAnnotations.isEmpty()) {
          processing++;
          continue;
        }

        for (BioTransformerAnnotation annotation : bioTransformerAnnotations) {
          flist.stream().forEach(
              r -> LocalCSVDatabaseSearchTask.checkMatchAnnotateRow(annotation, r, mzTolerance,
                  null, null, null));
        }

        processing++;
      }

      flist.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(BioTransformerModule.class, parameters,
              getModuleCallDate()));
    }
  }

  @NotNull
  public static List<BioTransformerAnnotation> singleRowPrediction(@NotNull FeatureListRow row,
      @NotNull String bestSmiles, @Nullable String prefix, @NotNull File bioTransformerPath,
      @NotNull ParameterSet parameters) {
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
    final List<BioTransformerAnnotation> bioTransformerAnnotations;
    try {
      bioTransformerAnnotations = BioTransformerUtil.parseLibrary(file,
          new IonType[]{new IonType(IonModification.H)}, new AtomicBoolean(), new AtomicInteger());
    } catch (IOException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
      return List.of();
    }
    bioTransformerAnnotations.forEach(a -> a.put(CompoundNameType.class,
        Objects.requireNonNullElse(prefix, "") + "_transformation"));
    return bioTransformerAnnotations;
  }

  /**
   * @param row          The row.
   * @param compoundName out - Will be set to the name of the compound if a smiles is found
   * @return The smiles of the first spectral library match or compound db match. (may be null)
   */
  @Nullable
  private String getBestSmiles(@NotNull FeatureListRow row, @Nullable StringProperty compoundName) {
    final List<SpectralDBAnnotation> spectralLibraryMatches = row.getSpectralLibraryMatches();
    if (!spectralLibraryMatches.isEmpty()) {
      final String smiles = spectralLibraryMatches.get(0).getEntry()
          .getOrElse(DBEntryField.SMILES, null);
      if (smiles != null) {
        if (compoundName != null) {
          compoundName.set(spectralLibraryMatches.get(0).getCompoundName());
        }
        return smiles;
      }
    }

    final List<CompoundDBAnnotation> compoundAnnotations = row.getCompoundAnnotations();
    if (!compoundAnnotations.isEmpty()) {
      if (compoundName != null) {
        compoundName.set(compoundAnnotations.get(0).getCompoundName());
      }
      return compoundAnnotations.get(0).getSmiles();
    }

    return null;
  }

  private int getNumEducts(ModularFeatureList featureList) {
    return (int) featureList.stream().filter(row -> getBestSmiles(row, null) != null).count();
  }
}
