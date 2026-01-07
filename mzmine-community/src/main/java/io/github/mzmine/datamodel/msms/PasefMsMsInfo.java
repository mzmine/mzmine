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

package io.github.mzmine.datamodel.msms;

import io.github.mzmine.datamodel.Frame;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.Nullable;

/**
 * Precursor information stored in IMS MS2 frames regarding their respective sub spectra.
 *
 * @author https://github.com/SteffenHeu
 */
public interface PasefMsMsInfo extends DDAMsMsInfo, IonMobilityMsMsInfo {

  public final int UNKNOWN_CHARGE = 0;
  public final double UNKNOWN_COLISSIONENERGY = -1d;

  /**
   * @return The most intense m/z of the detected precursor.
   */
  @Nullable double getIsolationMz();

  /**
   * @return Collision energy this precursor was fragmented at in the given range. May be null if
   * not set or 0 if unknown.
   */
  Float getActivationEnergy();

  /**
   * @return The charge of the precursor. 0 = unknown.
   */
  Integer getPrecursorCharge();

  Frame getParentFrame();

  void writeToXML(XMLStreamWriter writer) throws XMLStreamException;
}
