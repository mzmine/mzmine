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

package io.github.mzmine.util.io;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;
import java.io.InputStream;
import java.util.Properties;
import org.jetbrains.annotations.NotNull;

public class SemverVersionReader {

  @NotNull
  public static Semver getMZmineVersion() {
    try {
      ClassLoader myClassLoader = SemverVersionReader.class.getClassLoader();
      InputStream inStream = myClassLoader.getResourceAsStream("mzmineversion.properties");
      if (inStream == null) {
        return new Semver("4-SNAPSHOT", SemverType.LOOSE);
      }
      Properties properties = new Properties();
      properties.load(inStream);
      String versionString = properties.getProperty("version.semver");
      if ((versionString == null) || (versionString.startsWith("$"))) {
        return new Semver("4-SNAPSHOT", SemverType.LOOSE);
      }
      return new Semver(versionString, SemverType.LOOSE);
    } catch (Exception e) {
      return new Semver("4-SNAPSHOT", SemverType.LOOSE);
    }
  }

  @NotNull
  public static Semver getMZmineProVersion() {
    try {
      ClassLoader classLoader = SemverVersionReader.class.getClassLoader();
      InputStream inStream = classLoader.getResourceAsStream("mzmineproversion.properties");
      if (inStream == null) {
        return new Semver("4-NONE", SemverType.LOOSE);
      }
      Properties properties = new Properties();
      properties.load(inStream);
      String versionString = properties.getProperty("version.semver");
      if ((versionString == null) || (versionString.startsWith("$"))) {
        return new Semver("4-SNAPSHOT", SemverType.LOOSE);
      }
      return new Semver(versionString, SemverType.LOOSE);
    } catch (Exception e) {
      return new Semver("4-SNAPSHOT", SemverType.LOOSE);
    }
  }
}
