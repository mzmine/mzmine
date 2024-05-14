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
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import org.jetbrains.annotations.NotNull;

/**
 * A relationshiop between two data points in an MS/MS spectrum
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class MSMSIonRelationIdentity extends MSMSIonIdentity {

  @NotNull
  protected final DataPoint parentDP;
  @NotNull
  protected final Relation relation;

  /**
   * Create a new ion relationship in an MS/MS spectrum (between two data points dp -> parent)
   *
   * @param mzTolerance tolerance used to find this annotation
   * @param dp          data point that relates to parent by type
   * @param type        the type of relationship
   * @param parentMZ    the parent m/z value
   */
  public MSMSIonRelationIdentity(MZTolerance mzTolerance, DataPoint dp, IonType type,
      double parentMZ) {
    this(mzTolerance, dp, type, new SimpleDataPoint(parentMZ, 0));
  }

  /**
   * Create a new ion relationship in an MS/MS spectrum (between two data points dp -> parent)
   *
   * @param mzTolerance tolerance used to find this annotation
   * @param dp          data point that relates to parent by type
   * @param type        the type of relationship
   * @param parent      the parent data point
   */
  public MSMSIonRelationIdentity(MZTolerance mzTolerance, DataPoint dp, IonType type,
      @NotNull DataPoint parent) {
    super(mzTolerance, dp, type);
    this.parentDP = parent;
    this.relation = Relation.NEUTRAL_LOSS;
  }

  @Override
  public String getName() {
    switch (relation) {
      case NEUTRAL_LOSS:
        return type.getName();
    }
    return super.getName();
  }

  @NotNull
  public Relation getRelation() {
    return relation;
  }

  /**
   * m/z difference between parent and m/z
   *
   * @return parentMZ - getMZ()
   */
  public double getMzDelta() {
    return getParentMZ() - this.getMZ();
  }

  /**
   * The parent m/z
   *
   * @return mass to charge ratio
   */
  public double getParentMZ() {
    return parentDP.getMZ();
  }

  public enum Relation {
    NEUTRAL_LOSS
  }
}
