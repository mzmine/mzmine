/*
 * Copyright (c) 2004-2026 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_diffms;

import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Conventions for bundling a ready-to-run Python runtime with MZmine (e.g., via
 * conda-pack).
 * <p>
 * For the first customer roll-outs, the simplest approach is to ship an
 * extracted environment under
 * {@code external_tools/diffms/runtime/} and let MZmine auto-detect the python
 * executable.
 */
public final class DiffMSRuntimeFiles {

  private DiffMSRuntimeFiles() {
  }

  /**
   * Expected environment root under the MZmine distribution.
   */
  public static @Nullable File getBundledRuntimeRootDir() {
    return FileAndPathUtil.resolveInExternalToolsDir("diffms/runtime/");
  }

  /**
   * Tries to locate a bundled Python executable in
   * {@link #getBundledRuntimeRootDir()}.
   * <p>
   * Supports common conda/conda-pack layouts across platforms.
   */
  public static @Nullable File findBundledPythonExecutable() {
    final File root = getBundledRuntimeRootDir();
    if (root == null || !root.isDirectory()) {
      return null;
    }

    final List<File> candidates = List.of( //
        new File(root, "bin/python"), // unix conda env
        new File(root, "python.exe"), // windows conda env root
        new File(root, "Scripts/python.exe"), // windows conda env
        new File(root, "bin/python3"), // occasionally present
        new File(root, "python") // fallback
    );

    for (final File f : candidates) {
      if (f.isFile()) {
        return f;
      }
    }
    return null;
  }
}
