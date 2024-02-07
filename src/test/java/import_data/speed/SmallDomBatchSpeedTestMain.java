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

package import_data.speed;

import java.util.List;

/**
 * Speed test to log the time required to import files. Just define List of String that point to
 * resources files or absolute paths and add a test to the main method. The results will be appended
 * to speedTestFile define a full path here as it will otherwise be relative to build/target path.
 * When working with local files - put those into the import_data/local folder and create a new run
 * script similar to the main method here
 * <p>
 * Be sure to specify VM options -Xms16g -Xmx16g or similar to start with fixed memory
 */
public class SmallDomBatchSpeedTestMain {

  public static final List<String> samples = List.of("""
      rawdatafiles/DOM_a.mzML
      rawdatafiles/DOM_a_invalid_chars.mzML
      rawdatafiles/DOM_a_invalid_header.mzML
      rawdatafiles/DOM_b.mzXML
      rawdatafiles/DOM_b_invalid_header.mzXML
      """.split("\n"));

  public static void main(String[] args) {
    String speedTestFile = "D:\\git\\mzmine3\\src\\test\\java\\import_data\\local\\speed.jsonlines";
    String description = "RAM=16GB, MZmine3.10, smallDOM";
    // keep running and all in memory
    String inMemory = "all";
//    String inMemory = "none";
    boolean headLess = false;

    String batchFile = "rawdatafiles/test_batch_small.xml";

    List<BatchSpeedJob> jobs = List.of(new BatchSpeedJob(description, 3, batchFile, samples));

    BatchSpeedTestMain.startAndRunTests(speedTestFile, headLess, inMemory, jobs);
  }

}
