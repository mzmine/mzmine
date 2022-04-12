package io.github.mzmine.modules.dataprocessing.id_sirius_cli.sirius_import;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.modules.dataprocessing.id_sirius_cli.SiriusImportUtil;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SiriusResultsImportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(SiriusResultsImportTask.class.getName());

  private final ParameterSet parameters;
  private String desc = "Sirius results import.";
  private int total = 0;
  private int progress = 0;

  public SiriusResultsImportTask(@NotNull final ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.parameters = parameters;
  }

  @Override
  public String getTaskDescription() {
    return desc;
  }

  @Override
  public double getFinishedPercentage() {
    return total == 0 ? 0 : progress / (double) total;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final File siriusProjectDir = parameters.getValue(SiriusResultsImportParameters.projectDir);
    final ModularFeatureList flist = parameters.getValue(SiriusResultsImportParameters.flist)
        .getMatchingFeatureLists()[0];
    final ImportOption importOption = parameters.getValue(
        SiriusResultsImportParameters.importOption);
    final Boolean replaceOldAnnotations = parameters.getValue(
        SiriusResultsImportParameters.replaceOldAnnotations);

    if (!run(siriusProjectDir, flist, importOption, replaceOldAnnotations)) {
      return;
    }

    flist.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(SiriusResultsImportModule.class, parameters,
            getModuleCallDate()));
    setStatus(TaskStatus.FINISHED);
  }

  public boolean run(File siriusProjectDir, ModularFeatureList flist,
      ImportOption importOption, Boolean replaceOldAnnotations) {
    final Map<Integer, FeatureListRow> rowMap = flist.getRows().stream()
        .collect(Collectors.toMap(FeatureListRow::getID, row -> row));

    if (importOption == ImportOption.BEST) {
      logger.finest(() -> "Reading best compound ids.");
      final Map<Integer, CompoundDBAnnotation> database = SiriusImportUtil.readBestCompoundIdentifications(
          siriusProjectDir);

      logger.finest(() -> "Imported " + database.size() + " annotations from sirius project "
          + siriusProjectDir);

      for (Entry<Integer, CompoundDBAnnotation> entry : database.entrySet()) {
        var rowId = entry.getKey();
        var annotation = entry.getValue();

        final FeatureListRow row = rowMap.get(rowId);
        if (row == null) {
          setErrorMessage("Row ID " + rowId + " does not exist in feature list " + flist.getName()
              + ". Did you select the correct feature list?");
          logger.severe("Invalid row id " + rowId + "for feature list " + flist.getName());
          setStatus(TaskStatus.ERROR);
          return false;
        }

        final List<CompoundDBAnnotation> annotations = row.getCompoundAnnotations();
        final List<CompoundDBAnnotation> newList = new ArrayList<>();
        newList.add(annotation);
        if (!replaceOldAnnotations) {
          newList.addAll(annotations);
        }
        row.setCompoundAnnotations(newList);

        progress++;
      }
    } else {
      logger.finest(() -> "Reading best compound ids.");
      final Map<Integer, List<CompoundDBAnnotation>> database = SiriusImportUtil.readAllStructureCandidatesFromProject(
          siriusProjectDir);

      logger.finest(
          () -> "Imported annotations for " + database.size() + "rows from sirius project "
              + siriusProjectDir);

      for (Entry<Integer, List<CompoundDBAnnotation>> entry : database.entrySet()) {
        final var rowId = entry.getValue();
        var annotations = entry.getValue();

        if(annotations.isEmpty()) {
          continue;
        }

        if(importOption == ImportOption.TOP_TEN) {
          annotations = annotations.subList(0, Math.min(annotations.size() -1, 10));
        }

        final FeatureListRow row = rowMap.get(rowId);
        if (row == null) {
          setErrorMessage("Row ID " + rowId + " does not exist in feature list " + flist.getName()
              + ". Did you select the correct feature list?");
          logger.severe("Invalid row id " + rowId + "for feature list " + flist.getName());
          setStatus(TaskStatus.ERROR);
          return false;
        }

        final List<CompoundDBAnnotation> currentAnnotations = row.getCompoundAnnotations();
        final List<CompoundDBAnnotation> newList = new ArrayList<>();
        newList.addAll(annotations);
        if(!replaceOldAnnotations) {
          newList.addAll(currentAnnotations);
        }
        row.setCompoundAnnotations(newList);

        progress++;
      }
    }
    return true;
  }
}
