/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_rowsfilter;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.modules.dataanalysis.utils.imputation.ImputationFunctions;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.metadata.Metadata1GroupSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListTestUtils;
import io.github.mzmine.util.XMLUtils;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class RsdFilterTest {

  @Test
  void testUndetectedQcRowIsRejectedAfterMissingValueImputation() {
    final List<RawDataFileImpl> files = FeatureListTestUtils.createRawFiles(3, "sample",
        LocalDateTime.of(2026, 1, 1, 0, 0), Duration.ofMinutes(1));
    final List<RawDataFile> rawFiles = List.copyOf(files);
    final ModularFeatureList featureList = new ModularFeatureList("feature list", null,
        rawFiles.toArray(RawDataFile[]::new));
    final ModularFeatureListRow row = FeatureListTestUtils.addRow(featureList, 1, rawFiles,
        Arrays.asList(null, null, 100f));

    ProjectService.getProjectManager().setCurrentProject(new MZmineProjectImpl());
    final MetadataTable metadata = ProjectService.getMetadata();
    final MetadataColumn<String> sampleTypeColumn = metadata.getSampleTypeColumn();
    metadata.setValue(sampleTypeColumn, rawFiles.get(0), SampleType.QC.toString());
    metadata.setValue(sampleTypeColumn, rawFiles.get(1), SampleType.QC.toString());
    metadata.setValue(sampleTypeColumn, rawFiles.get(2), SampleType.SAMPLE.toString());

    final Metadata1GroupSelection qcSelection = new Metadata1GroupSelection(
        MetadataColumn.SAMPLE_TYPE_HEADER, SampleType.QC.toString());
    final RsdFilterParameters parameters = new RsdFilterParameters();
    parameters.setAll(AbundanceMeasure.Height, ImputationFunctions.GLOBAL_LIMIT_OF_DETECTION, 0.2,
        0.2, false, qcSelection);
    final RsdFilter filter = parameters.createFilter(featureList.getRows(), rawFiles);

    Assertions.assertEquals(rawFiles.subList(0, 2), filter.getGroupDataFiles());
    Assertions.assertEquals(2, filter.dataTable().getFeatureRow(0).countOriginalMissingValues());
    Assertions.assertFalse(filter.matches(row, 0));
  }

  @Test
  void testAppliedMethodRecordsFilesResolvedForRsdFilter() {
    final List<RawDataFileImpl> files = FeatureListTestUtils.createRawFiles(3, "sample",
        LocalDateTime.of(2026, 1, 1, 0, 0), Duration.ofMinutes(1));
    final List<RawDataFile> rawFiles = List.copyOf(files);
    final ModularFeatureList featureList = new ModularFeatureList("feature list", null,
        rawFiles.toArray(RawDataFile[]::new));
    FeatureListTestUtils.addRow(featureList, 1, rawFiles, Arrays.asList(100f, 101f, 300f));

    final MZmineProjectImpl project = new MZmineProjectImpl();
    ProjectService.getProjectManager().setCurrentProject(project);
    final MetadataTable metadata = ProjectService.getMetadata();
    final MetadataColumn<String> sampleTypeColumn = metadata.getSampleTypeColumn();
    metadata.setValue(sampleTypeColumn, rawFiles.get(0), SampleType.QC.toString());
    metadata.setValue(sampleTypeColumn, rawFiles.get(1), SampleType.QC.toString());
    metadata.setValue(sampleTypeColumn, rawFiles.get(2), SampleType.SAMPLE.toString());
    project.addFeatureList(featureList);

    final ParameterSet parameters = configuredRowsFilter(true);
    final RowsFilterTask task = new RowsFilterTask(project, featureList, parameters, null,
        Instant.now());
    task.run();
    Assertions.assertEquals(TaskStatus.FINISHED, task.getStatus());
    Assertions.assertNull(parameters.getParameter(RowsFilterParameters.cvFilter)
        .getEmbeddedParameters().getValue(RsdFilterParameters.resolvedGroupDataFiles));

    final RsdFilterParameters appliedRsd = featureList.getAppliedMethods().getLast()
        .getParameters().getParameter(RowsFilterParameters.cvFilter).getEmbeddedParameters();
    final RawDataFilesSelection recorded = appliedRsd.getValue(
        RsdFilterParameters.resolvedGroupDataFiles);
    Assertions.assertNotNull(recorded);
    Assertions.assertEquals(RawDataFilesSelectionType.SPECIFIC_FILES,
        recorded.getSelectionType());
    Assertions.assertArrayEquals(new String[]{rawFiles.get(0).getName(), rawFiles.get(1).getName()},
        Arrays.stream(recorded.getSpecificFilesPlaceholders()).map(RawDataFile::getName)
            .toArray(String[]::new));

    // Metadata may change after the task, but provenance remains the execution selection.
    metadata.setValue(sampleTypeColumn, rawFiles.get(0), SampleType.SAMPLE.toString());
    Assertions.assertArrayEquals(new String[]{rawFiles.get(0).getName(), rawFiles.get(1).getName()},
        Arrays.stream(recorded.getSpecificFilesPlaceholders()).map(RawDataFile::getName)
            .toArray(String[]::new));
  }

  @Test
  void testAppliedMethodDoesNotRetainResolvedFilesWhenRsdFilterIsDisabled() {
    final List<RawDataFileImpl> files = FeatureListTestUtils.createRawFiles(3, "sample",
        LocalDateTime.of(2026, 1, 1, 0, 0), Duration.ofMinutes(1));
    final List<RawDataFile> rawFiles = List.copyOf(files);
    final ModularFeatureList featureList = new ModularFeatureList("feature list", null,
        rawFiles.toArray(RawDataFile[]::new));
    FeatureListTestUtils.addRow(featureList, 1, rawFiles, Arrays.asList(100f, 101f, 300f));

    final MZmineProjectImpl project = new MZmineProjectImpl();
    ProjectService.getProjectManager().setCurrentProject(project);
    project.addFeatureList(featureList);

    final ParameterSet parameters = configuredRowsFilter(false);
    parameters.getParameter(RowsFilterParameters.cvFilter).getEmbeddedParameters()
        .recordResolvedGroupDataFiles(rawFiles.subList(0, 2));
    final RowsFilterTask task = new RowsFilterTask(project, featureList, parameters, null,
        Instant.now());
    task.run();
    Assertions.assertEquals(TaskStatus.FINISHED, task.getStatus());
    Assertions.assertNotNull(parameters.getParameter(RowsFilterParameters.cvFilter)
        .getEmbeddedParameters().getValue(RsdFilterParameters.resolvedGroupDataFiles));

    final RsdFilterParameters appliedRsd = featureList.getAppliedMethods().getLast()
        .getParameters().getParameter(RowsFilterParameters.cvFilter).getEmbeddedParameters();
    Assertions.assertNull(appliedRsd.getValue(RsdFilterParameters.resolvedGroupDataFiles));
  }

  @Test
  void testResolvedRsdGroupFilesRoundTripAndLegacyParameterSet() throws Exception {
    final List<RawDataFileImpl> files = FeatureListTestUtils.createRawFiles(2, "sample",
        LocalDateTime.of(2026, 1, 1, 0, 0), Duration.ofMinutes(1));
    final List<RawDataFile> rawFiles = List.copyOf(files);
    ProjectService.getProjectManager().setCurrentProject(new MZmineProjectImpl());

    final RsdFilterParameters parameters = (RsdFilterParameters) new RsdFilterParameters()
        .cloneParameterSet();
    parameters.recordResolvedGroupDataFiles(rawFiles);
    final String xml = ParameterUtils.saveValuesToXMLString(parameters);
    Assertions.assertFalse(xml.contains("path=\"null\""));

    final RsdFilterParameters loaded = (RsdFilterParameters) new RsdFilterParameters()
        .cloneParameterSet();
    ParameterUtils.loadValuesFromXMLString(loaded, xml);
    final RawDataFilesSelection recorded = loaded.getValue(
        RsdFilterParameters.resolvedGroupDataFiles);
    Assertions.assertNotNull(recorded);
    Assertions.assertEquals(RawDataFilesSelectionType.SPECIFIC_FILES,
        recorded.getSelectionType());
    Assertions.assertArrayEquals(new String[]{rawFiles.get(0).getName(), rawFiles.get(1).getName()},
        Arrays.stream(recorded.getSpecificFilesPlaceholders()).map(RawDataFile::getName)
            .toArray(String[]::new));

    final Document document = XMLUtils.load(xml);
    final Element parameterSet = (Element) document.getElementsByTagName("parameterset").item(0);
    final NodeList parameterElements = parameterSet.getElementsByTagName("parameter");
    for (int index = parameterElements.getLength() - 1; index >= 0; index--) {
      final Element parameter = (Element) parameterElements.item(index);
      if (RsdFilterParameters.resolvedGroupDataFiles.getName().equals(
          parameter.getAttribute("name"))) {
        parameterSet.removeChild(parameter);
      }
    }

    final RsdFilterParameters loadedLegacy = (RsdFilterParameters) new RsdFilterParameters()
        .cloneParameterSet();
    ParameterUtils.loadValuesFromXMLString(loadedLegacy, XMLUtils.saveToString(document));
    Assertions.assertNull(loadedLegacy.getValue(RsdFilterParameters.resolvedGroupDataFiles));
  }

  private static ParameterSet configuredRowsFilter(boolean enableRsdFilter) {
    final ParameterSet parameters = RowsFilterParameters.createDefaultAllOff();
    parameters.setParameter(RowsFilterParameters.handleOriginal,
        OriginalFeatureListOption.PROCESS_IN_PLACE);
    parameters.setParameter(RowsFilterParameters.cvFilter, enableRsdFilter);
    return parameters;
  }
}
