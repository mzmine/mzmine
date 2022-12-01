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

package io.github.mzmine.modules.io.projectsave;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.logging.Logger;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.project.impl.MZmineProjectImpl;

class UserParameterSaveHandler {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private MZmineProjectImpl project;
  private Hashtable<RawDataFile, String> dataFilesIDMap;
  private int numOfParameters, completedParameters;
  private OutputStream finalStream;
  private boolean canceled = false;

  UserParameterSaveHandler(OutputStream finalStream, MZmineProjectImpl project,
      Hashtable<RawDataFile, String> dataFilesIDMap) {
    this.finalStream = finalStream;
    this.project = project;
    this.dataFilesIDMap = dataFilesIDMap;
  }

  /**
   * Function which creates an XML file with user parameters
   */
  void saveParameters() throws SAXException, IOException, TransformerConfigurationException {

    logger.info("Saving user parameters");

    StreamResult streamResult = new StreamResult(finalStream);
    SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

    TransformerHandler hd =tf.newTransformerHandler();

    Transformer serializer = hd.getTransformer();
    serializer.setOutputProperty(OutputKeys.INDENT, "yes");
    serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

    hd.setResult(streamResult);
    hd.startDocument();

    UserParameter<?, ?> projectParameters[] = project.getParameters();

    AttributesImpl atts = new AttributesImpl();

    atts.addAttribute("", "", UserParameterElementName.COUNT.getElementName(), "CDATA",
        String.valueOf(projectParameters.length));

    hd.startElement("", "", UserParameterElementName.PARAMETERS.getElementName(), atts);

    atts.clear();

    // <PARAMETER>
    for (UserParameter<?, ?> parameter : project.getParameters()) {

      if (canceled)
        return;

      logger.finest("Saving user parameter " + parameter.getName());

      atts.addAttribute("", "", UserParameterElementName.NAME.getElementName(), "CDATA",
          parameter.getName());

      atts.addAttribute("", "", UserParameterElementName.TYPE.getElementName(), "CDATA",
          parameter.getClass().getSimpleName());

      hd.startElement("", "", UserParameterElementName.PARAMETER.getElementName(), atts);

      atts.clear();

      fillParameterElement(parameter, hd);

      hd.endElement("", "", UserParameterElementName.PARAMETER.getElementName());
      completedParameters++;
    }

    hd.endElement("", "", UserParameterElementName.PARAMETERS.getElementName());

    hd.endDocument();

  }

  /**
   * Create the part of the XML document related to the scans
   *
   * @param scan
   * @param element
   */
  private void fillParameterElement(UserParameter<?, ?> parameter, TransformerHandler hd)
      throws SAXException, IOException {

    AttributesImpl atts = new AttributesImpl();

    RawDataFile dataFiles[] = project.getDataFiles();

    if (parameter instanceof ComboParameter) {
      Object choices[] = ((ComboParameter<?>) parameter).getChoices().toArray();

      for (Object choice : choices) {
        hd.startElement("", "", UserParameterElementName.OPTION.getElementName(), atts);

        hd.characters(choice.toString().toCharArray(), 0, choice.toString().length());
        hd.endElement("", "", UserParameterElementName.OPTION.getElementName());
      }

    }

    for (RawDataFile dataFile : dataFiles) {

      Object value = project.getParameterValue(parameter, dataFile);

      if (value == null)
        continue;

      String valueString = String.valueOf(value);
      String dataFileID = dataFilesIDMap.get(dataFile);

      atts.addAttribute("", "", UserParameterElementName.DATA_FILE.getElementName(), "CDATA",
          dataFileID);

      hd.startElement("", "", UserParameterElementName.VALUE.getElementName(), atts);

      atts.clear();

      hd.characters(valueString.toCharArray(), 0, valueString.length());
      hd.endElement("", "", UserParameterElementName.VALUE.getElementName());

    }

  }

  /**
   *
   * @return the progress of these functions saving the raw data information to the zip file.
   */
  double getProgress() {
    if (numOfParameters == 0)
      return 0;
    return (double) completedParameters / numOfParameters;
  }

  void cancel() {
    canceled = true;
  }
}
