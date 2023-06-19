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

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.io.export_features_gnps.GNPSUtils;
import io.github.mzmine.modules.io.export_features_gnps.masst.MasstDatabase;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import io.github.mzmine.util.web.RequestResponse;
import java.io.IOException;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class GNPSTest {

  @Test
  @Disabled
  void testLibraryAccess() throws IOException {
    SpectralLibraryEntry spec = GNPSUtils.accessLibrarySpectrum("CCMSLIB00005463737");
    Assertions.assertNotNull(spec);
    Assertions.assertTrue(spec.getDataPoints().length > 0);
    Assertions.assertTrue(spec.getPrecursorMZ() > 0);
  }

  @Test
  void testMasstSearch() throws IOException {
    // GNPS demo data
    String demo = """
        463.381	43.591
        693.498	119.206
        694.496	42.985
        707.494	508.18
        708.512	197.117
        709.558	18.679
        723.4	43.831
        800.494	476.556
        801.518	196.451
        802.496	95.972
        814.513	86.182
        931.574	50.803
        972.868	14.634
        1016.62	66.809
        1017.58	16.578
        1025.57	22.426""";
    DataPoint[] dps = Arrays.stream(demo.split("\n")).map(line -> line.split("	"))
        .map(dp -> new SimpleDataPoint(Double.parseDouble(dp[0]), Double.parseDouble(dp[1])))
        .toArray(DataPoint[]::new);

    // the description will limit the submission to a test
    RequestResponse response = GNPSUtils.submitMASSTJob("MZMINE_TEST_SUBMISSION_ADD_TEST_PART", dps,
        1044.66, MasstDatabase.ALL, 0.7, 1d, 0.5, 6, false, "", "", "", false);
    Assert.assertEquals("Test Passed", response.response());
    Assert.assertTrue(response.isSuccess());
  }
}
