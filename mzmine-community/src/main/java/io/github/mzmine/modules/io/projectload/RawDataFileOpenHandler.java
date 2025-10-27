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

package io.github.mzmine.modules.io.projectload;

import com.vdurmont.semver4j.Semver;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.io.projectload.version_3_0.RawDataFileOpenHandler_3_0;
import io.github.mzmine.taskcontrol.Task;
import java.io.InputStream;
import java.time.Instant;
import java.util.zip.ZipFile;
import org.jetbrains.annotations.NotNull;

public interface RawDataFileOpenHandler extends Task {

  public static RawDataFileOpenHandler forVersion(Semver versionString, @NotNull Instant moduleCallDate) {
    return new RawDataFileOpenHandler_3_0(moduleCallDate);
  }

  void setBatchFileStream(InputStream batchFileStream);

  void setProject(MZmineProject project);

  void setZipFile(ZipFile zipFile);
}
