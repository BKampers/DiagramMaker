/*
** © Bart Kampers
*/

package bka.awt.chart.io;

import lombok.*;

@Data
public class DataRendererConfiguration {

    private String key;
    private String type;
    private Double base;
    private Integer width;
    private Integer height;
    private Integer points;
    private Integer innerRadius;
    private Integer outerRadius;
//    private String color;
//    private String[] colors;
//    private String borderColor;
    private AreaDrawStyleConfiguration areaDrawStyleConfiguration;
    private String image;

}
