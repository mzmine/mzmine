/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
