/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.io.projectload.version_3_0;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.io.projectload.UserParameterOpenHandler;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class UserParameterOpenHandler_3_0 extends DefaultHandler implements
    UserParameterOpenHandler {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private StringBuffer charBuffer;

  private UserParameter<?, ?> currentParameter;
  private ArrayList<String> currentOptions;
  private Hashtable<RawDataFile, Object> currentValues;
  private String currentDataFileID;

  private MZmineProject newProject;
  private Hashtable<String, RawDataFile> dataFilesIDMap;

  private int parsedParams, totalParams;

  private boolean canceled = false;

  public UserParameterOpenHandler_3_0(MZmineProject newProject,
      Hashtable<String, RawDataFile> dataFilesIDMap) {
    this.newProject = newProject;
    this.dataFilesIDMap = dataFilesIDMap;
    currentOptions = new ArrayList<String>();
    currentValues = new Hashtable<RawDataFile, Object>();
  }

  /**
   * Load the user parameters
   */
  @Override
  public void readUserParameters(InputStream inputStream)
      throws IOException, ParserConfigurationException, SAXException {

    logger.info("Loading user parameters");
    charBuffer = new StringBuffer();

    // Parse the XML file
    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser saxParser = factory.newSAXParser();
    saxParser.parse(inputStream, this);

  }

  /**
   * @return the progress of these functions loading the feature list from the zip file.
   */
  @Override
  public double getProgress() {
    if (totalParams == 0) {
      return 0;
    }
    return (double) parsedParams / totalParams;
  }

  @Override
  public void cancel() {
    canceled = true;
  }

  /**
   * @see DefaultHandler#startElement(String, String, String, Attributes)
   */
  @Override
  public void startElement(String namespaceURI, String lName, String qName, Attributes attrs)
      throws SAXException {

    if (canceled) {
      throw new SAXException("Parsing canceled");
    }

    // <PARAMETERS>
    if (qName.equals(UserParameterElementName_3_0.PARAMETERS.getElementName())) {
      String count = attrs.getValue(UserParameterElementName_3_0.COUNT.getElementName());
      totalParams = Integer.parseInt(count);
    }

    // <PARAMETER>
    if (qName.equals(UserParameterElementName_3_0.PARAMETER.getElementName())) {

      String name = attrs.getValue(UserParameterElementName_3_0.NAME.getElementName());
      String type = attrs.getValue(UserParameterElementName_3_0.TYPE.getElementName());

      if (type.equals(DoubleParameter.class.getSimpleName())) {
        currentParameter = new DoubleParameter(name, null);
      } else if (type.equals(StringParameter.class.getSimpleName())) {
        currentParameter = new StringParameter(name, null);
      } else if (type.equals(ComboParameter.class.getSimpleName())) {
        currentParameter = new ComboParameter<String>(name, null, new String[0]);
      } else {
        throw new SAXException("Unknown parameter type: " + type);
      }

      logger.finest("Loading parameter " + name);

      currentOptions.clear();
      currentValues.clear();

    }

    // <VALUE>
    if (qName.equals(UserParameterElementName_3_0.VALUE.getElementName())) {
      currentDataFileID = attrs.getValue(UserParameterElementName_3_0.DATA_FILE.getElementName());
    }

  }

  /**
   * @see DefaultHandler#endElement(String, String, String)
   */
  @Override
  @SuppressWarnings("unchecked")
  public void endElement(String namespaceURI, String sName, String qName) throws SAXException {

    if (canceled) {
      throw new SAXException("Parsing canceled");
    }

    // <OPTION>
    if (qName.equals(UserParameterElementName_3_0.OPTION.getElementName())) {
      String optionValue = getTextOfElement();
      currentOptions.add(optionValue);
    }

    // <VALUE>
    if (qName.equals(UserParameterElementName_3_0.VALUE.getElementName())) {
      RawDataFile currentDataFile = dataFilesIDMap.get(currentDataFileID);
      String valueString = getTextOfElement();
      Object value;
      if (currentParameter instanceof DoubleParameter) {
        value = Double.valueOf(valueString);
      } else {
        value = valueString;
      }
      currentValues.put(currentDataFile, value);
    }

    // <PARAMETER>
    if (qName.equals(UserParameterElementName_3_0.PARAMETER.getElementName())) {
      if (currentParameter instanceof ComboParameter) {
        String newChoices[] = currentOptions.toArray(new String[0]);
        ((ComboParameter<String>) currentParameter).setChoices(newChoices);
      }
      newProject.addParameter(currentParameter);

      for (RawDataFile dataFile : currentValues.keySet()) {
        Object value = currentValues.get(dataFile);
        newProject.setParameterValue(currentParameter, dataFile, value);
      }

      parsedParams++;

    }

  }

  /**
   * Return a string without tab an EOF characters
   *
   * @return String element text
   */
  private String getTextOfElement() {
    String text = charBuffer.toString();
    text = text.replaceAll("[\n\r\t]+", "");
    text = text.replaceAll("^\\s+", "");
    charBuffer.setLength(0);
    return text;
  }

  /**
   * characters()
   *
   * @see org.xml.sax.ContentHandler#characters(char[], int, int)
   */
  @Override
  public void characters(char buf[], int offset, int len) throws SAXException {
    charBuffer = charBuffer.append(buf, offset, len);
  }

}
