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

package io.github.mzmine.datamodel.identities.io;

import io.github.mzmine.datamodel.identities.IonLibraries;
import io.github.mzmine.datamodel.identities.IonLibrary;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IonLibraryIOTest {

  static final String expected = "{\"name\":\"mzmine default (+/-)\",\"parts\":{\"0\":{\"name\":\"H2O\",\"formula\":\"H2O\",\"mass\":18.010564684,\"charge\":0},\"1\":{\"name\":\"H\",\"formula\":\"H\",\"mass\":1.00727645209073,\"charge\":1},\"2\":{\"name\":\"e\",\"formula\":null,\"mass\":5.4857990927E-4,\"charge\":-1},\"3\":{\"name\":\"C2H3O2\",\"formula\":\"C2H3O2\",\"mass\":59.01385291590927,\"charge\":-1},\"4\":{\"name\":\"CHO2\",\"formula\":\"CHO2\",\"mass\":44.99820285190927,\"charge\":-1},\"5\":{\"name\":\"Ca\",\"formula\":\"Ca\",\"mass\":39.96149382018146,\"charge\":2},\"6\":{\"name\":\"Cl\",\"formula\":\"Cl\",\"mass\":34.96940125990927,\"charge\":-1},\"7\":{\"name\":\"Fe\",\"formula\":\"Fe\",\"mass\":55.933840340181455,\"charge\":2},\"8\":{\"name\":\"Fe\",\"formula\":\"Fe\",\"mass\":55.93329176027218,\"charge\":3},\"9\":{\"name\":\"K\",\"formula\":\"K\",\"mass\":38.96315810009073,\"charge\":1},\"10\":{\"name\":\"NH4\",\"formula\":\"H4N\",\"mass\":18.03382554809073,\"charge\":1},\"11\":{\"name\":\"Na\",\"formula\":\"Na\",\"mass\":22.98922070009073,\"charge\":1},\"12\":{\"name\":\"[79]Br\",\"formula\":\"[79]Br\",\"mass\":78.91888567990927,\"charge\":-1}},\"ionTypes\":[{\"parts\":[{\"id\":1,\"count\":-2}],\"molecules\":1},{\"parts\":[{\"id\":1,\"count\":-1}],\"molecules\":1},{\"parts\":[{\"id\":2,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":6,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":4,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":3,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":12,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":0,\"count\":-4},{\"id\":1,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":0,\"count\":-3},{\"id\":1,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":0,\"count\":-2},{\"id\":1,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":0,\"count\":-1},{\"id\":2,\"count\":-1}],\"molecules\":1},{\"parts\":[{\"id\":0,\"count\":-1},{\"id\":1,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":2,\"count\":-1}],\"molecules\":1},{\"parts\":[{\"id\":1,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":0,\"count\":-1},{\"id\":11,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":10,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":11,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":1,\"count\":-1},{\"id\":5,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":9,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":1,\"count\":-1},{\"id\":11,\"count\":2}],\"molecules\":1},{\"parts\":[{\"id\":1,\"count\":-2},{\"id\":8,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":1,\"count\":-1},{\"id\":7,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":2,\"count\":-2}],\"molecules\":1},{\"parts\":[{\"id\":1,\"count\":2}],\"molecules\":1},{\"parts\":[{\"id\":1,\"count\":1},{\"id\":10,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":1,\"count\":1},{\"id\":11,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":5,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":1,\"count\":1},{\"id\":9,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":1,\"count\":-1},{\"id\":8,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":7,\"count\":1}],\"molecules\":1},{\"parts\":[{\"id\":1,\"count\":3}],\"molecules\":1},{\"parts\":[{\"id\":1,\"count\":-1}],\"molecules\":2},{\"parts\":[{\"id\":6,\"count\":1}],\"molecules\":2},{\"parts\":[{\"id\":0,\"count\":-1},{\"id\":1,\"count\":1}],\"molecules\":2},{\"parts\":[{\"id\":1,\"count\":1}],\"molecules\":2},{\"parts\":[{\"id\":10,\"count\":1}],\"molecules\":2},{\"parts\":[{\"id\":11,\"count\":1}],\"molecules\":2},{\"parts\":[{\"id\":1,\"count\":1}],\"molecules\":3},{\"parts\":[{\"id\":11,\"count\":1}],\"molecules\":3}]}";

  @Test
  void toJson() {
    final String json = IonLibraryIO.toJson(IonLibraries.MZMINE_DEFAULT_DUAL_POLARITY);

    Assertions.assertNotNull(json);
    Assertions.assertEquals(expected, json);
  }

  @Test
  void fromJson() {
    final IonLibrary library = IonLibraryIO.loadFromJson(expected).library();

    Assertions.assertEquals(IonLibraries.MZMINE_DEFAULT_DUAL_POLARITY.getNumIons(),
        library.getNumIons());
    Assertions.assertEquals(IonLibraries.MZMINE_DEFAULT_DUAL_POLARITY.ions(), library.ions());
  }

  @Test
  void saveLoad() {
    final String json = IonLibraryIO.toJson(IonLibraries.MZMINE_DEFAULT_DUAL_POLARITY);
    final IonLibrary library = IonLibraryIO.loadFromJson(json).library();

    Assertions.assertEquals(IonLibraries.MZMINE_DEFAULT_DUAL_POLARITY.getNumIons(),
        library.getNumIons());
    Assertions.assertEquals(IonLibraries.MZMINE_DEFAULT_DUAL_POLARITY.ions(), library.ions());
  }

}