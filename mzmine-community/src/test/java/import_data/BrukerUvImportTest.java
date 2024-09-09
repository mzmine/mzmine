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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package import_data;

import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherSpectralData;
import io.github.mzmine.datamodel.otherdetectors.OtherSpectrum;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesData;
import io.github.mzmine.modules.io.import_rawdata_bruker_uv.BrukerUvReader;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.io.File;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class BrukerUvImportTest {

  @Test
  public void testTraceImport() {

    final URL resource = getClass().getClassLoader()
        .getResource("rawdatafiles/additional/bruker_uv");

    final RawDataFileImpl mockedFile = Mockito.mock(RawDataFileImpl.class);

    try (BrukerUvReader uvImport = BrukerUvReader.forFolder(new File(resource.getFile()))) {
      final List<OtherDataFile> traceFiles = uvImport.loadChromatograms(null, mockedFile);

      Assertions.assertEquals(1, traceFiles.size());

      final OtherDataFile traceFile = traceFiles.getFirst();
      Assertions.assertEquals(0, traceFile.getNumberOfSpectra());

      final OtherTimeSeriesData timeSeriesData = traceFile.getOtherTimeSeries();
      Assertions.assertEquals(ChromatogramType.ABSORPTION, timeSeriesData.getChromatogramType());
      Assertions.assertEquals(1, timeSeriesData.getNumberOfTimeSeries());
      final OtherFeature rawTrace = timeSeriesData.getRawTrace(0);
      final OtherTimeSeries featureData = rawTrace.getFeatureData();
      Assertions.assertEquals("UV_VIS_1", featureData.getName());
      Assertions.assertEquals(-0.6880331635475159d, featureData.getIntensity(150));
      Assertions.assertEquals(14685, featureData.getNumberOfValues());


    } catch (Exception e) {
      Assertions.fail(e);
    }
  }

  @Test
  public void testSpectraImport() {

    final URL resource = getClass().getClassLoader()
        .getResource("rawdatafiles/additional/bruker_uv");

    final RawDataFileImpl mockedFile = Mockito.mock(RawDataFileImpl.class);

    try (BrukerUvReader uvImport = BrukerUvReader.forFolder(new File(resource.getFile()))) {
      final List<OtherDataFile> spectraFiles = uvImport.loadSpectra(null, mockedFile);

      Assertions.assertEquals(1, spectraFiles.size());
      final OtherDataFile file = spectraFiles.getFirst();
      Assertions.assertEquals(25, file.getNumberOfSpectra());

      final OtherSpectralData spectralData = file.getOtherSpectralData();
      final OtherSpectrum spectrum = spectralData.getSpectra().get(13);
      Assertions.assertEquals(0.75f / 60, spectrum.getRetentionTime());
      Assertions.assertEquals("Wavelength", spectrum.getDomainLabel());
      Assertions.assertEquals("Intensity", spectrum.getRangeLabel());
      Assertions.assertEquals("a.u.", spectrum.getRangeUnit());
      Assertions.assertEquals(54, spectrum.getNumberOfValues());
    } catch (Exception e) {
      Assertions.fail(e);
    }
  }
}
