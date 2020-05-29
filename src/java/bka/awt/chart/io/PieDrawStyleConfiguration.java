/*
** © Bart Kampers
*/

package bka.awt.chart.io;

import java.util.*;
import lombok.*;

@Data
public class PieDrawStyleConfiguration {

    private Map<String, List<String>> colorsByKey;
    private String[][] colors;

}
