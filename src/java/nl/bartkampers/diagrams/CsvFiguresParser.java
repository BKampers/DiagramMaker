/*
** © Bart Kampers
*/

package nl.bartkampers.diagrams;

import java.util.*;


class CsvFiguresParser {


    public CsvFiguresParser() {
        this(",");
    }


    public CsvFiguresParser(String columnSeparator) {
        this(columnSeparator, "\n");
    }


    public CsvFiguresParser(String columnSeparator, String lineSeparator) {
        this.columnSeparator = columnSeparator;
        this.lineSeparator = lineSeparator;
    }


    public Figures parse(String source) throws UserDataException {
        Figures figures = new Figures();
        Map<Integer, String> columnHeaders = new HashMap();
        String[] lines = source.split(lineSeparator);
        String[] headers = lines[0].split(columnSeparator);
        for (int h = 1; h < headers.length; ++h) {
            columnHeaders.put(h, headers[h].trim());
        }
        for (int ln = 1; ln < lines.length; ++ln) {
            parseColums(lines[ln].split(columnSeparator), columnHeaders, figures);
        }
        return figures;
    }

    
    private void parseColums(String[] columns, Map<Integer, String> columnHeaders, Figures figures) throws UserDataException {
        if (columns.length > 1) {
            String x = columns[0].trim();
            for (int c = 1; c < columns.length; ++c) {
                String y = columns[c].trim();
                if (! y.isEmpty()) {
                    String key = columnHeaders.computeIfAbsent(c, i -> String.valueOf(i));
                    figures.add(key, x, y);
                }
            }
        }
    }


    private final String columnSeparator;
    private final String lineSeparator;

}
