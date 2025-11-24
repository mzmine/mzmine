/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util.reporting.jasper;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.TextParameter;
import java.util.Map;
import javax.validation.constraints.NotNull;

public class ReportingOrderParameters extends SimpleParameterSet {

  public static final StringParameter orderNumber = new StringParameter("Order number",
      "The order number.", "", false);
  public static final StringParameter orderRequestDate = new StringParameter("Order request date",
      "The date the order was requested on.", "", false);
  public static final StringParameter orderFinishedDate = new StringParameter("Order finished date",
      "The date the order was finished on.", "", false);
  public static final StringParameter orderSampleIds = new StringParameter("Order sample IDs",
      "The sample IDs in the order", "", false);
  public static final TextParameter orderDescription = new TextParameter("Order description",
      "Description on what was ordered.", "", false);

  public ReportingOrderParameters() {
    super(orderNumber, orderRequestDate, orderFinishedDate, orderSampleIds, orderDescription);
  }

  public void addToMetadata(@NotNull Map<String, Object> jasperParam) {
    jasperParam.put("META_ORDER_NUMBER", getValue(ReportingOrderParameters.orderNumber));
    jasperParam.put("META_ORDER_REQUEST_DATE", getValue(ReportingOrderParameters.orderRequestDate));
    jasperParam.put("META_ORDER_FINISHED_DATE",
        getValue(ReportingOrderParameters.orderFinishedDate));
    jasperParam.put("META_ORDER_SAMPLEIDS", getValue(ReportingOrderParameters.orderSampleIds));
    jasperParam.put("META_ORDER_DESC", getValue(ReportingOrderParameters.orderDescription));
  }
}
