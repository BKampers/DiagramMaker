/*
** © Bart Kampers
*/

package nl.bartkampers.diagrams;

import bka.awt.chart.*;
import bka.awt.chart.io.*;
import bka.awt.chart.render.*;
import java.awt.*;
import java.util.*;
import java.util.List;


public class ConfigurationCustomizer {


    public ConfigurationCustomizer(ChartConfiguration configuration, Figures figures) {
        this.configuration = configuration;
        this.figures = figures;
    }


    public void adjust() {
        if (configuration.getGraphs() != null) {
            configuration.getGraphs().keySet().forEach(key -> {
                if (configuration.getGraphs().get(key) == null) {
                    configuration.getGraphs().put(key, (configuration.getGraphDefaults() != null) ? configuration.getGraphDefaults() : new DataRendererConfiguration());
                }
            });
        }
        if (! allOfType(PIE)) {
            adjusXAxes();
            adjustYAxes();
        }
        if (allOfType(BAR) && configuration.getStack() == null) {
            adjustBars();
        }
        else {
            adjustDefault();
        }
        allRenderers().stream().filter(renderer -> "line".equals(renderer.getType())).forEach(renderer -> adjustLine(renderer));
    }

    
    private void adjustLine(DataRendererConfiguration renderer) {
        applyIfNull(renderer.getGraphDrawStyle(), () -> renderer.setGraphDrawStyle(createLineDrawStyle()));
    }


    private void adjusXAxes() {
        if (configuration.getXAxes() == null) {
            configuration.setXAxes(createAxesConfiguration());
        }
        if (! configuration.getXAxes().isEmpty()) {
            if (configuration.getBottomMargin() == null) {
                configuration.setBottomMargin(DEFAULT_BOTTOM_MARGIN);
            }
            if (configuration.getTopMargin() == null) {
                configuration.setTopMargin(DEFAULT_TOP_MARGIN);
            }
        }
    }


    private void adjustYAxes() {
        if (configuration.getYAxes() == null) {
            configuration.setYAxes(createAxesConfiguration());
        }
        if (! configuration.getYAxes().isEmpty()) {
            if (configuration.getLeftMargin() == null) {
                configuration.setLeftMargin(DEFAULT_LEFT_MARGIN);
            }
            if (configuration.getRightMargin() == null) {
                configuration.setRightMargin(DEFAULT_RIGHT_MARGIN);
            }
        }
    }


    private boolean allOfType(String type) {
        String defaultType = (configuration.getGraphDefaults() == null) ? null : configuration.getGraphDefaults().getType();
        if (configuration.getGraphs() == null || configuration.getGraphs().isEmpty()) {
            return type.equals(defaultType);
        }
        for (DataRendererConfiguration renderer : configuration.getGraphs().values()) {
            String rendererType = (renderer.getType() == null) ? defaultType : renderer.getType();
            if (! type.equals(rendererType)) {
                return false;
            }
        }
        return true;
    }

    private void adjustBars() {
        int width = configuration.getWidth();
        int graphCount = figures.getChartData().size();
        int pointCount = getPointCount();
        if (pointCount > 1) {
            applyIfNull(configuration.getLeftOffset(), () -> configuration.setLeftOffset(width / pointCount / 2));
            applyIfNull(configuration.getRightOffset(), () -> configuration.setRightOffset(width / pointCount / 2));
        }
        else {
            applyIfNull(configuration.getLeftOffset(), () -> configuration.setLeftOffset(0));
            applyIfNull(configuration.getRightOffset(), () -> configuration.setRightOffset(0));
        }
        applyIfNull(configuration.getGraphDefaults().getWidth(), () -> configuration.getGraphDefaults().setWidth(computeBarWidth(width, graphCount, pointCount)));
        if (! anyShift()) {
            applyIfNull(configuration.getGraphDefaults().getAutoShift(), () -> configuration.getGraphDefaults().setAutoShift(Boolean.TRUE));
        }
        adjustBarYWindow();
    }

    private int computeBarWidth(int width, int graphCount, int pointCount) {
        int barWidth = (width - configuration.getLeftOffset() - configuration.getRightOffset() - configuration.getLeftMargin() - configuration.getRightMargin()) / (graphCount * pointCount + 2);
        if (configuration.getGraphDefaults() == null) {
            configuration.setGraphDefaults(new DataRendererConfiguration());
        }
        return barWidth;
    }

    private int getPointCount() {
        List<Number> xValues = figures.xValues();
        int pointCount;
        if (xValues.size() >= 2) {
            double xMin = xValues.get(0).doubleValue();
            double xMax = xValues.get(xValues.size() - 1).doubleValue();
            pointCount = (int) ((xMax - xMin) / smallestDistance());
        }
        else {
            pointCount = 0;
            for (ChartPoints points : figures.getChartData().values()) {
                pointCount = Math.max(pointCount, points.size());
            }
        }
        return pointCount;
    }

    private double smallestDistance() {
        double smallest = Double.POSITIVE_INFINITY;
        Iterator<Number> it = figures.xValues().iterator();
        if (it.hasNext()) {
            double previous = it.next().doubleValue();
            while (it.hasNext()) {
                double x = it.next().doubleValue();
                smallest = Math.min(smallest, x - previous);
                previous = x;
            }
        }
        return smallest;
    }

    private boolean anyShift() {
        Integer defaultShift = (configuration.getGraphDefaults() == null) ? null : configuration.getGraphDefaults().getShift();
        if (configuration.getGraphs() == null || configuration.getGraphs().isEmpty()) {
            return defaultShift != null;
        }
        for (DataRendererConfiguration renderer : configuration.getGraphs().values()) {
            Integer rendererShift = (renderer.getShift() == null) ? defaultShift : renderer.getShift();
            if (rendererShift != null) {
                return true;
            }
        }
        return false;
    }

    private void adjustBarYWindow() {
        if (configuration.getYWindowMinimum() == null && configuration.getYWindowMaximum() == null) {
            double min = Double.MAX_VALUE;
            double max = - Double.MAX_VALUE;
            for (ChartPoints chartData : figures.getChartData().values()) {
                Iterator<ChartPoint> it = chartData.iterator();
                while (it.hasNext()) {
                    double value = it.next().getY().doubleValue();
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
            }
            if (configuration.getYWindowMinimum() == null && min >= 0.0 && max > 0.0) {
                configuration.setYWindowMinimum(0.0);
            }
            if (configuration.getYWindowMaximum() == null && min < 0.0 && max <= 0.0) {
                configuration.setYWindowMaximum(0.0);
            }
        }
    }


    private void adjustDefault() {
        Dimension symbolDimension = getSymbolDimension();
        if (configuration.getXAxes() != null && configuration.getXAxes().isEmpty()) {
            applyIfNull(configuration.getBottomMargin(), () -> configuration.setBottomMargin(symbolDimension.height / 2));
            applyIfNull(configuration.getTopMargin(), () -> configuration.setTopMargin(symbolDimension.height / 2));
        }
        if (configuration.getYAxes() != null && configuration.getYAxes().isEmpty()) {
            applyIfNull(configuration.getLeftMargin(), () -> configuration.setLeftMargin(symbolDimension.width / 2));
            applyIfNull(configuration.getRightMargin(), () -> configuration.setRightMargin(symbolDimension.width / 2));
        }
    }

    private Dimension getSymbolDimension() {
        Dimension dimension = new Dimension(Math.max(getDefaultSymbolWidth(), 10), Math.max(getDefaultSymbolHeight(), 10));
        if (configuration.getGraphs() != null) {
            for (DataRendererConfiguration renderer : configuration.getGraphs().values()) {
                dimension.width = Math.max(dimension.width, nonNullInt(renderer.getWidth()));
                dimension.height = Math.max(dimension.height, nonNullInt(renderer.getHeight()));
            }
        }
        return dimension;
    }
    

    private int getDefaultSymbolWidth() {
        return (configuration.getGraphDefaults() != null) ? nonNullInt(configuration.getGraphDefaults().getWidth()) : 0;
    }


    private int getDefaultSymbolHeight() {
        return (configuration.getGraphDefaults() != null) ? nonNullInt(configuration.getGraphDefaults().getHeight()) : 0;
    }

    private static void applyIfNull(Object test, Runnable runnable) {
        if (test == null) {
            runnable.run();
        }
    }


    private Collection<DataRendererConfiguration> allRenderers() {
        Collection<DataRendererConfiguration> allRenderers = new ArrayList<>();
        if (configuration.getGraphDefaults() != null) {
            allRenderers.add(configuration.getGraphDefaults());
        }
        if (configuration.getGraphs() != null) {
            allRenderers.addAll(configuration.getGraphs().values());
        }
        return allRenderers;
    }


    private static java.util.List<AxisConfiguration> createAxesConfiguration() {
        AxisConfiguration axisConfiguration = new AxisConfiguration();
        axisConfiguration.setPosition(ChartRenderer.AxisPosition.ORIGIN);
        axisConfiguration.setAxisStyle(createDefaultAxisStyle());
        return Arrays.asList(axisConfiguration);
    }


    private static AxisStyleConfiguration createDefaultAxisStyle() {
        AxisStyleConfiguration configuration = new AxisStyleConfiguration();
        configuration.setAxisColor(AXIS_COLOR);
        configuration.setMarkerColor(AXIS_COLOR);
        configuration.setLabelColor(AXIS_COLOR);
        return configuration;
    }


    private AreaDrawStyleConfiguration createLineDrawStyle() {
        AreaDrawStyleConfiguration style = new AreaDrawStyleConfiguration();
        StrokeConfiguration stroke = new StrokeConfiguration();
        stroke.setWidth(1.0f);
        style.setStroke(stroke);
        return style;
    }


    private static int nonNullInt(Integer value) {
        return (value != null) ? value : 0;
    }


    private final ChartConfiguration configuration;
    private final Figures figures;

    private static final int DEFAULT_BOTTOM_MARGIN = 25;
    private static final int DEFAULT_TOP_MARGIN = 25;
    private static final int DEFAULT_RIGHT_MARGIN = 40;
    private static final int DEFAULT_LEFT_MARGIN = 40;

    private static final String AXIS_COLOR = "777777";

    private static final String BAR = "bar";
    private static final String PIE = "pie";

}
