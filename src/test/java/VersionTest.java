/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;
import io.github.mzmine.main.MZmineCore;
import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class VersionTest {

  @Test
  public void testVersion() throws IOException {
    final Semver snapshot = new Semver("3.0.0-SNAPSHOT", SemverType.LOOSE);
    final Semver beta1 = new Semver("3.0.1-beta");
    final Semver beta2 = new Semver("3.0.2-beta");
    final Semver beta11 = new Semver("3.0.11-beta");
    Assertions.assertTrue(beta1.isLowerThan(beta2));
    Assertions.assertTrue(beta2.isLowerThan(beta11));
    Assertions.assertTrue(snapshot.isLowerThan(beta2));

    ClassLoader myClassLoader = MZmineCore.class.getClassLoader();
    Properties properties = new Properties();
    properties.load(myClassLoader.getResourceAsStream("mzmineversion.properties"));
    final Semver semver2 = new Semver(properties.getProperty("version.semver"), SemverType.LOOSE);
    System.out.println(semver2.toString());
  }
}
