/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_mzml;

import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds final LC/MS scans until their ordering has been checked, releasing XML metadata per scan.
 */
final class MzMLScanAssembler {

  private final File source;
  private final @Nullable MemoryMapStorage storage;
  private @Nullable RawDataFileImpl raw;
  private final List<SimpleScan> scans = new ArrayList<>();
  private boolean ordered = true;

  MzMLScanAssembler(@NotNull final File source, @Nullable final MemoryMapStorage storage) {
    this.source = source;
    this.storage = storage;
  }

  private @NotNull RawDataFileImpl file() {
    if (raw == null) {
      raw = new RawDataFileImpl(source.getName(), source.getAbsolutePath(), storage);
    }
    return raw;
  }

  void add(@NotNull final BuildingMzMLMsScan record, final boolean massDetected) {
    final SimpleScan scan = ConversionUtils.mzmlScanToSimpleScan(file(), record);
    if (massDetected) {
      scan.addMassList(new ScanPointerMassList(scan));
    }
    if (ordered && !scans.isEmpty()) {
      final SimpleScan previous = scans.getLast();
      ordered = scan.getRetentionTime() > previous.getRetentionTime()
          && scan.getScanNumber() > previous.getScanNumber();
    }
    scans.add(scan);
  }

  int size() {
    return scans.size();
  }

  /**
   * @return the LC/MS file with scans in their final order
   */
  @NotNull RawDataFileImpl finish(@NotNull final BooleanSupplier canceled) throws IOException {
    final RawDataFileImpl file = file();
    if (!ordered) {
      scans.sort(Comparator.comparingDouble(SimpleScan::getRetentionTime));
    }
    for (int i = 0; i < scans.size(); i++) {
      if (canceled.getAsBoolean()) {
        break;
      }
      final SimpleScan scan = scans.get(i);
      if (!ordered) {
        scan.setScanNumber(i + 1);
      }
      file.addScan(scan);
    }
    scans.clear();
    return file;
  }
}
