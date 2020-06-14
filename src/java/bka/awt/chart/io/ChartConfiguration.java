/*
** © Bart Kampers
*/

package bka.awt.chart.io;

import bka.awt.chart.render.ChartRenderer.GridMode;
import java.util.*;
import lombok.*;

@Data
public class ChartConfiguration {

    private Integer width;
    private Integer height;
    private String locale;
    private Integer leftMargin;
    private Integer rightMargin;
    private Integer topMargin;
    private Integer bottomMargin;
    private String title;
    private PointConfiguration legendPosition;
    private Double xWindowMinimum;
    private Double xWindowMaximum;
    private Double yWindowMinimum;
    private Double yWindowMaximum;
    private Map<String, RangeConfiguration> yWindows;
    private int leftOffset;
    private int rightOffset;
    private boolean xExpandToGrid;
    private boolean yExpandToGrid;
    private List<AxisConfiguration> xAxes;
    private List<AxisConfiguration> yAxes;
    private GridStyleConfiguration gridStyle;
    private GridConfiguration xGrid;
    private GridConfiguration yGrid;
    private GridMode gridMode;
    private DataRendererConfiguration graphDefaults;
    private Map<String, DataRendererConfiguration> graphs;
    private List<String> stack;

}
