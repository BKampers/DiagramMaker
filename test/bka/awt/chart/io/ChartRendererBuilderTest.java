package bka.awt.chart.io;

/*
 * © Bart Kampers
 */

import bka.awt.chart.render.*;
import java.io.*;
import java.util.*;
import net.sourceforge.yamlbeans.*;
import org.junit.*;


public class ChartRendererBuilderTest {

   @Test
   public void test() throws ChartConfigurationException, IOException, YamlException {
       StrokeConfiguration strokeConfiguration = new StrokeConfiguration();
       strokeConfiguration.setWidth(1.0f);
       strokeConfiguration.setDash(new float[] { 0.75f, 0.25f });
       GridStyleConfiguration gridStyleConfiguration = new GridStyleConfiguration();
       gridStyleConfiguration.setStroke(strokeConfiguration);
       gridStyleConfiguration.setColor("777777");
       ChartConfiguration chartConfiguration = new ChartConfiguration();
       chartConfiguration.setGridStyle(gridStyleConfiguration);
       chartConfiguration.setGridMode(ChartRenderer.GridMode.NONE);
       GridConfiguration xGrid = new GridConfiguration();
       xGrid.setType("Integer");
       chartConfiguration.setXGrid(xGrid);
       AxisConfiguration axis = new AxisConfiguration();
       axis.setPosition(ChartRenderer.AxisPosition.ORIGIN);
       axis.setTitle("t");
       axis.setUnit("s");
       ArrayList<AxisConfiguration> xAxes = new ArrayList<>();
       xAxes.add(axis);
       chartConfiguration.setXAxes(xAxes);
       chartConfiguration.setYAxes(Collections.emptyList());
       PointConfiguration legendPosition = new PointConfiguration();
       legendPosition.setX(1);
       legendPosition.setY(23);
       chartConfiguration.setLegendPosition(legendPosition);
       Map<String, DataRendererConfiguration> dataRendererConfigurations = new HashMap<>();
       dataRendererConfigurations.put("bar-graph", new DataRendererConfiguration());
       dataRendererConfigurations.get("bar-graph").setType("bar");
       chartConfiguration.setGraphs(dataRendererConfigurations);
//       ChartRendererBuilder builder = new ChartRendererBuilder(new Figures(), Collections.emptyMap());
       YamlWriter writer = new YamlWriter(new FileWriter("test/resources/renderer.yml"));
       writer.getConfig().setClassTag("Chart", ChartConfiguration.class);
       writer.getConfig().setClassTag("Axis", AxisConfiguration.class);
       writer.write(chartConfiguration);
       writer.close();
//       YamlReader reader = new YamlReader(new FileReader("test/resources/grid.json"));
//       reader.getConfig().setClassTag("Chart", ChartConfiguration.class);
//       reader.getConfig().setClassTag("Grid", GridConfiguration.class);
//       ChartConfiguration chartConfiguration2 = reader.read(ChartConfiguration.class);
//       reader.close();
//       ChartRenderer renderer = builder.buildChartRenderer(chartConfiguration2);
//       assertEquals(ChartRenderer.GridMode.NONE, renderer.getGridMode());
   }

}