/*
 * © Bart Kampers
 */

package nl.bartkampers.diagrams;

import bka.awt.chart.*;
import java.awt.*;
import java.math.*;
import java.text.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import org.json.*;
import org.junit.*;
import static org.junit.Assert.*;
import org.mockito.*;


public class ParserTest {
 
    @Before
    public void setUp() {
        parser = new Parser();
    }

    @Test
    public void testApplyDouble() throws JSONException, UserDataException {
        JSONObject object = new JSONObject();
        object.put("zero", 0.0);
        Consumer<Number> consumer = Mockito.mock(Consumer.class);
        parser.applyNumber(object, "zero", consumer);
        Mockito.verify(consumer, Mockito.times(1)).accept(0.0);
        parser.applyNumber(object, "nan", consumer);
        Mockito.verify(consumer, Mockito.times(0)).accept(Double.NaN);
    }

    @Test
    public void testGetChartDataCsv() throws JSONException, UserDataException  {
        Map<Object, ChartData<Number, Number>> chartDataMap = parser.getFigures("x,y,z\n10,100,1000\n20,200,2000").getChartData();
        assertEquals(2, chartDataMap.size());
        assertEquals(2, chartDataMap.get("y").size());
        assertEquals(10, chartDataMap.get("y").get(0).getKey().intValue());
        assertEquals(100, chartDataMap.get("y").get(0).getValue().intValue());
        assertEquals(20, chartDataMap.get("y").get(1).getKey().intValue());
        assertEquals(200, chartDataMap.get("y").get(1).getValue().intValue());
        assertEquals(2, chartDataMap.get("z").size());
        assertEquals(10, chartDataMap.get("z").get(0).getKey().intValue());
        assertEquals(1000, chartDataMap.get("z").get(0).getValue().intValue());
        assertEquals(20, chartDataMap.get("z").get(1).getKey().intValue());
        assertEquals(2000, chartDataMap.get("z").get(1).getValue().intValue());
    }

    @Test
    public void testGetChartDataJson() throws JSONException, UserDataException {
        Map<Object, ChartData<Number, Number>> chartDataMap = parser.getFigures("[{\"key\":\"y\",\"values\":[[10,100],[20,200]]},{\"key\":\"z\",\"values\":[[10,1000],[20,2000]]}]").getChartData();
        assertEquals(2, chartDataMap.size());
        assertEquals(2, chartDataMap.get("y").size());
        assertEquals(10, chartDataMap.get("y").get(0).getKey().intValue());
        assertEquals(100, chartDataMap.get("y").get(0).getValue().intValue());
        assertEquals(20, chartDataMap.get("y").get(1).getKey().intValue());
        assertEquals(200, chartDataMap.get("y").get(1).getValue().intValue());
        assertEquals(2, chartDataMap.get("z").size());
        assertEquals(10, chartDataMap.get("z").get(0).getKey().intValue());
        assertEquals(1000, chartDataMap.get("z").get(0).getValue().intValue());
        assertEquals(20, chartDataMap.get("z").get(1).getKey().intValue());
        assertEquals(2000, chartDataMap.get("z").get(1).getValue().intValue());
    }

    @Test
    public void testGetColorsKey() throws JSONException, UserDataException {
        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();
        array.put("0-0-255");
        array.put("255-255-255");
        array.put("255-0-0");
        object.put("colors", array);
        assertArrayEquals(new Color[] { Color.BLUE, Color.WHITE, Color.RED }, parser.getColors(object, "colors"));
    }

    @Test
    public void testGetColorsArray() throws  JSONException, UserDataException {
        JSONArray array = new JSONArray();
        array.put("0000ff");
        array.put("ffffff");
        array.put("ff0000");
        assertArrayEquals(new Color[] { Color.BLUE, Color.WHITE, Color.RED }, parser.getColors(array));
    }

    @Test
    public void testGetPrefixedColor() throws  JSONException, UserDataException {
        JSONObject object = new JSONObject();
        object.put("x_axis", "255-255-255");
        object.put("y_axis", "0-0-255");
        object.put("x_label", "0-255-0");
        object.put("y_label", "255-0-0");
        object.put("y_marker", JSONObject.NULL);
        assertEquals(Color.WHITE, parser.getColor(object, "x", "axis"));
        assertEquals(Color.BLUE, parser.getColor(object, "y", "axis"));
        assertEquals(Color.GREEN, parser.getColor(object, "x", "label"));
        assertEquals(Color.RED, parser.getColor(object, "y", "label"));
        assertEquals(Color.BLACK, parser.getColor(object, "x", "marker"));
        assertNull(parser.getColor(object, "y", "marker"));
    }

    @Test
    public void testGetColorFromArray() throws JSONException, UserDataException {
        JSONArray array = new JSONArray();
        array.put("255-0-0");
        array.put("0-255-0");
        array.put("0-0-255");
        assertEquals(Color.RED, parser.getColor(array, 0));
        assertEquals(Color.GREEN, parser.getColor(array, 1));
        assertEquals(Color.BLUE, parser.getColor(array, 2));
    }

    @Test
    public void testGetColorDefault() throws JSONException, UserDataException {
        JSONObject object = new JSONObject();
        object.put("white", "ffffff");
        assertEquals(Color.WHITE, parser.getColor(object, "white", Color.BLACK));
        assertEquals(Color.BLACK, parser.getColor(object, "undefined", Color.BLACK));
    }

    @Test
    public void testGetColor() throws JSONException, UserDataException {
        JSONObject object = new JSONObject();
        object.put("white", "ffffff");
        assertEquals(Color.WHITE, parser.getColor(object, "white"));
        assertNull(parser.getColor(object, "undefined"));
    }

    @Test
    public void testGetFloats() throws JSONException, UserDataException {
        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();
        array.put(0.0f);
        array.put(1.0f);
        object.put("floats", array);
        assertArrayEquals(new float[] { 0.0f, 1.0f }, parser.getFloats(object, "floats"), 0.0f);
    }

    @Test
    public void testGetFigure() throws UserDataException {
        assertEquals(BigDecimal.ZERO, parser.getFigure("0").number);
        assertEquals(BigDecimal.ONE, parser.getFigure(" 1 ").number);
        assertEquals(1, parser.getFigure("\"A label\"").number.intValue());
        assertEquals("A label", parser.getFigure("\"A label\"").number.toString());
        assertEquals(2, parser.getFigure("\"Another label\"").number.intValue());
        assertEquals("Another label", parser.getFigure("\"Another label\"").number.toString());
        assertEquals((long) -TimeZone.getDefault().getRawOffset(), parser.getFigure("1970-01-01").number);
    }
    
    @Test(expected=UserDataException.class)
    public void testGetNumberUserDataException() throws UserDataException {
        parser.getFigure("");
    }

    @Test
    public void testGetDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        assertEquals("2020-03-01T00:00:00.000", formatter.format(parser.getDate("2020-03")));
        assertEquals("2020-03-28T00:00:00.000", formatter.format(parser.getDate("2020-03-28")));
        assertEquals("1970-03-18T11:58:00.000", formatter.format(parser.getDate("1970-03-18T11:58")));
        assertEquals("1900-01-01T07:30:12.000", formatter.format(parser.getDate("1900-01-01T07:30:12")));
        assertEquals("1700-02-28T06:15:01.100", formatter.format(parser.getDate("1700-02-28T06:15:01.1")));
        assertEquals("1500-04-30T05:10:02.220", formatter.format(parser.getDate("1500-04-30T05:10:02.22")));
        assertEquals("1201-05-31T04:08:03.333", formatter.format(parser.getDate("1201-05-31T04:08:03.333")));
        assertEquals("0001-01-01T01:01:01.100", formatter.format(parser.getDate("1-1-1T1:1:1.1")));
    }


    @Test
    public void testGetDateInvalid() {
        assertNull(parser.getDate(""));
        assertNull(parser.getDate("T"));
        assertNull(parser.getDate("1234"));
        assertNull(parser.getDate("1234-01-"));
        assertNull(parser.getDate("1234-01-01T"));
        assertNull(parser.getDate("1234-01-01-01"));
        assertNull(parser.getDate("1234-01-01T12.00.00"));
        assertNull(parser.getDate("1234-01-01T12:00:00:00"));
    }

    @Test
    public void testGetPrefixedString() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("x_title", "X Title");
        object.put("x_unit", "X Unit");
        object.put("y_title", "Y Title");
        object.put("y_unit", "Y Unit");
        assertEquals("X Title", parser.getString(object, "x", "title"));
        assertEquals("Y Title", parser.getString(object, "y", "title"));
        assertEquals("X Unit", parser.getString(object, "x", "unit"));
        assertEquals("Y Unit", parser.getString(object, "y", "unit"));
    }

    @Test
    public void testClearLabeledNumbers() throws UserDataException {
        parser.getNumber("\"Label\"");
        assertFalse(parser.getLabeledNumbers().isEmpty());
        parser.clearLabeledNumbers();
        assertTrue(parser.getLabeledNumbers().isEmpty());
    }

    @Test
    public void testGetLabeledNumbers() throws UserDataException {
        assertTrue(parser.getLabeledNumbers().isEmpty());
        parser.getFigure("\"A\"");
        parser.getFigure("\"B\"");
        assertEquals("A", parser.getLabeledNumbers().get(1));
        assertEquals("B", parser.getLabeledNumbers().get(2));
    }


    private Parser parser;

}