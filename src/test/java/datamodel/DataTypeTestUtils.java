/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javafx.scene.paint.Color;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;

/**
 * Provides utility methods for loading and saving {@link DataType}s to/from an XML.
 *
 * @author https://github.com/SteffenHeu
 */
public class DataTypeTestUtils {

  /**
   * Saves and loads the data type and it's value to an ByteArrayStream. Fails the test if the
   * loaded value does not equal the saved value. The value is processed as a row type (feature and
   * file = null) and as a feature type. Also tests null as a value and expects null to be
   * returned.
   *
   * @param type  The data type.
   * @param value The value.
   */
  public static <T> void simpleDataTypeSaveLoadTest(DataType<T> type, T value) {

    RawDataFile file = null;
    file = new RawDataFileImpl("testfile", null, null, Color.BLACK);
    Assertions.assertNotNull(file);

    // test load/save for row
    final ModularFeatureList flist = new ModularFeatureList("flist", null, file);
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, 1);
    flist.addRow(row);

    final MZmineProject project = new MZmineProjectImpl();
    project.addFile(file);
    project.addFeatureList(flist);

    row.set(type, value);
    testSaveLoad(type, value, project, flist, row, null, null);

    // test save/load for row null value
    testSaveLoad(type, null, project, flist, row, null, null);

    // test load/save for features
    final ModularFeature feature = new ModularFeature(flist, file, null, null);
    feature.set(type, value);
    row.addFeature(file, feature);
    testSaveLoad(type, value, project, flist, row, feature, file);

    // test save/load for feature null value
    testSaveLoad(type, null, project, flist, row, feature, file);

    file.close();
  }

  /**
   * Tests loading and saving a data type with the given parameters. Will automatically test the
   * loaded value for equality via {@link Assertions#assertEquals(Object, Object)} (requires
   * implementation of {@link Object#equals(Object)} for the given datatype.).
   */
  public static void testSaveLoad(@NotNull DataType<?> type, @Nullable Object value,
      @NotNull MZmineProject project, @NotNull ModularFeatureList flist,
      @NotNull ModularFeatureListRow row, @Nullable ModularFeature feature,
      @Nullable RawDataFile file) {

    final Object loadedValue = saveAndLoad(type, value, project, flist, row, feature, file);
    Assertions.assertEquals(value, loadedValue,
        () -> "Loaded value does not equal saved value." + (feature == null ? " (row type)"
            : " (feature type)"));

  }

  /**
   * Tests saving and loads a data type with the given parameters. Can be used to manually retrieve
   * the loaded value in case {@link Object#equals(Object)} does not work.
   *
   * @return The loaded value or null if an error occurred.
   */
  public static Object saveAndLoad(@NotNull DataType<?> type, @Nullable Object value,
      @NotNull MZmineProject project, @NotNull ModularFeatureList flist,
      @NotNull ModularFeatureListRow row, @Nullable ModularFeature feature,
      @Nullable RawDataFile file) {

    // test row save
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    final XMLOutputFactory xof = XMLOutputFactory.newInstance();
    XMLStreamWriter writer = null;
    try {
      writer = new IndentingXMLStreamWriter(xof.createXMLStreamWriter(os, "UTF-8"));
    } catch (XMLStreamException e) {
      Assertions.fail("Cannot initialise xml writer.");
    }
    Assertions.assertNotNull(writer);

    try {
      writer.writeStartDocument();
      // write this element so we can test if the data type overshoots with it's reading method
      writer.writeStartElement("atestelelement");

      writer.writeStartElement(CONST.XML_DATA_TYPE_ELEMENT);
      writer.writeAttribute(CONST.XML_DATA_TYPE_ID_ATTR, type.getUniqueID());
      type.saveToXML(writer, value, flist, row, feature, file);
      writer.writeEndElement(); // datatype
      writer.writeEndElement(); // atestelement
      writer.writeEndDocument();
      writer.flush();
      writer.close();
    } catch (XMLStreamException e) {
      e.printStackTrace();
      Assertions.fail(() -> "Could not save data type " + type.getUniqueID() + ".");
    }

    // test row load
    InputStream is = new ByteArrayInputStream(os.toByteArray());
    XMLInputFactory xif = XMLInputFactory.newInstance();
    XMLStreamReader reader = null;
    try {
      reader = xif.createXMLStreamReader(is);
    } catch (XMLStreamException e) {
      Assertions.fail("Cannot initialise xml reader.");
    }
    Assertions.assertNotNull(reader);

    final int numLines = os.toString().split("\\n").length;

    try {
      while (reader.hasNext() && !(reader.isStartElement() && reader.getLocalName()
          .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
        reader.next();
      }

      if (!(reader.isStartElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)
          && reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR)
          .equals(type.getUniqueID()))) {
        Assertions.fail("Did not find data type element.");
      }
      final int preReadNumber = reader.getLocation().getLineNumber();
      Object loadedValue = type.loadFromXML(reader, project, flist, row, feature, file);
      final int diff = reader.getLocation().getLineNumber() - preReadNumber;
      if (diff >= numLines - preReadNumber) {
        Assertions.fail(
            "Data type " + type.getUniqueID() + " for overshot it's data type for value " + value
                + ".");
      }
      reader.close();

      try {
        os.close();
        is.close();
      } catch (IOException e) {
        // do nothing
      }
      return loadedValue;
    } catch (XMLStreamException e) {
      e.printStackTrace();
      Assertions.fail(() -> "Failed reading data type " + type.getUniqueID());
    }
    Assertions.fail(() -> "Failed reading data type " + type.getUniqueID());
    return null;
  }
}
