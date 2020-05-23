/*
** © Bart Kampers
*/

package bka.awt.chart.io;

import bka.awt.chart.render.ChartRenderer.GridMode;
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
    private int leftOffset;
    private int rightOffset;
    private AxisConfiguration[] xAxisConfigurations;
    private AxisConfiguration[] yAxisConfigurations;
    private GridConfiguration gridConfiguration;
    private GridMarkerConfiguration xGridMarkerConfiguration;
    private GridMarkerConfiguration yGridMarkerConfiguration;
    private GridMode gridMode;
    private DataRendererConfiguration[] dataRendererConfigurations;

}
