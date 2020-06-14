/*
** © Bart Kampers
*/

package bka.awt.chart.io;

import bka.awt.chart.render.*;
import lombok.*;

@Data
public class AxisConfiguration {

    String key;
    ChartRenderer.AxisPosition position;
    String title;
    String unit;
    AxisStyleConfiguration axisStyle;

}
