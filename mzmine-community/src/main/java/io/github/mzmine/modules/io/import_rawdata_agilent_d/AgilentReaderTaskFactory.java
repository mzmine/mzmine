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

package io.github.mzmine.modules.io.import_rawdata_agilent_d;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Creates the import task of one Agilent {@code .d} file for
 * {@link io.github.mzmine.gui.preferences.AgilentImportOptions#CUSTOM}. The implementation is
 * provided by mzmine pro and registered at startup with
 * {@link #register(AgilentReaderTaskFactory)}; without it, {@link AgilentImportTaskDelegator} falls
 * back to the native AgilentReader.
 */
@FunctionalInterface
public interface AgilentReaderTaskFactory {

  @NotNull RawDataImportTask create(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull File file,
      @NotNull Class<? extends MZmineModule> module, @NotNull ParameterSet parameters,
      @NotNull MZmineProject project, @Nullable ScanImportProcessorConfig processor);

  /**
   * Registers the factory of the mzio reader import, replacing a previous one.
   */
  static void register(@Nullable AgilentReaderTaskFactory factory) {
    Registry.FACTORY.set(factory);
  }

  /**
   * @return the registered factory or null if the mzio reader is not available
   */
  static @Nullable AgilentReaderTaskFactory registered() {
    return Registry.FACTORY.get();
  }

  final class Registry {

    private static final AtomicReference<AgilentReaderTaskFactory> FACTORY = new AtomicReference<>();

    private Registry() {
    }
  }
}
