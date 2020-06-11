/*
** © Bart Kampers
*/

package bka.awt.chart.io;

import lombok.*;

@Data
public class GridStyleConfiguration {

    private StrokeConfiguration stroke;
    private StrokeConfiguration xStroke;
    private StrokeConfiguration yStroke;
    private String color;
    private String xColor;
    private String yColor;
    private String[][] backgrounds;

}
