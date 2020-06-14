/*
** © Bart Kampers
*/

package bka.awt.chart.io;

import bka.awt.chart.render.*;
import lombok.*;

@Data
public class AxisConfiguration {

    private String key;
    private ChartRenderer.AxisPosition position;
    private String title;
    private String unit;
    private AxisStyleConfiguration axisStyle;

}
