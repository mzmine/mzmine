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

package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Vector;
import java.util.logging.Logger;
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
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.impl.MZmineProcessingStepImpl;
import net.sf.mzmine.parameters.ParameterSet;

public class DataPointProcessingQueue extends Vector<MZmineProcessingStep<DataPointProcessingModule>> {

  private static final long serialVersionUID = 1L;
  
  private static final Logger logger = Logger.getLogger(DataPointProcessingQueue.class.getName());
  
  private static final String DATA_POINT_PROCESSING_STEP_ELEMENT = "processingstep";
  private static final String METHOD_ELEMENT = "method";

  public static DataPointProcessingQueue loadfromXML(final Element xmlElement) {
    DataPointProcessingQueue queue = new DataPointProcessingQueue();

    // Get the loaded modules.
    final Collection<MZmineModule> allModules = MZmineCore.getAllModules();

    // Process the processing step elements.
    final NodeList nodes = xmlElement.getElementsByTagName(DATA_POINT_PROCESSING_STEP_ELEMENT);
    final int nodesLength = nodes.getLength();

    for (int i = 0; i < nodesLength; i++) {

      final Element stepElement = (Element) nodes.item(i);
      final String methodName = stepElement.getAttribute(METHOD_ELEMENT);
      logger.info("loading method " + methodName);

      for (MZmineModule module : allModules) {
        if (module instanceof DataPointProcessingModule
            && module.getClass().getName().equals(methodName)) {

          ParameterSet parameterSet =
              MZmineCore.getConfiguration().getModuleParameters(module.getClass());

          parameterSet.loadValuesFromXML(stepElement);
          queue.add(new MZmineProcessingStepImpl<DataPointProcessingModule>(
              (DataPointProcessingModule) module, parameterSet));
          // add to treeView
          break;
        }

      }

    }
    return queue;
  }
  
  public static DataPointProcessingQueue loadFromFile(File file) {
    try {
      Element element = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getDocumentElement();
      return loadfromXML(element);
    } catch (SAXException | IOException | ParserConfigurationException e) {
      e.printStackTrace();
      return new DataPointProcessingQueue();
    }
  }

  public void saveToXML(final Element xmlElement) {
    
    final Document document = xmlElement.getOwnerDocument();

    // Process each step.
    for (final MZmineProcessingStep<?> step : this) {

      // Append a new batch step element.
      final Element stepElement = document.createElement(DATA_POINT_PROCESSING_STEP_ELEMENT);
      
      stepElement.setAttribute(METHOD_ELEMENT, step.getModule().getClass().getName());
      xmlElement.appendChild(stepElement);

      // Save parameters.
      final ParameterSet parameters = step.getParameterSet();
      if (parameters != null) {
        parameters.saveValuesToXML(stepElement);
      }
    }
  }
  
  public void saveToFile(final File file) {
    try {
      Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      final Element element = document.createElement("DataPointProcessing");
      document.appendChild(element);

      // Serialize batch queue.
      this.saveToXML(element);

      // Create transformer.
      final Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

      // Write to file and transform.
      transformer.transform(new DOMSource(document), new StreamResult(new FileOutputStream(file)));

      logger.info("Saved " + this.size() + " processing step(s) to " + file.getName());
      
    } catch (ParserConfigurationException | TransformerFactoryConfigurationError | FileNotFoundException | TransformerException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return;
    }
    
  }
  
}
