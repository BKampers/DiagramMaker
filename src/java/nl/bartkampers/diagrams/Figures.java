/*
** © Bart Kampers
*/

package nl.bartkampers.diagrams;

import bka.awt.chart.*;
import bka.text.*;
import java.math.*;
import java.util.*;


public final class Figures {


    public enum DataType {
        TEXT, DATE, NUMBER;
    }


    public void add(Object key, String xText, String yText) throws UserDataException {
        ChartData<Number, Number> chartData = data.computeIfAbsent(key, k -> new ChartData<>());
        chartData.add(qualifiedNumber(key, xText, xTypes), qualifiedNumber(key, yText, yTypes));
    }


    private Number qualifiedNumber(Object key, String string, Map<Object, EnumSet<DataType>> types) throws UserDataException {
        Date date = TIMESTAMP_MATCHER.getDate(string);
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
            throw new UserDataException("Invalid number: " + string, ex);
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
            return string.equals(((Label) other).string);
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

    private static final TimestampMatcher TIMESTAMP_MATCHER = new TimestampMatcher();

}
