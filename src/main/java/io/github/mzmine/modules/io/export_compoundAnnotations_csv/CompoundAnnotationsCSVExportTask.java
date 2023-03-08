package io.github.mzmine.modules.io.export_compoundAnnotations_csv;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.io.export_features_csv_legacy.LegacyExportRowCommonElement;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class CompoundAnnotationsCSVExportTask extends AbstractTask {

    private static final Logger logger = Logger.getLogger(CompoundAnnotationsCSVExportTask.class.getName());
    private final FeatureList[] featureLists;
    private final File fileName;

    private int processedRows = 0, totalRows = 0;

    public CompoundAnnotationsCSVExportTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
        super(null, moduleCallDate);
        this.featureLists = parameters.getParameter(CompoundAnnotationsCSVExportParameters.featureLists).getValue()
                .getMatchingFeatureLists();
        fileName = parameters.getParameter(CompoundAnnotationsCSVExportParameters.filename).getValue();
    }

    public CompoundAnnotationsCSVExportTask(FeatureList[] featureLists, File fileName, @NotNull Instant moduleCallDate) {
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

        setStatus(TaskStatus.PROCESSING);

        // Shall export several files?
        String plNamePattern = "{}";
        boolean substitute = fileName.getPath().contains(plNamePattern);

        // Total number of rows
        for (FeatureList featureList : featureLists) {
            totalRows += featureList.getNumberOfRows();
        }

        // Process feature lists
        for (FeatureList featureList : featureLists) {
            // Cancel?
            if (isCanceled()) {
                return;
            }

            // Filename
            File curFile = fileName;
            if (substitute) {
                // Cleanup from illegal filename characters
                String cleanPlName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
                // Substitute
                String newFilename = fileName.getPath()
                        .replaceAll(Pattern.quote(plNamePattern), cleanPlName);
                curFile = new File(newFilename);
            }

            // Open file
            try (BufferedWriter writer = Files.newBufferedWriter(curFile.toPath(),
                    StandardCharsets.UTF_8)) {

                exportFeatureList(featureList, writer);

            } catch (IOException e) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Error during compound annotations csv export to " + curFile);
                logger.log(Level.WARNING,
                        "Error during compound annotations csv export of feature list: " + featureList.getName()
                                + ": " + e.getMessage(), e);
                return;
            }

            // If feature list substitution pattern wasn't found,
            // treat one feature list only
            if (!substitute) {
                break;
            }
        }

        if (getStatus() == TaskStatus.PROCESSING) {
            setStatus(TaskStatus.FINISHED);
        }


    }
    private void exportFeatureList(FeatureList featureList, BufferedWriter writer)
            throws IOException {
         // ToDO : Add headers and raws to csv
        try {

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

            writer.close();
            System.out.println("Export successful!");
        } catch (IOException e) {
            System.out.println("Error exporting feature annotations: " + e.getMessage());
        }
//        final NumberFormat mzForm = formats.mzFormat();
//        RawDataFile[] rawDataFiles = featureList.getRawDataFiles().toArray(RawDataFile[]::new);
//
//        // Buffer for writing
//        StringBuilder line = new StringBuilder();
//
//        // Write column headers
//
//        // Common elements
//        int length = commonElements.length;
//        String name;
//        for (int i = 0; i < length; i++) {
//            if (commonElements[i].equals(LegacyExportRowCommonElement.ROW_BEST_ANNOTATION_AND_SUPPORT)) {
//                line.append("best ion").append(fieldSeparator);
//                line.append("auto MS2 verify").append(fieldSeparator);
//                line.append("identified by n=").append(fieldSeparator);
//                line.append("partners").append(fieldSeparator);
//            } else if (commonElements[i].equals(LegacyExportRowCommonElement.ROW_BEST_ANNOTATION)) {
//                line.append("best ion").append(fieldSeparator);
//            } else {
//                name = commonElements[i].toString();
//                name = name.replace("Export ", "");
//                name = escapeStringForCSV(name);
//                line.append(name).append(fieldSeparator);
//            }
//        }
    }
}
