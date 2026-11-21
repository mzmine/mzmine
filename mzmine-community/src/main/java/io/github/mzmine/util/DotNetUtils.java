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

import java.util.OptionalInt;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects installed Microsoft .NET runtimes. Shared by vendor data importers whose native bridges
 * require a specific .NET version — e.g. the SCIEX Clearcore server and the Agilent MassHunter
 * bridge ({@code AgilentBridge.exe}, built for .NET Framework 4.8).
 */
public class DotNetUtils {

  /**
   * Minimum {@code Release} registry key for .NET Framework 4.7.2.
   */
  public static final int NET_FRAMEWORK_472_RELEASE_KEY = 461808;
  /**
   * Minimum {@code Release} registry key for .NET Framework 4.8. Later 4.8.x builds and newer
   * Windows versions use larger keys, so a {@code >=} comparison covers "4.8 or later".
   */
  public static final int NET_FRAMEWORK_48_RELEASE_KEY = 528040;

  private static final Logger logger = Logger.getLogger(DotNetUtils.class.getName());

  private static final String WINDOWS_DOT_NET_RELEASE_REGISTRY_PATH = "HKLM\\SOFTWARE\\Microsoft\\NET Framework Setup\\NDP\\v4\\Full";
  private static final Pattern WINDOWS_DOT_NET_RELEASE_PATTERN = Pattern.compile(
      "(?m)^\\s*Release\\s+REG_DWORD\\s+(0x[0-9a-fA-F]+|\\d+)\\s*$");
  private static final Pattern LINUX_DOT_NET_RUNTIME_PATTERN = Pattern.compile(
      "(?m)^Microsoft\\.NETCore\\.App\\s+([\\d]+)\\.\\d+\\.\\d+\\b.*$");


  private DotNetUtils() {
  }

  /**
   * The installed .NET Framework {@code Release} key, read from the 64-bit registry view, or empty
   * if it cannot be read (not Windows, key absent, or output unparsable). Compare against the
   * {@code NET_FRAMEWORK_*_RELEASE_KEY} constants to test for a minimum version.
   */
  public static OptionalInt windowsFrameworkReleaseKey() {
    // decision: query the 64-bit registry view because the bundled vendor bridges are x64.
    final String output = ShellUtils.runGetOutput("reg", "query",
        WINDOWS_DOT_NET_RELEASE_REGISTRY_PATH, "/v", "Release", "/reg:64");
    if (output == null || output.isBlank()) {
      return OptionalInt.empty();
    }
    final Matcher matcher = WINDOWS_DOT_NET_RELEASE_PATTERN.matcher(output);
    if (!matcher.find()) {
      return OptionalInt.empty();
    }
    try {
      return OptionalInt.of(Integer.decode(matcher.group(1)));
    } catch (NumberFormatException e) {
      logger.fine("Cannot parse .NET Framework release key from registry output: " + output);
      return OptionalInt.empty();
    }
  }

  /**
   * Whether a .NET Framework with at least {@code minReleaseKey} is installed. Windows only; always
   * false on other platforms (the registry query yields no output).
   */
  public static boolean isWindowsFrameworkInstalled(int minReleaseKey) {
    final OptionalInt key = windowsFrameworkReleaseKey();
    return key.isPresent() && key.getAsInt() >= minReleaseKey;
  }

  /**
   * Whether a .NET (Core) runtime with at least major version {@code minMajorVersion} is available
   * on the PATH, via {@code dotnet --list-runtimes} (used on Linux).
   */
  public static boolean isLinuxRuntimeInstalled(int minMajorVersion) {
    final String output = ShellUtils.runGetOutput("dotnet", "--list-runtimes");
    if (output == null || output.isBlank()) {
      return false;
    }
    final Matcher matcher = LINUX_DOT_NET_RUNTIME_PATTERN.matcher(output);
    if (matcher.find()) {
      try {
        return Integer.parseInt(matcher.group(1)) >= minMajorVersion;
      } catch (NumberFormatException e) {
        logger.fine("Cannot parse .NET runtime version from output: " + output);
      }
    }
    return false;
  }
}
