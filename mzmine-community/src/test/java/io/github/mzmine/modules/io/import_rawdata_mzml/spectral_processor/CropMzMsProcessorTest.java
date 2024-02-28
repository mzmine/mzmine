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

package io.github.mzmine.modules.io.import_rawdata_mzml.spectral_processor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.doReturn;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.processors.CropMzMsProcessor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class CropMzMsProcessorTest {

  @Mock
  Scan scan;

  double[] input = new double[]{0d, 5d, 10d, 11d};
  double[] result1_10 = new double[]{5d, 10d};
  double[] result_5only = new double[]{ 5d};

  @BeforeEach
  void setUp() {
    doReturn(1).when(scan).getMSLevel();
  }

  @Test
  void testProcessScan() {
    var indata = data(input);
    SimpleSpectralArrays result =  new CropMzMsProcessor(1d, 10d).processScan(scan, indata);
    assertNotEquals(indata, result);
    assertArrayEquals(data(result1_10).mzs(), result.mzs());

    result =  new CropMzMsProcessor(-1000d, 1000d).processScan(scan, indata);
    assertArrayEquals(indata.mzs(), result.mzs());

    result =  new CropMzMsProcessor(4d, 6d).processScan(scan, indata);
    assertArrayEquals(data(result_5only).mzs(), result.mzs());

    result =  new CropMzMsProcessor(0d, 11d).processScan(scan, indata);
    assertArrayEquals(indata.mzs(), result.mzs());

    result =  new CropMzMsProcessor(5d, 10d).processScan(scan, indata);
    assertArrayEquals(data(result1_10).mzs(), result.mzs());
  }

  @NotNull
  private SimpleSpectralArrays data(final double[] input) {
    return new SimpleSpectralArrays(input, input);
  }

}