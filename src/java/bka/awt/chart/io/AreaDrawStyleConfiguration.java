/*
** © Bart Kampers
*/

package bka.awt.chart.io;

import lombok.*;

@Data
public class AreaDrawStyleConfiguration {


    String image;
    String[] colors;
    String color;
    String borderColor;
    String centerColor;
    String edgeColor;
    String topAreaColor;
    String bottomAreaColor;
    StrokeConfiguration stroke;
    String font;
    AreaDrawStyleConfiguration markerDrawStyle;

}
