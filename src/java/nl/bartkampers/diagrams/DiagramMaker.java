/*
** © Bart Kampers
*/

package nl.bartkampers.diagrams;

import bka.awt.chart.*;
import bka.awt.chart.geometry.*;
import bka.awt.chart.io.*;
import bka.awt.chart.render.*;
import bka.mail.*;
import bka.text.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.text.*;
import java.util.List;
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
            return readFile(configuration);
        }
        if (configuration == null) {
            configuration = readFile("southpole_configuration_example.yml");
        }
        return configuration;
    }


    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }


    public String getFigures() {
        if (EXAMPLE.equals(source)) {
            return readFile(figures);
        }
        if (figures == null) {
            figures = readFile("southpole_figures_example.csv");
        }
        return figures;
    }

    
    public void setFigures(String figures) {
        figuresModified = figures != null && !figures.equals(this.figures);
        this.figures = figures;
    }


    public String getSource() {
        return source;
    }


    public void setSource(String source) {
        this.source = source;
    }


    public void setRequest(HttpServletRequest request) {
        try {
            request.setCharacterEncoding("UTF-8");
            this.request = request;
        }
        catch (UnsupportedEncodingException ex) {
            Logger.getLogger(DiagramMaker.class.getName()).log(Level.WARNING, null, ex);
        }
    }


    public void setSession(HttpSession session) {
        this.session = Objects.requireNonNull(session);
    }


    public String getBase64() {
        exceptionMessage = null;
        String image = createBase64(createImage());
        sendMail();
        return image;
    }


    public String getStatusText() {
        if (exceptionMessage != null) {
            return exceptionMessage;
        }
        return String.format("Image size: %d bytes. Rendered in %d ms. Streamed in %d ms.", imageSize, renderingDuration, streamingDuration);
    }


    public Map<String, String[]> getExamples() {
        Map<String, String> figuresMap = new HashMap<>();
        Map<String, String> configurationsMap = new HashMap<>();
        File resourcesDir = new File(getRealPath(RESOURCES_DIRECTORY));
        for (String filename : resourcesDir.list()) {
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
        Map<String, String[]> examples = new TreeMap<>();
        for (String key : configurationsMap.keySet()) {
            if (figuresMap.containsKey(key)) {
                examples.put(key, new String[] { configurationsMap.get(key), figuresMap.get(key) });
            }
        }
        return examples;
    }


    public void sendMail(String name, String address, String message) {
        Thread mailThread = new Thread(() -> mail("Message sent", String.format("Name: %s\nAddress: %s\n\nMessage:\n%s", name, address, message)));
        mailThread.start();
    }


    public String getException() {
        return exceptionMessage;
    }


    public String getDataCoordinates() {
        JSONArray coordinates = new JSONArray();
        areaGeometries.forEach((key, graphGeometry) -> {
            graphGeometry.stream()
                .filter(point -> point.getArea() != null)
                .forEach(point -> putCoordinate(key, point, coordinates));
        });
        return coordinates.toString();
    }

    private void putCoordinate(Object key, AreaGeometry point, JSONArray coordinates) {
        Number y = getYData(key, point.getX(), point.getY());
        coordinates.put(createAreaObject(point.getArea(), tooltipText(key, point.getX(), y)));
    }

    private Number getYData(Object key, Number xValue, Number yValue) {
        ChartPoints chartData = parsedFigures.getChartData().get(key);
        Iterator<ChartPoint> it = chartData.iterator();
        while (it.hasNext()) {
            ChartPoint element = it.next();
            if (xValue.equals(element.getX())) {
                return element.getY();
            }
        }
        return yValue;
    }


    private String tooltipText(Object key, Number x, Number y) {
        if (tooltipFormat == null) {
            return Objects.toString(key) + ": " + format(xFormat, x) + separator() + format(yFormat, y);
        }
        return tooltipFormat.replaceAll("\\{n\\}", Objects.toString(key)).replaceAll("\\{x\\}", format(xFormat, x)).replaceAll("\\{y\\}", format(yFormat, y));
    }

    private String separator() {
        return (symbols.getDecimalSeparator() == ',') ? "; " : ", ";
    }

    private String format(String format, Number number) {
        try {
            return FormatterUtil.format(format, number);
        }
        catch (RuntimeException ex) {
            getLogger().log(Level.WARNING, ex.getMessage());
            return number.toString();
        }
    }


    private static JSONObject createAreaObject(Shape shape, String label) {
        try {
            JSONObject areaObject = new JSONObject();
            areaObject.put("polygon", createPolygonArray(shape));
            areaObject.put("text", label);
            return areaObject;
        }
        catch (JSONException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static JSONArray createPolygonArray(Shape shape) throws JSONException {
        JSONArray polygon = new JSONArray();
        PathIterator it = shape.getPathIterator(null);
        while (!it.isDone()) {
            polygon.put(createPointObject(it));
            it.next();
        }
        return polygon;
    }

    private static JSONObject createPointObject(PathIterator it) throws JSONException {
        JSONObject point = new JSONObject();
        float[] coords = new float[6];
        it.currentSegment(coords);
        point.put("x", Math.round(coords[0]));
        point.put("y", Math.round(coords[1]));
        return point;
    }


    private BufferedImage createImage() {
        return createImage(BufferedImage.TYPE_INT_ARGB);
    }


    private BufferedImage createImage(int type) {
        try {
           return createImage(parseDrawable(), type);
        }
        catch (UserDataException ex) {
            log(Level.WARNING, "Could not read configuration", ex);
            return createImage(type, ex);
        }
    }

    private BufferedImage createImage(Drawable drawable, int type) {
        areaGeometries = null;
        BufferedImage image = new BufferedImage(drawable.width, drawable.height, type);
        Graphics2D g2d = image.createGraphics();
        try {
            renderingDuration = 0;
            long startTime = System.currentTimeMillis();
            drawable.chartRenderer.highlight(getClickCoordinate());
            drawable.chartRenderer.paint(g2d, new Rectangle(0, 0, image.getWidth(), image.getHeight()));
            renderingDuration = System.currentTimeMillis() - startTime;
            areaGeometries = drawable.chartRenderer.getAreaGeometries();
        }
        catch (ChartDataException ex) {
            log(Level.INFO, "Invalid user data", ex);
            drawException(g2d, ex);
        }
        return image;
    }

    private BufferedImage createImage(int type, UserDataException exception) {
        BufferedImage image = new BufferedImage(DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT, type);
        drawException((Graphics2D) image.getGraphics(), exception);
        return image;
    }

    private void drawException(Graphics2D g2d, Exception exception) {
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setColor(Color.RED);
        exceptionMessage = exception.getMessage();
        g2d.drawString(exceptionMessage, 0, 200);
    }

    private Point getClickCoordinate() {
        String x = request.getParameter("clickX");
        String y = request.getParameter("clickY");
        if (x != null && y!= null) {
            try {
                return new Point(Integer.parseInt(x), Integer.parseInt(y));
            }
            catch (NumberFormatException ex) {
                getLogger().log(Level.FINEST, x + "," + y, ex);
            }
        }
        return null;
    }

    private Drawable parseDrawable() throws UserDataException {
        Drawable drawable = new Drawable();
        try {
            parsedFigures = parseFigures();
            ChartConfiguration parsedConfiguration = parseConfiguration(new YamlReader(getConfiguration()));
            if (parsedConfiguration.getWidth() == null) {
                parsedConfiguration.setWidth(DEFAULT_IMAGE_WIDTH );
            }
            if (parsedConfiguration.getHeight() == null) {
                parsedConfiguration.setHeight(DEFAULT_IMAGE_HEIGHT );
            }
            ConfigurationCustomizer customizer = new ConfigurationCustomizer(parsedConfiguration, parsedFigures);
            customizer.adjust();
            ChartRendererBuilder builder = new ChartRendererBuilder(parsedFigures);
            drawable.chartRenderer = builder.buildChartRenderer(parsedConfiguration);
            for (Object key : parsedFigures.getChartData().keySet()) {
                drawable.chartRenderer.setHighlightFormat(key, "%s", "%s");
                DefaultPointHighlightRenderer highlightRenderer = new DefaultPointHighlightRenderer();
                highlightRenderer.setXFormat(parsedConfiguration.getTooltipXFormat());
                highlightRenderer.setYFormat(parsedConfiguration.getTooltipYFormat());
                drawable.chartRenderer.setPointHighlightRenderer(key, highlightRenderer);
            }
            drawable.chartRenderer.setCharts(parsedFigures.getChartData());
            drawable.width = parsedConfiguration.getWidth();
            drawable.height = parsedConfiguration.getHeight();
            tooltipFormat = parsedConfiguration.getTooltipFormat();
            xFormat = parsedConfiguration.getTooltipXFormat();
            yFormat = parsedConfiguration.getTooltipYFormat();
            symbols = new DecimalFormatSymbols(builder.buildLocale(parsedConfiguration));
            return drawable;
        }
        catch (YamlException | ChartConfigurationException ex) {
            throw new UserDataException(ex);
        }
    }


    private Figures parseFigures() throws UserDataException {
        String figuresSource = getFigures();
        try {
            YamlFiguresParser parser = new YamlFiguresParser();
            return parser.parse(figuresSource);
        }
        catch (UserDataException ex) {
            log(Level.FINEST, "Cannot parse YAML, try CSV", ex);
            CsvFiguresParser parser = new CsvFiguresParser();
            return parser.parse(figuresSource);
        }
    }


    private ChartConfiguration parseConfiguration(YamlReader reader) throws YamlException {
        populateYamlConfig(reader.getConfig());
        ChartConfiguration chartConfiguration = reader.read(ChartConfiguration.class);
        if (chartConfiguration == null) {
            chartConfiguration = new ChartConfiguration();
        }
        else if (atLocalHost()) {
            storeYaml(chartConfiguration);
        }
        return chartConfiguration;
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
                log(Level.FINE, "Error writing Yaml", ex);
            }
        }
    }


    private static void populateYamlConfig(YamlConfig config) {
        config.setClassTag("Chart", ChartConfiguration.class);
        config.setClassTag("DataRenderer", DataRendererConfiguration.class);
        config.setPropertyElementType(ChartConfiguration.class, "XAxes", AxisConfiguration.class);
        config.setPropertyElementType(ChartConfiguration.class, "YAxes", AxisConfiguration.class);
        config.setPropertyElementType(ChartConfiguration.class, "graphs", DataRendererConfiguration.class);
        config.setPropertyElementType(ChartConfiguration.class, "YWindows", RangeConfiguration.class);
    }


    private void sendMail() {
        if (figuresModified && exceptionMessage == null && FORM.equals(source) && ! atLocalHost()) {
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
        catch (IOException ex) {
            log(Level.SEVERE, "Could not create JPG bytes", ex);
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
    

    public String readFile(String filename) {
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
            exceptionMessage = message;
        }
    }


    private void log(Level level, String message, Throwable throwable) {
        getLogger().log(level, message, throwable);
        if (Level.INFO.equals(level)) {
            exceptionMessage = throwable.getMessage();
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
            newLog.append(getString(() -> request.getRemoteHost())).append('\n');
            newLog.append(getString(() -> request.getServerName())).append('\n');
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
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            perms.add(PosixFilePermission.GROUP_READ);
            perms.add(PosixFilePermission.OTHERS_READ);
            Files.setPosixFilePermissions(path, perms);
        }
        return path;
    }

    private Path getPath(String directory, String filename) {
        return FileSystems.getDefault().getPath(getRealPath(directory), filename);
    }


    private String getRealPath(String name) {
        return session.getServletContext().getRealPath(name);
    }

    
    private Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(DiagramMaker.class.getName());
        }
        return logger;
    }


    private class Drawable {
        int width;
        int height;
        ChartRenderer chartRenderer;
    }


    private HttpServletRequest request;
    private HttpSession session;

    private String configuration;
    private String figures;
    private String source;

    private Figures parsedFigures;
    private Map<Object, List<AreaGeometry>> areaGeometries;
    private String tooltipFormat;
    private String xFormat;
    private String yFormat;
    DecimalFormatSymbols symbols;

    private long renderingDuration;
    private long streamingDuration;
    private int imageSize;
    private String exceptionMessage;
    private boolean figuresModified;

    private Logger logger;

    private static final String EXAMPLE = "example";
    private static final String FORM = "form";
    private static final String LOCALHOST = "localhost";

    private static final String CONFIG_DIRECTORY = "config";
    private static final String LOGGING_DIRECTORY = "logging";
    private static final String RESOURCES_DIRECTORY = "resources";

    private static final int MAX_LOG_LENGTH = 100000;

    private static final int DEFAULT_IMAGE_HEIGHT = 400;
    private static final int DEFAULT_IMAGE_WIDTH = 500;

}
