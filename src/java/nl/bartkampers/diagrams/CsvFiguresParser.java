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
        String[] lines = source.split(lineSeparator);
        String[] headers = lines[0].split(columnSeparator);
        LinkedHashMap<String, List<String[]>> map = new LinkedHashMap<>();
        for (int h = 1; h < headers.length; ++h) {
            map.put(headers[h].trim(), new ArrayList<>());
        }
        for (int ln = 1; ln < lines.length; ++ln) {
            parseColums(lines[ln].split(columnSeparator), map);
        }
        for (Map.Entry<String, List<String[]>> entry : map.entrySet()) {
            for (String[] coordinate : entry.getValue()) {
                figures.add(entry.getKey(), coordinate[0], coordinate[1]);
            }
        }
        return figures;
    }

    private void parseColums(String[] columns, LinkedHashMap<String, List<String[]>> figures) throws UserDataException {
        if (columns.length > 1) {
            String x = columns[0].trim();
            Iterator<String> headers = figures.keySet().iterator();
            for (int c = 1; c < columns.length; ++c) {
                String header = (headers.hasNext()) ? headers.next() : Integer.toString(c);
                String y = columns[c].trim();
                List<String[]> list = figures.computeIfAbsent(header, h -> new ArrayList<>());
                if (!y.isEmpty()) {
                    list.add(new String[]{x, y});
                }
            }
        }
    }

    private final String columnSeparator;
    private final String lineSeparator;

}
