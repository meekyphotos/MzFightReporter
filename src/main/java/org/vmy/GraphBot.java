package org.vmy;

import eu.hansolo.fx.smoothcharts.SmoothedChart;

import org.vmy.util.FightReport;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;

import static eu.hansolo.fx.smoothcharts.SmoothedChart.TRANSPARENT_BACKGROUND;

public class GraphBot extends Application {

    private static FightReport report;

    public GraphBot() {
    }

    public static void main(final String[] args) throws Exception {
        final String homeDir = args[1];
        org.vmy.Parameters.getInstance().homeDir = homeDir;

        report = FightReport.readReportFile();
        launch();
    }

    @Override
    public void start(final Stage stage) throws Exception {

        try {
            stage.setTitle("Line Chart Sample");

            //defining the axes
            final NumberAxis xAxis = new NumberAxis();
            final NumberAxis yAxis = new NumberAxis();
            xAxis.setLabel("Timeline (seconds)");
            yAxis.setLabel("Damage");

            //creating the chart
            final SmoothedChart<Number, Number> lineChart = new SmoothedChart<Number, Number>(xAxis, yAxis);
            //LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.setTitle("Squad Damage Output");
            lineChart.setCreateSymbols(false);
            //lineChart.setLegendVisible(false);
            //lineChart.lookup(".chart-plot-background").setStyle("-fx-background-color: DIMGRAY;");
            lineChart.lookup(".axis-label").setStyle("-fx-text-fill: DIMGRAY;");
            lineChart.lookup(".chart-title").setStyle("-fx-text-fill: DIMGRAY;");

            //lineChart.setStyle("-fx-text-fill: WHITE;")

            final Comparator sortingByName = new Comparator() {
                @Override
                public int compare(final Object o1, final Object o2) {
                    final Map.Entry<String, List<Integer>> p1 = (Map.Entry<String, List<Integer>>) o1;
                    final Map.Entry<String, List<Integer>> p2 = (Map.Entry<String, List<Integer>>) o2;
                    final Integer p1Last = p1.getValue().get(p1.getValue().size() - 1);
                    final Integer p2Last = p2.getValue().get(p2.getValue().size() - 1);
                    return -p1Last.compareTo(p2Last);
                }
            };

            Object[] objects = report.getDmgMap().entrySet().toArray();
            //Arrays.stream(objects).sorted(sortingByName).limit(10).forEach((s)->System.out.println(s));
            objects = Arrays.stream(objects).sorted(sortingByName).limit(org.vmy.Parameters.getInstance().graphPlayerLimit).toArray();

            //populating the series with data
            //Object[] names = report.getDmgMap().keySet().toArray();
            for (int i = 0; i < objects.length; i++) {
                final XYChart.Series<Number, Number> series = new XYChart.Series<>();
                final Map.Entry<String, List<Integer>> m = (Map.Entry<String, List<Integer>>) objects[i];
                final String name = m.getKey();
                series.setName(m.getKey());
                final Object[] ao = report.getDmgMap().get(name).toArray();
                int lastValue = 0;
                //int interval = (int)Math.round(ao.length * 0.025);
                //interval = interval == 0 ? 1 : interval;
                final int interval = 1;
                for (int j = 0; j < ao.length; j = j + interval) {
                    final int nextValue = (Integer) ao[j];
                    final int diff = nextValue - lastValue;
                    final double perInterval = diff / interval;
                    series.getData().add(new XYChart.Data(j, perInterval));
                    lastValue = nextValue;
                }
                lineChart.getData().add(series);
            }

            // Set the chart type (AREA or LINE);
            lineChart.setChartType(SmoothedChart.ChartType.AREA);

            // Tweak the chart background
            final RadialGradient gradient = new RadialGradient(0, 0, 0.5, 0.25, 0.5, true, CycleMethod.NO_CYCLE,
                                                               new Stop(0, Color.web("#313A48")),
                                                               new Stop(1, Color.web("#26262D"))
            );
            lineChart.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));

            // Tweak the chart plot background
            lineChart.getChartPlotBackground().setBackground(TRANSPARENT_BACKGROUND);
            //lineChart.set

            // Tweak the legend
            lineChart.setLegendBackground(TRANSPARENT_BACKGROUND);
            lineChart.setLegendTextFill(Color.DIMGRAY);

            // Tweak the axis
//            lineChart.setXAxisTickLabelFill(Color.web("#7A808D"));
//            lineChart.setYAxisTickLabelFill(Color.web("#7A808D"));
//            lineChart.setAxisTickMarkFill(Color.TRANSPARENT);
            lineChart.setXAxisBorderColor(Color.TRANSPARENT);
            lineChart.setYAxisBorderColor(Color.TRANSPARENT);

            // Tweak the grid lines
            lineChart.getHorizontalGridLines().setStroke(Color.TRANSPARENT);
            final LinearGradient verticalGridLineGradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                                                                               new Stop(0, Color.TRANSPARENT),
                                                                               new Stop(0.35, Color.TRANSPARENT),
                                                                               new Stop(1, Color.web("#7A808D"))
            );

            lineChart.getVerticalGridLines().setStroke(verticalGridLineGradient);
            lineChart.setHorizontalZeroLineVisible(false);
            lineChart.setSymbolsVisible(false);

            // Tweak series colors
//            lineChart.setSeriesColor(tweakedSeries1, new LinearGradient(0, 0, 1, 0,
//                            true, CycleMethod.NO_CYCLE,
//                            new Stop(0, Color.web("#54D1FF")),
//                            new Stop(1, Color.web("#016AED"))),
//                    Color.TRANSPARENT);
//            lineChart.setSeriesColor(tweakedSeries2, new LinearGradient(0, 0, 1, 0,
//                            true, CycleMethod.NO_CYCLE,
//                            new Stop(0, Color.web("#F9348A")),
//                            new Stop(1, Color.web("#EB123A"))),
//                    Color.TRANSPARENT);
//            lineChart.setSeriesColor(tweakedSeries3, new LinearGradient(0, 0, 1, 0,
//                            true, CycleMethod.NO_CYCLE,
//                            new Stop(0, Color.web("#7BFB00")),
//                            new Stop(1, Color.web("#FCE207"))),
//                    Color.TRANSPARENT);

            final Scene scene = new Scene(lineChart, 800, 600);

//            try {
//                File f = new File(org.vmy.Parameters.getInstance().homeDir + "dark-theme.css");
//                String ex = f.toURI().toURL().toExternalForm();
//                scene.getStylesheets().add(ex);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

            stage.setScene(scene);
            lineChart.setAnimated(false);

            saveAsPng(scene, org.vmy.Parameters.getInstance().homeDir + File.separator + "fightreport.png");
            //stage.show();

            //terminate
            Platform.exit();
            System.exit(0);
        } catch (final Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void saveAsPng(final Scene scene, final String path) {
        final WritableImage image = scene.snapshot(null);
        final File file = new File(path);
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        } catch (final IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
