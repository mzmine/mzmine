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

package io.github.mzmine.modules.io.import_rawdata_wiff2;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.SimpleModularDataModel;
import io.github.mzmine.datamodel.features.rawfiletypes.AcquisitionSoftwareType;
import io.github.mzmine.datamodel.features.rawfiletypes.AcquisitionSoftwareVersionType;
import io.github.mzmine.datamodel.features.rawfiletypes.IonSourcesType;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.Sample;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Reads the sample info of a wiff2 file into the raw file metadata. Skipped if the example file is
 * not available.
 */
@DisabledOnOs({OS.MAC, OS.LINUX})
class Wiff2FileMetadataTest {

  private static final File WIFF2 = new File(
      "D:\\OneDrive - mzio GmbH\\Example data - Documents\\SCIEX\\ZenoTof 8600\\NIST 1950 Metabolomics (ZTScan 3)\\DDA Data Metabolomics (8600)\\01232026_NIST1950_DDA_Neg_8600_1.wiff2");

  @Test
  void sampleInfoMapsSoftwareAndIonSource() throws Exception {
    if (!WIFF2.exists()) {
      return;
    }
    final ModularDataModel metadata = new SimpleModularDataModel();
    try (var access = new Wiff2DataAccess(WIFF2, true, ScanImportProcessorConfig.createDefault())) {
      final List<Sample> samples = access.getSamples();
      Assertions.assertEquals(1, samples.size());
      access.applySampleInfoToFileMetadata(samples.getFirst(), metadata);
    }

    Assertions.assertEquals("SCIEX OS", metadata.get(AcquisitionSoftwareType.class));
    Assertions.assertEquals("4.0.0.8559", metadata.get(AcquisitionSoftwareVersionType.class));
    Assertions.assertEquals(List.of("OptiFlow Pro Analytical >200 \u00b5L/Cal"),
        metadata.get(IonSourcesType.class));
  }
}
