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

package io.github.mzmine.datamodel.features;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.types.numbers.AreaRsdType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.util.FeatureListTestUtils;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testutils.MZmineTestUtil;

/**
 * Feature lists keep a weak link to the project they were added to. Naming and the RSD calculation
 * use that project and not the current project.
 */
class FeatureListProjectLinkTest {

  private MZmineProject current;
  private MZmineProject other;
  private List<RawDataFile> files;

  @BeforeAll
  static void initMzmine() {
    MZmineTestUtil.startMzmineCore();
  }

  @BeforeEach
  void setUp() {
    current = new MZmineProjectImpl();
    ProjectService.getProjectManager().setCurrentProject(current);
    other = new MZmineProjectImpl();
    files = List.copyOf(
        FeatureListTestUtils.createRawFiles(3, "sample", LocalDateTime.of(2026, 1, 1, 0, 0),
            Duration.ofMinutes(1)));
  }

  @AfterEach
  void clearProject() {
    MZmineTestUtil.clearProjectAndLibraries();
  }

  @Test
  void addAndRemoveSetsProject() {
    final ModularFeatureList flist = FeatureListTestUtils.createFeatureList("a", files, 1, 100f);
    Assertions.assertNull(flist.getProject());

    other.addFeatureList(flist);
    Assertions.assertSame(other, flist.getProject());

    other.removeFeatureList(flist);
    Assertions.assertNull(flist.getProject());
  }

  @Test
  void renameIsUniqueInOwnProject() {
    // the current project has a list with the same name, which must not matter
    current.addFeatureList(FeatureListTestUtils.createFeatureList("b", files, 1, 100f));

    final ModularFeatureList a = FeatureListTestUtils.createFeatureList("a", files, 1, 100f);
    final ModularFeatureList b = FeatureListTestUtils.createFeatureList("b", files, 1, 100f);
    other.addFeatureList(a);
    other.addFeatureList(b);
    Assertions.assertEquals("b", b.getName());

    a.setName("b");
    Assertions.assertEquals("b (1)", a.getName());
  }

  @Test
  void rsdUsesMetadataOfOwnProject() {
    final ModularFeatureList flist = FeatureListTestUtils.createFeatureList("a", files, 0, 100f);
    final ModularFeatureListRow row = FeatureListTestUtils.addRow(flist, 1, files,
        List.of(100f, 200f, 50f));
    final AreaRsdType rsdType = new AreaRsdType();

    // no project, no sample types
    Assertions.assertNull(rsdType.getValue(row));

    final MetadataTable metadata = other.getProjectMetadata();
    metadata.setValue(metadata.getSampleTypeColumn(), files.get(0), SampleType.QC.toString());
    metadata.setValue(metadata.getSampleTypeColumn(), files.get(1), SampleType.QC.toString());
    metadata.setValue(metadata.getSampleTypeColumn(), files.get(2), SampleType.SAMPLE.toString());
    other.addFeatureList(flist);

    Assertions.assertNotNull(rsdType.getValue(row));
  }
}
