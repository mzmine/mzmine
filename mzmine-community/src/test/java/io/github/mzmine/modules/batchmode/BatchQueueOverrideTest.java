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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchModule;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchParameters;
import io.github.mzmine.modules.io.projectload.ProjectLoadModule;
import io.github.mzmine.modules.io.projectload.ProjectLoaderParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.util.XMLUtils;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testutils.MZmineTestUtil;

/**
 * Verifies {@link BatchQueue#overrideStepParameter} replaces the value of a parameter inside a
 * specific step of a loaded batch.
 */
class BatchQueueOverrideTest {

  private static final String BATCH_RESOURCE = "batch_override/comp_db_project_load.mzbatch";

  @BeforeAll
  static void initMzmine() {
    MZmineTestUtil.startMzmineCore();
  }

  @Test
  void overrideCsvDatabaseAndProjectFile() throws Exception {
    final BatchQueue queue = loadBatch();

    // sanity: batch contains both target steps with the values from the example file
    final ParameterSet csvParams = queue.findFirst(LocalCSVDatabaseSearchModule.class)
        .orElseThrow(() -> new AssertionError(
            "Batch resource is missing a Local CSV database search step"));
    final ParameterSet projectParams = queue.findFirst(ProjectLoadModule.class)
        .orElseThrow(
            () -> new AssertionError("Batch resource is missing a Project import step"));

    final File originalCsv = csvParams.getValue(LocalCSVDatabaseSearchParameters.dataBaseFile);
    final File originalProject = projectParams.getValue(ProjectLoaderParameters.projectFile);
    assertNotNull(originalCsv, "CSV db parameter should be loaded from the batch");
    assertNotNull(originalProject, "Project file parameter should be loaded from the batch");

    final File newCsv = new File("override/new_database.tsv");
    final File newProject = new File("override/new_project.mzmine");

    assertTrue(queue.overrideStepParameter(LocalCSVDatabaseSearchModule.class,
            LocalCSVDatabaseSearchParameters.dataBaseFile, newCsv),
        "overrideStepParameter should succeed for the CSV database file");
    assertTrue(queue.overrideStepParameter(ProjectLoadModule.class,
        ProjectLoaderParameters.projectFile, newProject),
        "overrideStepParameter should succeed for the project file");

    // re-fetch the parameter sets to make sure the change is observable through the queue
    final Optional<ParameterSet> csvAfter = queue.findFirst(LocalCSVDatabaseSearchModule.class);
    final Optional<ParameterSet> projectAfter = queue.findFirst(ProjectLoadModule.class);

    assertEquals(newCsv,
        csvAfter.orElseThrow().getValue(LocalCSVDatabaseSearchParameters.dataBaseFile));
    assertEquals(newProject,
        projectAfter.orElseThrow().getValue(ProjectLoaderParameters.projectFile));
  }

  private static BatchQueue loadBatch() throws Exception {
    final URL resource = BatchQueueOverrideTest.class.getClassLoader().getResource(BATCH_RESOURCE);
    assertNotNull(resource, "Test resource not found on classpath: " + BATCH_RESOURCE);
    final File batchFile = new File(resource.toURI());
    return BatchQueue.loadFromXml(XMLUtils.load(batchFile).getDocumentElement(), new ArrayList<>(),
        true);
  }
}