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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel;

import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingQueue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DPPParameterValueWrapper {
  DataPointProcessingQueue[] queues;
  Boolean differentiateMSn;

  // private static final String MSANY_QUEUE_ELEMENT = "ms-any";
  // private static final String MSONE_QUEUE_ELEMENT = "ms-one";
  // private static final String MSMS_QUEUE_ELEMENT = "ms-ms";
  private final String[] MSLEVEL_VALUE_ELEMENT = new String[MSLevel.cropValues().length];
  private static final String WRAPPER_ELEMENT = "queuewrapper";
  private static final String MSLEVEL_ELEMENT = "mslevel";
  private static final String DIFFMSN_ELEMENT = "differentiatemsn";
  private static final String MAINFILE_ELEMENT = "DPPParameters";

  public DPPParameterValueWrapper() {
    for (MSLevel mslevel : MSLevel.cropValues())
      MSLEVEL_VALUE_ELEMENT[mslevel.ordinal()] = mslevel.toString();

    differentiateMSn = false;
    queues = new DataPointProcessingQueue[MSLevel.cropValues().length];
    for (int i = 0; i < queues.length; i++) {
      queues[i] = new DataPointProcessingQueue();
    }
  }

  public DataPointProcessingQueue[] getQueues() {
    return queues;
  }

  public boolean isDifferentiateMSn() {
    return differentiateMSn;
  }

  public void setQueues(DataPointProcessingQueue[] queues) {
    this.queues = queues;
  }

  public void setDifferentiateMSn(boolean differentiateMSn) {
    this.differentiateMSn = differentiateMSn;
  }

  public boolean checkValue(Collection<String> errorMessage) {
    Boolean val = true;;
    if (differentiateMSn == null) {
      errorMessage.add("Value of boolean parameter differentiateMSn == null.");
      val = false;
    }
    if (queues == null) {
      errorMessage.add("Value of queues array == null.");
      return false;
    }
    for (int i = 0; i < queues.length; i++) {
      if (queues[i] == null) {
        errorMessage.add("Value of queues[" + i + "] == null.");
        val = false;
      }
    }
    return val;
  }

  public void saveValueToXML(@NotNull Element xmlElement) {
    final Document document = xmlElement.getOwnerDocument();

    final Element[] msLevelElements = new Element[MSLevel.cropValues().length];

    xmlElement.setAttribute(DIFFMSN_ELEMENT, differentiateMSn.toString());

    for (MSLevel mslevel : MSLevel.cropValues()) {
      msLevelElements[mslevel.ordinal()] = document.createElement(WRAPPER_ELEMENT);
      msLevelElements[mslevel.ordinal()].setAttribute(MSLEVEL_ELEMENT,
          MSLEVEL_VALUE_ELEMENT[mslevel.ordinal()]);
      xmlElement.appendChild(msLevelElements[mslevel.ordinal()]);
      queues[mslevel.ordinal()].saveToXML(msLevelElements[mslevel.ordinal()]);
    }
  }

  public void loadfromXML(final @NotNull Element xmlElement) {

    setDifferentiateMSn(Boolean.valueOf(xmlElement.getAttribute(DIFFMSN_ELEMENT)));

    final NodeList nodes = xmlElement.getElementsByTagName(WRAPPER_ELEMENT);
    final int nodesLength = nodes.getLength();

    for (int i = 0; i < nodesLength; i++) {
      final Element queueElement = (Element) nodes.item(i);
      final String levelName = queueElement.getAttribute(MSLEVEL_ELEMENT);

      for (MSLevel mslevel : MSLevel.cropValues()) {
        if (levelName.equals(MSLEVEL_VALUE_ELEMENT[mslevel.ordinal()])) {
          queues[mslevel.ordinal()] = DataPointProcessingQueue.loadfromXML(queueElement);
        }
      }
    }
  }

  public void saveToFile(final @NotNull File file) {
    try {
      Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      final Element element = document.createElement(MAINFILE_ELEMENT);
      document.appendChild(element);

      this.saveValueToXML(element);

      // Create transformer.
      final Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

      FileOutputStream os = new FileOutputStream(file);
      StreamResult res = new StreamResult(os);
      // Write to file and transform.
      transformer.transform(new DOMSource(document), res);
      os.close();
    } catch (ParserConfigurationException | TransformerFactoryConfigurationError | TransformerException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void loadFromFile(@NotNull File file) {
    try {
      Element element = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
          .getDocumentElement();
      loadfromXML(element);
    } catch (SAXException | IOException | ParserConfigurationException e) {
      e.printStackTrace();
    }
  }

  public @NotNull DataPointProcessingQueue getQueue(int ordinal) {
    checkValues();
    return this.queues[ordinal];
  }

  public @NotNull DataPointProcessingQueue getQueue(MSLevel mslevel) {
    return getQueue(mslevel.ordinal());
  }

  public void setQueue(int ordinal, DataPointProcessingQueue queue) {
    checkValues();
    queues[ordinal] = queue;
  }

  public void setQueue(MSLevel mslevel, DataPointProcessingQueue queue) {
    setQueue(mslevel.ordinal(), queue);
  }

  @Override
  public DPPParameterValueWrapper clone() {
    DPPParameterValueWrapper clone = new DPPParameterValueWrapper();
    clone.setDifferentiateMSn(this.isDifferentiateMSn());
    for (MSLevel mslevel : MSLevel.cropValues())
      clone.setQueue(mslevel, getQueue(mslevel).clone());
    return clone;
  }

  private void checkValues() {
    if (differentiateMSn == null)
      differentiateMSn = false;
    if (queues == null)
      queues = new DataPointProcessingQueue[MSLevel.cropValues().length];
    for (int i = 0; i < queues.length; i++) {
      if (queues[i] == null)
        queues[i] = new DataPointProcessingQueue();
    }
  }
}
