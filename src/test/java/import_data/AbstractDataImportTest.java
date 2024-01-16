/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package import_data;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import testutils.MZmineTestUtil;

@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractDataImportTest {

  private static final Logger logger = Logger.getLogger(AbstractDataImportTest.class.getName());

  public AbstractDataImportTest() {
  }

  public abstract List<String> getFileNames();

  /**
   * Init MZmine core in headless mode with the options -r (keep running) and -m (keep in memory)
   */
  @BeforeAll
  public void init() {
    logger.info("Getting project");
    try {
      MZmineTestUtil.importFiles(getFileNames(), 60);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @AfterAll
  public void tearDown() {
    //clean the project after this integration test
    MZmineTestUtil.cleanProject();
  }

  @Test
//    @Order(1)
//  @Disabled
  @DisplayName("Test data import of mzML and mzXML without advanced parameters")
  void dataImportTest() {
    Map<String, DataFileStats> stats = DataFileStatsIO.readJson(getClass());
    DataImportTestUtils.testDataStatistics(getFileNames(), stats);
  }
}
