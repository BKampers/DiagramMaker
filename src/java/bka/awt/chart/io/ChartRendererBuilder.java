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


    public ChartRendererBuilder(Figures figures) {
        this.figures = figures;
    }


    public ChartRenderer buildChartRenderer(ChartConfiguration chartConfiguration) throws ChartConfigurationException {
        ChartRenderer chartRenderer = new ChartRenderer();
        chartRenderer.setLocale(buildLocale(chartConfiguration.getLocale()));
        chartRenderer.setMargins(
            nonNullInt(chartConfiguration.getLeftMargin(), 0),
            nonNullInt(chartConfiguration.getRightMargin(), 0),
            nonNullInt(chartConfiguration.getTopMargin(), 0),
            nonNullInt(chartConfiguration.getBottomMargin(), 0));
        chartRenderer.setTitle(chartConfiguration.getTitle());
        if (chartConfiguration.getLegendPosition() != null) {
            chartRenderer.setLegendRenderer(new DefaultLegendRenderer(chartConfiguration.getLegendPosition().getX(), chartConfiguration.getLegendPosition().getY()));
        }
        chartRenderer.setXWindowMinimum(chartConfiguration.getXWindowMinimum());
        chartRenderer.setXWindowMaximum(chartConfiguration.getXWindowMaximum());
        chartRenderer.setYWindowMinimum(chartConfiguration.getYWindowMinimum());
        chartRenderer.setYWindowMaximum(chartConfiguration.getYWindowMaximum());
        if (chartConfiguration.getYWindows() != null) {
            chartConfiguration.getYWindows().forEach((key, range) -> chartRenderer.setYWindow(key, range.getMinimum(), range.getMaximum()));
        }
        chartRenderer.setOffsets(nonNullInt(chartConfiguration.getLeftOffset()), nonNullInt(chartConfiguration.getRightOffset()));
        chartRenderer.setExpandToGrid(chartConfiguration.isXExpandToGrid(), chartConfiguration.isYExpandToGrid());
        chartRenderer.setGridRenderer(buildGridRenderer(chartConfiguration.getGridStyle()), chartConfiguration.getGridMode());
        chartRenderer.setXGrid(buildGrid(chartConfiguration.getXGrid(), figures.getXTypes()));
        chartRenderer.setYGrid(buildGrid(chartConfiguration.getYGrid(), figures.getYTypes()));
        buildDataRenderers(chartConfiguration).forEach((key, renderer) -> chartRenderer.setRenderer(key, renderer));
        if (chartConfiguration.getXAxes() != null) {
            chartRenderer.setXAxisRenderers(buildAxisRenderers(chartConfiguration.getXAxes(), chartConfiguration.getAxisStyleDefaults()));
        }
        if (chartConfiguration.getYAxes() != null) {
            chartRenderer.setYAxisRenderers(buildAxisRenderers(chartConfiguration.getYAxes(), chartConfiguration.getAxisStyleDefaults()));
        }
        return chartRenderer;
    }


    private Map<String, AbstractDataAreaRenderer> buildDataRenderers(ChartConfiguration chartConfiguration) throws ChartConfigurationException {
        Map<String, AbstractDataAreaRenderer> renderers = new LinkedHashMap<>();
        if (chartConfiguration.getGraphs() != null) {
            buildConfiguredDataRenderers(chartConfiguration, renderers);
        }
        else {
            buildDefaultDataRenderers(chartConfiguration, renderers);
        }
        stackRenderers(chartConfiguration.getStack(), renderers);
        return renderers;
    }


    private void buildConfiguredDataRenderers(ChartConfiguration chartConfiguration, Map<String, AbstractDataAreaRenderer> renderers) throws ChartConfigurationException {
        rendererIndex = 0;
        for (Map.Entry<String, DataRendererConfiguration> entry : chartConfiguration.getGraphs().entrySet()) {
            AbstractDataAreaRenderer renderer = buildDataRenderer(MergeUtil.merge(
                chartConfiguration.getGraphDefaults(),
                entry.getValue()));
            renderers.put(entry.getKey(), renderer);
            rendererIndex++;
        }
    }


    private void buildDefaultDataRenderers(ChartConfiguration chartConfiguration, Map<String, AbstractDataAreaRenderer> renderers) throws ChartConfigurationException {
        ArrayList<String> keys = new ArrayList(figures.getChartData().keySet());
        for (rendererIndex = 0; rendererIndex < keys.size(); ++rendererIndex) {
            AbstractDataAreaRenderer renderer = buildDataRenderer(chartConfiguration.getGraphDefaults());
            if (renderer == null) {
                renderer = defaultDataRenderer(defaultColor());
            }
            renderers.put(keys.get(rendererIndex), renderer);
        }
    }


    private static void stackRenderers(java.util.List<String> stackArray, Map<String, AbstractDataAreaRenderer> renderers) throws ChartConfigurationException {
        if (stackArray != null) {
            LinkedList<String> stack = new LinkedList(stackArray);
            AbstractDataAreaRenderer stackBase = null;
            while (! stack.isEmpty()) {
                String key = stack.remove();
                AbstractDataAreaRenderer renderer = renderers.get(key);
                if (renderer == null) {
                    throw new ChartConfigurationException("Unknown graph in stack" + key);
                }
                if (stack.contains(key)) {
                    throw new ChartConfigurationException("Duplicate graph in stack" + key);
                }
                if (stackBase != null) {
                    if (! renderer.supportStack() || ! stackBase.supportStack()) {
                        throw new ChartConfigurationException("Stacking not supported by graph");
                    }
                    renderer.setStackBase(stackBase);
                }
                stackBase = renderer;
            }
        }
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


    private Collection<AxisRenderer> buildAxisRenderers(java.util.List<AxisConfiguration> axisConfigurations, AxisStyleConfiguration defaultAxisStyleConfiguration) throws ChartConfigurationException {
        Collection<AxisRenderer> axisRenderers = new ArrayList<>();
        if (axisConfigurations == null) {
            axisRenderers.add(new DefaultAxisRenderer(ChartRenderer.AxisPosition.ORIGIN, Color.GRAY));
        }
        else {
            for (AxisConfiguration axisConfiguration : axisConfigurations) {
                axisRenderers.add(buildAxisRenderer(axisConfiguration, defaultAxisStyleConfiguration));
            }
        }
        return axisRenderers;
    }


    private AxisRenderer buildAxisRenderer(AxisConfiguration axisConfiguration, AxisStyleConfiguration defaultAxisStyleConfiguration) throws ChartConfigurationException {
        DefaultAxisRenderer axisRenderer = new DefaultAxisRenderer(
            (axisConfiguration.getPosition() == null) ? ChartRenderer.AxisPosition.ORIGIN : axisConfiguration.getPosition(),
            buildAxisStyle(MergeUtil.merge(axisConfiguration.getAxisStyle(), defaultAxisStyleConfiguration)),
            axisConfiguration.getKey());
        axisRenderer.setTitle(axisConfiguration.getTitle());
        axisRenderer.setUnit(axisConfiguration.getUnit());
        return axisRenderer;
    }


    private AxisStyle buildAxisStyle(AxisStyleConfiguration axisStyleConfiguration) throws ChartConfigurationException {
        AxisStyle axisStyle = new AxisStyle(null);
        if (axisStyleConfiguration != null) {
            axisStyle.setLabelOffset(nonNullInt(axisStyleConfiguration.getLabelOffset(), DEFAULT_LABEL_OFFSET));
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


    private GridRenderer buildGridRenderer(GridStyleConfiguration gridStyleConfiguration) throws ChartConfigurationException {
        if (gridStyleConfiguration == null) {
            return null;
        }
        GridRenderer gridRenderer = new DefaultGridRenderer(buildGridStyle(gridStyleConfiguration));
        return gridRenderer;
    }


    private Grid buildGrid(GridConfiguration gridMarkerConfiguration, Map<Object, EnumSet<Figures.DataType>> dataTypes) {
        if (gridMarkerConfiguration != null && gridMarkerConfiguration.getType() != null) {
            switch (gridMarkerConfiguration.getType()) {
                case "Text":
                    return new MapGrid(figures.getLabels());
                case "Integer":
                    return new IntegerGrid();
            }
        }
        return buildGrid(figures.typeSet(dataTypes));
    }


    private Grid buildGrid(EnumSet<Figures.DataType> types) {
        switch (getMajorDataType(types)) {
            case TEXT:
                return new MapGrid(figures.getLabels() );
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
        Stroke xStroke = buildStroke((gridStyleConfiguration.getXStroke() != null) ? gridStyleConfiguration.getXStroke() : gridStyleConfiguration.getStroke());
        Stroke yStroke = buildStroke((gridStyleConfiguration.getYStroke() != null) ? gridStyleConfiguration.getYStroke() : gridStyleConfiguration.getStroke());
        Color xColor = awtBuilder.buildColor((gridStyleConfiguration.getXColor() != null) ? gridStyleConfiguration.getXColor() : gridStyleConfiguration.getColor());
        Color yColor = awtBuilder.buildColor((gridStyleConfiguration.getYColor() != null) ? gridStyleConfiguration.getYColor() : gridStyleConfiguration.getColor());
        if (xStroke == null && xColor != null) {
            xStroke = new BasicStroke();
        }
        if (yStroke == null && yColor != null) {
            yStroke = new BasicStroke();
        }
        return GridStyle.create(xStroke, xColor, yStroke, yColor);
    }


    private AbstractDataAreaRenderer buildDataRenderer(DataRendererConfiguration dataRendererConfiguration) throws ChartConfigurationException {
        if (dataRendererConfiguration == null) {
            return null;
        }
        if (dataRendererConfiguration.getType() != null) {
            switch (dataRendererConfiguration.getType()) {
                case "pie":
                    return buildPieRenderer(dataRendererConfiguration);
                case "scatter":
                    return buildScatterRenderer(dataRendererConfiguration);
                case "line":
                    return buildLineRenderer(dataRendererConfiguration);
                case "bar":
                    return buildBarRenderer(dataRendererConfiguration);
            }
        }
        return buildMarkerRenderer(dataRendererConfiguration);
    }


    private AbstractDataAreaRenderer buildPieRenderer(DataRendererConfiguration dataRendererConfiguration) throws ChartConfigurationException {
        return new DefaultPieSectorRenderer(buildPieDrawStyle(dataRendererConfiguration.getPieDrawStyle()));
    }


    private AbstractDataAreaRenderer buildScatterRenderer(DataRendererConfiguration dataRendererConfiguration) throws ChartConfigurationException {
        return new ScatterRenderer(buildPointDrawStyle(dataRendererConfiguration.getGraphDrawStyle()), 3.0f);
    }


    private AbstractDataAreaRenderer buildLineRenderer(DataRendererConfiguration dataRendererConfiguration) throws ChartConfigurationException {
        CoordinateAreaRenderer markerRenderer = null;
        if (dataRendererConfiguration.getMarker() != null) {
            markerRenderer = buildMarkerRenderer(dataRendererConfiguration.getMarker());
        }
        return new DefaultLineRenderer(buildLineDrawStyle(dataRendererConfiguration.getGraphDrawStyle()), markerRenderer);
    }


    private CoordinateAreaRenderer buildMarkerRenderer(DataRendererConfiguration dataRendererConfiguration) throws ChartConfigurationException {
        if (dataRendererConfiguration.getType() != null) {
            switch (dataRendererConfiguration.getType()) {
                case "rectangle":
                    return buildRectangleRenderer(dataRendererConfiguration);
                case "star":
                    return buildStarRenderer(dataRendererConfiguration);
            }
        }
        return buildOvalDotRenderer(dataRendererConfiguration);

    }


    private BarRenderer buildBarRenderer(DataRendererConfiguration dataRendererConfiguration) throws ChartConfigurationException {
        final int width = (dataRendererConfiguration.getWidth() != null) ? dataRendererConfiguration.getWidth() : DEFAULT_BAR_WIDTH;
        BarRenderer renderer = new BarRenderer(
            width,
            buildBarDrawStyle(dataRendererConfiguration.getGraphDrawStyle()),
            dataRendererConfiguration.getBase());
        if (Boolean.TRUE.equals(dataRendererConfiguration.getAutoShift())) {
            renderer.setShift(Math.round((-(figures.getChartData().size() / 2.0f) + rendererIndex) * width + width / 2.0f));
        }
        else if (dataRendererConfiguration.getShift() != null) {
            renderer.setShift(dataRendererConfiguration.getShift());
        }
        return renderer;
    }


    private CoordinateAreaRenderer buildRectangleRenderer(DataRendererConfiguration dataRendererConfiguration) throws ChartConfigurationException {
        return new RectangleDotRenderer(
            (dataRendererConfiguration.getWidth() != null) ? dataRendererConfiguration.getWidth() : DEFAULT_RECTANGLE_WIDTH,
            (dataRendererConfiguration.getHeight() != null) ? dataRendererConfiguration.getHeight() : DEFAULT_RECTANGLE_HEIGHT,
            buildAreaDrawStyle(dataRendererConfiguration.getGraphDrawStyle()));
    }


    private CoordinateAreaRenderer buildOvalDotRenderer(DataRendererConfiguration dataRendererConfiguration) throws ChartConfigurationException {
        return new OvalDotRenderer(
            (dataRendererConfiguration.getWidth() != null) ? dataRendererConfiguration.getWidth() : DEFAULT_SIZE,
            (dataRendererConfiguration.getHeight() != null) ? dataRendererConfiguration.getWidth() : DEFAULT_SIZE,
            buildAreaDrawStyle(dataRendererConfiguration.getGraphDrawStyle()));
    }


    private CoordinateAreaRenderer buildStarRenderer(DataRendererConfiguration dataRendererConfiguration) throws ChartConfigurationException {
        return new PolygonDotRenderer(
            PolygonFactory.createStar((dataRendererConfiguration.getPoints() != null) ? Math.max(dataRendererConfiguration.getPoints(), MIN_STAR_POINTS) : rendererIndex + 3,
                (dataRendererConfiguration.getInnerRadius() != null) ? dataRendererConfiguration.getInnerRadius() : DEFAULT_INNER_RADIUS,
                (dataRendererConfiguration.getOuterRadius() != null) ? dataRendererConfiguration.getOuterRadius() : DEFAULT_OUTER_RADIUS),
            buildAreaDrawStyle(dataRendererConfiguration.getGraphDrawStyle()));
    }


    private AbstractDataAreaRenderer defaultDataRenderer(java.awt.Color color) {
        return new OvalDotRenderer(DEFAULT_SIZE, DEFAULT_SIZE, DefaultDrawStyle.createSolid(color));
    }


    private LineDrawStyle buildLineDrawStyle(AreaDrawStyleConfiguration areaDrawStyleConfiguration) throws ChartConfigurationException {
        Color color = awtBuilder.buildColor(areaDrawStyleConfiguration.getColor());
        Stroke stroke = buildStroke(areaDrawStyleConfiguration.getStroke());
        if (color == null && stroke != null) {
            color = defaultColor();
        }
        if (stroke == null && color != null) {
            stroke = new BasicStroke();
        }
        AreaDrawStyle markerDrawStyle = buildAreaDrawStyle(areaDrawStyleConfiguration.getMarkerDrawStyle());
        if (markerDrawStyle == null) {
            markerDrawStyle = DefaultDrawStyle.createSolid(defaultColor());
        }
        LineDrawStyle style = LineDrawStyle.create(color, stroke, markerDrawStyle);
        style.setBottomAreaPaint(awtBuilder.buildColor(areaDrawStyleConfiguration.getBottomAreaColor()));
        style.setTopAreaPaint(awtBuilder.buildColor(areaDrawStyleConfiguration.getTopAreaColor()));
        return style;
    }


    private PieDrawStyle buildPieDrawStyle(PieDrawStyleConfiguration pieDrawStyleConfiguration) throws ChartConfigurationException {
        Color[] defaults = Palette.generateColors(figures.getChartData().values().iterator().next().size());
        if (pieDrawStyleConfiguration == null) {
            return PieDrawStyle.create(defaults);
        }
        if (pieDrawStyleConfiguration.getSliceColors() != null) {
            return PieDrawStyle.create(buildColors(pieDrawStyleConfiguration.getSliceColors(), defaults));
        }
        return PieDrawStyle.create(awtBuilder.buildColors(pieDrawStyleConfiguration.getColors()));
    }


    private PointDrawStyle buildPointDrawStyle(AreaDrawStyleConfiguration areaDrawStyleConfiguration) throws ChartConfigurationException {
        PointDrawStyle style = PointDrawStyle.createLinear(awtBuilder.buildColors(areaDrawStyleConfiguration.getColors()));
        style.setBorder(awtBuilder.buildColor(areaDrawStyleConfiguration.getBorderColor()));
        return style;
    }


    private BarDrawStyle buildBarDrawStyle(AreaDrawStyleConfiguration drawStyleConfiguration) throws ChartConfigurationException {
        if (drawStyleConfiguration == null) {
            return BarDrawStyle.create(defaultColor());
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
        return new BasicStroke(strokeConfiguration.getWidth() != null ? strokeConfiguration.getWidth() : 1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, strokeConfiguration.getDash(), 10.0f);
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


    private Color defaultColor() {
        return Palette.generateColor(rendererIndex, figures.getChartData().size());
    }


    private static int nonNullInt(Integer value) {
        return nonNullInt(value, 0);
    }


    private static int nonNullInt(Integer value, int defaultValue) {
        return (value != null) ? value : defaultValue;
    }


    private int rendererIndex;
   
    private final Figures figures;
   
    private final AwtBuilder awtBuilder = new AwtBuilder();

    private static final int DEFAULT_LABEL_OFFSET = -4;
    private static final int DEFAULT_BAR_WIDTH = 3;
    private static final int DEFAULT_RECTANGLE_HEIGHT = 5;
    private static final int DEFAULT_RECTANGLE_WIDTH = 5;
    private static final int DEFAULT_SIZE = 10;
    private static final int DEFAULT_OUTER_RADIUS = 8;
    private static final int DEFAULT_INNER_RADIUS = 3;
    private static final int MIN_STAR_POINTS = 3;

}
