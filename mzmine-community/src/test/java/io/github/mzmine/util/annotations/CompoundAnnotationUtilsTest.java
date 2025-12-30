/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.util.annotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.annotations.MissingValueType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.identities.IonType;
import io.github.mzmine.datamodel.identities.IonTypes;
import io.github.mzmine.util.collections.SortOrder;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompoundAnnotationUtilsTest {

  private final Random rand = new Random(42);
  private List<CompoundDBAnnotation> list;

  @BeforeEach
  void setUp() {
    String n1 = "Steffen";
    String n2 = "Robin";
    list = List.of(createCompound(n1), createCompound(n1), createCompound(n1), createCompound(n2),
        createCompound(n2), createCompound(n1), createCompound(n2), createCompound(n1));
  }

  public SimpleCompoundDBAnnotation createCompound(String name) {
    var db = new SimpleCompoundDBAnnotation();
    db.put(CompoundNameType.class, name);
    db.setScore(rand.nextFloat());

    IonType ion = switch (rand.nextInt(4)) {
      case 0 -> IonTypes.M2_H.asIonType();
      case 1 -> IonTypes.H.asIonType();
      case 2 -> IonTypes.H_H2O.asIonType();
      default -> null;
    };
    db.put(IonTypeType.class, ion);
    return db;
  }

  @Test
  void testSorterLeastModifiedCompoundFirst() {
    var list = this.list.stream()
        .sorted(CompoundAnnotationUtils.getSorterLeastModifiedCompoundFirst()).toList();

    for (int i = 0; i < list.size() - 1; i++) {
      var a = list.get(i).getAdductType();
      var b = list.get(i + 1).getAdductType();
      if (a == null || b == null) {
        continue;
      }

      assertTrue(a.getTotalPartsCount() <= b.getTotalPartsCount(),
          "Sorting for minimum modified ions is wrong");
    }
  }

  @Test
  void testBestMatchesPerCompoundName() {
    var matchesPerCompoundName = CompoundAnnotationUtils.getBestMatchesPerCompoundName(list);
    assertEquals(2, matchesPerCompoundName.size());

    var compoundA = matchesPerCompoundName.get(0);
    var compoundB = matchesPerCompoundName.get(1);

    assertTrue(compoundB.getScore() <= compoundA.getScore(),
        "Score sorting descending did not work");
  }

  @Test
  void rankUniqueAnnotationTypes() {
    List<DataType> types = DataTypes.getAll(FormulaType.class, MZType.class,
        SmilesStructureType.class, LipidMatchListType.class, SpectralLibraryMatchesType.class,
        MissingValueType.class, MissingValueType.class);
    var map = CompoundAnnotationUtils.rankUniqueAnnotationTypes((Collection) types,
        SortOrder.DESCENDING);
    assertEquals(3, map.size());
    assertEquals(0, map.get(DataTypes.get(SpectralLibraryMatchesType.class)));
    assertEquals(1, map.get(DataTypes.get(LipidMatchListType.class)));
    assertEquals(2, map.get(DataTypes.get(MissingValueType.class)));
    map = CompoundAnnotationUtils.rankUniqueAnnotationTypes((Collection) types,
        SortOrder.ASCENDING);
    assertEquals(3, map.size());
    assertEquals(0, map.get(DataTypes.get(MissingValueType.class)));
    assertEquals(1, map.get(DataTypes.get(LipidMatchListType.class)));
    assertEquals(2, map.get(DataTypes.get(SpectralLibraryMatchesType.class)));
  }
}