/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
