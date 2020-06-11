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
        Class type = base.getClass();
        T merged = newInstance(type);
        for (Field field : type.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                if (isConfiguration(field.getType())) {
                    field.set(merged, merge(field.get(base), field.get(additional)));
                }
                else {
                    field.set(merged, (field.get(additional) != null) ? field.get(additional) : field.get(base));
                }
            }
            catch (IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(DataRendererConfiguration.class.getName()).log(Level.SEVERE, "Merge failed for field " + field.getName(), ex);
            }

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


    private static boolean isConfiguration(Class type) {
        return MergeUtil.class.getPackage().equals(type.getPackage()) && type.getSimpleName().endsWith("Configuration");
    }


}
