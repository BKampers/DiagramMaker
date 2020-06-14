/*
** © Bart Kampers
*/

package bka.awt.chart.io;

import java.util.*;
import lombok.*;

@Data
public class PieDrawStyleConfiguration {

    Map<String, List<String>> sliceColors;
    String[][] colors;

}
