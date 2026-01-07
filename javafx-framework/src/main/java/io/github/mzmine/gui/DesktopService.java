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

package io.github.mzmine.gui;

import java.io.Console;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Get the desktop that may be headless or GUI
 */
public class DesktopService {

  public static Desktop instance = new DefaultHeadlessDesktop();

  public static synchronized void setDesktop(@NotNull Desktop desktop) {
    instance = desktop;
  }

  @NotNull
  public static Desktop getDesktop() {
    return instance;
  }

  public static boolean isHeadLess() {
    return instance.isHeadLess();
  }

  public static boolean isGUI() {
    return instance.isGUI();
  }

  /**
   * True if {@link System#console#isTerminal} is true and shows that console input is available.
   * False if not terminal or if console==null
   */
  public static boolean hasTerminalInput() {
    final @Nullable Console console = System.console();
    return console != null && console.isTerminal();
  }
}
