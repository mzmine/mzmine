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

package io.github.mzmine.modules.visualization.networking.visual.stylers;

import io.github.mzmine.modules.visualization.networking.visual.enums.GraphObject;
import io.github.mzmine.modules.visualization.networking.visual.enums.GraphStyleAttribute;

public sealed abstract class AbstractGraphStyler implements GraphStyler permits GraphColorStyler,
    GraphLabelStyler, GraphSizeStyler {

  protected final GraphObject go;

  public AbstractGraphStyler(final GraphObject go) {
    this.go = go;
  }

  @Override
  public GraphObject getGraphObject() {
    return go;
  }

  @Override
  public boolean matches(GraphObject go, GraphStyleAttribute gsa) {
    if (!this.go.equals(go)) {
      return false;
    }
    return getGraphStyleAttribute().equals(gsa);
  }

  @Override
  public GraphStyleAttribute getGraphStyleAttribute() {
    return switch (this) {
      case GraphColorStyler __ -> GraphStyleAttribute.COLOR;
      case GraphSizeStyler __ -> GraphStyleAttribute.SIZE;
      case GraphLabelStyler __ -> GraphStyleAttribute.LABEL;
    };
  }
}
