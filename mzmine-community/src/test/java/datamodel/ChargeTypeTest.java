/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package datamodel;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import io.github.mzmine.datamodel.features.types.numbers.ChargeType;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ChargeTypeTest {

  private final ChargeType type = new ChargeType();

  // ---- mapper tests ----

  @Test
  void mapperReturnsNullForNull() {
    Assertions.assertNull(ChargeType.mapper.apply(null));
  }

  @Test
  void mapperReturnsNullForBlank() {
    Assertions.assertNull(ChargeType.mapper.apply(""));
    Assertions.assertNull(ChargeType.mapper.apply("   "));
  }

  @Test
  void mapperPositiveInteger() {
    Assertions.assertEquals(1, ChargeType.mapper.apply("1"));
    Assertions.assertEquals(2, ChargeType.mapper.apply("2"));
    Assertions.assertEquals(3, ChargeType.mapper.apply("3"));
  }

  @Test
  void mapperStripsWhitespace() {
    Assertions.assertEquals(2, ChargeType.mapper.apply(" 2 "));
  }

  @Test
  void mapperZeroCharge() {
    Assertions.assertEquals(0, ChargeType.mapper.apply("0"));
  }

  @Test
  void mapperNonNumericDefaultsToPositiveOne() {
    // decision: unparseable strings without "-" default to charge +1
    Assertions.assertEquals(1, ChargeType.mapper.apply("abc"));
  }

  @Test
  void mapperNonNumericWithMinusDefaultsToNegativeOne() {
    // decision: unparseable strings with "-" default to charge -1
    Assertions.assertEquals(-1, ChargeType.mapper.apply("-abc"));
  }

  @Test
  void mapperNegativeInteger() {
    Assertions.assertEquals(-1, ChargeType.mapper.apply("-1"));
    Assertions.assertEquals(-2, ChargeType.mapper.apply("-2"));
  }

  @Test
  void mapperExplicitPlusSign() {
    Assertions.assertEquals(2, ChargeType.mapper.apply("+2"));
  }

  @Test
  void mapperPlusMinusThrows() {
    // strings with both + and - are ambiguous and invalid
    Assertions.assertThrows(IllegalArgumentException.class, () -> ChargeType.mapper.apply("+-2"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> ChargeType.mapper.apply("-+1"));
  }

  // ---- evaluateBindings CONSENSUS tests ----

  @Test
  void consensusEmptyListReturnsNull() {
    Assertions.assertNull(type.evaluateBindings(BindingsType.CONSENSUS, List.of()));
  }

  @Test
  void consensusAllNullChargesReturnsNull() {
    ModularDataModel m1 = mockModel(null);
    ModularDataModel m2 = mockModel(null);
    Assertions.assertNull(type.evaluateBindings(BindingsType.CONSENSUS, List.of(m1, m2)));
  }

  @Test
  void consensusSingleModelReturnsItsCharge() {
    ModularDataModel m = mockModel(2);
    Assertions.assertEquals(2, type.evaluateBindings(BindingsType.CONSENSUS, List.of(m)));
  }

  @Test
  void consensusAllSameChargeReturnsThatCharge() {
    List<ModularDataModel> models = List.of(mockModel(2), mockModel(2), mockModel(2));
    Assertions.assertEquals(2, type.evaluateBindings(BindingsType.CONSENSUS, models));
  }

  @Test
  void consensusMajorityWins() {
    // charge 2 appears 3×, charge 1 appears once → consensus is 2
    List<ModularDataModel> models = List.of(mockModel(2), mockModel(2), mockModel(2), mockModel(1));
    Assertions.assertEquals(2, type.evaluateBindings(BindingsType.CONSENSUS, models));
  }

  @Test
  void consensusNullModelsAreSkipped() {
    // null model entries must not cause NPE and are ignored
    List<ModularDataModel> models = List.of(mockModel(3), mockModel(3));
    Assertions.assertEquals(3, type.evaluateBindings(BindingsType.CONSENSUS, models));
  }

  @Test
  void consensusNullChargesAreIgnored() {
    // models that hold null for this type are skipped in counting
    List<ModularDataModel> models = List.of(mockModel(2), mockModel(null), mockModel(2));
    Assertions.assertEquals(2, type.evaluateBindings(BindingsType.CONSENSUS, models));
  }

  // ---- helper ----

  private ModularDataModel mockModel(Integer charge) {
    ModularDataModel model = Mockito.mock(ModularDataModel.class);
    Mockito.when(model.get(type)).thenReturn(charge);
    return model;
  }
}
