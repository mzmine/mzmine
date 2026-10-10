/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import java.io.File;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OptimizationBatchEvaluatorMetadataTest {

  @Test
  void copiesManualMetadataToIsolatedRawFileByAbsolutePath() {
    final File path = new File("/tmp/optimizer-isolation-sample.mzML");
    final RawDataFile isolatedFile = Mockito.mock(RawDataFile.class);
    Mockito.when(isolatedFile.getName()).thenReturn("optimizer-isolation-sample.mzML");
    Mockito.when(isolatedFile.getAbsoluteFilePath()).thenReturn(path);
    final MZmineProjectImpl isolatedProject = new MZmineProjectImpl();
    isolatedProject.addFile(isolatedFile);

    final StringMetadataColumn sampleType = new StringMetadataColumn("mzmine_sample_type", "");
    OptimizationBatchEvaluator.copyMetadataByPath(Map.of(path, Map.of(sampleType, "reference")),
        isolatedProject);

    assertEquals("reference", isolatedProject.getProjectMetadata().getValue(sampleType,
        isolatedFile));
  }

  @Test
  void disablesRepeatedImportMetadataWithoutMutatingTheSourceParameters() {
    final AllSpectralDataImportParameters source = new AllSpectralDataImportParameters();
    source.setParameter(AllSpectralDataImportParameters.metadataFile, true);
    source.setParameter(AllSpectralDataImportParameters.extractMetadata, true);
    source.setParameter(AllSpectralDataImportParameters.sortAndRecolor, true);
    final AllSpectralDataImportParameters trial =
        (AllSpectralDataImportParameters) source.cloneParameterSet();
    final BatchQueue queue = new BatchQueue();
    queue.add(new MZmineProcessingStepImpl<>(new AllSpectralDataImportModule(), trial));

    OptimizationBatchEvaluator.disableTrialImportFollowups(queue);

    assertFalse(trial.getParameter(AllSpectralDataImportParameters.metadataFile).getValue());
    assertFalse(trial.getParameter(AllSpectralDataImportParameters.extractMetadata).getValue());
    assertFalse(trial.getValue(AllSpectralDataImportParameters.sortAndRecolor));
    assertTrue(source.getParameter(AllSpectralDataImportParameters.metadataFile).getValue());
    assertTrue(source.getParameter(AllSpectralDataImportParameters.extractMetadata).getValue());
    assertTrue(source.getValue(AllSpectralDataImportParameters.sortAndRecolor));
  }
}
