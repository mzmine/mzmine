---
name: New chart
description: How to create a new chart, renderer, label generator in the mzmine framework.
---

- When creating new charts, check if we have an appropriate chart, dataset and/or renderer in
  io.github.mzmine.gui.chartbasics.simplechart.
- When reacting to Chart items, check if we have the property exposed in FxBaseChartModel,
  FxXYPlotModel, or FxXYPlot. Most SimpleChart extend one of those classes.
- At minimum use an EChartViewer.
- When passing data to a chart, check if we have an appropriate provider in
  io.github.mzmine.gui.chartbasics.simplechart.providers.
- The providers can be passed to a ColoredXYDataset and a ColoredXYZDataset.
