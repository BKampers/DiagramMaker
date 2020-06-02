/*
** © Bart Kampers
*/

package bka.awt.chart.io;

import java.lang.reflect.*;
import java.util.logging.*;
import lombok.*;

@Data
public class DataRendererConfiguration {


    public static DataRendererConfiguration merge(DataRendererConfiguration base, DataRendererConfiguration additional) {
        if (additional == null) {
            return base;
        }
        if (base == null) {
            return additional;
        }
        DataRendererConfiguration configuration = new DataRendererConfiguration();
        for (Field field : DataRendererConfiguration.class.getDeclaredFields()) {
            try {
                field.set(configuration, (field.get(additional) != null) ? field.get(additional) : field.get(base));
            }
            catch (IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(DataRendererConfiguration.class.getName()).log(Level.SEVERE, "Merge failed for field " + field.getName(), ex);
            }
        }
        return configuration;
    }


    private String type;
    private Double base;
    private Integer width;
    private Integer height;
    private Integer points;
    private Integer innerRadius;
    private Integer outerRadius;
    private AreaDrawStyleConfiguration areaDrawStyleConfiguration;
    private PieDrawStyleConfiguration pieDrawStyleConfiguration;
    private String image;

}
