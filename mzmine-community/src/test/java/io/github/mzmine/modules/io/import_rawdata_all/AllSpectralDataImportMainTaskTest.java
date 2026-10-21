/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.io.import_rawdata_all;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.main.MZmineCore;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AllSpectralDataImportMainTaskTest {

  @BeforeAll
  static void initializeConfiguration() {
    MZmineCore.getConfiguration().getPreferences();
  }

  @Test
  void importOutcomeDistinguishesRequestedReusedAndNewFiles() {
    final File reusedPath = new File("/tmp/reused.mzML");
    final File newPath = new File("/tmp/new.mzML");
    final RawDataFile reusedFile = mock(RawDataFile.class);
    when(reusedFile.getAbsoluteFilePath()).thenReturn(reusedPath);
    final MZmineProject project = mock(MZmineProject.class);
    when(project.getCurrentRawDataFiles()).thenReturn(List.of(reusedFile));

    final AllSpectralDataImportParameters parameters = new AllSpectralDataImportParameters();
    parameters.setParameter(AllSpectralDataImportParameters.fileNames,
        new File[]{reusedPath, newPath});
    final Task failedImport = mock(Task.class);
    when(failedImport.getTaskPriority()).thenReturn(TaskPriority.NORMAL);
    when(failedImport.getStatus()).thenReturn(TaskStatus.ERROR);

    final AllSpectralDataImportMainTask task = new AllSpectralDataImportMainTask(project,
        List.of(failedImport), List.of(failedImport), parameters);

    final var outcome = task.getImportOutcome();
    assertEquals(2, outcome.selectedRawFileCount());
    assertEquals(1, outcome.reusedRawFileCount());
    assertEquals(0, outcome.importedRawFileCount());
    assertEquals(1, outcome.failedRawFileCount());
  }
}
