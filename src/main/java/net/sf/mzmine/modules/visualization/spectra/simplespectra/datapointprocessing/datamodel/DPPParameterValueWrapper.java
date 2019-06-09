/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import javax.annotation.Nonnull;
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
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingManager.MSLevel;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingQueue;

public class DPPParameterValueWrapper {
  DataPointProcessingQueue[] queues;
  Boolean differentiateMSn;

  private static final String MSANY_QUEUE_ELEMENT = "ms-any";
  private static final String MSONE_QUEUE_ELEMENT = "ms-one";
  private static final String MSMS_QUEUE_ELEMENT = "ms-ms";
  private static final String WRAPPER_ELEMENT = "queuewrapper";
  private static final String MSLEVEL_ELEMENT = "mslevel";
  private static final String DIFFMSN_ELEMENT = "differentiatemsn";
  private static final String MAINFILE_ELEMENT = "DPPParameters";

  public DPPParameterValueWrapper() {
    differentiateMSn = false;
    queues = new DataPointProcessingQueue[MSLevel.values().length];
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
    for (int i = 0; i < 3; i++) {
      if (queues[i] == null) {
        errorMessage.add("Value of queues[" + i + "] == null.");
        val = false;
      }
    }
    return val;
  }

  public void saveValueToXML(@Nonnull Element xmlElement) {
    final Document document = xmlElement.getOwnerDocument();

    final Element msanyElement = document.createElement(WRAPPER_ELEMENT);
    msanyElement.setAttribute(MSLEVEL_ELEMENT, MSANY_QUEUE_ELEMENT);
    final Element msoneElement = document.createElement(WRAPPER_ELEMENT);
    msoneElement.setAttribute(MSLEVEL_ELEMENT, MSONE_QUEUE_ELEMENT);
    final Element msmsElement = document.createElement(WRAPPER_ELEMENT);
    msmsElement.setAttribute(MSLEVEL_ELEMENT, MSMS_QUEUE_ELEMENT);
    xmlElement.setAttribute(DIFFMSN_ELEMENT, differentiateMSn.toString());

    xmlElement.appendChild(msanyElement);
    xmlElement.appendChild(msoneElement);
    xmlElement.appendChild(msmsElement);


    if (queues[0] != null)
      queues[0].saveToXML(msanyElement);
    if (queues[1] != null)
      queues[1].saveToXML(msoneElement);
    if (queues[2] != null)
      queues[2].saveToXML(msmsElement);
  }

  public void loadfromXML(final @Nonnull Element xmlElement) {

    setDifferentiateMSn(Boolean.valueOf(xmlElement.getAttribute(DIFFMSN_ELEMENT)));

    final NodeList nodes = xmlElement.getElementsByTagName(WRAPPER_ELEMENT);
    final int nodesLength = nodes.getLength();

    for (int i = 0; i < nodesLength; i++) {
      final Element queueElement = (Element) nodes.item(i);
      final String levelName = queueElement.getAttribute(MSLEVEL_ELEMENT);

      if (levelName.equals(MSANY_QUEUE_ELEMENT))
        queues[0] = DataPointProcessingQueue.loadfromXML(queueElement);
      if (levelName.equals(MSONE_QUEUE_ELEMENT))
        queues[1] = DataPointProcessingQueue.loadfromXML(queueElement);
      if (levelName.equals(MSMS_QUEUE_ELEMENT))
        queues[2] = DataPointProcessingQueue.loadfromXML(queueElement);
    }
  }

  public void saveToFile(final @Nonnull File file) {
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

      // Write to file and transform.
      transformer.transform(new DOMSource(document), new StreamResult(new FileOutputStream(file)));

    } catch (ParserConfigurationException | TransformerFactoryConfigurationError
        | FileNotFoundException | TransformerException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void loadFromFile(@Nonnull File file) {
    try {
      Element element = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
          .getDocumentElement();
      loadfromXML(element);
    } catch (SAXException | IOException | ParserConfigurationException e) {
      e.printStackTrace();
    }
  }

  public @Nonnull DataPointProcessingQueue getQueue(int ordinal) {
    checkValues();
    return this.queues[ordinal];
  }

  public @Nonnull DataPointProcessingQueue getQueue(MSLevel mslevel) {
    return getQueue(mslevel.ordinal());
  }

  public @Nonnull void setQueue(int ordinal, DataPointProcessingQueue queue) {
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
    for (MSLevel mslevel : MSLevel.values())
      clone.setQueue(mslevel, getQueue(mslevel).clone());
    return clone;
  }

  private void checkValues() {
    if (differentiateMSn == null)
      differentiateMSn = false;
    if (queues == null)
      queues = new DataPointProcessingQueue[MSLevel.values().length];
    for (int i = 0; i < queues.length; i++) {
      if (queues[i] == null)
        queues[i] = new DataPointProcessingQueue();
    }
  }
}
