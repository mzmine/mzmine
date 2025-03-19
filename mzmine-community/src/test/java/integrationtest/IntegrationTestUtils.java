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

package integrationtest;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.io.export_features_csv.CSVExportModularModule;
import io.github.mzmine.modules.io.export_features_csv.CSVExportModularParameters;
import io.github.mzmine.modules.tools.output_compare_csv.CheckResult;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.util.XMLUtils;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public class IntegrationTestUtils {

  public static List<CheckResult> runBatchCompareToCsv(File batchFile, File csvFile, File tempDir) {

    MZmineProject project = new MZmineProjectImpl();
    final List<String> batchLoadErrors = new ArrayList<>();
    final BatchQueue queue;
    try {
      queue = BatchQueue.loadFromXml(XMLUtils.load(batchFile).getDocumentElement(), batchLoadErrors,
          true);
    } catch (ParserConfigurationException | IOException | SAXException e) {
      throw new RuntimeException("Error while loading batch file" + e);
    }
    final MZmineProcessingStep<MZmineProcessingModule> last = queue.getLast();
    if (last.getModule().getName().equals(CSVExportModularModule.MODULE_NAME)) {
      final ParameterSet parameters = last.getParameterSet();
      final File originalExportFile = parameters.getValue(CSVExportModularParameters.filename);

    }

    BatchModeModule.runBatch(project, batchFile, null, null, null, null, Instant.now());
  }
}
