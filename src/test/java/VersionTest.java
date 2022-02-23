/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
