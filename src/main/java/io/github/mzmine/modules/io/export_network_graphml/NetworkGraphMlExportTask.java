/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.io.export_network_graphml;

import static io.github.mzmine.util.files.FileAndPathUtil.getRealFilePathWithSuffix;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.visualization.networking.visual.FeatureNetworkGenerator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.graphstream.stream.file.FileSinkGraphML;
import org.jetbrains.annotations.NotNull;

public class NetworkGraphMlExportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(NetworkGraphMlExportTask.class.getName());
  private final FeatureList[] featureLists;
  private final File fileName;
  private final ParameterSet parameters;
  private final double step;

  private double progress;

  public NetworkGraphMlExportTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.featureLists = parameters.getValue(NetworkGraphMlExportParameters.featureLists)
        .getMatchingFeatureLists();
    fileName = parameters.getValue(NetworkGraphMlExportParameters.filename);
    this.parameters = parameters;
    // four steps each
    step = 1.0 / 4.0 / featureLists.length;
  }

  @Override
  public String getTaskDescription() {
    return "Exporting molecular networks to graphml. feature list(s) " + Arrays.toString(
        featureLists);
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // Shall export several files?
    String plNamePattern = "{}";
    boolean substitute = fileName.getPath().contains(plNamePattern);

    FileSinkGraphML writer = new FileSinkGraphML();
    // Process feature lists
    for (FeatureList flist : featureLists) {
      // Cancel?
      if (isCanceled()) {
        return;
      }

      boolean hasIonIdentity = flist.stream().anyMatch(FeatureListRow::hasIonIdentity);

      // Filename
      File curFile = fileName;
      if (substitute) {
        // Cleanup from illegal filename characters
        String cleanPlName = flist.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        // Substitute
        String newFilename = fileName.getPath()
            .replaceAll(Pattern.quote(plNamePattern), cleanPlName);
        curFile = FileAndPathUtil.getRealFilePath(new File(newFilename), "graphml");
      }

      try {
        exportGraphMl(writer, flist, false, curFile);

        if (hasIonIdentity) {
          exportGraphMl(writer, flist, true, curFile);
        }
      } catch (IOException e) {
        logger.log(Level.WARNING, "Cannot write graphml" + e.getMessage(), e);
        setStatus(TaskStatus.ERROR);
        setErrorMessage(e.getMessage());
        return;
      }

      // If feature list substitution pattern wasn't found,
      // treat one feature list only
      if (!substitute) {
        break;
      }
    }

    if (getStatus() == TaskStatus.PROCESSING) {
      SimpleFeatureListAppliedMethod appliedMethod = new SimpleFeatureListAppliedMethod(
          NetworkGraphMlExportModule.class, parameters, moduleCallDate);
      for (final FeatureList featureList : featureLists) {
        featureList.addDescriptionOfAppliedTask(appliedMethod);
      }

      setStatus(TaskStatus.FINISHED);
    }
  }

  private void exportGraphMl(final FileSinkGraphML saveGraphML, final FeatureList featureList,
      boolean useIonIdentity, final File curFile) throws IOException {
    File file = getRealFilePathWithSuffix(curFile, useIonIdentity ? "_iimn" : "_fbmn", "graphml");
    FileAndPathUtil.createDirectory(file.getParentFile());

    var graph = new FeatureNetworkGenerator().createNewGraph(file.getName(), featureList,
        useIonIdentity, true, false);
    progress += step;

    saveGraphML.writeAll(graph, file.getAbsolutePath());
    progress += step;
  }

}
