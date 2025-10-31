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
import java.util.Map;
import javax.validation.constraints.NotNull;

public class ReportingCustomerParameters extends SimpleParameterSet {

  public static final StringParameter customerName = new StringParameter("Customer name",
      "The name of the customer.");
  public static final StringParameter customerDepartment = new StringParameter(
      "Customer department", "The department of the customer.");
  public static final StringParameter customerAddress = new StringParameter("Customer address",
      "The address of the customer.");
  public static final StringParameter customerProject = new StringParameter(
      "Customer project reference", "A reference provided to the customer.");

  public ReportingCustomerParameters() {
    super(customerName, customerDepartment, customerAddress, customerProject);
  }

  public void addToMetadata(@NotNull Map<String, Object> jasperParam) {
    jasperParam.put("META_CUSTOMER_NAME", getValue(ReportingCustomerParameters.customerName));
    jasperParam.put("META_CUSTOMER_DEPARTMENT",
        getValue(ReportingCustomerParameters.customerDepartment));
    jasperParam.put("META_CUSTOMER_ADDRESS", getValue(ReportingCustomerParameters.customerAddress));
    jasperParam.put("META_CUSTOMER_PROJECT", getValue(ReportingCustomerParameters.customerProject));
  }
}
