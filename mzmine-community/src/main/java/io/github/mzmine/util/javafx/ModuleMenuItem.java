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

package io.github.mzmine.util.javafx;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineRunnableModule;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCodeCombination;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ModuleMenuItem extends MenuItem {

  private static final Logger logger = Logger.getLogger(ModuleMenuItem.class.getName());

  private final Class<? extends MZmineRunnableModule> moduleClass;

  public ModuleMenuItem(@NotNull Class<? extends MZmineRunnableModule> moduleClass) {
    this(moduleClass, null);
  }
  
  public ModuleMenuItem(@NotNull Class<? extends MZmineRunnableModule> moduleClass,
      @Nullable KeyCodeCombination accelerator) {
    super(getText(moduleClass));

    this.moduleClass = moduleClass;
    setOnAction(_ -> MZmineCore.setupAndRunModule(moduleClass));

    if (accelerator != null) {
      setAccelerator(accelerator);
    }
  }

  public Class<? extends MZmineRunnableModule> getModuleClass() {
    return moduleClass;
  }

  private static String getText(@NotNull Class<? extends MZmineRunnableModule> moduleClass) {
    try {
      final MZmineRunnableModule moduleInstance = MZmineCore.getModuleInstance(moduleClass);
      return moduleInstance.getName();
    } catch (Exception e) {
      logger.log(Level.INFO, "Error instantiating module class %s".formatted(moduleClass.getName()),
          e);
      return moduleClass.getSimpleName();
    }
  }
}
