/*
** © Bart Kampers
*/

package bka.awt.chart.io;

import lombok.*;

@Data
public class DataRendererConfiguration {


    String type;
    Double base;
    Integer width;
    Integer height;
    Integer points;
    Integer innerRadius;
    Integer outerRadius;
    DataRendererConfiguration marker;
    AreaDrawStyleConfiguration graphDrawStyle;
    PieDrawStyleConfiguration pieDrawStyle;
    String image;
    Integer shift;
    Boolean autoShift;

}
