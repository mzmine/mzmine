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

package io.mzmine.reports;

/**
 * 227 x 146 pixels.
 * <br>
 * figure must be the bytes of an SVG-XML or a BufferedImage.
 */
public class TwoColumnRow {

  public static final int WIDTH = 227;
  public static final int HEIGHT = 146;

  private final Object figure1;
  private final String figure1Caption;
  private final Object figure2;
  private final String figure2Caption;

  public TwoColumnRow(Object figure1, String figure1Caption, Object figure2, String figure2Caption) {
    this.figure1 = figure1;
    this.figure1Caption = figure1Caption;
    this.figure2 = figure2;
    this.figure2Caption = figure2Caption;
  }

  public Object getFigure1() {
    return figure1;
  }

  public String getFigure1Caption() {
    return figure1Caption;
  }

  public Object getFigure2() {
    return figure2;
  }

  public String getFigure2Caption() {
    return figure2Caption;
  }
}
