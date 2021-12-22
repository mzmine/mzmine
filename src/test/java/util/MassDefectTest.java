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

import io.github.mzmine.parameters.parametertypes.massdefect.MassDefectFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MassDefectTest {

  @Test
  public void testMassDefectFilter() {
    final MassDefectFilter filter = new MassDefectFilter(0.1000, 0.500);
    Assertions.assertTrue(filter.contains(542.234));
    Assertions.assertTrue(filter.contains(542.500));
    Assertions.assertFalse(filter.contains(542.5001));
    Assertions.assertFalse(filter.contains(542.6730));

    final MassDefectFilter filter2 = new MassDefectFilter(0.9, 0.1);
    Assertions.assertTrue(filter2.contains(412.9612));
    Assertions.assertTrue(filter2.contains(412.0832));
    Assertions.assertFalse(filter2.contains(412.8999));

    final MassDefectFilter filter3 = new MassDefectFilter(0.3, 0.85);
    Assertions.assertFalse(filter3.contains(785.8999));
    Assertions.assertTrue(filter3.contains(785.5398));
    Assertions.assertFalse(filter3.contains(785.2846));

    Assertions.assertThrows(IllegalArgumentException.class, () -> new MassDefectFilter(1.3, 0.3));
    Assertions.assertThrows(IllegalArgumentException.class, () -> new MassDefectFilter(-1.1, -0.2));
    Assertions.assertThrows(IllegalArgumentException.class, () -> new MassDefectFilter(0.9, -0.2));
  }
}
