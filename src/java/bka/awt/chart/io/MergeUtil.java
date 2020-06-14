/*
** © Bart Kampers
*/

package bka.awt.chart.io;

import java.lang.reflect.*;
import java.util.logging.*;


class MergeUtil {


    private MergeUtil() {}


    public static <T> T merge(T base, T additional) {
        if (additional == null) {
            return base;
        }
        if (base == null) {
            return additional;
        }
        return mergeFields(base, additional);
    }


    private static <T> T mergeFields(T base, T additional)  {
        Class type = base.getClass();
        T merged = newInstance(type);
        for (Field field : type.getDeclaredFields()) {
            mergeField(field, merged, base, additional);

        }
        return merged;
    }


    private static <T> T newInstance(Class type) {
        try {
            return (T) type.newInstance();
        }
        catch (InstantiationException | IllegalAccessException ex) {
            throw new IllegalArgumentException("Default constructor not accecssible for class " + type, ex);
        }
    }


    private static <T> void mergeField(Field field, T merged, T base, T additional) {
        try {
            field.setAccessible(true);
            if (isConfiguration(field.getType())) {
                field.set(merged, merge(field.get(base), field.get(additional)));
            }
            else {
                Object additionalValue = field.get(additional);
                field.set(merged, (additionalValue != null) ? additionalValue : field.get(base));
            }
        }
        catch (IllegalArgumentException | IllegalAccessException ex) {
            Logger.getLogger(DataRendererConfiguration.class.getName()).log(Level.SEVERE, "Merge failed for field " + field.getName(), ex);
        }
    }


    private static boolean isConfiguration(Class type) {
        return MergeUtil.class.getPackage().equals(type.getPackage()) && type.getSimpleName().endsWith("Configuration");
    }


}
