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

package io.github.mzmine.modules.io.import_rawdata_all.spectral_processor;

import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.processors.MassDetectorMsProcessor;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.processors.SortByMzMsProcessor;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration that controls scan filtering and processing during data import
 */
public final class ScanImportProcessorConfig {

  private final ScanSelection scanFilter;
  private final MsProcessorList processor;
  private final boolean ms1MassDetectActive;
  private final boolean ms2MassDetectActive;

  public ScanImportProcessorConfig(final ScanSelection scanFilter,
      final MsProcessorList processor) {
    this.scanFilter = scanFilter;
    this.processor = processor;
    Optional<MassDetectorMsProcessor> md = processor.findFirst(MassDetectorMsProcessor.class);
    ms1MassDetectActive = md.isPresent() && md.get().isMs1Active();
    ms2MassDetectActive = md.isPresent() && md.get().isMsnActive();
  }

  /**
   * Will only sort the scans by mz and apply no further processing
   *
   * @return the default scan processor config
   */
  @NotNull
  public static ScanImportProcessorConfig createDefault() {
    List<MsProcessor> processors = new ArrayList<>();
    processors.add(new SortByMzMsProcessor());
    return new ScanImportProcessorConfig(ScanSelection.ALL_SCANS, new MsProcessorList(processors));
  }

  @Override
  public String toString() {
    return "ScanImportProcessorConfig: scanFilter=%s\napplyMassDetection: MS1=%s; MS2..n=%s\n%s".formatted(
        scanFilter, ms1MassDetectActive, ms2MassDetectActive, processor.description());
  }

  public ScanSelection scanFilter() {
    return scanFilter;
  }

  public MsProcessorList processor() {
    return processor;
  }

  public boolean isMassDetectActive(int msLevel) {
    return (msLevel <= 1 && ms1MassDetectActive) || (msLevel > 1 && ms2MassDetectActive);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (ScanImportProcessorConfig) obj;
    return Objects.equals(this.scanFilter, that.scanFilter) && Objects.equals(this.processor,
        that.processor) && Objects.equals(this.ms1MassDetectActive, that.ms1MassDetectActive)
           && Objects.equals(this.ms2MassDetectActive, that.ms2MassDetectActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scanFilter, processor, ms1MassDetectActive, ms2MassDetectActive);
  }


  public boolean hasProcessors() {
    return processor().size() > 0;
  }
}
