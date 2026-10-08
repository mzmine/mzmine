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

import com.google.common.reflect.ClassPath;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DataTypesTest {

  private static final Logger logger = Logger.getLogger(DataTypesTest.class.getName());

  /**
   * Same packages as scanned by {@link DataTypes}
   */
  private static final List<String> TYPE_PACKAGES = List.of(
      "io.github.mzmine.datamodel.features.types",
      "io.github.mzmine.datamodel.features.rawfiletypes");

  @Test
  public void testUniqueID() {

    // map unique ID to instance
    final HashMap<String, DataType<?>> map = new HashMap<>();

    try {
      ClassPath classPath = ClassPath.from(DataType.class.getClassLoader());
      TYPE_PACKAGES.stream().map(classPath::getTopLevelClassesRecursive).flatMap(Set::stream)
          .forEach(classInfo -> {
            try {
              final Class<?> clazz = classInfo.load();
              clazz.asSubclass(DataType.class);
              Object o = clazz.getDeclaredConstructor().newInstance();
              if (o instanceof DataType dt) {
                var value = map.put(dt.getUniqueID(), dt);
                if (value != null) {
                  Assertions.fail(
                      "FATAL: Multiple data types with unique ID " + dt.getUniqueID() + "\n"
                          + value.getClass().getName() + "\n" + dt.getClass().getName());
                }
              }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
              if (!Modifier.isAbstract(classInfo.load().getModifiers()) && !classInfo.load()
                  .isInterface()) {
                Assertions.fail("Cannot instantiate DataType class " + classInfo.load().getName()
                    + ". Is the constructor not public?");
              }
            } catch (ClassCastException e) {
              // can go silent, not a DataType
            }
          });
    } catch (IOException e) {
      Assertions.fail("Cannot instantiate classPath for DataType.class. Cannot load projects.");
    }
  }

  @Test
  public void testSimpleClassName() {
    // map unique ID to instance
    final HashMap<String, DataType<?>> map = new HashMap<>();

    try {
      ClassPath classPath = ClassPath.from(DataType.class.getClassLoader());
      TYPE_PACKAGES.stream().map(classPath::getTopLevelClassesRecursive).flatMap(Set::stream)
          .forEach(classInfo -> {
            try {
              final Class<?> clazz = classInfo.load();
              clazz.asSubclass(DataType.class);
              Object o = clazz.getDeclaredConstructor().newInstance();
              if (o instanceof DataType dt) {
                var value = map.put(dt.getClass().getSimpleName(), dt);
                if (value != null) {
                  Assertions.fail(
                      "FATAL: Multiple data types with class name " + dt.getClass().getSimpleName()
                          + "\n" + value.getClass().getName() + "\n" + dt.getClass().getName());
                }
              }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
              if (!Modifier.isAbstract(classInfo.load().getModifiers()) && !classInfo.load()
                  .isInterface()) {
                Assertions.fail("Cannot instantiate DataType class " + classInfo.load().getName()
                    + ". Is the constructor not public?");
              }
            } catch (ClassCastException e) {
              // can go silent, not a DataType
            }
          });
    } catch (IOException e) {
      Assertions.fail("Cannot instantiate classPath for DataType.class. Cannot load projects.");
    }
  }


  @Test
  public void testGetAll() {
    List<DataType> all = DataTypes.getAll(MZType.class, RTType.class, HeightType.class);
    Assertions.assertEquals(3, all.size());
    Assertions.assertEquals(new MZType(), all.get(0));
    Assertions.assertEquals(new RTType(), all.get(1));
    Assertions.assertEquals(new HeightType(), all.get(2));
  }

  /**
   * Ensures context-free types, including sub-column types, do not implement XML behavior only in
   * the context-aware overloads.
   */
  @Test
  public void testContextFreeXmlOverrides() throws NoSuchMethodException {
    final List<String> violations = new ArrayList<>();
    final Class<?>[] fullSave = {XMLStreamWriter.class, Object.class, ModularFeatureList.class,
        ModularFeatureListRow.class, ModularFeature.class, RawDataFile.class};
    final Class<?>[] simpleSave = {XMLStreamWriter.class, Object.class};
    final Class<?>[] fullLoad = {XMLStreamReader.class, MZmineProject.class,
        ModularFeatureList.class, ModularFeatureListRow.class, ModularFeature.class,
        RawDataFile.class};
    final Class<?>[] simpleLoad = {XMLStreamReader.class};

    for (final DataType<?> type : DataTypes.getInstances()) {
      if (type.requiresFeatureListContext()) {
        continue;
      }
      checkContextFreeOverride(type, "saveToXML", fullSave, simpleSave, violations);
      checkContextFreeOverride(type, "loadFromXML", fullLoad, simpleLoad, violations);
      if (type instanceof SubColumnsFactory factory) {
        for (int i = 0; i < factory.getNumberOfSubColumns(); i++) {
          final DataType<?> subType = factory.getType(i);
          if (subType.requiresFeatureListContext()) {
            violations.add(type.getClass().getName() + " contains context-dependent sub-type "
                + subType.getClass().getName());
          } else {
            checkContextFreeOverride(subType, "saveToXML", fullSave, simpleSave, violations);
            checkContextFreeOverride(subType, "loadFromXML", fullLoad, simpleLoad, violations);
          }
        }
      }
    }
    Assertions.assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
  }

  /**
   * Ensures types that require feature-list context reject both context-free XML overloads before
   * reading or writing a value.
   */
  @Test
  public void testContextDependentTypesRejectSimpleXml() throws XMLStreamException {
    for (final DataType<?> type : DataTypes.getInstances()) {
      if (!type.requiresFeatureListContext()) {
        continue;
      }
      final XMLStreamWriter writer = XMLOutputFactory.newInstance()
          .createXMLStreamWriter(new StringWriter());
      final XMLStreamReader reader = XMLInputFactory.newInstance()
          .createXMLStreamReader(new StringReader("<datatype/>"));
      writer.writeStartElement("datatype");
      try {
        Assertions.assertThrows(UnsupportedOperationException.class,
            () -> type.saveToXML(writer, null), type.getClass().getName() + " saveToXML");
        Assertions.assertThrows(UnsupportedOperationException.class,
            () -> type.loadFromXML(reader), type.getClass().getName() + " loadFromXML");
      } finally {
        writer.writeEndElement();
        writer.close();
        reader.close();
      }
    }
  }

  /**
   * Records a violation when a full-signature XML override is not matched by a context-free
   * override in the same class or a subclass. Inherited full-signature methods from
   * {@link DataType} already delegate to the context-free overload.
   *
   * @param type         the data type to inspect
   * @param method       the XML method name
   * @param fullParams   parameter types of the context-aware overload
   * @param simpleParams parameter types of the context-free overload
   * @param violations   receives any mismatch found
   * @throws NoSuchMethodException if either XML overload is missing
   */
  private static void checkContextFreeOverride(final DataType<?> type, final String method,
      final Class<?>[] fullParams, final Class<?>[] simpleParams,
      final List<String> violations) throws NoSuchMethodException {
    final Class<?> fullDecl = type.getClass().getMethod(method, fullParams).getDeclaringClass();
    if (fullDecl == DataType.class) {
      return;
    }
    final Class<?> simpleDecl = type.getClass().getMethod(method, simpleParams).getDeclaringClass();
    if (!fullDecl.isAssignableFrom(simpleDecl)) {
      violations.add(type.getClass().getName() + "." + method
          + ": move the implementation to the context-free method, or return true in "
          + "requiresFeatureListContext()");
    }
  }
}
