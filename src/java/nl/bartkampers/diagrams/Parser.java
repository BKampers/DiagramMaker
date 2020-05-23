/*
** © Bart Kampers
*/

package nl.bartkampers.diagrams;

import java.awt.*;
import java.math.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;
import org.json.*;


class Parser {


    void applyNumber(JSONObject object, String key, Consumer<Number> consumer) throws UserDataException {
        Number number = getNumber(object.opt(key));
        if (number != null) {
            consumer.accept(number);
        }
    }


    void applyColor(JSONObject object, String key, Consumer<Color> consumer) throws UserDataException {
        if (object.has(key)) {
            Color color = getColor(object, key);
            consumer.accept(color);
        }
    }


    Figures getFigures(String source) throws JSONException, UserDataException {
        if (source.startsWith("[")) {
            return createFiguresFromJson(source);
        }
        return createFiguresFromCsv(source);
    }


    private Figures createFiguresFromJson(String source) throws UserDataException, JSONException {
        Figures figures = new Figures();
        JSONArray array = new JSONArray(source);
        for (int i = 0; i < array.length(); ++i) {
            JSONObject graph = array.getJSONObject(i);
            String key = graph.getString("key");
            JSONArray values = graph.getJSONArray("values");
            for (int v = 0; v < values.length(); ++v) {
                JSONArray point = values.getJSONArray(v);
                if (point.length() != 2) {
                    throw new UserDataException(String.format("Invalid point: %s", point));
                }
                figures.add(key, Figures.DataType.NUMBER, getNumber(point.get(0)), Figures.DataType.NUMBER, getNumber(point.get(1)));
            }
        }
        return figures;
    }


    private Figures createFiguresFromCsv(String source) throws UserDataException {
        Figures figures = new Figures();
        Map<Integer, String> columnHeaders = new HashMap();
        String[] lines = source.split("\n");
        String[] headers = lines[0].split(",");
        for (int h = 1; h < headers.length; ++h) {
            columnHeaders.put(h, headers[h].trim());
        }
        for (int ln = 1; ln < lines.length; ++ln) {
            String[] columns = lines[ln].split(",");
            if (columns.length > 1) {
                Figure x = getFigure(columns[0].trim());
                for (int c = 1; c < columns.length; ++c) {
                    String yText = columns[c].trim();
                    if (! yText.isEmpty()) {
                        String columnHeader = columnHeaders.computeIfAbsent(c, i -> String.valueOf(i));
                        Figure y = getFigure(yText);
                        figures.add(columnHeader, x.type, x.number, y.type, y.number);
                    }
                }
            }
        }
        return figures;
    }


    Color[] getColors(JSONObject object, String key) throws UserDataException {
        JSONArray array = object.optJSONArray(key);
        if (array == null) {
            return null;
        }
        return getColors(array);
    }


    Color[] getColors(JSONArray array) throws UserDataException {
        Color[] colors = new Color[array.length()];
        for (int i = 0; i < array.length(); ++i) {
            colors[i] = getColor(array, i);
        }
        return colors;
    }


    Color getColor(JSONObject object, String prefix, String key) throws UserDataException {
        String prefixedKey = prefixed(prefix, key);
        return getColor(object, prefixedKey, getColor(object, key, Color.BLACK));
    }


    Color getColor(JSONArray array, int index) throws UserDataException {
        return getColor(array.optString(index), () -> array.optInt(index));
    }


    Color getColor(JSONObject object, String key, Color defaultColor) throws UserDataException {
        if (! object.has(key)) {
            return defaultColor;
        }
        if (JSONObject.NULL.equals(object.opt(key))) {
            return null;
        }
        return getColor(object, key);
    }


    Color getColor(JSONObject object, String key) throws UserDataException {
         if (! object.has(key) || object.isNull(key)) {
             return null;
        }
        return getColor(object.optString(key), () -> object.optInt(key));
    }


    private Color getColor(String string, Supplier<Integer> integerSupplier) throws UserDataException {
        int rgba = (string.isEmpty()) ? integerSupplier.get() : getRgba(string);
        return new Color(rgba, (rgba & 0xFF000000) != 0);
    }


    private int getRgba(String string) throws UserDataException {
        try {
            String[] split = string.split("-");
            if (split.length > 1) {
                int rgba = 0;
                for (int i = 0; i < split.length; ++i) {
                    rgba = (rgba << 8) | Integer.parseInt(split[i]);
                }
                return rgba;
            }
            return (int) Long.parseLong(string, 16);
        }
        catch (NumberFormatException ex) {
            throw new UserDataException(ex);
        }
    }


    float[] getFloats(JSONObject object, String key) throws UserDataException {
        try {
            JSONArray array = object.optJSONArray(key);
            if (array == null) {
                return null;
            }
            float[] floats = new float[array.length()];
            for (int i = 0; i < array.length(); ++i) {
                floats[i] = (float) array.getDouble(i);
            }
            return floats;
        }
        catch (JSONException ex) {
            throw new UserDataException(ex);
        }
    }


    Figure getFigure(String string) throws UserDataException {
        try {
            Figure figure = new Figure();
            String value = string.trim();
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                figure.type = Figures.DataType.TEXT;
                figure.number = getLabeledNumber(value.substring(1, value.length() - 1));
            }
            else {
                Date date = getDate(value);
                if (date != null) {
                    figure.type = Figures.DataType.DATE;
                    figure.number = date.getTime();
                }
                else {
                    figure.type = Figures.DataType.NUMBER;
                    figure.number = new BigDecimal(value);
                }
            }
            return figure;
        }
        catch (NumberFormatException ex) {
            throw new UserDataException(ex);
        }
    }


    Number getNumber(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return (Number) value;
        }
        if (value instanceof String) {
            Date date = getDate((String) value);
            if (date != null) {
                return date.getTime();
            }
        }
        return getLabeledNumber(value.toString());
    }


    private Label getLabeledNumber(String string) {
        Label label = new Label(string, labeledNumbers.size() + 1);
        int index = labeledNumbers.indexOf(label);
        if (index < 0) {
            labeledNumbers.add(label);
            return label;
        }
        return labeledNumbers.get(index);
    }


    Date getDate(String value) {
        Matcher matcher = datePattern().matcher(value);
        if (! matcher.matches()) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.ERA, GregorianCalendar.AD);
        setCalendarField(calendar, Calendar.YEAR, matcher.group(YEAR));
        setCalendarField(calendar, Calendar.MONTH, month(matcher.group(MONTH)));
        setCalendarField(calendar, Calendar.DATE, matcher.group(DATE));
        setCalendarField(calendar, Calendar.HOUR, matcher.group(HOUR));
        setCalendarField(calendar, Calendar.MINUTE, matcher.group(MINUTE));
        setCalendarField(calendar, Calendar.SECOND, matcher.group(SECOND));
        setCalendarField(calendar, Calendar.MILLISECOND, thousandths(matcher.group(MILLI)));
        return calendar.getTime();
    }


    private static Pattern datePattern() {
        return Pattern.compile(
            namedDecimalGroup(YEAR) +
            namedDecimalGroup("\\-", MONTH) +
            optionalNamedDecimalGroup("\\-", DATE,
            optionalNamedDecimalGroup("T", HOUR,
            optionalNamedDecimalGroup("\\:", MINUTE,
            optionalNamedDecimalGroup("\\:", SECOND, 
            optionalNamedDecimalGroup("\\.", MILLI))))));
    }


    private static String optionalNamedDecimalGroup(String separator, String name) {
        return optionalNamedDecimalGroup(separator, name, "");
    }


    private static String optionalNamedDecimalGroup(String separator, String name, String post) {
        return "(" + separator + namedDecimalGroup(name) + post + ")?";
    }


    private static String namedDecimalGroup(String separator, String name) {
        return separator + '(' + regexName(name) + "\\d+)";
    }


    private static String namedDecimalGroup(String name) {
        return "(" + regexName(name) + "\\d+)";
    }


    private static String regexName(String name) {
        return "?<" + name + '>';
    }


    private static String month(String monthIndex) {
        if (monthIndex == null) {
            return null;
        }
        return Integer.toString(Integer.parseInt(monthIndex) - 1);
    }


    private static String thousandths(String units) {
        if (units == null) {
            return null;
        }
        StringBuilder thousandths = new StringBuilder(units);
        while (thousandths.length() < 3) {
            thousandths.append('0');
        }
        return thousandths.toString();
    }


    private static void setCalendarField(Calendar calendar, int field, String value) {
        if (value != null) {
            calendar.set(field, Integer.parseInt(value));
        }
    }


    String getString(JSONObject configurationObject, String prefix, String key) {
        String prefixedKey = prefixed(prefix, key);
        return configurationObject.optString(prefixedKey);
    }


    private static String prefixed(String prefix, String key) {
        return prefix + '_' + key;
    }


    void clearLabeledNumbers() {
        labeledNumbers.clear();
    }


    class Figure {
        Figures.DataType type;
        Number number;
    }


    Map<Number, String> getLabeledNumbers() {
        Map<Number, String> map = new HashMap<>();
        for (int i = 0; i < labeledNumbers.size(); ++i) {
            map.put(i + 1, labeledNumbers.get(i).toString());
        }
        return map;
    }


    class Label extends Number {

        Label(String string, int value) {
            this.string = string;
            this.value = value;
        }

        @Override
        public String toString() {
            return string;
        }

        @Override
        public int intValue() {
            return value;
        }

        @Override
        public long longValue() {
            return value;
        }

        @Override
        public float floatValue() {
            return value;
        }

        @Override
        public double doubleValue() {
            return value;
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (! (other instanceof Label)) {
                return false;
            }
            return Objects.equals(string, ((Label) other).string);
        }

        @Override
        public int hashCode() {
            return string.hashCode();
        }

        private final String string;
        private final int value;

    }


    private final ArrayList<Label> labeledNumbers = new ArrayList<>();

    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String DATE = "date";
    private static final String HOUR = "hour";
    private static final String MINUTE = "minute";
    private static final String SECOND = "second";
    private static final String MILLI = "milli";

}
