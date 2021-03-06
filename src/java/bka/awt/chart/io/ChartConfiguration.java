/*
** © Bart Kampers
*/

package bka.awt.chart.io;

import bka.awt.chart.render.ChartRenderer.GridMode;
import java.util.*;
import lombok.*;

@Data
public class ChartConfiguration {

    Integer width;
    Integer height;
    String locale;
    Integer leftMargin;
    Integer rightMargin;
    Integer topMargin;
    Integer bottomMargin;
    String title;
    PointConfiguration legendPosition;
    Double xWindowMinimum;
    Double xWindowMaximum;
    Double yWindowMinimum;
    Double yWindowMaximum;
    Map<String, RangeConfiguration> yWindows;
    Integer leftOffset;
    Integer rightOffset;
    boolean xExpandToGrid;
    boolean yExpandToGrid;
    AxisStyleConfiguration axisStyleDefaults;
    List<AxisConfiguration> xAxes;
    List<AxisConfiguration> yAxes;
    GridStyleConfiguration gridStyle;
    GridConfiguration xGrid;
    GridConfiguration yGrid;
    GridMode gridMode;
    DataRendererConfiguration graphDefaults;
    Map<String, DataRendererConfiguration> graphs;
    List<String> stack;
    String tooltipFormat;
    String tooltipXFormat;
    String tooltipYFormat;

}
