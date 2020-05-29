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
import java.io.*;
import java.net.*;
import java.util.*;
import javax.imageio.*;
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
        chartRenderer.setOffsets(chartConfiguration.getLeftOffset(), chartConfiguration.getRightOffset());
        chartRenderer.setGridRenderer(buildGridRenderer(chartConfiguration.getGridConfiguration()), chartConfiguration.getGridMode());
        chartRenderer.setXGrid(buildGrid(chartConfiguration.getXGridMarkerConfiguration(), figures.getXTypes()));
        chartRenderer.setYGrid(buildGrid(chartConfiguration.getYGridMarkerConfiguration(), figures.getYTypes()));
        ArrayList keys = new ArrayList(figures.getChartData().keySet());
        axisesRequired = true;
        if (chartConfiguration.getDataRendererConfigurations() == null) {
            Color[] palette = Palette.generateColors(keys.size());
            for (int i = 0; i < keys.size(); ++i) {
                chartRenderer.setRenderer(keys.get(i), defaultDataRenderer(palette[i]));
            }
        }
        else {
            axisesRequired = false;
            for (DataRendererConfiguration dataRendererConfiguration : chartConfiguration.getDataRendererConfigurations()) {
                chartRenderer.setRenderer(dataRendererConfiguration.getKey(), buildDataRenderer(dataRendererConfiguration));
            }
        }
        if (axisesRequired) {
            chartRenderer.setXAxisRenderers(buildAxisRenderers(chartConfiguration.getXAxisConfigurations()));
            chartRenderer.setYAxisRenderers(buildAxisRenderers(chartConfiguration.getYAxisConfigurations()));
        }
        return chartRenderer;
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
            buildAxisStyle(axisConfiguration.getAxisStyleConfiguration()));
        axisRenderer.setTitle(axisConfiguration.getTitle());
        axisRenderer.setUnit(axisConfiguration.getUnit());
        return axisRenderer;
    }


    private AxisStyle buildAxisStyle(AxisStyleConfiguration axisStyleConfiguration) throws ChartConfigurationException {
        AxisStyle axisStyle = new AxisStyle();
        if (axisStyleConfiguration != null) {
            if (axisStyleConfiguration.getAxisColor() != null) {
                axisStyle.setAxisColor(buildColor(axisStyleConfiguration.getAxisColor()));
            }
            if (axisStyleConfiguration.getMarkerColor() != null) {
                axisStyle.setMarkerColor(buildColor(axisStyleConfiguration.getMarkerColor()));
            }
            if (axisStyleConfiguration.getLabelColor() != null) {
                axisStyle.setLabelColor(buildColor(axisStyleConfiguration.getLabelColor()));
            }
            if (axisStyleConfiguration.getTitleColor() != null) {
                axisStyle.setTitleColor(buildColor(axisStyleConfiguration.getTitleColor()));
            }
            if (axisStyleConfiguration.getUnitColor() != null) {
                axisStyle.setUnitColor(buildColor(axisStyleConfiguration.getUnitColor()));
            }
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
        return bka.awt.chart.custom.GridStyle.create(buildStroke(gridStyleConfiguration.getStroke()), buildColor(gridStyleConfiguration.getColor()));
    }


    private AbstractDataAreaRenderer buildDataRenderer(DataRendererConfiguration dataRendererConfiguration) throws ChartConfigurationException {
        if (dataRendererConfiguration.getType() != null) {
            switch (dataRendererConfiguration.getType()) {
                case "pie":
                    return buildPieRenderer(dataRendererConfiguration);
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


    private PieDrawStyle buildPieDrawStyle(PieDrawStyleConfiguration pieDrawStyleConfiguration) throws ChartConfigurationException {
        if (pieDrawStyleConfiguration.getColorsByKey() != null) {
            return PieDrawStyle.create(buildColors(pieDrawStyleConfiguration.getColorsByKey()));
        }
        return PieDrawStyle.create(buildColors(pieDrawStyleConfiguration.getColors()));
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
            style = BarDrawStyle.create(buildColor(drawStyleConfiguration.getColor()));
        }
        else if (drawStyleConfiguration.getCenterColor() != null && drawStyleConfiguration.getEdgeColor() != null) {
            style = BarDrawStyle.create(buildColor(drawStyleConfiguration.getCenterColor()), buildColor(drawStyleConfiguration.getEdgeColor()));
        }
        else {
            style = BarDrawStyle.create(Color.BLUE, Color.BLUE);
        }
        if (drawStyleConfiguration.getBorderColor() != null) {
            if (drawStyleConfiguration.getStroke() != null) {
                style.setBorder(buildColor(drawStyleConfiguration.getBorderColor()), buildStroke(drawStyleConfiguration.getStroke()));
            }
            else {
                style.setBorder(buildColor(drawStyleConfiguration.getBorderColor()));
            }
        }
        return style;
    }


    private AreaDrawStyle buildAreaDrawStyle(AreaDrawStyleConfiguration areaDrawStyleConfiguration) throws ChartConfigurationException {
        if (areaDrawStyleConfiguration == null) {
            return null;
        }
        if (areaDrawStyleConfiguration.getColors() != null) {
            PointDrawStyle drawStyle = PointDrawStyle.createLinear(buildColors(areaDrawStyleConfiguration.getColors()));
            if (areaDrawStyleConfiguration.getBorderColor() != null) {
                drawStyle.setBorder(buildColor(areaDrawStyleConfiguration.getBorderColor()));
            }
            return drawStyle;
        }
        if (areaDrawStyleConfiguration.getColor() != null && areaDrawStyleConfiguration.getBorderColor() != null && areaDrawStyleConfiguration.getStroke() != null) {
            return DefaultDrawStyle.create(buildColor(areaDrawStyleConfiguration.getColor()), buildColor(areaDrawStyleConfiguration.getBorderColor()), buildStroke(areaDrawStyleConfiguration.getStroke()));
        }
        if (areaDrawStyleConfiguration.getColor() != null && areaDrawStyleConfiguration.getBorderColor() != null) {
            return DefaultDrawStyle.create(buildColor(areaDrawStyleConfiguration.getColor()), buildColor(areaDrawStyleConfiguration.getBorderColor()));
        }
        if (areaDrawStyleConfiguration.getColor() != null) {
            return DefaultDrawStyle.createSolid(buildColor(areaDrawStyleConfiguration.getColor()));
        }
        if (areaDrawStyleConfiguration.getBorderColor() != null && areaDrawStyleConfiguration.getStroke() != null) {
            return DefaultDrawStyle.createBorder(buildColor(areaDrawStyleConfiguration.getBorderColor()), buildStroke(areaDrawStyleConfiguration.getStroke()));
        }
        if (areaDrawStyleConfiguration.getBorderColor() != null) {
            return DefaultDrawStyle.createBorder(buildColor(areaDrawStyleConfiguration.getBorderColor()));
        }
        if (areaDrawStyleConfiguration.getImage() != null) {
            return DefaultDrawStyle.createImage(buildImage(areaDrawStyleConfiguration.getImage()));
        }
        return null;
    }


    private Image buildImage(String imageUrl) throws ChartConfigurationException {
        try {
            return ImageIO.read(new URL(imageUrl));
        }
        catch (IOException ex) {
            throw new ChartConfigurationException("Cannot load image from " + imageUrl, ex);
        }
    }


    private Font buildFont(String fontCode) {
        return Font.getFont(fontCode);
    }


    private Stroke buildStroke(StrokeConfiguration strokeConfiguration) {
        return new BasicStroke(strokeConfiguration.getWidth(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, strokeConfiguration.getDash(), 10.0f);
    }


    private Color[][] buildColors(Map<String, java.util.List<String>> colorsByKey) throws ChartConfigurationException {
        ChartData<Number, Number> chartData = figures.getChartData().values().iterator().next();
        Color[] defaults = Palette.generateColors(chartData.size());
        Color[][] colors = new Color[chartData.size()][];
        int i = 0;
        for (ChartDataElement<Number, Number> element : chartData) {
            java.util.List<String> colorCodes = colorsByKey.get(Objects.toString(element.getKey()));
            if (colorCodes != null) {
                colors[i] = buildColors(colorCodes);
            }
            else {
                colors[i] = new Color[] { defaults[i], defaults[i].darker() };
            }
            ++i;
        }
        return colors;
    }


    private Color[][] buildColors(String[][] colorCodes) throws ChartConfigurationException {
        Color[][] colors = new Color[colorCodes.length][];
        for (int i = 0; i < colorCodes.length; ++i) {
            colors[i] = buildColors(colorCodes[i]);
        }
        return colors;
    }


    private Color[] buildColors(String[] colorCodes) throws ChartConfigurationException {
        Color[] colors = new Color[colorCodes.length];
        for (int i = 0; i < colorCodes.length; ++i) {
            colors[i] = buildColor(colorCodes[i]);
        }
        return colors;
    }


    private Color[] buildColors(java.util.List<String> colorCodes) throws ChartConfigurationException {
        Color[] colors = new Color[colorCodes.size()];
        for (int i = 0; i < colorCodes.size(); ++i) {
            colors[i] = buildColor(colorCodes.get(i));
        }
        return colors;
    }


    private Color buildColor(String colorCode) throws ChartConfigurationException {
        if (NULL.equals(colorCode)) {
            return null;
        }
        try {
            String[] split = colorCode.split("-");
            if (split.length == 1 && (colorCode.length() == 6 || colorCode.length() == 8)) {
                long rgba = Long.parseLong(colorCode, 16);
                return new Color((int) rgba, colorCode.length() == 8);
            }
            if (2 <= split.length && split.length <= 4) {
                int rgba = 0;
                for (int i = 0; i < split.length; ++i) {
                    rgba = (rgba << 8) | Integer.parseInt(split[i]);
                }
                return new Color(rgba, (split.length == 4));
            }
            throw new ChartConfigurationException("Invalid color code " + colorCode);
        }
        catch (NumberFormatException ex) {
            throw new ChartConfigurationException("Invalid color code " + colorCode, ex);
        }
    }

    private boolean axisesRequired;

    private final Figures figures;
    private final Map<Number, String> labeledNumbers;

    private static final String NULL = "null";

    private static final int DEFAULT_BOTTOM_MARGIN = 25;
    private static final int DEFAULT_TOP_MARGIN = 25;
    private static final int DEFAULT_RIGHT_MARGIN = 40;
    private static final int DEFAULT_LEFT_MARGIN = 40;
    private static final int DEFAULT_BAR_WIDTH = 3;
    private static final int DEFAULT_RECTANGLE_HEIGHT = 5;
    private static final int DEFAULT_RECTANGLE_WIDTH = 5;
    private static final int DEFAULT_SIZE = 10;
    private static final int DEFAULT_OUTER_RADIUS = 8;
    private static final int DEFAULT_INNER_RADIUS = 3;
    private static final int DEFAULT_STAR_POINTS = 5;
    private static final int MIN_STAR_POINTS = 3;

}
