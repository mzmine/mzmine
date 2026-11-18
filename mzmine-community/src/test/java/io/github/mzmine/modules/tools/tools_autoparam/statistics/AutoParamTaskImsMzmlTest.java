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

package io.github.mzmine.modules.tools.tools_autoparam.statistics;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import testutils.MZmineTestUtil;

@DisabledOnOs(OS.MAC)
class AutoParamTaskImsMzmlTest {

  /**
   * Two MS1 frames and one MS2 frame, the frames of mzML files contain no summed spectra.
   */
  private static final String FILE = "rawdatafiles/additional/lc-tims-ms-pasef-a_2frames.mzML";

  @BeforeAll
  static void initialize() throws InterruptedException {
    MZmineTestUtil.startMzmineCore();
    MZmineTestUtil.importFiles(List.of(FILE), 60);
  }

  @Test
  void mzmlFramesGetMergedMs1Spectra() {
    final RawDataFile file = MZmineTestUtil.streamDataFiles(List.of(FILE)).findFirst()
        .orElseThrow();
    final IMSRawDataFile imsFile = Assertions.assertInstanceOf(IMSRawDataFile.class, file);
    final List<? extends Frame> ms1Frames = ScanSelection.MS1.getMatchingScans(imsFile.getFrames());
    Assertions.assertFalse(ms1Frames.isEmpty());
    Assertions.assertTrue(ms1Frames.stream().allMatch(frame -> frame.getNumberOfDataPoints() == 0));

    final DataFileStatistics stats = new AutoParamTask(null, Instant.now(),
        AutoParamParameters.of(List.of(file)), AutoParamModule.class, file, null,
        false).runAndGet();
    Assertions.assertNotNull(stats);

    // IMS files are only searched within the high-resolution tolerances
    final List<MZTolerance> imsTolerances = MzToleranceSearchOptions.ALL_TOLERANCE_OPTIONS
        .subList(0, MzToleranceSearchOptions.MAX_HIGH_RESOLUTION_INDEX + 1);
    for (final MzToMzTolerancePair pair : stats.getBestTolerances()) {
      Assertions.assertTrue(imsTolerances.contains(pair.tolerance()), pair::toString);
    }

    for (final Frame frame : imsFile.getFrames()) {
      if (frame.getMSLevel() == 1) {
        Assertions.assertTrue(frame.getNumberOfDataPoints() > 0,
            "MS1 frame %d has no merged spectrum".formatted(frame.getFrameId()));
        // the reference merge is restored after the tolerance search
        Assertions.assertInstanceOf(ScanPointerMassList.class, frame.getMassList());
      } else {
        Assertions.assertEquals(0, frame.getNumberOfDataPoints(),
            "MSn frame %d was merged".formatted(frame.getFrameId()));
      }
    }
  }
}
