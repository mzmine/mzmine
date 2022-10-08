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

package io.github.mzmine.gui.chartbasics.simplechart.providers;


/**
 * Combines all necessary interfaces of a given dataset to be plotted in an XY-Chart. Check the
 * specific interfaces for a detailed desription.
 * <p></p>
 * Basically, any class implementing this interface can be conveniently plotted in a
 * {@link io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart} without the need to create a
 * specific {@link org.jfree.chart.JFreeChart} plot class, dataset, renderer, label generator or
 * tooltip generator.
 * <p></p>
 * Since some datasets require a computation of their values, the {@link XYValueProvider} interface
 * offers the {@link XYValueProvider#computeValues} method to move these computations to a different
 * thread. The values are grabbed after the computation has finished and the plot is updated
 * automatically.
 * <p></p>
 * For a more detailed description, one shall be referred to the interfaces listed below.
 *
 * @author https://github.com/SteffenHeu
 * @see io.github.mzmine.gui.chartbasics.simplechart.providers.ColorProvider
 * @see io.github.mzmine.gui.chartbasics.simplechart.providers.LabelTextProvider
 * @see io.github.mzmine.gui.chartbasics.simplechart.providers.SeriesKeyProvider
 * @see io.github.mzmine.gui.chartbasics.simplechart.providers.ToolTipTextProvider
 * @see io.github.mzmine.gui.chartbasics.simplechart.providers.XYValueProvider
 * @see ExampleXYProvider
 */
public interface PlotXYDataProvider extends XYValueProvider, SeriesKeyProvider<Comparable<?>>,
    LabelTextProvider, ToolTipTextProvider, ColorProvider {

}
