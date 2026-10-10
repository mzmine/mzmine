/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.io.import_rawdata_all;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AllSpectralDataImportMainTaskTest {

  @Test
  void fixedProjectTaskCancelsBeforeStartingAgainstAnotherProject() {
    final MZmineProject reviewedProject = Mockito.mock(MZmineProject.class);
    final AllSpectralDataImportMainTask task = AllSpectralDataImportMainTask.forFixedProject(
        List.of(), List.of(), new AllSpectralDataImportParameters(), reviewedProject);

    task.run();

    assertEquals(TaskStatus.CANCELED, task.getStatus());
    assertEquals("The reviewed project changed before import could continue.",
        task.getErrorMessage());
  }
}
