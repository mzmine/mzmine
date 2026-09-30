/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package datamodel;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.rawfiletypes.RawFileMetadataType;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.lang.reflect.Method;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

/**
 * Save/load tests for {@link RawFileMetadataType}s. The feature list and row are strict dummies
 * that fail the test on any access, because raw file metadata is saved without a feature list.
 */
public class RawFileMetadataTypeTestUtils {

  private RawFileMetadataTypeTestUtils() {
  }

  /**
   * @return a feature list mock that throws an {@link AssertionError} on any method call except
   * toString, hashCode, and equals
   */
  public static @NotNull ModularFeatureList createStrictDummyFeatureList() {
    return createStrictMock(ModularFeatureList.class);
  }

  /**
   * @return a row mock that throws an {@link AssertionError} on any method call except toString,
   * hashCode, and equals
   */
  public static @NotNull ModularFeatureListRow createStrictDummyRow() {
    return createStrictMock(ModularFeatureListRow.class);
  }

  private static <T> @NotNull T createStrictMock(@NotNull final Class<T> mockedClass) {
    return Mockito.mock(mockedClass,
        Mockito.withSettings().defaultAnswer(strictAnswer(mockedClass)));
  }

  private static @NotNull Answer<Object> strictAnswer(@NotNull final Class<?> mockedClass) {
    return invocation -> {
      final Method method = invocation.getMethod();
      // decision: object methods may be called by logging or formatting and are not a violation
      if (method.getName().equals("toString") && method.getParameterCount() == 0) {
        return "strict dummy " + mockedClass.getSimpleName();
      }
      if (method.getName().equals("hashCode") && method.getParameterCount() == 0) {
        return System.identityHashCode(invocation.getMock());
      }
      if (method.getName().equals("equals") && method.getParameterCount() == 1) {
        return invocation.getMock() == invocation.getArgument(0);
      }
      // AssertionError is not caught by the RuntimeException handlers of the project io
      throw new AssertionError("%s must not access the dummy %s: %s()".formatted(
          RawFileMetadataType.class.getSimpleName(), mockedClass.getSimpleName(),
          method.getName()));
    };
  }

  /**
   * Saves and loads the value and null with strict dummy feature list and row, a null feature, and
   * a raw data file, the same way as the project save and load of raw file metadata. Also tests the
   * string conversion.
   */
  public static <T> void rawFileMetadataSaveLoadTest(@NotNull final DataType<T> type,
      @Nullable final T value) {
    Assertions.assertInstanceOf(RawFileMetadataType.class, type,
        () -> type.getClass().getName() + " is not a " + RawFileMetadataType.class.getSimpleName());

    final RawDataFile file = new RawDataFileImpl("testfile", null, null, Color.BLACK);
    final MZmineProject project = new MZmineProjectImpl();
    project.addFile(file);

    final ModularFeatureList flist = createStrictDummyFeatureList();
    final ModularFeatureListRow row = createStrictDummyRow();

    DataTypeTestUtils.testSaveLoad(type, value, project, flist, row, null, file);
    DataTypeTestUtils.testSaveLoad(type, null, project, flist, row, null, file);

    DataTypeTestUtils.testStringConversion(type, value);
    file.close();
  }
}
