package io.github.mzmine.modules.io.export_compoundAnnotations_csv;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CompoundAnnotationsCSVExportTask extends AbstractTask {

    private static final Logger logger = Logger.getLogger(CompoundAnnotationsCSVExportTask.class.getName());
    private final FeatureList[] featureLists;
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

        //ToDO : implement buffered writer, CSV file

        setStatus(TaskStatus.PROCESSING);

        // Shall export several files?
        String plNamePattern = "{}";
        boolean substitute = fileName.getPath().contains(plNamePattern);

        try {
            FileWriter writer = new FileWriter("output.txt");

            // loop through all selected feature lists
            for (FeatureList featureList : featureLists) {
                // loop through all rows in the feature list
                for (FeatureListRow row : featureList.getRows()) {
                    List<Object> featureAnnotations = row.getAllFeatureAnnotations();
                    for (Object object : featureAnnotations) {
                        if (object instanceof FeatureAnnotation annotation) {
                            // Export fields from the FeatureAnnotation object
                            String CompoundName = annotation.getCompoundName();
                            double precursorMZ = annotation.getPrecursorMZ();
                            IonType adductType = annotation.getAdductType();
                            Float mobility = annotation.getMobility();
                            Float getCCS = annotation.getCCS();
                            Float getRT = annotation.getRT();
                            Float getScore = annotation.getScore();

                            // Export the fields as needed
                            writer.write(CompoundName + "," + precursorMZ + "," +adductType + "," +mobility + "," +getCCS + "," +getRT + "," +getScore);

                        }
                    }
                        // write the feature annotation to the output file
                        // Write the feature annotation to the file
//                        writer.append(annotation.getCompoundName());
//                        writer.append(",");
//                        writer.append(annotation.getFeatureValue());
//                        writer.append("\n");
                }
            }

            writer.close();
            System.out.println("Export successful!");
        } catch (IOException e) {
            System.out.println("Error exporting feature annotations: " + e.getMessage());
        }

//            // Open file
//            // Open file
//            try (BufferedWriter writer = Files.newBufferedWriter(curFile.toPath(),
//                    StandardCharsets.UTF_8)) {
//
//                exportFeatureList(featureList, writer);
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
