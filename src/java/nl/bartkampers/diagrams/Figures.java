/*
** © Bart Kampers
*/

package nl.bartkampers.diagrams;

import bka.awt.chart.*;
import java.util.*;


public final class Figures {


    public enum DataType {
        TEXT, DATE, NUMBER;

        static DataType of(Number number) {
            if (number instanceof Parser.Label) {
                return TEXT;
            }
            return NUMBER;
        }

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


    public void add(Object key, DataType xType, Number x, DataType yType, Number y) {
        ChartData<Number, Number> chartData = data.computeIfAbsent(key, k -> new ChartData<>());
        chartData.add(x, y);
        addType(xTypes, key, xType);
        addType(yTypes, key, yType);
    }


    private static void addType(Map<Object, EnumSet<DataType>> map, Object key, DataType type) {
        EnumSet<DataType> set = map.computeIfAbsent(key, k -> EnumSet.noneOf(DataType.class));
        set.add(type);
    }


    public void add(Object key, Number x, Number y) {
        ChartData<Number, Number> chartData = data.computeIfAbsent(key, k -> new ChartData<>());
        chartData.add(x, y);
        addType(xTypes, key, x);
        addType(yTypes, key, y);
    }


    private static void addType(Map<Object, EnumSet<DataType>> map, Object key, Number number) {
        EnumSet<DataType> set = map.computeIfAbsent(key, k -> EnumSet.noneOf(DataType.class));
        set.add(DataType.of(number));
    }


    private final Map<Object, ChartData<Number, Number>> data = new LinkedHashMap<>();
    private final Map<Object, EnumSet<DataType>> xTypes = new HashMap<>();
    private final Map<Object, EnumSet<DataType>> yTypes = new HashMap<>();

}
