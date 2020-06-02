/*
** © Bart Kampers
*/

package bka.awt.chart.io;

import bka.awt.*;
import bka.awt.chart.*;
import bka.awt.chart.custom.*;
import bka.awt.chart.grid.*;
import bka.awt.chart.render.*;
import java.awt.*;
import java.util.*;
import nl.bartkampers.diagrams.*;


public class ChartRendererBuilder {


    public ChartRendererBuilder(Figures figures, Map<Number, String> labeledNumbers) {
        this.figures = figures;
        this.labeledNumbers = new HashMap<>(labeledNumbers);
    }


    public ChartRenderer buildChartRenderer(ChartConfiguration chartConfiguration) throws ChartConfigurationException {
        ChartRenderer chartRenderer = new ChartRenderer();
        chartRenderer.setLocale(buildLocale(chartConfiguration.getLocale()));
        chartRenderer.setMargins((chartConfiguration.getLeftMargin() != null) ? chartConfiguration.getLeftMargin() : DEFAULT_LEFT_MARGIN,
            (chartConfiguration.getRightMargin() != null) ? chartConfiguration.getRightMargin() : DEFAULT_RIGHT_MARGIN,
            (chartConfiguration.getTopMargin() != null) ? chartConfiguration.getTopMargin() : DEFAULT_TOP_MARGIN,
            (chartConfiguration.getBottomMargin() != null) ? chartConfiguration.getBottomMargin() : DEFAULT_BOTTOM_MARGIN);
        chartRenderer.setTitle(chartConfiguration.getTitle());
        if (chartConfiguration.getLegendPosition() != null) {
            chartRenderer.setLegendRenderer(new DefaultLegendRenderer(chartConfiguration.getLegendPosition().getX(), chartConfiguration.getLegendPosition().getY()));
        }
        chartRenderer.setXWindowMinimum(chartConfiguration.getXWindowMinimum());
        chartRenderer.setXWindowMaximum(chartConfiguration.getXWindowMaximum());
        chartRenderer.setYWindowMinimum(chartConfiguration.getYWindowMinimum());
        chartRenderer.setYWindowMaximum(chartConfiguration.getYWindowMaximum());
        if (chartConfiguration.getWindowConfigurations() != null) {
            chartConfiguration.getWindowConfigurations().forEach((key, range) -> chartRenderer.setYWindow(key, range.getYWindowMinimum(), range.getYWindowMaximum()));
        }
        chartRenderer.setOffsets(chartConfiguration.getLeftOffset(), chartConfiguration.getRightOffset());
        chartRenderer.setGridRenderer(buildGridRenderer(chartConfiguration.getGridConfiguration()), chartConfiguration.getGridMode());
        chartRenderer.setXGrid(buildGrid(chartConfiguration.getXGridMarkerConfiguration(), figures.getXTypes()));
        chartRenderer.setYGrid(buildGrid(chartConfiguration.getYGridMarkerConfiguration(), figures.getYTypes()));
        ArrayList<String> keys = new ArrayList(figures.getChartData().keySet());
        axisesRequired = false;
        Map<String, AbstractDataAreaRenderer> renderers = new HashMap<>();
        if (chartConfiguration.getDataRendererConfigurations() == null) {
            Color[] palette = Palette.generateColors(keys.size());
            for (int i = 0; i < keys.size(); ++i) {
                AbstractDataAreaRenderer renderer = buildDataRenderer(chartConfiguration.getDefaultDataRendererConfiguration());
                if (renderer == null) {
                    renderer = defaultDataRenderer(palette[i]);
                }
                renderers.put(keys.get(i), renderer);
                chartRenderer.setRenderer(keys.get(i), renderer);
            }
        }
        else {
            for (Map.Entry<String, DataRendererConfiguration> entry : chartConfiguration.getDataRendererConfigurations().entrySet()) {
                AbstractDataAreaRenderer renderer = buildDataRenderer(DataRendererConfiguration.merge(
                    chartConfiguration.getDefaultDataRendererConfiguration(),
                    entry.getValue()));
                renderers.put(entry.getKey(), renderer);
                chartRenderer.setRenderer(entry.getKey(), renderer);
            }
        }
        if (chartConfiguration.getStack() != null) {
            for (int i = 1; i < chartConfiguration.getStack().length; ++i) {
                AbstractDataAreaRenderer stackBase = renderers.get(chartConfiguration.getStack()[i - 1]);
                if (stackBase == null || ! stackBase.supportStack()) {
                    throw new ChartConfigurationException("Invalid stackBase " + chartConfiguration.getStack()[i - 1]);
                }
                AbstractDataAreaRenderer stacked = renderers.get(chartConfiguration.getStack()[i]);
                if (stacked == null || ! stacked.supportStack()) {
                    throw new ChartConfigurationException("Invalid stack " + chartConfiguration.getStack()[i]);
                }
                stacked.setStackBase(stackBase);
            }
        }
        if (axisesRequired) {
            chartRenderer.setXAxisRenderers(buildAxisRenderers(chartConfiguration.getXAxisConfigurations()));
            chartRenderer.setYAxisRenderers(buildAxisRenderers(chartConfiguration.getYAxisConfigurations()));
        }
        return chartRenderer;
    }


    private java.util.List<Object> getGraphOrder(String[] stack) throws ChartConfigurationException {
        java.util.List<Object> order = new ArrayList<>();
        if (stack != null) {
            for (String key : stack) {
                if (order.contains(key)) {
                    throw new ChartConfigurationException("Duplicate graph on stack: " + key);
                }
                if (! figures.getChartData().keySet().contains(key)) {
                    throw new ChartConfigurationException("Unknown graph on stack: " + key);
                }
                order.add(key);
            }
        }
        for (Object key : figures.getChartData().keySet()) {
            if (! order.contains(key)) {
                order.add(key);
            }
        }
        return order;
    }


    private static Locale buildLocale(String localeCode) {
        if (localeCode != null && ! localeCode.isEmpty()) {
            String[] locales = localeCode.split("-");
            switch (locales.length) {
                case 1:
                    return new Locale(locales[0]);
                case 2:
                    return new Locale(locales[0], locales[1]);
                default:
                    return new Locale(locales[0], locales[1], locales[2]);
            }
        }
        return Locale.getDefault();
    }


    private Collection<AxisRenderer> buildAxisRenderers(AxisConfiguration[] axisConfigurations) throws ChartConfigurationException {
        Collection<AxisRenderer> axisRenderers = new ArrayList<>();
        if (axisConfigurations == null) {
            axisRenderers.add(new DefaultAxisRenderer(ChartRenderer.AxisPosition.MINIMUM));
        }
        else {
            for (AxisConfiguration axisConfiguration : axisConfigurations) {
                axisRenderers.add(buildAxisRenderer(axisConfiguration));
            }
        }
        return axisRenderers;
    }


    private AxisRenderer buildAxisRenderer(AxisConfiguration axisConfiguration) throws ChartConfigurationException {
        DefaultAxisRenderer axisRenderer = new DefaultAxisRenderer(
            (axisConfiguration.getPosition() == null) ? ChartRenderer.AxisPosition.MINIMUM : axisConfiguration.getPosition(),
            buildAxisStyle(axisConfiguration.getAxisStyleConfiguration()),
            axisConfiguration.getKey());
        axisRenderer.setTitle(axisConfiguration.getTitle());
        axisRenderer.setUnit(axisConfiguration.getUnit());

        return axisRenderer;
    }


    private AxisStyle buildAxisStyle(AxisStyleConfiguration axisStyleConfiguration) throws ChartConfigurationException {
        AxisStyle axisStyle = new AxisStyle();
        if (axisStyleConfiguration != null) {
            axisStyle.setLabelOffset((axisStyleConfiguration.getLabelOffset() != null) ? axisStyleConfiguration.getLabelOffset() : DEFAULT_LABEL_OFFSET);
            if (axisStyleConfiguration.getAxisColor() != null) {
                axisStyle.setAxisColor(awtBuilder.buildColor(axisStyleConfiguration.getAxisColor()));
            }
            if (axisStyleConfiguration.getMarkerColor() != null) {
                axisStyle.setMarkerColor(awtBuilder.buildColor(axisStyleConfiguration.getMarkerColor()));
            }
            if (axisStyleConfiguration.getLabelColor() != null) {
                axisStyle.setLabelColor(awtBuilder.buildColor(axisStyleConfiguration.getLabelColor()));
            }
            if (axisStyleConfiguration.getTitleColor() != null) {
                axisStyle.setTitleColor(awtBuilder.buildColor(axisStyleConfiguration.getTitleColor()));
            }
            if (axisStyleConfiguration.getUnitColor() != null) {
                axisStyle.setUnitColor(awtBuilder.buildColor(axisStyleConfiguration.getUnitColor()));
            }
        }
        else {
            axisStyle.setLabelOffset(DEFAULT_LABEL_OFFSET);
        }
        return axisStyle;
    }


    private GridRenderer buildGridRenderer(GridConfiguration gridConfiguration) throws ChartConfigurationException {
        if (gridConfiguration == null) {
            return null;
        }
        GridRenderer gridRenderer = new DefaultGridRenderer(buildGridStyle(gridConfiguration.getGridStyleConfiguration()));
        return gridRenderer;
    }


    private Grid buildGrid(GridMarkerConfiguration gridMarkerConfiguration, Map<Object, EnumSet<Figures.DataType>> dataTypes) {
        if (gridMarkerConfiguration != null && gridMarkerConfiguration.getType() != null) {
            switch (gridMarkerConfiguration.getType()) {
                case "Text":
                    return new MapGrid(labeledNumbers);
                case "Integer":
                    return new IntegerGrid();
            }
        }
        return gridType(figures.typeSet(dataTypes));
    }


    private Grid gridType(EnumSet<Figures.DataType> types) {
        switch (getMajorDataType(types)) {
            case TEXT:
                return new MapGrid(labeledNumbers);
            case DATE:
                return new CalendarGrid();
            case NUMBER:
            default:
                return new NumberGrid();
        }
    }


    private static Figures.DataType getMajorDataType(EnumSet<Figures.DataType> types) {
        for (Figures.DataType type : Figures.DataType.values()) {
            if (types.contains(type)) {
                return type;
            }
        }
        return Figures.DataType.NUMBER;
    }


    private GridStyle buildGridStyle(GridStyleConfiguration gridStyleConfiguration) throws ChartConfigurationException {
        if (gridStyleConfiguration.getBackgrounds() != null) {
            return GridStyle.create(buildStroke(gridStyleConfiguration.getStroke()), awtBuilder.buildColor(gridStyleConfiguration.getColor()), awtBuilder.buildPaintBox(gridStyleConfiguration.getBackgrounds()));
        }
        return GridStyle.create(buildStroke(gridStyleConfiguration.getStroke()), awtBuilder.buildColor(gridStyleConfiguration.getColor()));
    }


    private AbstractDataAreaRenderer buildDataRenderer(DataRendererConfiguration dataRendererConfiguration) throws ChartConfigurationException {
        if (dataRendererConfiguration == null) {
            return null;
        }
        if (dataRendererConfiguration.getType() != null) {
            switch (dataRendererConfiguration.getType()) {
                case "pie":
                    return buildPieRenderer(dataRendererConfiguration);
                case "line":
                    axisesRequired = true;
                    return buildLineRenderer(dataRendererConfiguration);
                case "bar":
                    axisesRequired = true;
                    return buildBarRenderer(dataRendererConfiguration);
                case "rectangle":
                    axisesRequired = true;
                    return buildRectangleRenderer(dataRendererConfiguration);
                case "star":
                    axisesRequired = true;
                    return buildStarRenderer(dataRendererConfiguration);
            }
        }
        axisesRequired = true;
        return buildOvalDotRenderer(dataRendererConfiguration);
    }


    private AbstractDataAreaRenderer buildPieRenderer(DataRendererConfiguration dataRendererConfiguration) throws ChartConfigurationException {
        return new DefaultPieSectorRenderer(buildPieDrawStyle(dataRendererConfiguration.getPieDrawStyleConfiguration()));
    }


    private AbstractDataAreaRenderer buildLineRenderer(DataRendererConfiguration dataRendererConfiguration) throws ChartConfigurationException {
        return new DefaultLineRenderer(buildLineDrawStyle(dataRendererConfiguration.getAreaDrawStyleConfiguration()));
    }


    private LineDrawStyle buildLineDrawStyle(AreaDrawStyleConfiguration areaDrawStyleConfiguration) throws ChartConfigurationException {
        return LineDrawStyle.create(awtBuilder.buildColor(areaDrawStyleConfiguration.getColor()));
    }


    private PieDrawStyle buildPieDrawStyle(PieDrawStyleConfiguration pieDrawStyleConfiguration) throws ChartConfigurationException {
        Color[] defaults = Palette.generateColors(figures.getChartData().values().iterator().next().size());
        if (pieDrawStyleConfiguration == null) {
            return PieDrawStyle.create(defaults);
        }
        if (pieDrawStyleConfiguration.getColorsByKey() != null) {
            return PieDrawStyle.create(buildColors(pieDrawStyleConfiguration.getColorsByKey(), defaults));
        }
        return PieDrawStyle.create(awtBuilder.buildColors(pieDrawStyleConfiguration.getColors()));
    }


    private BarRenderer buildBarRenderer(DataRendererConfiguration dataRendererConfiguration) throws ChartConfigurationException {
        return new BarRenderer(
            (dataRendererConfiguration.getWidth() != null) ? dataRendererConfiguration.getWidth() : DEFAULT_BAR_WIDTH,
            buildBarDrawStyle(dataRendererConfiguration.getAreaDrawStyleConfiguration()),
            dataRendererConfiguration.getBase());
    }


    private AbstractDataAreaRenderer buildRectangleRenderer(DataRendererConfiguration dataRendererConfiguration) throws ChartConfigurationException {
        return new RectangleDotRenderer(
            (dataRendererConfiguration.getWidth() != null) ? dataRendererConfiguration.getWidth() : DEFAULT_RECTANGLE_WIDTH,
            (dataRendererConfiguration.getHeight() != null) ? dataRendererConfiguration.getHeight() : DEFAULT_RECTANGLE_HEIGHT,
            buildAreaDrawStyle(dataRendererConfiguration.getAreaDrawStyleConfiguration()));
    }


    private AbstractDataAreaRenderer buildOvalDotRenderer(DataRendererConfiguration dataRendererConfiguration) throws ChartConfigurationException {
        return new OvalDotRenderer(
            (dataRendererConfiguration.getWidth() != null) ? dataRendererConfiguration.getWidth() : DEFAULT_SIZE,
            (dataRendererConfiguration.getHeight() != null) ? dataRendererConfiguration.getWidth() : DEFAULT_SIZE,
            buildAreaDrawStyle(dataRendererConfiguration.getAreaDrawStyleConfiguration()));
    }


    private AbstractDataAreaRenderer buildStarRenderer(DataRendererConfiguration dataRendererConfiguration) throws ChartConfigurationException {
        return new PolygonDotRenderer(
            PolygonFactory.createStar((dataRendererConfiguration.getPoints() != null) ? Math.max(dataRendererConfiguration.getPoints(), MIN_STAR_POINTS) : DEFAULT_STAR_POINTS,
                (dataRendererConfiguration.getInnerRadius() != null) ? dataRendererConfiguration.getInnerRadius() : DEFAULT_INNER_RADIUS,
                (dataRendererConfiguration.getOuterRadius() != null) ? dataRendererConfiguration.getOuterRadius() : DEFAULT_OUTER_RADIUS),
            buildAreaDrawStyle(dataRendererConfiguration.getAreaDrawStyleConfiguration()));
    }


    private AbstractDataAreaRenderer defaultDataRenderer(java.awt.Color color) {
        return new OvalDotRenderer(DEFAULT_SIZE, DEFAULT_SIZE, DefaultDrawStyle.createSolid(color));
    }


    private BarDrawStyle buildBarDrawStyle(AreaDrawStyleConfiguration drawStyleConfiguration) throws ChartConfigurationException {
        if (drawStyleConfiguration == null) {
            return null;
        }
        BarDrawStyle style;
        if (drawStyleConfiguration.getColor() != null) {
            style = BarDrawStyle.create(awtBuilder.buildColor(drawStyleConfiguration.getColor()));
        }
        else if (drawStyleConfiguration.getColors() != null && drawStyleConfiguration.getColors().length > 0) {
            if (drawStyleConfiguration.getColors().length == 1) {
                style = BarDrawStyle.create(awtBuilder.buildColor(drawStyleConfiguration.getColors()[0]));
            }
            else {
                style = BarDrawStyle.create(
                    awtBuilder.buildColor(drawStyleConfiguration.getColors()[0]),
                    awtBuilder.buildColor(drawStyleConfiguration.getColors()[1]));
            }
        }
        else if (drawStyleConfiguration.getCenterColor() != null && drawStyleConfiguration.getEdgeColor() != null) {
            style = BarDrawStyle.create(awtBuilder.buildColor(drawStyleConfiguration.getCenterColor()), awtBuilder.buildColor(drawStyleConfiguration.getEdgeColor()));
        }
        else {
            style = BarDrawStyle.create(Color.BLUE, Color.BLUE);
        }
        if (drawStyleConfiguration.getBorderColor() != null) {
            if (drawStyleConfiguration.getStroke() != null) {
                style.setBorder(awtBuilder.buildColor(drawStyleConfiguration.getBorderColor()), buildStroke(drawStyleConfiguration.getStroke()));
            }
            else {
                style.setBorder(awtBuilder.buildColor(drawStyleConfiguration.getBorderColor()));
            }
        }
        return style;
    }


    private AreaDrawStyle buildAreaDrawStyle(AreaDrawStyleConfiguration areaDrawStyleConfiguration) throws ChartConfigurationException {
        if (areaDrawStyleConfiguration == null) {
            return null;
        }
        if (areaDrawStyleConfiguration.getColors() != null) {
            PointDrawStyle drawStyle = PointDrawStyle.createLinear(awtBuilder.buildColors(areaDrawStyleConfiguration.getColors()));
            if (areaDrawStyleConfiguration.getBorderColor() != null) {
                drawStyle.setBorder(awtBuilder.buildColor(areaDrawStyleConfiguration.getBorderColor()));
            }
            return drawStyle;
        }
        if (areaDrawStyleConfiguration.getColor() != null && areaDrawStyleConfiguration.getBorderColor() != null && areaDrawStyleConfiguration.getStroke() != null) {
            return DefaultDrawStyle.create(awtBuilder.buildColor(areaDrawStyleConfiguration.getColor()), awtBuilder.buildColor(areaDrawStyleConfiguration.getBorderColor()), buildStroke(areaDrawStyleConfiguration.getStroke()));
        }
        if (areaDrawStyleConfiguration.getColor() != null && areaDrawStyleConfiguration.getBorderColor() != null) {
            return DefaultDrawStyle.create(awtBuilder.buildColor(areaDrawStyleConfiguration.getColor()), awtBuilder.buildColor(areaDrawStyleConfiguration.getBorderColor()));
        }
        if (areaDrawStyleConfiguration.getColor() != null) {
            return DefaultDrawStyle.createSolid(awtBuilder.buildColor(areaDrawStyleConfiguration.getColor()));
        }
        if (areaDrawStyleConfiguration.getBorderColor() != null && areaDrawStyleConfiguration.getStroke() != null) {
            return DefaultDrawStyle.createBorder(awtBuilder.buildColor(areaDrawStyleConfiguration.getBorderColor()), buildStroke(areaDrawStyleConfiguration.getStroke()));
        }
        if (areaDrawStyleConfiguration.getBorderColor() != null) {
            return DefaultDrawStyle.createBorder(awtBuilder.buildColor(areaDrawStyleConfiguration.getBorderColor()));
        }
        if (areaDrawStyleConfiguration.getImage() != null) {
            return DefaultDrawStyle.createImage(awtBuilder.buildImage(areaDrawStyleConfiguration.getImage()));
        }
        return null;
    }


    private static Stroke buildStroke(StrokeConfiguration strokeConfiguration) {
        if (strokeConfiguration == null) {
            return null;
        }
        return new BasicStroke(strokeConfiguration.getWidth(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, strokeConfiguration.getDash(), 10.0f);
    }


    private Color[][] buildColors(Map<String, java.util.List<String>> colorsByKey, Color[] defaults) throws ChartConfigurationException {
        ChartData<Number, Number> chartData = figures.getChartData().values().iterator().next();
        Color[][] colors = new Color[chartData.size()][];
        int i = 0;
        for (ChartDataElement<Number, Number> element : chartData) {
            java.util.List<String> colorCodes = colorsByKey.get(Objects.toString(element.getKey()));
            if (colorCodes != null) {
                colors[i] = awtBuilder.buildColors(colorCodes);
            }
            else {
                colors[i] = new Color[] { defaults[i], defaults[i].darker() };
            }
            ++i;
        }
        return colors;
    }


    private boolean axisesRequired;

    private final Figures figures;
    private final Map<Number, String> labeledNumbers;

    private final AwtBuilder awtBuilder = new AwtBuilder();

    private static final int DEFAULT_BOTTOM_MARGIN = 25;
    private static final int DEFAULT_TOP_MARGIN = 25;
    private static final int DEFAULT_RIGHT_MARGIN = 40;
    private static final int DEFAULT_LEFT_MARGIN = 40;
    private static final int DEFAULT_LABEL_OFFSET = -4;
    private static final int DEFAULT_BAR_WIDTH = 3;
    private static final int DEFAULT_RECTANGLE_HEIGHT = 5;
    private static final int DEFAULT_RECTANGLE_WIDTH = 5;
    private static final int DEFAULT_SIZE = 10;
    private static final int DEFAULT_OUTER_RADIUS = 8;
    private static final int DEFAULT_INNER_RADIUS = 3;
    private static final int DEFAULT_STAR_POINTS = 5;
    private static final int MIN_STAR_POINTS = 3;

}
