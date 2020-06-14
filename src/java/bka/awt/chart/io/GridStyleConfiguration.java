/*
** © Bart Kampers
*/

package bka.awt.chart.io;

import lombok.*;

@Data
public class GridStyleConfiguration {

    StrokeConfiguration stroke;
    StrokeConfiguration xStroke;
    StrokeConfiguration yStroke;
    String color;
    String xColor;
    String yColor;
    String[][] backgrounds;

}
