/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import io.github.mzmine.modules.io.import_rawdata_masslynx.MassLynxLib;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

public class WatersImportTest {

  private static final Logger logger = Logger.getLogger(WatersImportTest.class.getName());

  @Test
  void test() {

    final Arena arena = Arena.ofAuto();
    String path = "D:/OneDrive - mzio GmbH/mzio/Example data/Waters/mse_20180205_0125.raw";
    final MemorySegment pathBuffer = arena.allocateFrom(path);
    final MemorySegment fileHandle = MassLynxLib.openFile(pathBuffer);

    final int numberOfFunctions = MassLynxLib.getNumberOfFunctions(fileHandle);
    logger.info("Number of functions: " + numberOfFunctions);
  }

}
