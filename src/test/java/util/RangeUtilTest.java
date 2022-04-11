/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

/*
 * Code created was by or on behalf of Syngenta and is released under the open source license in use
 * for the pre-existing code or project. Syngenta does not assert ownership or copyright any over
 * pre-existing work.
 */

package util;

import com.google.common.collect.Range;
import io.github.mzmine.util.RangeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RangeUtilTest {

  @Test
  public void testDecimalRange() {
    Assertions
        .assertEquals(Range.closedOpen(5.255, 5.256), RangeUtils.getRangeToCeilDecimal("5.255"));
    Assertions.assertEquals(Range.closedOpen(5.25, 5.26), RangeUtils.getRangeToCeilDecimal("5.25"));
    Assertions.assertEquals(Range.closedOpen(5.5, 5.6), RangeUtils.getRangeToCeilDecimal("5.5"));
    Assertions.assertEquals(Range.closedOpen(5d, 6d), RangeUtils.getRangeToCeilDecimal("5."));
    Assertions.assertEquals(Range.closedOpen(5d, 6d), RangeUtils.getRangeToCeilDecimal("5"));
  }
}
