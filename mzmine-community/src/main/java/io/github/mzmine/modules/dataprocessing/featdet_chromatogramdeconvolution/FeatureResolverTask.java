/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.ImageType;
import io.github.mzmine.datamodel.features.types.MaldiSpotType;
import io.github.mzmine.datamodel.features.types.MobilityUnitType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.modules.dataprocessing.featdet_ML.MLFeatureResolver;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2Processor;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2SubParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.maths.CenterFunction;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class FeatureResolverTask extends AbstractTask {

    // Logger.
    private static final Logger logger = Logger.getLogger(FeatureResolverTask.class.getName());

    // Feature lists.
    private final MZmineProject project;
    private final FeatureList originalPeakList;
    // User parameters
    private final ParameterSet parameters;
    // function to find center mz of all feature data points
    private final CenterFunction mzCenterFunction;
    private FeatureList newPeakList;
    // Counters.
    private int processedRows;
    private int totalRows;
    private String errorMsg;
    private boolean setMSMSRange, setMSMSRT;
    private double msmsRange;
    private float RTRangeMSMS;
    private GroupMS2Processor groupMS2Task;

    /**
     * Create the task.
     *
     * @param storage
     * @param list         feature list to operate on.
     * @param parameterSet task parameters.
     */
    public FeatureResolverTask(final MZmineProject project, MemoryMapStorage storage,
            final FeatureList list, final ParameterSet parameterSet, CenterFunction mzCenterFunction,
            @NotNull Instant moduleCallDate) {
        super(storage, moduleCallDate);

        // Initialize.
        this.project = project;
        parameters = parameterSet;
        originalPeakList = list;
        newPeakList = null;
        processedRows = 0;
        totalRows = 0;
        this.mzCenterFunction = mzCenterFunction;
    }

    private static void exportFeatureToTsv(List<IonTimeSeries<? extends Scan>> resolvedSeries,
            ModularFeature originalFeature, FeatureDataAccess access) {
        final File dir = new File(
                "D:/export/max_features/%s".formatted(originalFeature.getRawDataFile().getName()));
        dir.mkdirs();
        final float maxHeight = originalFeature.getHeight();
        if (!resolvedSeries.isEmpty()) {
            try (var writer = Files.newBufferedWriter(new File(
                    "D:/export/max_features/%s".formatted(originalFeature.getRawDataFile().getName()),
                    "feature_%d_full.tsv".formatted(originalFeature.getRow().getID())).toPath(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                writer.write("x\ty\tfactor=%f".formatted(maxHeight));
                writer.newLine();
                for (int i = 0; i < access.getNumberOfValues(); i++) {
                    writer.write("%f".formatted(access.getRetentionTime(i)));
                    writer.write("\t");
                    writer.write("%f".formatted(access.getIntensity(i) / maxHeight));
                    writer.newLine();
                }
            } catch (IOException e) {
                //
            }
            try (var writer = Files.newBufferedWriter(new File(
                    "D:/export/max_features/%s".formatted(originalFeature.getRawDataFile().getName()),
                    "feature_%d_ranges.tsv".formatted(originalFeature.getRow().getID())).toPath(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                writer.write("start\tend\tmax");
                writer.newLine();
                for (IonTimeSeries<? extends Scan> resolved : resolvedSeries) {
                    writer.write("%f".formatted(resolved.getRetentionTime(0)));
                    writer.write("\t");
                    writer.write(
                            "%f".formatted(resolved.getRetentionTime(resolved.getNumberOfValues() - 1)));
                    writer.write("\t");
                    writer.write("%f".formatted(
                            resolved.getRetentionTime(FeatureDataUtils.getMostIntenseIndex(resolved))));
                    writer.newLine();
                }
            } catch (IOException e) {
                //
            }
        }
    }

    @Override
    public String getTaskDescription() {
        if (groupMS2Task != null) {
            return groupMS2Task.getTaskDescription();
        }
        return "Feature recognition on " + originalPeakList;
    }

    @Override
    public double getFinishedPercentage() {
        if (groupMS2Task != null) {
            return groupMS2Task.getFinishedPercentage();
        }
        return totalRows == 0 ? 0.0 : processedRows / (double) totalRows;
    }

    @Override
    public void run() {
        errorMsg = null;

        if (!isCanceled()) {

            setStatus(TaskStatus.PROCESSING);
            logger.info("Started feature resolving on " + originalPeakList);

            // Check raw data files.
            if (originalPeakList.getNumberOfRawDataFiles() > 1) {

                setStatus(TaskStatus.ERROR);
                setErrorMessage(
                        "Feature resolving can only be performed on feature lists with a single raw data file");
            } else {
                try {
                    if (((GeneralResolverParameters) parameters).getResolver(parameters,
                            (ModularFeatureList) originalPeakList) != null) {
                        dimensionIndependentResolve((ModularFeatureList) originalPeakList);
                    }
                    // resolving finished

                    // sort and reset IDs here to ahve the same sorting for every feature list
                    FeatureListUtils.sortByDefaultRT(newPeakList, true);

                    // group MS2 with features
                    var groupMs2Param = parameters.getParameter(GeneralResolverParameters.groupMS2Parameters);
                    if (groupMs2Param.getValue()) {
                        GroupMS2SubParameters ms2params = groupMs2Param.getEmbeddedParameters();
                        groupMS2Task = new GroupMS2Processor(this, newPeakList, ms2params);
                        // group all features with MS/MS
                        groupMS2Task.process();
                        groupMs2Param = null; // clear progress
                    }

                    if (!isCanceled()) {
                        // add new list and remove old if requested
                        final var handleOriginal = parameters.getValue(
                                GeneralResolverParameters.handleOriginal);
                        final String suffix = parameters.getValue(GeneralResolverParameters.SUFFIX);
                        handleOriginal.reflectNewFeatureListToProject(suffix, project, newPeakList,
                                originalPeakList);

                        setStatus(TaskStatus.FINISHED);
                        logger.info("Finished feature resolving on " + originalPeakList);
                    }
                } catch (Exception e) {
                    setStatus(TaskStatus.ERROR);
                    setErrorMessage(e.getMessage());
                    logger.log(Level.SEVERE, "Feature resolving error: " + e.getMessage(), e);
                }

                // Report error.
                if (errorMsg != null) {
                    setErrorMessage(errorMsg);
                    setStatus(TaskStatus.ERROR);
                }
            }
        }
    }

    private void dimensionIndependentResolve(ModularFeatureList originalFeatureList) {
        final Resolver resolver = ((GeneralResolverParameters) parameters).getResolver(parameters,
                originalFeatureList);
        if (resolver == null) {
            setErrorMessage("Resolver could not be initialised.");
            setStatus(TaskStatus.ERROR);
            return;
        }

        final RawDataFile dataFile = originalFeatureList.getRawDataFile(0);
        final ModularFeatureList resolvedFeatureList = createNewFeatureList(originalFeatureList);

        final FeatureDataAccess access = EfficientDataAccess.of(originalFeatureList,
                EfficientDataAccess.FeatureDataType.INCLUDE_ZEROS, dataFile);

        processedRows = 0;
        totalRows = originalFeatureList.getNumberOfRows();
        int peakId = 1;

        int c = 0;

        while (access.hasNextFeature()) {
            final ModularFeature originalFeature = (ModularFeature) access.nextFeature();
            final List<IonTimeSeries<? extends Scan>> resolvedSeries = resolver.resolve(access,
                    getMemoryMapStorage());
            if (resolvedSeries.size() > 0) {

                for (IonTimeSeries<? extends Scan> resolved : resolvedSeries) {
                    int i = 0;
                    final ModularFeatureListRow newRow = new ModularFeatureListRow(resolvedFeatureList,
                            peakId++);
                    final ModularFeature f = new ModularFeature(resolvedFeatureList,
                            originalFeature.getRawDataFile(), resolved, originalFeature.getFeatureStatus());

                    if (originalFeature.get(MaldiSpotType.class) != null) {
                        f.set(MobilityUnitType.class, originalFeature.getMobilityUnit());
                    }
                    if (originalFeature.get(ImageType.class) != null) {
                        f.set(ImageType.class, true);
                    }
                    if (originalFeature.get(MaldiSpotType.class) != null) {
                        f.set(MaldiSpotType.class, originalFeature.get(MaldiSpotType.class));
                    }

                    newRow.addFeature(originalFeature.getRawDataFile(), f);
                    resolvedFeatureList.addRow(newRow);
                    if (resolved.getSpectra().size() <= 3) {
                        c++;
                    }
                }
            }
            processedRows++;
        }

        if (resolver instanceof MLFeatureResolver mlFeatureResolver) {
            mlFeatureResolver.closeModel();
        }

        logger.info(c + "/" + resolvedFeatureList.getNumberOfRows()
                + " have less than 4 scans (frames for IMS data)");
        // QualityParameters.calculateAndSetModularQualityParameters(resolvedFeatureList);

        resolvedFeatureList.addDescriptionOfAppliedTask(
                new SimpleFeatureListAppliedMethod(resolver.getModuleClass(), parameters,
                        getModuleCallDate()));

        newPeakList = resolvedFeatureList;
    }

    @Override
    public void cancel() {
        super.cancel();
    }

    private ModularFeatureList createNewFeatureList(ModularFeatureList originalFeatureList) {
        if (originalFeatureList.getRawDataFiles().size() > 1) {
            throw new IllegalArgumentException("Resolving cannot be applied to aligned feature lists.");
        }
        final RawDataFile dataFile = originalFeatureList.getRawDataFile(0);

        // create a new feature list and don't copy. Previous annotations of features
        // are invalidated
        // during resolution
        final ModularFeatureList resolvedFeatureList = new ModularFeatureList(
                originalFeatureList.getName() + " " + parameters.getParameter(
                        GeneralResolverParameters.SUFFIX).getValue(),
                storage, dataFile);

        // DataTypeUtils.addDefaultChromatographicTypeColumns(resolvedFeatureList);
        resolvedFeatureList.setSelectedScans(dataFile, originalFeatureList.getSeletedScans(dataFile));

        // since we dont create a copy, we have to copy manually
        originalFeatureList.getAppliedMethods()
                .forEach(m -> resolvedFeatureList.getAppliedMethods().add(m));
        // the new method is added later, since we don't know here which resolver module
        // is used.

        // check the actual feature data. IMSRawDataFiles can also be built as classic
        // lc-ms features
        final Feature exampleFeature = originalFeatureList.getNumberOfRows() > 0
                ? originalFeatureList.getRow(0).getBestFeature()
                : null;

        boolean isImagingFile = (originalFeatureList.getRawDataFile(0) instanceof ImagingRawDataFile);
        if (exampleFeature != null
                && exampleFeature.getFeatureData() instanceof IonMobilogramTimeSeries) {
            DataTypeUtils.addDefaultIonMobilityTypeColumns(resolvedFeatureList);
        }
        if (originalFeatureList.hasRowType(RTType.class) && !isImagingFile) {
            DataTypeUtils.addDefaultChromatographicTypeColumns(resolvedFeatureList);
        }
        if (isImagingFile) {
            DataTypeUtils.addDefaultImagingTypeColumns(resolvedFeatureList);
        }

        return resolvedFeatureList;
    }
}
