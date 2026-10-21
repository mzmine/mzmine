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

package io.github.mzmine.parameters.parametertypes.filenames;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.util.XMLUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;

class DirectoryListParameterTest {

  @TempDir
  Path root;

  @Test
  void loadsExistingSingleDirectoryPreference() throws Exception {
    var legacy = new DirectoryParameter("Temporary file directory", "", root.toString());
    Element xml = XMLUtils.newDocument().createElement("parameter");
    legacy.saveValueToXML(xml);
    var parameter = new DirectoryListParameter(legacy.getName(), "", new File[0]);

    parameter.loadValueFromXML(xml);

    assertArrayEquals(new File[]{root.toFile()}, parameter.getValue());
  }

  @Test
  void savesSingleDirectoryForBothOldAndNewVersions() throws Exception {
    File directory = root.resolve("temp & data").toFile();
    var parameter = new DirectoryListParameter("Temporary file directory", "",
        new File[]{directory});
    Element xml = XMLUtils.newDocument().createElement("parameter");

    parameter.saveValueToXML(xml);

    assertEquals(directory.getPath(), xml.getTextContent());
    assertEquals(0, xml.getElementsByTagName("file").getLength());
    var legacy = new DirectoryParameter(parameter.getName(), "");
    legacy.loadValueFromXML(xml);
    assertEquals(directory, legacy.getValue());
    var loaded = new DirectoryListParameter(parameter.getName(), "");
    loaded.loadValueFromXML(xml);
    assertArrayEquals(new File[]{directory}, loaded.getValue());
  }

  @Test
  void roundTripPreservesDirectoryOrderAndMissingDirectories() throws Exception {
    File[] directories = {root.resolve("second drive").toFile(), root.toFile(),
        root.resolve("third drive").toFile()};
    var parameter = new DirectoryListParameter("Temporary file directory", "", directories);
    Element xml = XMLUtils.newDocument().createElement("parameter");
    parameter.saveValueToXML(xml);
    assertEquals(directories.length, xml.getElementsByTagName("file").getLength());
    var loaded = new DirectoryListParameter(parameter.getName(), "", new File[0]);

    loaded.loadValueFromXML(xml);

    assertArrayEquals(directories, loaded.getValue());
    assertTrue(loaded.checkValue(new ArrayList<>()));
  }

  @Test
  void clonePreservesDirectoryParameterAndCopiesValues() {
    var parameter = new DirectoryListParameter("Temporary file directory", "description",
        new File[]{root.toFile()});

    var copy = parameter.cloneParameter();

    assertEquals(parameter.getName(), copy.getName());
    assertEquals(parameter.getDescription(), copy.getDescription());
    assertArrayEquals(parameter.getValue(), copy.getValue());
    assertNotSame(parameter.getValue(), copy.getValue());
  }

  @Test
  void loadsExistingFileListXml() throws Exception {
    File[] directories = {root.resolve("first").toFile(), root.resolve("second").toFile()};
    var legacy = new FileNamesParameter("Temporary file directory", "", List.of(), null,
        directories, null, FileNamesParameter.DEFAULT_ALL_FILES_MAPPER);
    Element xml = XMLUtils.newDocument().createElement("parameter");
    legacy.saveValueToXML(xml);
    var loaded = new DirectoryListParameter(legacy.getName(), "");

    loaded.loadValueFromXML(xml);

    assertArrayEquals(directories, loaded.getValue());
  }

  @Test
  void directoryOrderIsPartOfTheParameterValue() {
    File first = root.resolve("first").toFile();
    File second = root.resolve("second").toFile();
    var parameter = new DirectoryListParameter("Directories", "", new File[]{first, second});
    var copy = parameter.cloneParameter();
    assertTrue(parameter.valueEquals(copy));

    copy.setValue(new File[]{second, first});

    assertFalse(parameter.valueEquals(copy));
  }

  @Test
  void rejectsFilesAndEmptyListsButAcceptsDirectoriesToBeCreated() throws Exception {
    var parameter = new DirectoryListParameter("Temporary file directory", "", new File[0]);
    assertFalse(parameter.checkValue(new ArrayList<>()));
    parameter.setValue(new File[]{Files.createFile(root.resolve("file")).toFile()});
    assertFalse(parameter.checkValue(new ArrayList<>()));
    parameter.setValue(new File[]{root.resolve("new-directory").toFile()});
    assertTrue(parameter.checkValue(new ArrayList<>()));
  }
}
