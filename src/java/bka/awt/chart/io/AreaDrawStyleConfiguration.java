/*
** © Bart Kampers
*/

package bka.awt.chart.io;

import lombok.*;

@Data
public class AreaDrawStyleConfiguration {


    private String image;
    private String[] colors;
    private String color;
    private String borderColor;
    private String centerColor;
    private String edgeColor;
    private String topAreaColor;
    private String bottomAreaColor;
    private StrokeConfiguration stroke;
    private String font;
    private AreaDrawStyleConfiguration markerDrawStyle;

}
