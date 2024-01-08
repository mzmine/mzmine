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

package metadata;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.projectmetadata.io.WideTableIOUtils;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.io.File;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetadataIOTest {

  RawDataFile rawA = new RawDataFileImpl("a.mzML", null, null);
  RawDataFile rawB = new RawDataFileImpl("b.mzML", null, null);

  @BeforeEach
  public void init() {
    //    logger.info("Running MZmine");
    //    MZmineCore.main(new String[]{"-r", "-m", "all"});
    var project = MZmineCore.getProjectManager().getCurrentProject();
    project.addFile(rawA);
    project.addFile(rawB);
  }

  @Test
  void testImportWideFormat() {
    File file = new File(
        MetadataIOTest.class.getClassLoader().getResource("metadata/metadata_wide.tsv").getFile());
    File file2 = new File(
        MetadataIOTest.class.getClassLoader().getResource("metadata/metadata_wide_defined.tsv")
            .getFile());
    WideTableIOUtils importer = new WideTableIOUtils(new MetadataTable());
    Assertions.assertTrue(importer.importFrom(file, false));
    Assertions.assertTrue(importer.importFrom(file2, false));
  }

  @Test
  void testImportWideNumberFormatError() {
    File file = new File(MetadataIOTest.class.getClassLoader()
        .getResource("metadata/metadata_wide_defined_numberparse_exception.tsv").getFile());
    WideTableIOUtils importer = new WideTableIOUtils(new MetadataTable());
    Assertions.assertTrue(importer.importFrom(file, true));
    importer = new WideTableIOUtils(new MetadataTable());
    Assertions.assertFalse(importer.importFrom(file, false));
  }
}
