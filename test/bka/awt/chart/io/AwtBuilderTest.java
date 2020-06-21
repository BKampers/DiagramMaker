/*
** © Bart Kampers
*/

package bka.awt.chart.io;

import java.awt.*;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;


public class AwtBuilderTest {

    @Before
    public void init() {
        builder = new AwtBuilder();
    }


    @Test
    public void testBuildColor() throws ChartConfigurationException {
        assertEquals(new Color(0x0a0b0c), builder.buildColor("0A0B0C"));
        assertEquals(new Color(0x0a0b0c), builder.buildColor("10-11-12"));
        assertEquals(new Color(0x7f0a0b0c, true), builder.buildColor("7f0a0b0c"));
        assertEquals(new Color(0x7f0a0b0c, true), builder.buildColor("127-10-11-12"));
    }


    @Test
    public void testBuildColors1d() throws ChartConfigurationException {
        String[] array = new String[] { "0A0B0C", "0D0E0F" };
        Color[] expected = new Color[] { new Color(0x0a0b0c), new Color(0x0d0e0f) };
        Assert.assertArrayEquals(expected, builder.buildColors(array));
        Assert.assertArrayEquals(expected, builder.buildColors(Arrays.asList(array)));
    }

    
    @Test
    public void testBuildColors2d() throws ChartConfigurationException {
        String[][] array = new String[][] {
            new String[] { "0A0B0C", "0D0E0F" },
            new String[] { "010203", "040506" } };
        Color[][] expected = new Color[][] {
            new Color[] { new Color(0x0a0b0c), new Color(0x0d0e0f) },
            new Color[] { new Color(0x010203), new Color(0x040506) } };
        Assert.assertArrayEquals(expected, builder.buildColors(array));
    }


    private AwtBuilder builder;

}
