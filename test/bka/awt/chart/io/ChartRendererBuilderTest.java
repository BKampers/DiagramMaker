package bka.awt.chart.io;

/*
 * © Bart Kampers
 */

import bka.awt.chart.render.*;
import java.io.*;
import java.util.*;
import net.sourceforge.yamlbeans.*;
import nl.bartkampers.diagrams.*;
import org.junit.*;
import static org.junit.Assert.*;


public class ChartRendererBuilderTest {

   @Test
   public void test() throws ChartConfigurationException, IOException, YamlException {
       StrokeConfiguration strokeConfiguration = new StrokeConfiguration();
       strokeConfiguration.setWidth(1.0f);
       strokeConfiguration.setDash(new float[] { 0.75f, 0.25f });
       GridStyleConfiguration gridStyleConfiguration = new GridStyleConfiguration();
       gridStyleConfiguration.setStroke(strokeConfiguration);
       gridStyleConfiguration.setColor("777777");
       GridConfiguration gridConfiguration = new GridConfiguration();
       gridConfiguration.setGridStyleConfiguration(gridStyleConfiguration);
       ChartConfiguration chartConfiguration = new ChartConfiguration();
       chartConfiguration.setGridConfiguration(gridConfiguration);
       chartConfiguration.setGridMode(ChartRenderer.GridMode.NONE);
       GridMarkerConfiguration xGrid = new GridMarkerConfiguration();
       xGrid.setType("Integer");
       chartConfiguration.setXGridMarkerConfiguration(xGrid);
       AxisConfiguration axis = new AxisConfiguration();
       axis.setPosition(ChartRenderer.AxisPosition.ORIGIN);
       axis.setTitle("t");
       axis.setUnit("s");
       AxisConfiguration[] xAxis = new AxisConfiguration[] { axis };
       chartConfiguration.setXAxisConfigurations(xAxis);
       chartConfiguration.setYAxisConfigurations(new AxisConfiguration[] {});
       PointConfiguration legendPosition = new PointConfiguration();
       legendPosition.setX(1);
       legendPosition.setY(23);
       chartConfiguration.setLegendPosition(legendPosition);
       ChartRendererBuilder builder = new ChartRendererBuilder(new Figures(), Collections.emptyMap());
       YamlWriter writer = new YamlWriter(new FileWriter("test/resources/renderer.yml"));
       writer.getConfig().setClassTag("Chart", ChartConfiguration.class);
       writer.getConfig().setClassTag("Axis", AxisConfiguration.class);
       writer.write(chartConfiguration);
       writer.close();
       YamlReader reader = new YamlReader(new FileReader("test/resources/grid.json"));
//       reader.getConfig().setClassTag("Chart", ChartConfiguration.class);
//       reader.getConfig().setClassTag("Grid", GridConfiguration.class);
       ChartConfiguration chartConfiguration2 = reader.read(ChartConfiguration.class);
       reader.close();
       ChartRenderer renderer = builder.buildChartRenderer(chartConfiguration2);
       assertEquals(ChartRenderer.GridMode.NONE, renderer.getGridMode());
   }

}