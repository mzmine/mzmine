/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 */

package util.lipidannotationtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LipidAnnotationLevelTest {

  @Test
  void persistedIdsAndRanksAreStable() {
    final Map<LipidAnnotationLevel, String> ids = Map.of(
        LipidAnnotationLevel.SPECIES_LEVEL, "species_level",
        LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "molec_species_level",
        LipidAnnotationLevel.DOUBLE_BOND_POSITION_LEVEL, "db_position_level");
    assertEquals(ids, Map.of(
        LipidAnnotationLevel.SPECIES_LEVEL, LipidAnnotationLevel.SPECIES_LEVEL.getUniqueID(),
        LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL,
        LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL.getUniqueID(),
        LipidAnnotationLevel.DOUBLE_BOND_POSITION_LEVEL,
        LipidAnnotationLevel.DOUBLE_BOND_POSITION_LEVEL.getUniqueID()));
    assertTrue(LipidAnnotationLevel.SPECIES_LEVEL.rank()
        < LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL.rank());
    assertTrue(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL.rank()
        < LipidAnnotationLevel.DOUBLE_BOND_POSITION_LEVEL.rank());
  }

  @Test
  void orderingAndParsingCoverEveryPair() {
    for (final LipidAnnotationLevel first : LipidAnnotationLevel.values()) {
      assertEquals(first, UniqueIdSupplier.parseOrElse(first.name(), LipidAnnotationLevel.values(),
          null));
      assertEquals(first, UniqueIdSupplier.parseOrElse(first.getUniqueID(),
          LipidAnnotationLevel.values(), null));
      for (final LipidAnnotationLevel second : LipidAnnotationLevel.values()) {
        final LipidAnnotationLevel expected = first.rank() <= second.rank() ? first : second;
        assertEquals(expected, LipidAnnotationLevel.min(first, second));
        assertEquals(first.rank() >= second.rank(), first.isAtLeast(second));
      }
    }
  }
}
