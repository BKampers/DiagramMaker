/*
** © Bart Kampers
*/

package nl.bartkampers.diagrams;

import bka.awt.*;
import bka.awt.chart.*;
import bka.awt.chart.custom.*;
import bka.awt.chart.io.*;
import bka.awt.chart.render.*;
import bka.mail.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.function.*;
import java.util.logging.*;
import javax.imageio.*;
import javax.mail.*;
import javax.servlet.http.*;
import net.sourceforge.yamlbeans.*;
import org.json.*;


public class DiagramMaker {


    public String getConfiguration() {
        if (EXAMPLE.equals(source)) {
            return readString(configuration);
        }
        if (configuration == null) {
            configuration = readString("southpole_configuration_example.json");
        }
        return configuration;
    }


    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }


    public String getFigures() {
        if (EXAMPLE.equals(source)) {
            return readString(figures);
        }
        if (figures == null) {
            figures = readString("southpole_figures_example.csv");
        }
        return figures;
    }

    
    public void setFigures(String figures) { 
        figuresModified = figures != null && ! figures.equals(this.figures);
        this.figures = figures;
    }


    public String getSource() {
        return source;
    }


    public void setSource(String source) {
        this.source = source;
    }


    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    
    public String getBase64() {
        exception = null;
        try {
            String image = createBase64(createImage());
            sendMail();
            return image;
        }
        catch (JSONException ex) {
            log(Level.INFO, "Could not create image", ex);
            return "";
        }
    }


    public String getStatusText() {
        if (exception != null) {
            return exception;
        }
        return String.format("Image size: %d bytes. Rendered in %d ms. Streamed in %d ms.", imageSize, renderingDuration, streamingDuration);
    }


    public Map<String, String[]> getExamples() {
        Map<String, String> figuresMap = new HashMap<>();
        Map<String, String> configurationsMap = new HashMap<>();
        File resourcesDir = new File(getRealPath(RESOURCES_DIRECTORY));
        for (String filename: resourcesDir.list()) {
            int index = filename.indexOf("_figures_example");
            if (index > 0) {
                figuresMap.put(filename.substring(0, index), filename);
            }
            index = filename.indexOf("_configuration_example");
            if (index > 0) {
                String key = filename.substring(0, index);
                if (! configurationsMap.containsKey(key) || filename.endsWith(".yml")) {
                    configurationsMap.put(key, filename);
                }
            }
        }
        Map<String, String[]> examples = new HashMap<>();
        for (String key : configurationsMap.keySet()) {
            if (figuresMap.containsKey(key)) {
                examples.put(key, new String[] {configurationsMap.get(key), figuresMap.get(key)});
            }
        }
        return examples;
    }


    public void sendMail(String name, String address, String message) {
        Thread mailThread = new Thread(() -> mail("Message sent", String.format("Name: %s\nAddress: %s\n\nMessage:\n%s", name, address, message)));
        mailThread.start();
    }


    public String getException() {
        return exception;
    }


    private BufferedImage createImage() throws JSONException {
        return createImage(BufferedImage.TYPE_INT_ARGB);
    }


    private BufferedImage createImage(int type) throws JSONException {
        Configuration loaded;
//        try {
            try {
               loaded = loadConfiguration(new YamlReader(getConfiguration()));
            }
            catch (ChartConfigurationException ex) {
                getLogger().log(Level.WARNING, "Could not read configuration", ex);
//                loaded = loadConfiguration();
                return null;
            }
            return createImage(loaded, type);
//        }
//        catch (UserDataException ex) {
//            log(Level.INFO, "Invalid user data", ex);
//            return null;
//        }
    }


    private BufferedImage createImage(Configuration loaded, int type) {
        BufferedImage image = new BufferedImage((loaded.width != null) ? loaded.width : 500, (loaded.height != null) ? loaded.height : 400, type);
        Graphics2D g2d = image.createGraphics();
        try {
            renderingDuration = 0;
            long startTime = System.currentTimeMillis();
            loaded.chartRenderer.paint(g2d, new Rectangle(0, 0, image.getWidth(), image.getHeight()));
            renderingDuration = System.currentTimeMillis() - startTime;
        }
        catch (ChartDataException ex) {
            log(Level.INFO, "Invalid user data", ex);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setColor(Color.RED);
            exception = ex.getMessage();
            g2d.drawString(exception, 0, 200);
        }
        return image;
    }

    
//    private Configuration loadConfiguration() throws JSONException, UserDataException {
//        Configuration loaded = new Configuration();
//        loaded.chartRenderer = getChartRenderer();
//        JSONObject configurationObject = getConfigurationObject();
//        loaded.width = (configurationObject.has(WIDTH)) ? configurationObject.optInt(WIDTH) : null;
//        loaded.height = (configurationObject.has(HEIGHT)) ? configurationObject.optInt(HEIGHT) : null;
//        return loaded;
//    }


    private class Configuration {
        Integer width;
        Integer height;
        ChartRenderer chartRenderer;
    }


//    private Configuration loadConfiguration(File file) throws ChartConfigurationException, JSONException, UserDataException {
//        try {
//            YamlReader reader = new YamlReader(new FileReader(file));
//            return loadConfiguration(reader);
//        }
//        catch (FileNotFoundException ex) {
//            throw new ChartConfigurationException(ex);
//        }
//    }


    private Configuration loadConfiguration(YamlReader reader) throws ChartConfigurationException {
        Configuration result = new Configuration();
        try {
            Figures parsedFigures = PARSER.getFigures(getFigures());
            populateYamlConfig(reader.getConfig());
            ChartConfiguration chart = reader.read(ChartConfiguration.class);
            if (chart == null) {
                chart = new ChartConfiguration();
            }
            else {
                storeYaml(chart);
            }
            ChartRendererBuilder builder = new ChartRendererBuilder(parsedFigures, PARSER.getLabeledNumbers());
            result.chartRenderer = builder.buildChartRenderer(chart);
            Map<Object, ChartData<Number, Number>> chartData = parsedFigures.getChartData();
            result.chartRenderer.setCharts(chartData);
            result.width = chart.getWidth();
            result.height = chart.getHeight();
            return result;
        }
        catch (YamlException | JSONException | UserDataException ex) {
            throw new ChartConfigurationException(ex);
        }
    }


    private void storeYaml(ChartConfiguration chartConfiguration) {
        File file = getPath(RESOURCES_DIRECTORY, "out.yaml").toFile();
        if (! file.exists()) {
            try {
                YamlWriter writer = new YamlWriter(new FileWriter(file));
                populateYamlConfig(writer.getConfig());
                writer.write(chartConfiguration);
                writer.close();
            }
            catch (IOException | YamlException ex) {
                Logger.getLogger(DiagramMaker.class.getName()).log(Level.FINE, "Error writing Yaml", ex);
            }
        }
    }


    private static void populateYamlConfig(YamlConfig config) {
        config.setClassTag("Chart", ChartConfiguration.class);
        config.setClassTag("DataRenderer", DataRendererConfiguration.class);
        config.setPropertyElementType(ChartConfiguration.class, "XAxisConfigurations", AxisConfiguration.class);
        config.setPropertyElementType(ChartConfiguration.class, "YAxisConfigurations", AxisConfiguration.class);
        config.setPropertyElementType(ChartConfiguration.class, "dataRendererConfigurations", DataRendererConfiguration.class);
        config.setPropertyElementType(ChartConfiguration.class, "windowConfigurations", RangeConfiguration.class);
    }


//    private ChartRenderer getChartRenderer() throws UserDataException, JSONException {
//        JSONObject configurationObject = getConfigurationObject();
//        ChartRenderer chartRenderer = new ChartRenderer();
//        chartRenderer.setMargins(
//            configurationObject.optInt(LEFT_MARGIN, 40),
//            configurationObject.optInt(RIGHT_MARGIN, 40),
//            configurationObject.optInt(TOP_MARGIN, 25),
//            configurationObject.optInt(BOTTOM_MARGIN, 25));
//        chartRenderer.setOffsets(configurationObject.optInt(LEFT_OFFSET), configurationObject.optInt(RIGHT_OFFSET));
//        chartRenderer.setExpandToGrid(configurationObject.optBoolean(X_EXPAND_TO_GRID), configurationObject.optBoolean(Y_EXPAND_TO_GRID));
//        PARSER.applyNumber(configurationObject, X_WINDOW_MINIMUM, chartRenderer::setXWindowMinimum);
//        PARSER.applyNumber(configurationObject, Y_WINDOW_MINIMUM, chartRenderer::setYWindowMinimum);
//        PARSER.applyNumber(configurationObject, X_WINDOW_MAXIMUM, chartRenderer::setXWindowMaximum);
//        PARSER.applyNumber(configurationObject, Y_WINDOW_MAXIMUM, chartRenderer::setYWindowMaximum);
//        JSONArray legendPosition = configurationObject.optJSONArray("legend_position");
//        if (legendPosition != null) {
//            chartRenderer.setLegendRenderer(new DefaultLegendRenderer(legendPosition.getInt(0), legendPosition.getInt(1)));
//        }
//        PARSER.clearLabeledNumbers();
//        Figures parsedFigures = PARSER.getFigures(getFigures());
//        Map<Object, ChartData<Number, Number>> chartData = parsedFigures.getChartData();
//        Map<Object, AbstractDataAreaRenderer> rendererMap = new HashMap<>();
//        Color[] palette = Palette.generateColors(chartData.keySet().size());
//        JSONArray stack = configurationObject.optJSONArray(STACK);
//        Set<Object> keySet = new HashSet<>(chartData.keySet());
//        java.util.List<Object> keyList = new ArrayList<>();
//        int stackLength = 0;
//        if (stack != null) {
//            stackLength = stack.length();
//            for (int s = 0; s < stackLength; ++s) {
//                Object key = stack.opt(s);
//                if (! keySet.contains(key)) {
//                    throw new UserDataException(String.format("Undefined graph '%s' in stack", key));
//                }
//                if (keyList.contains(key)) {
//                    throw new UserDataException(String.format("Duplicate graph '%s' in stack", key));
//                }
//                keyList.add(key);
//            }
//        }
//        for (Object key : keySet) {
//            if (! keyList.contains(key)) {
//                keyList.add(key);
//            }
//        }
//        JSONObject rendererObject = configurationObject.optJSONObject(GRAPH);
//        boolean drawGrids = false;
//        for (int k = 0; k < keyList.size(); ++k) {
//            Object key = keyList.get(k);
//            AbstractDataAreaRenderer stackBase = null;
//            if (0 < k && k < stackLength) {
//                stackBase = rendererMap.get(keyList.get(k - 1));
//            }
//            JSONObject mergedRendererObject = rendererObject;
//            if (configurationObject.has(GRAPHS)) {
//                JSONObject renderers = configurationObject.getJSONObject(GRAPHS);
//                if (renderers.has(key.toString())) {
//                    mergedRendererObject = merge(rendererObject, renderers.getJSONObject(key.toString()));
//                }
//            }
//            AbstractDataAreaRenderer renderer = createRenderer(parsedFigures, mergedRendererObject, palette, k);
//            drawGrids |= ! (renderer instanceof PieSectorRenderer);
//            if (stackBase != null) {
//                if (! renderer.supportStack() || ! stackBase.supportStack()) {
//                    throw new UserDataException("Stacking not supported by graph");
//                }
//                renderer.setStackBase(stackBase);
//            }
//            if (keySet.contains(key)) {
//                chartRenderer.setRenderer(key, renderer);
//            }
//            rendererMap.put(key, renderer);
//        }
//        if (drawGrids) {
//            setGrids(chartRenderer, configurationObject, parsedFigures);
//        }
//        chartRenderer.setTitle(configurationObject.optString(TITLE));
//        setLocale(chartRenderer, configurationObject);
//        chartRenderer.setCharts(chartData);
//        return chartRenderer;
//    }


//    private JSONObject getConfigurationObject() throws JSONException {
//        String json = getConfiguration().trim();
//        if (json.isEmpty()) {
//            return new JSONObject();
//        }
//        return new JSONObject(json);
//    }
//
//
//    private static void setLocale(ChartRenderer chartRenderer, JSONObject configurationObject) {
//        String locale = configurationObject.optString(LOCALE);
//        if (! locale.isEmpty()) {
//            String[] locales = locale.split("-");
//            switch (locales.length) {
//                case 1:
//                    chartRenderer.setLocale(new Locale(locales[0]));
//                    break;
//                case 2:
//                    chartRenderer.setLocale(new Locale(locales[0], locales[1]));
//                    break;
//                default:
//                    chartRenderer.setLocale(new Locale(locales[0], locales[1], locales[2]));
//                    break;
//            }
//        }
//    }


//    private static ChartRenderer.AxisPosition getAxisPosition(JSONObject configurationObject, String position) {
//        switch (configurationObject.optString(position)) {
//            case MINIMUM:
//                return ChartRenderer.AxisPosition.MINIMUM;
//            case MAXIMUM:
//                return ChartRenderer.AxisPosition.MAXIMUM;
//            default:
//                return ChartRenderer.AxisPosition.ORIGIN;
//        }
//    }


//    private void setGrids(ChartRenderer chartRenderer, JSONObject configurationObject, Figures parsedFigures) throws UserDataException, JSONException {
//        JSONObject gridConfiguration = configurationObject.optJSONObject(GRID);
//        if (gridConfiguration != null) {
//            GridStyle gridStyle = getGridStyle(gridConfiguration);
//            chartRenderer.setGridRenderer(new DefaultGridRenderer(gridStyle), getGridMode(gridConfiguration));
//            chartRenderer.setXGrid(createGrid(gridConfiguration, X, defaultXGridType(parsedFigures)));
//            chartRenderer.setYGrid(null, createGrid(gridConfiguration, Y, defaultYGridType(parsedFigures)));
//            acceptAxisRenderers(chartRenderer, chartRenderer::setXAxisRenderers, gridConfiguration.optJSONArray(X_AXIS), DEFAULT_X_LABEL_OFFSET);
//            acceptAxisRenderers(chartRenderer, chartRenderer::setYAxisRenderers, gridConfiguration.optJSONArray(Y_AXIS), DEFAULT_Y_LABEL_OFFSET);
//        }
//        else {
//            chartRenderer.setXAxisRenderer(createAxisRenderer(DEFAULT_X_LABEL_OFFSET));
//            chartRenderer.setYAxisRenderer(createAxisRenderer(DEFAULT_Y_LABEL_OFFSET));
//        }
//    }


//    private String defaultXGridType(Figures parsedFigures) {
//        return gridType(parsedFigures.typeSet(parsedFigures.getXTypes()));
//    }
//
//
//    private String defaultYGridType(Figures parsedFigures) {
//        return gridType(parsedFigures.typeSet(parsedFigures.getYTypes()));
//    }


//    private String gridType(EnumSet<Figures.DataType> types) {
//        switch (getMajorDataType(types)) {
//            case TEXT:
//                return TEXT;
//            case DATE:
//                return CALENDAR;
//            case NUMBER:
//            default:
//                return NUMBER;
//        }
//    }


//    private static void acceptAxisRenderers(ChartRenderer chartRenderer, Consumer<Collection<AxisRenderer>> axisRenderers, JSONArray axisesArray, int defaultOffset) throws UserDataException {
//        ArrayList<AxisRenderer> axisRendererList = new ArrayList<>();
//        if (axisesArray == null) {
//            axisRendererList.add(createAxisRenderer(defaultOffset));
//        }
//        else {
//            for (int i = 0; i < axisesArray.length(); ++i) {
//                JSONObject axisObject = axisesArray.optJSONObject(i);
//                String graphName = axisObject.optString(GRAPH);
//                Double min = axisObject.has(MINIMUM) ? axisObject.optDouble(MINIMUM) : null;
//                Double max = axisObject.has(MAXIMUM) ? axisObject.optDouble(MAXIMUM) : null;
//                if (! graphName.isEmpty()) {
//                    if (min <= max) {
//                        chartRenderer.setYWindow(graphName, min, max);
//                    }
//                    else {
//                        throw new UserDataException(min + " > " + max);
//                    }
//                }
//                axisRendererList.add(createAxisRenderer(axisObject, defaultOffset));
//            }
//        }
//        axisRenderers.accept(axisRendererList);
//    }
//
//
//    private static Figures.DataType getMajorDataType(EnumSet<Figures.DataType> types) {
//        for (Figures.DataType type : Figures.DataType.values()) {
//            if (types.contains(type)) {
//                return type;
//            }
//        }
//        return Figures.DataType.NUMBER;
//    }


//    private static DefaultAxisRenderer createAxisRenderer(int defaultOffset) {
//        AxisStyle axisStyle = new AxisStyle();
//        axisStyle.setLabelOffset(defaultOffset);
//        DefaultAxisRenderer axisRenderer = new DefaultAxisRenderer(ChartRenderer.AxisPosition.ORIGIN, axisStyle);
//        return axisRenderer;
//    }
//
//
//    private static AxisRenderer createAxisRenderer(JSONObject axisObject, int defaultOffset) throws UserDataException {
//        AxisStyle axisStyle = new AxisStyle();
//        PARSER.applyColor(axisObject, AXIS_COLOR, axisStyle::setAxisColor);
//        PARSER.applyColor(axisObject, MARKER_COLOR, axisStyle::setMarkerColor);
//        PARSER.applyColor(axisObject, LABEL_COLOR, axisStyle::setLabelColor);
//        PARSER.applyColor(axisObject, TITLE_COLOR, axisStyle::setTitleColor);
//        PARSER.applyColor(axisObject, UNIT_COLOR, axisStyle::setUnitColor);
//        axisStyle.setLabelOffset(axisObject.optInt(LABEL_OFFSET, defaultOffset));
//        DefaultAxisRenderer axisRenderer = new DefaultAxisRenderer(getAxisPosition(axisObject, POSITION), axisStyle, axisObject.optString(GRAPH, null));
//        acceptIfNotEmpty(axisObject.optString(TITLE), axisRenderer::setTitle);
//        acceptIfNotEmpty(axisObject.optString(UNIT), axisRenderer::setUnit);
//        return axisRenderer;
//    }


//    private static void acceptIfNotEmpty(String string, Consumer<String> consumer) {
//        if (! string.isEmpty()) {
//            consumer.accept(string);
//        }
//    }
//
//
//    private Grid createGrid(JSONObject gridConfiguration, String axis, String defaultType) {
//        JSONObject axisConfiguration = gridConfiguration.optJSONObject(axis);
//        String type = (axisConfiguration == null) ? defaultType : axisConfiguration.optString(TYPE, defaultType);
//        switch (type) {
//            case TEXT:
//                return new MapGrid(PARSER.getLabeledNumbers());
//            case CALENDAR:
//                return new CalendarGrid();
//            case TIMESTAMP:
//                return new TimestampGrid();
//            case INTEGER:
//                return new IntegerGrid();
//            default:
//                return new NumberGrid();
//        }
//    }
//
//
//    private static ChartRenderer.GridMode getGridMode(JSONObject gridConfiguration) {
//        switch (gridConfiguration.optString(FILL_MODE)) {
//            case X:
//                return ChartRenderer.GridMode.X;
//            case Y:
//                return ChartRenderer.GridMode.Y;
//            default:
//                return ChartRenderer.GridMode.NONE;
//        }
//    }
//
//
//    private static GridStyle getGridStyle(JSONObject gridConfiguration) throws UserDataException {
//        return GridStyle.create(
//            axisStroke(gridConfiguration, X),
//            axisColor(gridConfiguration, X),
//            axisStroke(gridConfiguration, Y),
//            axisColor(gridConfiguration, Y),
//            createPaintBox(gridConfiguration));
//    }


//    private static GridStyle.PaintBox createPaintBox(JSONObject gridConfiguration) throws UserDataException {
//        JSONArray backgrounds = gridConfiguration.optJSONArray(BACKGROUNDS);
//        if (backgrounds == null || backgrounds.length() < 1) {
//            return null;
//        }
//        if (backgrounds.optJSONArray(0) != null) {
//            return createGradientPaintBox(backgrounds);
//        }
//        return createSolidPaintBox(backgrounds);
//    }


//    private static GridStyle.PaintBox createSolidPaintBox(JSONArray backgrounds) throws UserDataException {
//        return GridStyle.createSolidPaintBox(PARSER.getColors(backgrounds));
//    }


//    private static GridStyle.PaintBox createGradientPaintBox(JSONArray backgrounds) throws UserDataException {
//        if (backgrounds.length() != 2) {
//            throw new UserDataException(String.format("Cannot create grid background from %s", backgrounds));
//        }
//        Color[] colors1 = PARSER.getColors(backgrounds.optJSONArray(0));
//        Color[] colors2 = PARSER.getColors(backgrounds.optJSONArray(1));
//        return GridStyle.createGradientPaintBox(colors1, colors2);
//    }
//
//
//    private static Stroke axisStroke(JSONObject gridConfiguration, String axis) throws UserDataException {
//        JSONObject axisConfiguration = gridConfiguration.optJSONObject(axis);
//        if (axisConfiguration == null) {
//            return null;
//        }
//        return createStroke(axisConfiguration);
//    }
//
//
//    private static Color axisColor(JSONObject gridConfiguration, String axis) throws UserDataException {
//        JSONObject axisConfiguration = gridConfiguration.optJSONObject(axis);
//        if (axisConfiguration == null) {
//            return null;
//        }
//        return PARSER.getColor(axisConfiguration, COLOR);
//    }
//
//
//    private static JSONObject merge(JSONObject... objects) throws JSONException {
//        JSONObject merged = new JSONObject();
//        for (JSONObject object : objects) {
//            override(merged, object);
//        }
//        return merged;
//    }
//
//
//    private static void override(JSONObject target, JSONObject overrides) throws JSONException {
//        if (overrides != null) {
//            Iterator it = overrides.keys();
//            while (it.hasNext()) {
//                String name = it.next().toString();
//                target.put(name, overrides.get(name));
//            }
//        }
//    }


//    private static PolygonDotRenderer createStarRenderer(JSONObject object, int i, Color[] palette) throws UserDataException {
//        final int points = Math.max(object.optInt(POINTS, i + 3), 3);
//        final int innerRadius = object.optInt(INNER_RADIUS, 3);
//        final int outerRadius = object.optInt(OUTER_RADIUS, 8);
//        final Color color = PARSER.getColor(object, COLOR, palette[i]);
//        final PointDrawStyle pointDrawStyle = createPointDrawStyle(color);
//        return new PolygonDotRenderer(PolygonFactory.createStar(points, innerRadius, outerRadius), pointDrawStyle);
//    }
    

//    private static AbstractDataAreaRenderer createRenderer(Figures parsedFigures, JSONObject object, Color[] palette, int i) throws UserDataException, JSONException {
//        if (object == null) {
//            return new OvalDotRenderer(8, DefaultDrawStyle.create(palette[i], palette[i].darker()));
//        }
//        switch (object.optString(TYPE)) {
//            case "bar":
//                return createBarRenderer(object, palette, i);
//            case "line":
//                return createLineRenderer(object, palette, i);
//            case "pie":
//                return new DefaultPieSectorRenderer(PieDrawStyle.create(createPiePalette(parsedFigures, object)));
//            case "scatter":
//                return new ScatterRenderer(createPointDrawStyle(object, palette[i]), (float) object.optDouble(SIZE, 3.0));
//            default:
//                return createRenderer(object, palette, i);
//        }
//    }


//    private static Color[][] createPiePalette(Figures parsedFigures, JSONObject object) throws JSONException, UserDataException {
//        JSONObject colorConfiguration = object.optJSONObject(COLORS);
//        ChartData<Number, Number> chartData = parsedFigures.getChartData().values().iterator().next();
//        Color[] defaults = Palette.generateColors(chartData.size());
//        Color[][] colors = new Color[chartData.size()][];
//        int i = 0;
//        for (ChartDataElement<Number, Number> element : chartData) {
//            Color[] configuredColors = (colorConfiguration != null) ? PARSER.getColors(colorConfiguration, element.getKey().toString()) : new Color[] { defaults[i], defaults[i].darker() };
//            colors[i] = configuredColors;
//            ++i;
//        }
//        return colors;
//    }
//
//
//    private static LineRenderer createLineRenderer(JSONObject object, Color[] palette, int i) throws UserDataException, JSONException {
//        JSONObject markerObject = object.optJSONObject(MARKER);
//        LineDrawStyle lineDrawStyle;
//        PointDrawStyle pointDrawStyle = null;
//        if (markerObject != null && markerObject.has(COLORS)) {
//            pointDrawStyle = PointDrawStyle.createRadial(PARSER.getColors(markerObject, COLORS));
//        }
//        Color color = PARSER.getColor(object, COLOR, palette[i]);
//        lineDrawStyle = createLineDrawStyle(color, (float) object.optDouble(WIDTH, 1.0), PARSER.getFloats(object, DASH_ARRAY), pointDrawStyle);
//        if (object.has(TOP_AREA_COLOR)) {
//            lineDrawStyle.setTopAreaPaint(PARSER.getColor(object, TOP_AREA_COLOR));
//        }
//        if (object.has(BOTTOM_AREA_COLOR)) {
//            lineDrawStyle.setBottomAreaPaint(PARSER.getColor(object, BOTTOM_AREA_COLOR));
//        }
//        if (markerObject != null) {
//            CoordinateAreaRenderer markerRenderer = createMarkerRenderer(markerObject, palette, i);
//            return new DefaultLineRenderer(lineDrawStyle, markerRenderer);
//        }
//        return new DefaultLineRenderer(lineDrawStyle);
//    }


//    private static CoordinateAreaRenderer createMarkerRenderer(JSONObject markerObject, Color[] palette, int i) throws UserDataException {
//        if (markerObject == null) {
//            return new OvalDotRenderer(8, DefaultDrawStyle.create(palette[i], palette[i].darker()));
//        }
//        return createRenderer(markerObject, palette, i);
//    }


//    private static CoordinateAreaRenderer createRenderer(JSONObject object, Color[] palette, int i) throws UserDataException {
//        switch (object.optString(TYPE)) {
//            case "star":
//                return createStarRenderer(object, i, palette);
//            case "rectangle":
//                return new RectangleDotRenderer(object.optInt(WIDTH, 5), object.optInt(HEIGHT, 5), DefaultDrawStyle.create(PARSER.getColor(object, COLOR, palette[i]), PARSER.getColor(object, BORDER, palette[i])));
//            default:
//                return createDefaultRenderer(object, palette[i]);
//        }
//    }


//    private static CoordinateAreaRenderer createDefaultRenderer(JSONObject object, Color defaultColor) throws UserDataException {
//        String imageUrl = object.optString(IMAGE);
//        if (imageUrl != null) {
//            return createImageRenderer(imageUrl, object);
//        }
//        Color[] colors = PARSER.getColors(object, COLORS);
//        if (colors == null || colors.length == 0) {
//            return new OvalDotRenderer(8, DefaultDrawStyle.create(defaultColor, defaultColor.darker()));
//        }
//        return new OvalDotRenderer(8, DefaultDrawStyle.create(colors[0], colors[1 % colors.length]));
//    }


//    private static CoordinateAreaRenderer createImageRenderer(String imageUrl, JSONObject object) throws UserDataException {
//        try {
//            URL url = new URL(imageUrl);
//            DefaultDrawStyle drawStyle = DefaultDrawStyle.createImage(ImageIO.read(url));
//            Image image = drawStyle.getImage();
//            int width = object.optInt(WIDTH, image.getWidth(null));
//            int height = object.optInt(HEIGHT, image.getHeight(null));
//            return new RectangleDotRenderer(width, height, drawStyle);
//        }
//        catch (IOException ex) {
//            throw new UserDataException("Cannot load image from " + imageUrl, ex);
//        }
//    }


//    private static AbstractDataAreaRenderer createBarRenderer(JSONObject object, Color[] palette, int i) throws UserDataException {
//        JSONArray colors = object.optJSONArray(COLORS);
//        Color color = (colors != null && colors.length() > 0) ? PARSER.getColor(colors, 0) : palette[i];
//        Color edgeColor = (colors != null && colors.length() > 1) ? PARSER.getColor(colors, 1)  : color.darker();
//        AreaDrawStyle style = BarDrawStyle.create(color, edgeColor);
//        int width = object.optInt(WIDTH, 10);
//        Number base = (object.has(BASE)) ? object.optDouble(BASE) : null;
//        BarRenderer renderer = new BarRenderer(width, style, base);
//        if (object.has(SHIFT)) {
//            renderer.setShift(object.optInt(SHIFT));
//        }
//        else if (object.optBoolean(AUTO_SHIFT)) {
//            renderer.setShift(Math.round((-(palette.length / 2.0f) + i) * width + width / 2.0f));
//        }
//        return renderer;
//    }
//
//
//    private static AreaDrawStyle<AreaGeometry> createPointDrawStyle(JSONObject object, Color color) throws UserDataException {
//        Color[] colors = PARSER.getColors(object, COLORS);
//        if (colors == null || colors.length == 0) {
//            colors = new Color[] { color.brighter(), color, color.darker() };
//        }
//        else if (colors.length == 1) {
//            return DefaultDrawStyle.create(colors[0], PARSER.getColor(object, BORDER));
//        }
//        PointDrawStyle pointDrawStyle = PointDrawStyle.createLinear(colors);
//        pointDrawStyle.setBorder(PARSER.getColor(object, BORDER));
//        return pointDrawStyle;
//    }
//
//
//    private static LineDrawStyle createLineDrawStyle(Color color, float width, float[] dash, PointDrawStyle pointDrawStyle) throws UserDataException {
//        Stroke stroke = createStroke(width, dash);
//        LineDrawStyle lineDrawStyle = LineDrawStyle.create(color, stroke, pointDrawStyle);
//        return lineDrawStyle;
//    }
//
//
//    private static Stroke createStroke(JSONObject object) throws UserDataException {
//        return createStroke((float) object.optDouble(WIDTH, 1.0), PARSER.getFloats(object, DASH_ARRAY));
//    }
//
//
//    private static Stroke createStroke(float width, float[] dash) throws UserDataException {
//        Stroke stroke;
//        try {
//            stroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
//        }
//        catch (IllegalArgumentException ex) {
//            throw new UserDataException(ex);
//        }
//        return stroke;
//    }


//    private static PointDrawStyle createPointDrawStyle(Color color) {
//        PointDrawStyle pointDrawStyle = PointDrawStyle.createLinear(new Color[] { Color.WHITE, color });
//        pointDrawStyle.setBorder(color.darker());
//        return pointDrawStyle;
//    }


    private void sendMail() {
        if (figuresModified && FORM.equals(source) && ! atLocalHost()) {
            mailChartDrawn();
        }
    }
    
    
    private String createBase64(BufferedImage image) {
        try {
            streamingDuration = 0;
            long startTime = System.currentTimeMillis();
            byte[] base64 = Base64.getEncoder().encode(createBytes(image));
            streamingDuration = System.currentTimeMillis() - startTime;
            return new String(base64);
        }
        catch (IOException ex) {
            log(Level.WARNING, "Could not create Base64 for image", ex);
            return null;
        }
    }


    public byte[] createJpgBytes() {
        try {
            return createBytes(createImage(BufferedImage.TYPE_INT_RGB), "jpg");
        }
        catch (JSONException | IOException ex) {
            Logger.getLogger(DiagramMaker.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }


    private byte[] createBytes(BufferedImage image) throws IOException {
        return createBytes(image, "png");
    }


    private byte[] createBytes(BufferedImage image, String format) throws IOException {
        ImageIO.setUseCache(false);
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, stream);
            stream.flush();
            byte[] bytes = stream.toByteArray();
            imageSize = bytes.length;
            return bytes;
        }
    }


    private void mailChartDrawn() {
        mail("Chart drawn", String.format("Configuration:\n%s\n\nFigures:\n%s", configuration, figures));
    }


    private void mail(String subject, String message) {
        Properties properties = getMailProperties();
        try {
            Client client = new Client(properties);
            String remoteHost = getString(() -> request.getRemoteHost());
            String serverName = getString(() -> request.getServerName());
            String bodyText = String.format("Host: %s\nServer: %s\n\n%s", remoteHost, serverName, message);
            client.send("diagrams@bartkampers.nl", subject, bodyText);
        }
        catch (MessagingException ex) {
            log(Level.WARNING, "Mail failure", ex);
        }
    }


    private Properties getMailProperties() {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(getPath(CONFIG_DIRECTORY, "mail.properties").toFile()));
            properties.put("mail.smtp.user", "test@bartkampers.nl");
            properties.put("mail.smtp.password", "1@pril");
        }
        catch (IOException ex) {
            log(Level.WARNING, "Could not load mail properties", ex);
        }
        return properties;
    }


    private String getString(Supplier<String> supplier) {
        try {
            return supplier.get();
        }
        catch (RuntimeException ex) {
            return ex.getMessage();
        }
    }
    

    public String readString(String filename) {
        try {
            return new String(Files.readAllBytes(getPath(RESOURCES_DIRECTORY, filename)));
        }
        catch (IOException ex) {
            log(Level.WARNING, "read string", ex);
            return "";
        }
    }


    private boolean atLocalHost() {
        return LOCALHOST.equals(request.getServerName());
    }
    
    
    private void log(Level level, String message) {
        getLogger().log(level, message);
        if (Level.INFO.equals(level)) {
            exception = message;
        }
    }


    private void log(Level level, String message, Throwable throwable) {
        getLogger().log(level, message, throwable);
        if (Level.INFO.equals(level)) {
            exception = throwable.getMessage();
        }
        if (level.intValue() >= Level.WARNING.intValue()) {
            saveLog(throwable);
        }
    }


    private void saveLog(Throwable throwable) {
        try {
            Path path = getEnsuredPath(LOGGING_DIRECTORY, "log.txt");
            StringBuilder logs = new StringBuilder(new String(Files.readAllBytes(path)));
            StringBuilder newLog = new StringBuilder(new Date().toString()).append('\n');
            newLog.append(getStackTrace(throwable)).append('\n');
            int overSize = logs.length() + newLog.length() - MAX_LOG_LENGTH;
            if (overSize > 0) {
                logs.delete(0, overSize);
            }
            logs.append(newLog);
            Files.write(path, logs.toString().getBytes());
        }
        catch (IOException ex) {
            log(Level.INFO, ex.getMessage());
        }
    }


    private static String getStackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }


    private Path getEnsuredPath(String directory, String filename) throws IOException {
        Path path = getPath(directory, filename);
        if (! Files.exists(path)) {
            Files.createFile(path);
            Set<PosixFilePermission> perms = new HashSet<>();
            //add owners permission
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            //add group permissions
            perms.add(PosixFilePermission.GROUP_READ);
//            perms.add(PosixFilePermission.GROUP_WRITE);
//            perms.add(PosixFilePermission.GROUP_EXECUTE);
            //add others permissions
            perms.add(PosixFilePermission.OTHERS_READ);
//            perms.add(PosixFilePermission.OTHERS_WRITE);
//            perms.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(path, perms);
        }
        return path;
    }


    private Path getPath(String directory, String filename) {
        return FileSystems.getDefault().getPath(getRealPath(directory), filename);
    }


    private String getRealPath(String name) {
        return request.getSession(false).getServletContext().getRealPath(name);
    }

    
    private Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(DiagramMaker.class.getName());
        }
        return logger;
    }


    private String configuration;
    private String figures;
    private String source;

    private HttpServletRequest request;

    private long renderingDuration;
    private long streamingDuration;
    private int imageSize;
    private String exception;
    private boolean figuresModified;

    private Logger logger;

    private static final Parser PARSER = new Parser();

    private static final String EXAMPLE = "example";
    private static final String FORM = "form";
    private static final String LOCALHOST = "localhost";

//    private static final String AUTO_SHIFT = "auto_shift";
//    private static final String AXIS_COLOR = "axis_color";
//    private static final String BACKGROUNDS = "backgrounds";
//    private static final String BASE = "base";
//    private static final String BORDER = "border";
//    private static final String BOTTOM_AREA_COLOR = "bottom_area_color";
//    private static final String BOTTOM_MARGIN = "bottom_margin";
//    private static final String CALENDAR = "calendar";
//    private static final String COLOR = "color";
//    private static final String COLORS = "colors";
//    private static final String DASH_ARRAY = "dash_array";
//    private static final String FILL_MODE = "fill_mode";
//    private static final String GRAPH = "graph";
//    private static final String GRAPHS = "graphs";
//    private static final String GRID = "grid";
//    private static final String HEIGHT = "height";
//    private static final String IMAGE = "image";
//    private static final String INNER_RADIUS = "inner_radius";
//    private static final String INTEGER = "integer";
//    private static final String LABEL_COLOR = "label_color";
//    private static final String LABEL_OFFSET = "label_offset";
//    private static final String LEFT_MARGIN = "left_margin";
//    private static final String LEFT_OFFSET = "left_offset";
//    private static final String LOCALE = "locale";
//    private static final String MARKER = "marker";
//    private static final String MARKER_COLOR = "marker_color";
//    private static final String MAXIMUM = "maximum";
//    private static final String MINIMUM = "minimum";
//    private static final String NUMBER = "number";
//    private static final String OUTER_RADIUS = "outer_radius";
//    private static final String POINTS = "points";
//    private static final String POSITION = "position";
//    private static final String RIGHT_MARGIN = "right_margin";
//    private static final String RIGHT_OFFSET = "right_offset";
//    private static final String SHIFT = "shift";
//    private static final String SIZE = "size";
//    private static final String STACK = "stack";
//    private static final String TEXT = "text";
//    private static final String TIMESTAMP = "timestamp";
//    private static final String TITLE = "title";
//    private static final String TITLE_COLOR = "title_color";
//    private static final String TOP_AREA_COLOR = "top_area_color";
//    private static final String TOP_MARGIN = "top_margin";
//    private static final String TYPE = "type";
//    private static final String UNIT = "unit";
//    private static final String UNIT_COLOR = "unit_color";
//    private static final String WIDTH = "width";
//    private static final String X = "x";
//    private static final String X_AXIS = "x_axis";
//    private static final String X_EXPAND_TO_GRID = "x_expand_to_grid";
//    private static final String X_WINDOW_MAXIMUM = "x_window_maximum";
//    private static final String X_WINDOW_MINIMUM = "x_window_minimum";
//    private static final String Y = "y";
//    private static final String Y_AXIS = "y_axis";
//    private static final String Y_EXPAND_TO_GRID = "y_expand_to_grid";
//    private static final String Y_WINDOW_MAXIMUM = "y_window_maximum";
//    private static final String Y_WINDOW_MINIMUM = "y_window_minimum";

    private static final String CONFIG_DIRECTORY = "config";
    private static final String LOGGING_DIRECTORY = "logging";
    private static final String RESOURCES_DIRECTORY = "resources";


//    private static final int DEFAULT_X_LABEL_OFFSET = 0;
//    private static final int DEFAULT_Y_LABEL_OFFSET = -4;

    private static final int MAX_LOG_LENGTH = 20000;

}
