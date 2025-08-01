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
import static org.mockito.Mockito.doReturn;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectors;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.factor_of_lowest.FactorOfLowestMassDetectorParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.processors.MassDetectorMsProcessor;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MassDetectorMsProcessorTest {

  @Mock
  Scan scan;

  double[] input = new double[]{3d, 5d, 10d, 11d, 100d, 200d, 300d, 400d};
  double[] expected = new double[]{10d, 11d, 100d, 200d, 300d, 400d};
  private ParameterSet advanced;

  @BeforeEach
  void setUp() {
    doReturn(2).when(scan).getMSLevel();

    advanced = new AdvancedSpectraImportParameters().cloneParameterSet();
    advanced.setParameter(AdvancedSpectraImportParameters.ms2MassDetection, true);

    ParameterSet mdParam = MassDetectors.FACTOR_OF_LOWEST.getModuleParameters().cloneParameterSet();
    mdParam.setParameter(FactorOfLowestMassDetectorParameters.noiseFactor, 2d);
    advanced.getParameter(AdvancedSpectraImportParameters.ms2MassDetection).getEmbeddedParameter()
        .setValue(MassDetectors.FACTOR_OF_LOWEST, mdParam);
  }

  @Test
  void testProcessScan() {
    var indata = data(input);
    SimpleSpectralArrays result = new MassDetectorMsProcessor(advanced).processScan(scan, indata);
    assertArrayEquals(expected, result.mzs());
  }

  @NotNull
  private SimpleSpectralArrays data(final double[] input) {
    return new SimpleSpectralArrays(input, input);
  }
}