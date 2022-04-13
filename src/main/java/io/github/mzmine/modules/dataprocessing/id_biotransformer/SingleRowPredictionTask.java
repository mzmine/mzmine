package io.github.mzmine.modules.dataprocessing.id_biotransformer;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class SingleRowPredictionTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(SingleRowPredictionTask.class.getName());

  private final ModularFeatureListRow row;
  private final String smiles;
  private final String prefix;
  private final ParameterSet parameters;
  private final File bioPath;
  private final String transformationType;
  private final Integer steps;
  private final MZTolerance mzTolerance;
  private String description;

  public SingleRowPredictionTask(ModularFeatureListRow row, String smiles, String prefix,
      @NotNull ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.row = row;
    this.smiles = smiles;
    this.prefix = prefix;
    this.parameters = parameters;
    bioPath = parameters.getValue(BioTransformerParameters.bioPath);
//    final String cmdOptions = parameters.getValue(BioTransformerParameters.cmdOptions);
    transformationType = parameters.getValue(BioTransformerParameters.transformationType);
    steps = parameters.getValue(BioTransformerParameters.steps);
    mzTolerance = parameters.getValue(BioTransformerParameters.mzTol);

    description = "Biotransformer task - SMILES: " + smiles;
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (isCanceled()) {
      setStatus(TaskStatus.FINISHED);
      return;
    }

    if (smiles == null) {
      setStatus(TaskStatus.FINISHED);
      return;
    }

    description = "Biotransformer task - SMILES: " + smiles;

    final List<CompoundDBAnnotation> bioTransformerAnnotations = BioTransformerTask.singleRowPrediction(
        row, smiles, prefix, bioPath, parameters);

    if (bioTransformerAnnotations.isEmpty()) {
      setStatus(TaskStatus.FINISHED);
      return;
    }

    final ModularFeatureList flist = row.getFeatureList();
    for (CompoundDBAnnotation annotation : bioTransformerAnnotations) {
      flist.stream().forEach(
          r -> LocalCSVDatabaseSearchTask.checkMatchAnnotateRow(annotation, r, mzTolerance, null,
              null, null));
    }

    setStatus(TaskStatus.FINISHED);
  }
}
