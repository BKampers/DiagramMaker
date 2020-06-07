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
    private Map<String, RangeConfiguration> windowConfigurations;
    private int leftOffset;
    private int rightOffset;
    private List<AxisConfiguration> xAxisConfigurations;
    private List<AxisConfiguration> yAxisConfigurations;
    private GridConfiguration gridConfiguration;
    private GridMarkerConfiguration xGridMarkerConfiguration;
    private GridMarkerConfiguration yGridMarkerConfiguration;
    private GridMode gridMode;
    private DataRendererConfiguration defaultDataRendererConfiguration;
    private Map<String, DataRendererConfiguration> dataRendererConfigurations;
    private List<String> stack;

}
