/*
** © Bart Kampers
*/

package nl.bartkampers.diagrams;

import java.io.*;
import java.util.*;
import net.sourceforge.yamlbeans.*;


public class YamlFiguresParser {


    public Figures parse(String source) throws UserDataException {
        try {
            YamlReader reader = new YamlReader(source);
            Figures figures = parseChartDataMap(reader.read(Map.class));
            reader.close();
            return figures;
        }
        catch (IOException | YamlException ex) {
            throw new UserDataException(ex);
        }
    }


    private Figures parseChartDataMap(Map<Object, List<Map>> root) throws UserDataException {
        Figures figures = new Figures();
        for (Map.Entry<Object, List<Map>> entry : root.entrySet()) {
            for (Map dataPoint : entry.getValue()) {
                figures.add(entry.getKey(), get(dataPoint, "x"), get(dataPoint, "y"));
            }
        }
        return figures;
    }


    private static String get(Map map, String key) throws UserDataException {
        if (! map.containsKey(key)) {
            throw new UserDataException("Missing: " + key);
        }
        return map.get(key).toString();
    }

}
