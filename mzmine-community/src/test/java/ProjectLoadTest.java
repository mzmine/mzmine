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

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.io.projectload.ProjectLoaderParameters;
import io.github.mzmine.modules.io.projectload.ProjectOpeningTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import java.io.File;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import testutils.MZmineTestUtil;

@DisplayName("Test Project Load Finding")
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@DisabledOnOs(OS.MAC)
public class ProjectLoadTest {

  private MZmineProject currentProject;

  @BeforeAll
  public void init() {
    //    logger.info("Running MZmine");
    MZmineTestUtil.startMzmineCore();
  }

  @AfterAll
  public void tearDown() {
    // we need to clean the project after this integration test
    MZmineTestUtil.cleanProject();
  }

  @Test
  @Order(1)
  void testOpenProject() throws InterruptedException {
    final ParameterSet param = new ProjectLoaderParameters().cloneParameterSet();
    param.setParameter(ProjectLoaderParameters.projectFile, new File(
        ProjectLoaderParameters.class.getClassLoader().getResource("rawdatafiles/dom_test.mzmine")
            .getFile()));
    ProjectOpeningTask newTask = new ProjectOpeningTask(param, Instant.now());
    newTask.run();

    var flist = ProjectService.getProjectManager().getCurrentProject()
        .getFeatureList("Aligned feature list corr PEARSON r greq 0.85 dp greq 5");

    assertEquals(2, flist.getRawDataFiles().size());
    // methods +1
    assertEquals(8, flist.getAppliedMethods().size());
    // less feature list rows
    assertEquals(49, flist.getNumberOfRows());
    assertEquals(24, flist.stream()
            .filter(row -> row.getFeatures().stream().filter(Objects::nonNull).count() == 2).count(),
        "Number of aligned features changed");
    // two library matches
    assertEquals(2, flist.getRows().stream().filter(FeatureListRow::isIdentified).count());

    assertEquals(14,
        flist.getRows().stream().max(Comparator.comparingInt(r -> r.getTypes().size())).get()
            .getTypes().size());
    assertEquals(16, flist.getRows().stream().flatMap(r -> r.streamFeatures())
        .max(Comparator.comparingInt(f -> f.getTypes().size())).get().getTypes().size());

    ProjectService.getProjectManager().clearProject();
    ProjectService.getProject().clearSpectralLibrary();
  }
}
