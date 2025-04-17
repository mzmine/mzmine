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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.featuredata.impl;

import static io.github.mzmine.datamodel.featuredata.impl.StorageUtils.EMPTY_DOUBLE_SEGMENT;

import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import java.lang.foreign.MemorySegment;
import java.util.List;

public interface StoredMobilograms {

  MobilogramStorageResult EMPTY = new MobilogramStorageResult(List.of(), EMPTY_DOUBLE_SEGMENT,
      EMPTY_DOUBLE_SEGMENT);

  List<IonMobilitySeries> storedMobilograms();

  MemorySegment storedMzValues();

  MemorySegment storedIntensityValues();

  IonMobilitySeries mobilogram(int index);
}
