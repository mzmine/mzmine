/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test ion identity networking
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(OrderAnnotation.class)
public class ProjectTest {

  @Mock
  RawDataFile raw;

  @BeforeEach
  void initEach() {
    // each test a new project
  }


  @Test
  @Order(1)
  void projectTest() {
    MZmineTestUtil.cleanProject();
    // state for testing should be headless
    assertEquals(true, MZmineCore.isHeadLessMode());

    MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
    assertNotNull(project);

    ModularFeatureList flist = new ModularFeatureList("A", null, raw);
    project.addFeatureList(flist);

    assertEquals(1, project.getCurrentFeatureLists().size());

    project.addFile(raw);
    assertEquals(1, project.getCurrentRawDataFiles().size());

    // clean project. Otherwise next tests will see objects from this test
    MZmineTestUtil.cleanProject();
  }

  @Test
  @Order(2)
  void testEmptyProjectAfterTest() {
    final MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
    assertNotNull(project);

    assertEquals(0, project.getCurrentFeatureLists().size());
    assertEquals(0, project.getCurrentRawDataFiles().size());
  }

  @Test
  @Order(3)
  void setNewProjectTest() {
    final MZmineProject old = MZmineCore.getProjectManager().getCurrentProject();
    MZmineCore.getProjectManager().setCurrentProject(new MZmineProjectImpl());
    assertNotEquals(old, MZmineCore.getProjectManager().getCurrentProject());
  }

}
