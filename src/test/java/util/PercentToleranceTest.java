/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package util;

import io.github.mzmine.parameters.parametertypes.tolerances.PercentTolerance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PercentToleranceTest {

  @Test
  void testPercentTolerance() {
    final PercentTolerance tol = new PercentTolerance(0.05);

    Assertions.assertTrue(tol.matches(100, 105));
    Assertions.assertTrue(tol.matches(100, 95));
    Assertions.assertFalse(tol.matches(100, 106));
    Assertions.assertFalse(tol.matches(100d, 105.0001d));
    Assertions.assertTrue(tol.matches(100f, 101.0001f));

    Assertions.assertTrue(tol.matches(100.003d, 105L));
  }
}
