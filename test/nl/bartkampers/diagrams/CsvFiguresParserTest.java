/*
 * © Bart Kampers
 */

package nl.bartkampers.diagrams;

import bka.awt.chart.*;
import java.math.*;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;


public class CsvFiguresParserTest {


    @Before
    public void init() {
        parser = new CsvFiguresParser();
    }


    @Test
    public void testPlain() throws UserDataException {
        Figures figures = parser.parse(SOURCE);
        Map<Object, ChartPoints> graphs = figures.getChartData();
        assertEquals(3, graphs.size());
        assertEquals(3, graphs.get("A").size());
        assertEquals(expectedData("1.0", "0.1"), graphs.get("A").get(0));
        assertEquals(expectedData("2.0", "0.2"), graphs.get("A").get(1));
        assertEquals(expectedData("3.0", "0.3"), graphs.get("A").get(2));
        assertEquals(3, graphs.get("B").size());
        assertEquals(expectedData("1.0", "11"), graphs.get("B").get(0));
        assertEquals(expectedData("2.0", "12"), graphs.get("B").get(1));
        assertEquals(expectedData("3.0", "13"), graphs.get("B").get(2));
        assertEquals(1, graphs.get("3").size());
        assertEquals(expectedData("2.0", "2"), graphs.get("3").get(0));
        assertTrue(figures.getLabels().isEmpty());
    }


    @Test
    public void testLabels() throws UserDataException {
        Figures figures = parser.parse(SOURCE_WITH_LABELS);
        Map<Number, String> labels = figures.getLabels();
        assertEquals(3, labels.size());
        assertEquals("a", labels.get(1));
        assertEquals("bb", labels.get(2));
        assertEquals("", labels.get(3));
        ChartPoints graph = figures.getChartData().get("A");
        assertEquals(1, graph.get(0).getX().intValue());
        assertEquals(2, graph.get(1).getX().intValue());
        assertEquals(3, graph.get(2).getX().intValue());
    }


    @Test
    public void testDates() throws UserDataException {
        Map<Object, ChartPoints> graphs = parser.parse(SOURCE_WITH_DATES).getChartData();
        assertEquals(timestamp(1, Calendar.DECEMBER, 1, 0, 0, 0, 0), graphs.get("A").get(0).getX().longValue());
        assertEquals(timestamp(1970, Calendar.JANUARY, 1, 0, 0, 0, 0), graphs.get("A").get(1).getX().longValue());
        assertEquals(timestamp(1970, Calendar.FEBRUARY, 1, 12, 0, 0, 0), graphs.get("A").get(2).getX().longValue());
        assertEquals(timestamp(1970, Calendar.MARCH, 18, 11, 58, 0, 0), graphs.get("A").get(3).getX().longValue());
        assertEquals(timestamp(2000, Calendar.APRIL, 30, 1, 0, 30, 0), graphs.get("A").get(4).getX().longValue());
        assertEquals(timestamp(2001, Calendar.MAY, 31, 22, 30, 15, 100), graphs.get("A").get(5).getX().longValue());
        assertEquals(timestamp(2002, Calendar.JUNE, 4, 3, 45, 59, 990), graphs.get("A").get(6).getX().longValue());
        assertEquals(timestamp(2003, Calendar.JULY, 14, 4, 1, 10, 1), graphs.get("A").get(7).getX().longValue());
        assertEquals(timestamp(2004, Calendar.AUGUST, 24, 5, 2, 20, 123), graphs.get("A").get(8).getX().longValue());
        assertEquals(timestamp(2005, Calendar.SEPTEMBER, 29, 6, 03, 30, 124), graphs.get("A").get(9).getX().longValue());
        assertEquals(timestamp(2006, Calendar.OCTOBER, 30, 7, 4, 41, 0), graphs.get("A").get(10).getX().longValue());
    }


    long timestamp(int year, int month, int date, int hour, int minute, int second, int milli) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, date, hour, minute, second);
        calendar.set(Calendar.MILLISECOND, milli);
        return calendar.getTimeInMillis();
    }


    private ChartPoint expectedData(String x, String y) {
        return new ChartPoint(new BigDecimal(x), new BigDecimal(y));
    }


    private CsvFiguresParser parser;


    private static final String SOURCE
        = "   ,   A,   B\n"
        + "1.0,  0.1, 11, \n"
        + "2.0,  0.2, 12, 2\n"
        + "3.0,  0.3, 13\n";

    private static final String SOURCE_WITH_LABELS
        = "      ,   A\n"
        + "\"a\" ,  0.1\n"
        + "\"bb\",  0.2\n"
        + "\"\"  ,  0.3\n";

    private static final String SOURCE_WITH_DATES
        = "                        ,  A\n"
        + "   1-12                 , 0.1\n"
        + "1970-01-01              , 0.2\n "
        + "1970-02-01T12           , 0.3\n"
        + "1970-03-18T11:58        , 0.4\n"
        + "2000-04-30T01:00:30     , 0.5\n"
        + "2001-05-31T22:30:15.1   , 0.6\n"
        + "2002-06-04T03:45:59.99  , 0.7\n"
        + "2003-07-14T04:01:10.001 , 0.8\n"
        + "2004-08-24T05:02:20.1234, 0.9\n"
        + "2005-09-29T06:03:30.1235, 1.0\n"
        + "2006-10-30T07:04:40.9999, 1.0\n";
}
