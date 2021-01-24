/*
 * © Bart Kampers
 */

package nl.bartkampers.diagrams;

import bka.awt.chart.*;
import java.math.*;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;


public class YamlFiguresParserTest {


    @Before
    public void init() {
        parser = new YamlFiguresParser();
    }


    @Test
    public void testYaml() throws UserDataException {
        assertCharts(parser.parse(YAML_SOURCE).getChartData());
    }


    @Test
    public void testJson() throws UserDataException {
        assertCharts(parser.parse(JSON_SOURCE).getChartData());
    }


    private void assertCharts(Map<Object, ChartPoints> graphs) {
        assertEquals(2, graphs.size());
        assertEquals(5, graphs.get("A").size());
        assertEquals(expectedData("1.0", "0.1"), graphs.get("A").get(0));
        assertEquals(expectedData("2.0", "0.2"), graphs.get("A").get(1));
        assertEquals(expectedData("2.0", "0.2"), graphs.get("A").get(2));
        assertEquals(expectedData("3.0", "0.3"), graphs.get("A").get(3));
        assertEquals(expectedData("3.0", "0.4"), graphs.get("A").get(4));
        assertEquals(3, graphs.get("B").size());
        assertEquals(expectedData("10.0", "11"), graphs.get("B").get(0));
        assertEquals(expectedData("20.0", "12"), graphs.get("B").get(1));
        assertEquals(expectedData("30.0", "13"), graphs.get("B").get(2));
    }


    private ChartPoint expectedData(String x, String y) {
        return new ChartPoint(new BigDecimal(x), new BigDecimal(y));
    }


    private static final String YAML_SOURCE =
        "A:\n" +
        "-  x: 1.0\n" +
        "   y: 0.1\n" +
        "-  x: 2.0\n" +
        "   y: 0.2\n" +
        "-  x: 2.0\n" +
        "   y: 0.2\n" +
        "-  x: 3.0\n" +
        "   y: 0.3\n" +
        "-  x: 3.0\n" +
        "   y: 0.4\n" +
        "B:\n" +
        "-  x: 10.0\n" +
        "   y: 11\n" +
        "-  x: 20.0\n" +
        "   y: 12\n" +
        "-  x: 30.0\n" +
        "   y: 13";

    private static final String JSON_SOURCE =
        "{" +
        "  \"A\": [" +
        "    { \"x\": 1.0, \"y\": 0.1 }," +
        "    { \"x\": 2.0, \"y\": 0.2 }," +
        "    { \"x\": 2.0, \"y\": 0.2 }," +
        "    { \"x\": 3.0, \"y\": 0.3 }," +
        "    { \"x\": 3.0, \"y\": 0.4 }" +
        "  ]," +
        "  \"B\": [" +
        "    { \"x\": 10.0, \"y\": 11 }," +
        "    { \"x\": 20.0, \"y\": 12 }," +
        "    { \"x\": 30.0, \"y\": 13 }" +
        "  ]" +
        "}";


        private YamlFiguresParser parser;
        
}