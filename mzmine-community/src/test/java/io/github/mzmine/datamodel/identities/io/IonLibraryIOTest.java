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

package io.github.mzmine.datamodel.identities.io;

import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonTypes;
import io.github.mzmine.datamodel.identities.iontype.LibraryOrigin;
import io.github.mzmine.datamodel.identities.iontype.UnmodifiableIonLibrary;
import io.github.mzmine.util.XMLUtils;
import java.io.IOException;
import java.util.UUID;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

class IonLibraryIOTest {


  static final IonLibrary LIBRARY = new UnmodifiableIonLibrary(
      UUID.fromString("ed133c0d-4287-3f0b-a1af-c5b903bd9e02"), LibraryOrigin.BUILTIN, "Test lib",
      IonTypes.listIons(false, IonTypes.BR, IonTypes.CA, IonTypes.NA, IonTypes.H, IonTypes.H_H2O,
          IonTypes.M2_H_H2O));

  static final String expected = """
      {"id":"ed133c0d-4287-3f0b-a1af-c5b903bd9e02","origin":{"kind":"builtin"},"name":"Test lib","savedDate":[2026,4,22,15,33,35,800709700],"parts":[{"id":0,"name":"H2O","formula":"H2O","mass":18.010564684,"charge":0},{"id":1,"name":"Ca","formula":"Ca","mass":39.96149382018146,"charge":2},{"id":2,"name":"H","formula":"H","mass":1.00727645209073,"charge":1},{"id":3,"name":"Na","formula":"Na","mass":22.98922070009073,"charge":1},{"id":4,"name":"[79]Br","formula":"[79]Br","mass":78.91888567990927,"charge":-1}],"ionTypes":[{"parts":[{"id":4,"count":1}],"molecules":1},{"parts":[{"id":0,"count":-1},{"id":2,"count":1}],"molecules":1},{"parts":[{"id":2,"count":1}],"molecules":1},{"parts":[{"id":3,"count":1}],"molecules":1},{"parts":[{"id":1,"count":1}],"molecules":1},{"parts":[{"id":0,"count":-1},{"id":2,"count":1}],"molecules":2}]}""";

  @Test
  void fromJson() {
    final IonLibrary library = IonLibraryIO.loadFromJson(expected).library();

    Assertions.assertEquals(LIBRARY.getNumIons(), library.getNumIons());
    Assertions.assertEquals(LIBRARY.ions(), library.ions());
  }

  @Test
  void saveLoad() {
    final String json = IonLibraryIO.toJson(LIBRARY);
    final IonLibrary library = IonLibraryIO.loadFromJson(json).library();

    Assertions.assertEquals(LIBRARY.getNumIons(), library.getNumIons());
    Assertions.assertEquals(LIBRARY.ions(), library.ions());
  }

  @Test
  void saveLoadXML() throws ParserConfigurationException, TransformerException {
    final Document document = XMLUtils.newDocument();
    final Element element = document.createElement("root");
    document.appendChild(element);
    IonLibraryIO.saveToXML(element, LIBRARY);
//    String xml = XMLUtils.saveToString(document);

    final Element ionLibElement = XMLUtils.findChildElement(element, "ionLibrary");
    final LoadedIonLibrary loadedIonLibrary = IonLibraryIO.loadFromXML(ionLibElement);
    Assertions.assertNotNull(loadedIonLibrary);
    final IonLibrary lib = loadedIonLibrary.library();

    Assertions.assertEquals(LIBRARY, lib);
    Assertions.assertEquals(LIBRARY.getNumIons(), lib.getNumIons());
    Assertions.assertEquals(LIBRARY.ions(), lib.ions());
  }

  @Test
  void loadOldXML() throws ParserConfigurationException, IOException, SAXException {
    final Document document = XMLUtils.load(expectedXML);
    final LoadedIonLibrary loadedIonLibrary = IonLibraryIO.loadFromXML(
        (Element) document.getFirstChild());
    Assertions.assertNotNull(loadedIonLibrary);
    final IonLibrary lib = loadedIonLibrary.library();

    Assertions.assertEquals(LIBRARY, lib);
    Assertions.assertEquals(LIBRARY.getNumIons(), lib.getNumIons());
    Assertions.assertEquals(LIBRARY.ions(), lib.ions());
  }

  static final String expectedXML = """
      <ionLibrary id="ed133c0d-4287-3f0b-a1af-c5b903bd9e02"><origin kind="builtin"/><name>Test lib</name><savedDate>2026-04-22T15:36:35.3025287</savedDate><parts><parts charge="0" formula="H2O" id="0" mass="18.010564684" name="H2O"/><parts charge="2" formula="Ca" id="1" mass="39.96149382018146" name="Ca"/><parts charge="1" formula="H" id="2" mass="1.00727645209073" name="H"/><parts charge="1" formula="Na" id="3" mass="22.98922070009073" name="Na"/><parts charge="-1" formula="[79]Br" id="4" mass="78.91888567990927" name="[79]Br"/></parts><ionTypes><ionTypes molecules="1"><part count="1" id="4"/></ionTypes><ionTypes molecules="1"><part count="-1" id="0"/><part count="1" id="2"/></ionTypes><ionTypes molecules="1"><part count="1" id="2"/></ionTypes><ionTypes molecules="1"><part count="1" id="3"/></ionTypes><ionTypes molecules="1"><part count="1" id="1"/></ionTypes><ionTypes molecules="2"><part count="-1" id="0"/><part count="1" id="2"/></ionTypes></ionTypes></ionLibrary>""";
}