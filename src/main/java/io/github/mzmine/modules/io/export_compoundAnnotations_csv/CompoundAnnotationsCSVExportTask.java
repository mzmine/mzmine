package io.github.mzmine.modules.io.export_compoundAnnotations_csv;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.ProcessedItemsCounter;
import io.github.mzmine.taskcontrol.TaskStatus;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class CompoundAnnotationsCSVExportTask extends AbstractTask implements ProcessedItemsCounter {

    private static final Logger logger = Logger.getLogger(CompoundAnnotationsCSVExportTask.class.getName());
    private final ModularFeatureList[] featureLists;
    private final File fileName;

    public CompoundAnnotationsCSVExportTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
        super(null, moduleCallDate);
        this.featureLists = parameters.getParameter(CompoundAnnotationsCSVExportParameters.featureLists).getValue()
                .getMatchingFeatureLists();
        fileName = parameters.getParameter(CompoundAnnotationsCSVExportParameters.filename).getValue();
    }

    public CompoundAnnotationsCSVExportTask(ModularFeatureList[] featureLists, File fileName, @NotNull Instant moduleCallDate) {
        super(null, moduleCallDate);
        this.featureLists = featureLists;
        this.fileName = fileName;
    }

    @Override
    public int getProcessedItems() {
        return 0; //exportedRows.get();
    }

    @Override
    public String getTaskDescription() {
        return "Exporting feature list(s) " + Arrays.toString(featureLists)
                + " to CSV file(s) ";
    }

    @Override
    public double getFinishedPercentage() {
        return 0;
    }

    @Override
    public void run() {

//        setStatus(TaskStatus.PROCESSING);
//
//        // Shall export several files?
//        String plNamePattern = "{}";
//        boolean substitute = fileName.getPath().contains(plNamePattern);
//
//        // Total number of rows
//        for (FeatureList featureList : featureLists) {
//           // totalRows += featureList.getNumberOfRows();
//        }
//
//
//
//        // Process feature lists
//        for (FeatureList featureList : featureLists) {
//            // Cancel?
//            if (isCanceled()) {
//                return;
//            }
//
//            // Filename
//            File curFile = fileName;
//            if (substitute) {
//                // Cleanup from illegal filename characters
//                String cleanPlName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
//                // Substitute
//                String newFilename = fileName.getPath()
//                        .replaceAll(Pattern.quote(plNamePattern), cleanPlName);
//                curFile = new File(newFilename);
//            }
//
//            // Open file
//            // Open file
//            try (BufferedWriter writer = Files.newBufferedWriter(curFile.toPath(),
//                    StandardCharsets.UTF_8)) {
//
//               // exportFeatureList(featureList, writer);
//
//            } catch (IOException e) {
//                setStatus(TaskStatus.ERROR);
//                setErrorMessage("Error during legacy csv export to " + curFile);
//                logger.log(Level.WARNING,
//                        "Error during MZmine 2 legacy csv export of feature list: " + featureList.getName()
//                                + ": " + e.getMessage(), e);
//                return;
//            }
//
//            // If feature list substitution pattern wasn't found,
//            // treat one feature list only
//            if (!substitute) {
//                break;
//            }
//        }
//
//        if (getStatus() == TaskStatus.PROCESSING) {
//            setStatus(TaskStatus.FINISHED);
//        }

    }
}
