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

import io.github.mzmine.modules.io.import_rawdata_mzml.spectral_processor.processors.SortByMzMsProcessor;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration that controls scan filtering and processing during data import
 *
 * @param scanFilter
 * @param processor
 * @param applyMassDetection
 */
public record ScanImportProcessorConfig(ScanSelection scanFilter, MsProcessorList processor,
                                        boolean applyMassDetection) {

  public static ScanImportProcessorConfig createDefault() {
    List<MsProcessor> processors = new ArrayList<>();
    processors.add(new SortByMzMsProcessor());
    return new ScanImportProcessorConfig(ScanSelection.ALL_SCANS, new MsProcessorList(processors),
        false);
  }

  @Override
  public String toString() {
    return "ScanImportProcessorConfig: scanFilter=%s, applyMassDetection=%s".formatted(scanFilter,
        applyMassDetection) + "\n" + processor.description();
  }

}
