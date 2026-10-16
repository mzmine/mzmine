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

package io.github.mzmine.modules.batchmode;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.projectload.ProjectLoadModule;
import io.github.mzmine.modules.io.projectload.ProjectLoaderParameters;
import io.github.mzmine.modules.tools.clear_project.ClearProjectModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.net.URISyntaxException;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testutils.MZmineTestUtil;

/**
 * Tests that the {@link BatchTask} runs on its explicit project and continues on the result
 * project if a step replaces the project.
 */
class BatchTaskProjectTest {

  private static final String PROJECT_FILE = "rawdatafiles/integration_tests/workshop_dataset/project_keep_processing.mzmine";

  @BeforeAll
  static void initMzmine() {
    MZmineTestUtil.startMzmineCore();
  }

  @AfterEach
  void clearProject() {
    MZmineTestUtil.clearProjectAndLibraries();
  }

  @Test
  void projectLoadReplacesNonCurrentBatchProject() throws URISyntaxException {
    final MZmineProject current = setNewCurrentProject();
    final MZmineProject batchProject = new MZmineProjectImpl();

    final BatchTask batch = runBatch(batchProject, createProjectLoadStep(false));

    Assertions.assertEquals(TaskStatus.FINISHED, batch.getStatus(), batch.getErrorMessage());
    final MZmineProject result = batch.getProject();
    Assertions.assertNotSame(batchProject, result);
    Assertions.assertTrue(result.getNumberOfDataFiles() > 0);
    Assertions.assertTrue(result.getNumberOfFeatureLists() > 0);
    // the current GUI project is not touched by a batch on another project
    Assertions.assertSame(current, ProjectService.getProject());
    Assertions.assertEquals(0, current.getNumberOfDataFiles());
    Assertions.assertEquals(0, current.getNumberOfFeatureLists());

    // close the data files of the loaded project
    ProjectService.getProjectManager().clearProject(result);
  }

  @Test
  void projectLoadReplacesCurrentProject() throws URISyntaxException {
    final MZmineProject current = setNewCurrentProject();

    final BatchTask batch = runBatch(current, createProjectLoadStep(false));

    Assertions.assertEquals(TaskStatus.FINISHED, batch.getStatus(), batch.getErrorMessage());
    final MZmineProject result = batch.getProject();
    Assertions.assertNotSame(current, result);
    Assertions.assertTrue(result.getNumberOfFeatureLists() > 0);
    // the GUI follows if the current project was replaced
    Assertions.assertSame(result, ProjectService.getProject());
  }

  @Test
  void projectLoadMergesIntoBatchProject() throws URISyntaxException {
    final MZmineProject current = setNewCurrentProject();
    final MZmineProject batchProject = new MZmineProjectImpl();

    final BatchTask batch = runBatch(batchProject, createProjectLoadStep(true));

    Assertions.assertEquals(TaskStatus.FINISHED, batch.getStatus(), batch.getErrorMessage());
    Assertions.assertSame(batchProject, batch.getProject());
    Assertions.assertTrue(batchProject.getNumberOfFeatureLists() > 0);
    Assertions.assertSame(current, ProjectService.getProject());
    Assertions.assertEquals(0, current.getNumberOfFeatureLists());

    ProjectService.getProjectManager().clearProject(batchProject);
  }

  @Test
  void clearProjectReplacesBatchProject() {
    final MZmineProject current = setNewCurrentProject();
    final MZmineProject batchProject = new MZmineProjectImpl();
    batchProject.addFile(new RawDataFileImpl("a.mzML", null, null));

    final BatchTask batch = runBatch(batchProject,
        new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(ClearProjectModule.class),
            new SimpleParameterSet()));

    Assertions.assertEquals(TaskStatus.FINISHED, batch.getStatus(), batch.getErrorMessage());
    final MZmineProject result = batch.getProject();
    Assertions.assertNotSame(batchProject, result);
    Assertions.assertEquals(0, result.getNumberOfDataFiles());
    // the current GUI project is not touched by a batch on another project
    Assertions.assertSame(current, ProjectService.getProject());
  }

  @Test
  void clearProjectReplacesCurrentProject() {
    final MZmineProject current = setNewCurrentProject();
    current.addFile(new RawDataFileImpl("a.mzML", null, null));

    final BatchTask batch = runBatch(current,
        new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(ClearProjectModule.class),
            new SimpleParameterSet()));

    Assertions.assertEquals(TaskStatus.FINISHED, batch.getStatus(), batch.getErrorMessage());
    final MZmineProject result = batch.getProject();
    Assertions.assertNotSame(current, result);
    Assertions.assertEquals(0, result.getNumberOfDataFiles());
    // the GUI follows if the current project was replaced
    Assertions.assertSame(result, ProjectService.getProject());
  }

  private static @NotNull MZmineProject setNewCurrentProject() {
    final MZmineProject current = new MZmineProjectImpl();
    ProjectService.getProjectManager().setCurrentProject(current);
    return current;
  }

  @SafeVarargs
  private static @NotNull BatchTask runBatch(@NotNull final MZmineProject project,
      @NotNull final MZmineProcessingStepImpl<MZmineProcessingModule>... steps) {
    final BatchQueue queue = new BatchQueue();
    for (final MZmineProcessingStepImpl<MZmineProcessingModule> step : steps) {
      queue.add(step);
    }
    final ParameterSet parameters = new BatchModeParameters().cloneParameterSet();
    parameters.setParameter(BatchModeParameters.batchQueue, queue);

    final BatchTask batch = new BatchTask(project, parameters, Instant.now(), null);
    batch.run();
    return batch;
  }

  private static @NotNull MZmineProcessingStepImpl<MZmineProcessingModule> createProjectLoadStep(
      final boolean merge) throws URISyntaxException {
    final File projectFile = new File(
        BatchTaskProjectTest.class.getClassLoader().getResource(PROJECT_FILE).toURI());
    final ProjectLoaderParameters parameters = ProjectLoaderParameters.create(projectFile, merge,
        true);
    // keep the global configuration of the test unchanged
    parameters.setParameter(ProjectLoaderParameters.loadProjectConfiguration, false);
    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(ProjectLoadModule.class),
        parameters);
  }
}
