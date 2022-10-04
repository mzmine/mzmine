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

package io.github.mzmine.datamodel.identities.ms2;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.ms2.interf.AbstractMSMSDataPointIdentity;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;

/**
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class MSMSIonIdentity extends AbstractMSMSDataPointIdentity {

  protected final IonType type;

  public MSMSIonIdentity(MZTolerance mzTolerance, DataPoint dp, IonType b) {
    super(mzTolerance, dp);
    this.type = b;
  }

  @Override
  public String getName() {
    return type.toString(false);
  }

  /**
   * The ion type behind this annotation
   *
   * @return an ion type
   */
  public IonType getType() {
    return type;
  }
}
