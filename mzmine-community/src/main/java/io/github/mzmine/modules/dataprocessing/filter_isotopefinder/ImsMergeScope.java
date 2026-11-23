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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

/**
 * The retention time scope the isotope finder merges an IMS feature's mobility scans over. The
 * mobility scope is always the mobility FWHM (or the feature's full mobility range when no FWHM can
 * be determined); this only decides how many frames contribute.
 * <p>
 * Not a user parameter yet - flip {@link IsotopeFinderTask#IMS_MERGE_SCOPE} to compare the two on
 * real data before deciding which one to ship.
 */
public enum ImsMergeScope {

  /**
   * Only the mobility scans of the feature's representative (apex) frame. Cheapest and the least
   * exposed to co-eluting ions, but on TIMS the mobility FWHM of one frame is often only a handful
   * of scans, so weak M+1/M+2 signals can still fall below the mass detection threshold.
   */
  APEX_FRAME,

  /**
   * The mobility scans of every frame within the feature's retention time FWHM (all frames of the
   * feature when no RT FWHM is available). Roughly an order of magnitude more scans than
   * {@link #APEX_FRAME}, so much better counting statistics, at the cost of admitting ions that
   * co-elute AND co-migrate across the whole peak width.
   */
  RT_FWHM
}
