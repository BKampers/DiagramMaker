/*
** © Bart Kampers
*/

package bka.awt.chart.io;

import bka.awt.chart.custom.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.imageio.*;


public class AwtBuilder {


    public Image buildImage(String imageUrl) throws ChartConfigurationException {
        try {
            return ImageIO.read(new URL(imageUrl));
        }
        catch (IOException ex) {
            throw new ChartConfigurationException("Cannot load image from " + imageUrl, ex);
        }
    }


    public GridStyle.PaintBox buildPaintBox(String[][] backgrounds) throws ChartConfigurationException {
        if (backgrounds == null || backgrounds.length == 0) {
            return null;
        }
        if (backgrounds.length == 1) {
            return createSolidPaintBox(backgrounds[0]);
        }
        return createGradientPaintBox(backgrounds);
    }


    private GridStyle.PaintBox createSolidPaintBox(String[] backgrounds) throws ChartConfigurationException {
        return GridStyle.createSolidPaintBox(buildColors(backgrounds));
    }


    private GridStyle.PaintBox createGradientPaintBox(String[][] backgrounds) throws ChartConfigurationException {
        if (backgrounds.length != 2) {
            throw new ChartConfigurationException("Cannot create gradient from " + Objects.toString(backgrounds));
        }
        Color[] colors1 = buildColors(backgrounds[0]);
        Color[] colors2 = buildColors(backgrounds[1]);
        return GridStyle.createGradientPaintBox(colors1, colors2);
    }


    public Color[][] buildColors(String[][] colorCodes) throws ChartConfigurationException {
        Color[][] colors = new Color[colorCodes.length][];
        for (int i = 0; i < colorCodes.length; ++i) {
            colors[i] = buildColors(colorCodes[i]);
        }
        return colors;
    }


    public Color[] buildColors(String[] colorCodes) throws ChartConfigurationException {
        Color[] colors = new Color[colorCodes.length];
        for (int i = 0; i < colorCodes.length; ++i) {
            colors[i] = buildColor(colorCodes[i]);
        }
        return colors;
    }


    public Color[] buildColors(java.util.List<String> colorCodes) throws ChartConfigurationException {
        Color[] colors = new Color[colorCodes.size()];
        for (int i = 0; i < colorCodes.size(); ++i) {
            colors[i] = buildColor(colorCodes.get(i));
        }
        return colors;
    }


    public Color buildColor(String colorCode) throws ChartConfigurationException {
        if (colorCode == null || NULL.equals(colorCode)) {
            return null;
        }
        try {
            String[] split = colorCode.split("-");
            if (split.length == 1 && (colorCode.length() == 6 || colorCode.length() == 8)) {
                return new Color(buildRgba(colorCode), colorCode.length() == 8);
            }
            if (2 <= split.length && split.length <= 4) {
                return new Color(buildRgba(split), (split.length == 4));
            }
            throw new ChartConfigurationException("Invalid color code " + colorCode);
        }
        catch (NumberFormatException ex) {
            throw new ChartConfigurationException("Invalid color code " + colorCode, ex);
        }
    }


    private static int buildRgba(String colorCode) {
        return (int) Long.parseLong(colorCode, 16);
    }


    private static int buildRgba(String[] rgbaCodes) {
        int rgba = 0;
        for (int i = 0; i < rgbaCodes.length; ++i) {
            rgba = (rgba << 8) | Integer.parseInt(rgbaCodes[i]);
        }
        return rgba;
    }


    private static final String NULL = "null";

}
