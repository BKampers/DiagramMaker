/*
** © Bart Kampers
*/

package bka.awt.chart.io;

import bka.awt.chart.render.*;
import lombok.*;

@Data
public class AxisConfiguration {

    private ChartRenderer.AxisPosition position;
    private String title;
    private String unit;

}
