package io.github.mzmine.modules.io.export_compoundAnnotations_csv;

import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ScoreType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.io.CSVUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        try {
            // Create a list of column DataTypes
            var columns = Stream.of(
                            CompoundNameType.class, IonTypeType.class, ScoreType.class,
                            PrecursorMZType.class, MobilityType.class, CCSType.class, RTType.class)
                    .map(c -> DataTypes.get((Class) c))
                    .toList();

           // Create a header string by joining the unique IDs of the DataTypes with commas
            var header = columns.stream()
                    .map(DataType::getUniqueID)
                    .collect(Collectors.joining(","));

            // loop through all rows in the feature list
            for (FeatureListRow row : featureList.getRows()) {
                List<Object> featureAnnotations = row.getAllFeatureAnnotations();
                for (Object object : featureAnnotations) {
                    if (object instanceof FeatureAnnotation annotation) {
                        // Export fields from the FeatureAnnotation object
                        String compoundName = annotation.getCompoundName();
                        IonType adductType = annotation.getAdductType();
                        String scoreType = annotation.getScoreString();
                        double precursorMZ = annotation.getPrecursorMZ();
                        Float mobility = annotation.getMobility();
                        Float getCCS = annotation.getCCS();
                        Float getRT = annotation.getRT();

                        String result = Stream.of(compoundName, adductType, scoreType, precursorMZ, mobility, getCCS, getRT)
                                .map(o -> (o == null) ? "" : CSVUtils.escape(o.toString()))
                                .collect(Collectors.joining(","));

                        // Export the fields as needed
                        writer.write(result);

                    }
                }
            }

            writer.close();
            System.out.println("Export successful!");
        } catch (IOException e) {
            System.out.println("Error exporting feature annotations: " + e.getMessage());
        }
    }
}
