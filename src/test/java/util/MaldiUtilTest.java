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

package util;

import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.MaldiSpotInfo;
import io.github.mzmine.modules.tools.timstofmaldiacq.TimsTOFAcquisitionUtils;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MaldiUtilTest {

  @Test
  void testOffsets() {
    Assertions.assertTrue(Arrays.equals(new int[]{20, 0},
        TimsTOFAcquisitionUtils.getOffsetsForIncrementCounter(1, 4, 20, 20)));

    Assertions.assertTrue(Arrays.equals(new int[]{3 * 20, 1 * 20},
        TimsTOFAcquisitionUtils.getOffsetsForIncrementCounter(7, 4, 20, 20)));

    Assertions.assertTrue(Arrays.equals(new int[]{2 * 20, 2 * 20},
        TimsTOFAcquisitionUtils.getOffsetsForIncrementCounter(10, 4, 20, 20)));
  }

  @Test
  void testDistance() {

    final MaldiSpotInfo spot1_1 = new MaldiSpotInfo(0, 0, "", 1, 1, 1, 0, 0, 0);
    final MaldiSpotInfo spot25_25 = new MaldiSpotInfo(0, 0, "", 1, 25, 25, 0, 0, 0);
    final MaldiSpotInfo spot50_50 = new MaldiSpotInfo(0, 0, "", 1, 50, 50, 0, 0, 0);
    final MaldiSpotInfo spot50_0 = new MaldiSpotInfo(0, 0, "", 1, 50, 0, 0, 0, 0);
    final MaldiSpotInfo spot0_50 = new MaldiSpotInfo(0, 0, "", 1, 0, 50, 0, 0, 0);
    final MaldiSpotInfo spot23_34 = new MaldiSpotInfo(0, 0, "", 1, 23, 34, 0, 0, 0);

    Assertions.assertEquals(69.29646455628166,
        TimsTOFAcquisitionUtils.getDistanceForSpots(spot1_1, spot50_50));
    Assertions.assertEquals(33.94112549695428,
        TimsTOFAcquisitionUtils.getDistanceForSpots(spot1_1, spot25_25));

    Assertions.assertTrue(TimsTOFAcquisitionUtils.checkDistanceForSpot(spot1_1, spot50_50, 60));
    Assertions.assertFalse(TimsTOFAcquisitionUtils.checkDistanceForSpot(spot1_1, spot25_25, 60));

    Assertions.assertTrue(TimsTOFAcquisitionUtils.checkDistanceForSpots(30,
        List.of(spot25_25, spot50_50, spot50_0, spot0_50), spot1_1));
    Assertions.assertFalse(TimsTOFAcquisitionUtils.checkDistanceForSpots(35,
        List.of(spot25_25, spot50_50, spot50_0, spot0_50), spot1_1));
    Assertions.assertFalse(TimsTOFAcquisitionUtils.checkDistanceForSpots(30,
        List.of(spot1_1, spot25_25, spot50_50, spot50_0, spot0_50), spot23_34));
  }

}
