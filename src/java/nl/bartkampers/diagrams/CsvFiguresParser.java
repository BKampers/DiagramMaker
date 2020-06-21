/*
** © Bart Kampers
*/

package nl.bartkampers.diagrams;

import java.util.*;


class CsvFiguresParser {


    public Figures parse(String source) throws UserDataException {
        Figures figures = new Figures();
        Map<Integer, String> columnHeaders = new HashMap();
        String[] lines = source.split("\n");
        String[] headers = lines[0].split(",");
        for (int h = 1; h < headers.length; ++h) {
            columnHeaders.put(h, headers[h].trim());
        }
        for (int ln = 1; ln < lines.length; ++ln) {
            String[] columns = lines[ln].split(",");
            if (columns.length > 1) {
                String xText = columns[0].trim();
                for (int col = 1; col < columns.length; ++col) {
                    String yText = columns[col].trim();
                    if (! yText.isEmpty()) {
                        String key = columnHeaders.computeIfAbsent(col, i -> String.valueOf(i));
                        figures.add(key, xText, yText);
                    }
                }
            }
        }
        return figures;
    }


}
