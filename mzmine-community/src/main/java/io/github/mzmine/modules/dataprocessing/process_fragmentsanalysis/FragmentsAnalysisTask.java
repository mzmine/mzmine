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

package io.github.mzmine.modules.dataprocessing.process_fragmentsanalysis;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.MGFEntryGenerator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNullElse;

/**
 * The task will be scheduled by the TaskController. Progress is calculated from the
 * finishedItems/totalItems
 */
class FragmentsAnalysisTask extends AbstractFeatureListTask {

    private static final Logger logger = Logger.getLogger(FragmentsAnalysisTask.class.getName());

    private final List<FeatureList> featureLists;
    private final File outFile;

    /**
     * Constructor is used to extract all parameters
     *
     * @param featureLists data source is featureLists
     * @param parameters   user parameters
     */
    public FragmentsAnalysisTask(MZmineProject project, List<FeatureList> featureLists, ParameterSet parameters,
                                 @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
                                 @NotNull Class<? extends MZmineModule> moduleClass) {
        super(storage, moduleCallDate, parameters, moduleClass);
        this.featureLists = featureLists;
        // important to track progress with totalItems and finishedItems
        totalItems = featureLists.stream().mapToInt(FeatureList::getNumberOfRows).sum();
        // Get parameter values for easier use
        outFile = parameters.getValue(FragmentsAnalysisParameters.outFile);
        // TODO
        // tolerance = ...
    }

    @Override
    protected void process() {
        // Open file
        try (BufferedWriter writer = Files.newBufferedWriter(outFile.toPath(),
                StandardCharsets.UTF_8)) {
            logger.fine(() -> String.format("Exporting GDebunk mgf for feature list: to file %s",
                    outFile.getAbsolutePath()));
            for (FeatureList featureList : featureLists) {
                for (var row : featureList.getRows()) {
                    processRow(writer, row);
                }
            }
        } catch (IOException e) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage("Could not open file " + outFile + " for writing.");
            logger.log(Level.WARNING, String.format(
                    "Error writing GDebunk mgf format to file: %s. Message: %s",
                    outFile.getAbsolutePath(), e.getMessage()), e);
        }
    }

    private void processRow(BufferedWriter writer, FeatureListRow row) throws NullPointerException, IOException {

        Scan bestMs1 = row.getBestFeature().getRepresentativeScan();
        // TODO Don't know why but this is failing looks like it fails when NULL?
        // int scanNumberBestMs1 = bestMs1.getScanNumber();
        // We can do it only on this one as it will remain the same
        // String fileUSI = Path.of(requireNonNullElse(bestMs1.getDataFile().getAbsolutePath(),bestMs1.getDataFile().getName())).getFileName().toString();
        // String fileUSIBestMs1 = fileUSI + ":" + scanNumberBestMs1;
        for (Scan ms2 : row.getAllFragmentScans()) {
            // int scanNumberMs2 = ms2.getScanNumber();
            // String fileUSIMs2 = fileUSI + ":" + scanNumberMs2;
            Scan previousMs1 = ScanUtils.findPrecursorScan(ms2);
            // int scanNumberPreviousMs1 = previousMs1.getScanNumber();
            // String fileUSIPreviousMs1 = fileUSI + ":" + scanNumberPreviousMs1;
            Scan nextMs1 = ScanUtils.findSucceedingPrecursorScan(ms2);
            // int scanNumberNextMs1 = previousMs1.getScanNumber();
            // String fileUSINextMs1 = fileUSI + ":" + scanNumberNextMs1;
            // exportScan(writer, row, ms2, fileUSIMs2);
            // exportScan(writer, row, previousMs1, fileUSIPreviousMs1);
            // exportScan(writer, row, nextMs1, fileUSINextMs1);
            exportScan(writer, row, ms2);
            exportScan(writer, row, previousMs1);
            exportScan(writer, row, nextMs1);
        }
        // exportScan(writer, row, bestMs1, fileUSIBestMs1);
        exportScan(writer, row, bestMs1);

// TODO Count the maximum number of common fragments between MS1 and MS2 in MS2
// TODO Count the maximum number of common fragments between MS1 and MS2 in MS1
//        int maxCommonFragmentsInMs2 = row.getAllFragmentScans().stream()
//                .mapToInt(ms2 -> countCommonFragments(bestMs1, ms2, tolerance))
//                .max().orElse(0);
//        int maxCommonFragmentsInMs1 = row.getAllFragmentScans().stream()
//                .mapToInt(ms2 -> countCommonFragments(ms2, bestMs1, tolerance))
//                .max().orElse(0);
//        row.set(SomeTypeForMaxCommonFragmentsInMs2, maxCommonFragmentsInMs2);
//        row.set(SomeTypeForMaxCommonFragmentsInMs1, maxCommonFragmentsInMs1);
    }

    private void exportScan(
            BufferedWriter writer,
            FeatureListRow row,
            Scan scan
            // String name
    ) throws IOException {
        SpectralLibraryEntry entry = SpectralLibraryEntry.create(null, row.getAverageMZ(), ScanUtils.extractDataPoints(scan, true));
        // entry.putIfNotNull(DBEntryField.ENTRY_ID, name);
        final String mgfEntry = MGFEntryGenerator.createMGFEntry(entry);
        writer.write(mgfEntry);
        writer.newLine();
    }

    @Override
    public String getTaskDescription() {
        return STR."Fragments analysis task runs on \{featureLists}";
    }

    @Override
    protected @NotNull List<FeatureList> getProcessedFeatureLists() {
        return featureLists;
    }

// TODO
//    private int countCommonFragments(Scan ms1, Scan ms2, double tolerance) {
//        DoubleStream ms1Fragments = DoubleStream.of(ScanUtils.getMzValues(ms1));
//        DoubleStream ms2Fragments = DoubleStream.of(ScanUtils.getMzValues(ms2));
//
//        List<Double> ms1FragmentList = ms1Fragments.boxed().collect(Collectors.toList());
//        List<Double> ms2FragmentList = ms2Fragments.boxed().collect(Collectors.toList());
//
//        int commonFragments = 0;
//        for (Double fragment : ms1FragmentList) {
//            for (Double ms2Fragment : ms2FragmentList) {
//                if (Math.abs(fragment - ms2Fragment) <= tolerance) {
//                    commonFragments++;
//                    break; // move to next fragment in ms1FragmentList
//                }
//            }
//        }
//        return commonFragments;
//    }
}
