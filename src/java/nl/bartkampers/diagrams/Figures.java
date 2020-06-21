/*
** © Bart Kampers
*/

package nl.bartkampers.diagrams;

import bka.awt.chart.*;
import java.math.*;
import java.util.*;
import java.util.regex.*;


public final class Figures {


    public enum DataType {
        TEXT, DATE, NUMBER;
    }


    public void add(Object key, String xText, String yText) throws UserDataException {
        ChartData<Number, Number> chartData = data.computeIfAbsent(key, k -> new ChartData<>());
        chartData.add(qualifiedNumber(key, xText, xTypes), qualifiedNumber(key, yText, yTypes));
    }


    private Number qualifiedNumber(Object key, String string, Map<Object, EnumSet<DataType>> types) throws UserDataException {
        Date date = getDate(string);
        if (date != null) {
            return qualifiedNumber(key, date.getTime(), DataType.DATE, types);
        }
        if (string.length() > 1 && string.startsWith("\"") && string.endsWith("\"")) {
            return qualifiedNumber(key, getLabel(string), DataType.TEXT, types);
        }
        try {
            return qualifiedNumber(key, new BigDecimal(string), DataType.NUMBER, types);
        }
        catch (NumberFormatException ex) {
            throw new UserDataException(ex);
        }
    }


    private static Number qualifiedNumber(Object key, Number number, DataType type, Map<Object, EnumSet<DataType>> types) {
        addType(types, key, type);
        return number;
    }


    public Map<Object, ChartData<Number, Number>> getChartData() {
        return Collections.unmodifiableMap(data);
    }


    public Map<Object, EnumSet<DataType>> getXTypes() {
        return Collections.unmodifiableMap(xTypes);
    }


    public Map<Object, EnumSet<DataType>> getYTypes() {
        return Collections.unmodifiableMap(yTypes);
    }


    public EnumSet<DataType> typeSet(Map<Object, EnumSet<DataType>> typeMap) {
        EnumSet<DataType> types = EnumSet.noneOf(DataType.class);
        for (EnumSet<DataType> typeSet : typeMap.values()) {
            types.addAll(typeSet);
        }
        return types;
    }


    public Map<Number, String> getLabels() {
        Map<Number, String> map = new HashMap<>();
        for (int i = 0; i < labels.size(); ++i) {
            map.put(i + 1, labels.get(i).toString());
        }
        return map;
    }


    private static void addType(Map<Object, EnumSet<DataType>> map, Object key, DataType type) {
        EnumSet<DataType> set = map.computeIfAbsent(key, k -> EnumSet.noneOf(DataType.class));
        set.add(type);
    }


    private Label getLabel(String string) {
        Label label = new Label(string.substring(1, string.length() - 1), labels.size() + 1);
        int index = labels.indexOf(label);
        if (index < 0) {
            labels.add(label);
            return label;
        }
        return labels.get(index);
    }

  
    private static Date getDate(String value) {
        Matcher matcher = datePattern().matcher(value);
        if (! matcher.matches()) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.ERA, GregorianCalendar.AD);
        applyIfSet(calendar, Calendar.YEAR, matcher.group(YEAR));
        applyIfSet(calendar, Calendar.MONTH, month(matcher.group(MONTH)));
        applyIfSet(calendar, Calendar.DATE, matcher.group(DATE));
        applyIfSet(calendar, Calendar.HOUR, matcher.group(HOUR));
        applyIfSet(calendar, Calendar.MINUTE, matcher.group(MINUTE));
        applyIfSet(calendar, Calendar.SECOND, matcher.group(SECOND));
        applyIfSet(calendar, Calendar.MILLISECOND, thousandths(matcher.group(MILLI)));
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
        return separator + "(" + regexName(name) + "\\d+)";
    }


    private static String namedDecimalGroup(String name) {
        return "(" + regexName(name) + "\\d+)";
    }


    private static String regexName(String name) {
        return "?<" + name + ">";
    }


    private static String month(String monthIndex) {
        if (monthIndex == null) {
            return null;
        }
        return Integer.toString(Integer.parseInt(monthIndex) - 1);
    }


    private static String thousandths(String units) {
        if (units == null || units.length() >= THOUSANDTHS_LENGTH) {
            return units;
        }
        StringBuilder thousandths = new StringBuilder(units);
        while (thousandths.length() < 3) {
            thousandths.append('0');
        }
        return thousandths.toString();
    }


    private static void applyIfSet(Calendar calendar, int field, String value) {
        if (value != null) {
            calendar.set(field, Integer.parseInt(value));
        }
    }


    private class Label extends Number {

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


    private final Map<Object, ChartData<Number, Number>> data = new LinkedHashMap<>();
    private final Map<Object, EnumSet<DataType>> xTypes = new HashMap<>();
    private final Map<Object, EnumSet<DataType>> yTypes = new HashMap<>();

    private final ArrayList<Label> labels = new ArrayList<>();

    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String DATE = "date";
    private static final String HOUR = "hour";
    private static final String MINUTE = "minute";
    private static final String SECOND = "second";
    private static final String MILLI = "milli";

    private static final int THOUSANDTHS_LENGTH = 3;

}
