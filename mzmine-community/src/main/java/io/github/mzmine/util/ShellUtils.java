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

package io.github.mzmine.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.jetbrains.annotations.Nullable;

public class ShellUtils {

  /**
   * Runs a shell command and retrieves the output.
   */
  public static @Nullable String runGetOutput(String... command) {
    try {
      return ShellRunner.run(command);
    } catch (Exception e) {
      return null;
    }
  }

  /// Source - https://stackoverflow.com/a/57949752 Posted by Gili, modified by community. See post
  /// 'Timeline' for change history Retrieved 2026-05-05, License - CC BY-SA 4.0
  private final class ShellRunner {

    private static final String NEWLINE = System.getProperty("line.separator");

    /**
     * @param command the command to run
     * @return the output of the command
     * @throws IOException if an I/O error occurs
     */
    public static String run(String... command) throws IOException {
      ProcessBuilder pb = new ProcessBuilder(command).redirectErrorStream(true);
      Process process = pb.start();
      StringBuilder result = new StringBuilder(2048);
      try (BufferedReader in = new BufferedReader(
          new InputStreamReader(process.getInputStream()))) {
        while (true) {
          String line = in.readLine();
          if (line == null) {
            break;
          }
          result.append(line).append(NEWLINE);
        }
      }
      return result.toString();
    }

    /**
     * Prevent construction.
     */
    private ShellRunner() {
    }
  }

}
