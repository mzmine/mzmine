/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.io.import_rawdata_all;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NativeImportSelectionTest {

  @Test
  void taskPreparationIsDetachedAndOnlyReadyWithTasks() {
    final Task task = Mockito.mock(Task.class);
    final List<Task> source = new ArrayList<>(List.of(task));
    final NativeImportSelection.ImportTaskPreparation preparation =
        new NativeImportSelection.ImportTaskPreparation(ExitCode.OK, source);

    source.clear();

    assertTrue(preparation.isReadyToSubmit());
    assertEquals(List.of(task), preparation.tasks());
    assertThrows(UnsupportedOperationException.class, () -> preparation.tasks().clear());
    assertFalse(new NativeImportSelection.ImportTaskPreparation(ExitCode.OK, List.of())
        .isReadyToSubmit());
    assertFalse(new NativeImportSelection.ImportTaskPreparation(ExitCode.ERROR, List.of(task))
        .isReadyToSubmit());
  }
}
