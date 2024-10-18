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

package io.github.mzmine.modules.io.download;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mzmine.modules.io.download.DownloadAsset.Builder;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class FileDownloadTaskTest {

  //  @TempDir
  File tmpDir = new File("D:\\tmp\\downloadTest\\");

  @Test
  void testDownloadZenodoArchive() {
    var asset = Builder.ofZenodo(ExternalAsset.MS2DEEPSCORE, "12628368").create();
    var task = new FileDownloadTask(asset, tmpDir);
    task.run();
    assertEquals(TaskStatus.FINISHED, task.getStatus());
  }

  @Test
  void testDownloadZenodoFileFilter() {
    var asset = Builder.ofZenodo(ExternalAsset.MSnLib, "11163380") //
//        .fileNamePattern(".*_neg_ms2.mgf").create();
        .fileNamePattern(".*otavapep_neg_ms2.mgf").create();
    var task = new FileDownloadTask(asset);
    task.run();
    assertEquals(TaskStatus.FINISHED, task.getStatus());
  }

}