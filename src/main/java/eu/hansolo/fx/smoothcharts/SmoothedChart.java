/*
 * Copyright (c) 2017 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.fx.smoothcharts;

import com.sun.javafx.charts.Legend;
import com.sun.javafx.charts.Legend.LegendItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.IntegerPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.Axis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.util.Duration;


/**
 * User: hansolo
 * Date: 03.11.17
 * Time: 04:50
 */
public class SmoothedChart<X, Y> extends AreaChart<X, Y> {

    public static final Background TRANSPARENT_BACKGROUND = new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));
    private static final int MAX_SUBDIVISIONS = 64;
    private static final int MAX_DECIMALS = 10;
    private boolean _smoothed;
    private BooleanProperty smoothed;
    private ChartType _chartType;
    private ObjectProperty<ChartType> chartType;
    private int _subDivisions;
    private IntegerProperty subDivisions;
    private boolean _snapToTicks;
    private BooleanProperty snapToTicks;
    private boolean _symbolsVisible;
    private BooleanProperty symbolsVisible;
    private Color _selectorFillColor;
    private ObjectProperty<Color> selectorFillColor;
    private Color _selectorStrokeColor;
    private ObjectProperty<Color> selectorStrokeColor;
    private double _selectorSize;
    private DoubleProperty selectorSize;
    private int _decimals;
    private IntegerProperty decimals;
    private String formatString;
    private Circle selector;
    private Tooltip selectorTooltip;
    private Region chartPlotBackground;
    private PauseTransition timeBeforeFadeOut;
    private SequentialTransition fadeInFadeOut;
    private List<Path> strokePaths;
    private boolean _interactive;
    private BooleanProperty interactive;
    private double _tooltipTimeout;
    private DoubleProperty tooltipTimeout;
    private Path horizontalGridLines;
    private Path verticalGridLines;
    private Line horizontalZeroLine;
    private Line verticalZeroLine;
    private EventHandler<MouseEvent> clickHandler;
    private EventHandler<ActionEvent> endOfTransformationHandler;
    private ListChangeListener<Series<X, Y>> seriesListener;
    // ******************** Constructors **************************************
    public SmoothedChart(final Axis<X> xAxis, final Axis<Y> yAxis) {
        super(xAxis, yAxis);
        init();
        registerListeners();
    }


    public SmoothedChart(final Axis<X> xAxis, final Axis<Y> yAxis, final ObservableList<Series<X, Y>> data) {
        super(xAxis, yAxis, data);
        init();
        registerListeners();
    }

    // ******************** Initialization ************************************
    private void init() {
        getStyleClass().add("smoothed-chart");

        _smoothed = true;
        _chartType = ChartType.LINE;
        _subDivisions = 16;
        _snapToTicks = false;
        _selectorFillColor = Color.WHITE;
        _selectorStrokeColor = Color.RED;
        _selectorSize = 10;
        _decimals = 2;
        _interactive = false;
        _tooltipTimeout = 2000;
        formatString = "%.2f";
        strokePaths = new ArrayList<>();
        clickHandler = e -> select(e);
        endOfTransformationHandler = e -> selectorTooltip.hide();
        seriesListener = change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(addedItem -> {
                        final Series<X, Y> series = addedItem;
                        final Path strokePath = (Path) ((Group) series.getNode()).getChildren().get(1);
                        final Path fillPath = (Path) ((Group) series.getNode()).getChildren().get(0);
                        fillPath.addEventHandler(MouseEvent.MOUSE_PRESSED, clickHandler);
                        strokePath.addEventHandler(MouseEvent.MOUSE_PRESSED, clickHandler);
                        strokePaths.add(strokePath);
                    });
                } else if (change.wasRemoved()) {
                    change.getRemoved().forEach(removedItem -> {
                        final Series<X, Y> series = removedItem;
                        final Path strokePath = (Path) ((Group) series.getNode()).getChildren().get(1);
                        final Path fillPath = (Path) ((Group) series.getNode()).getChildren().get(0);
                        fillPath.removeEventHandler(MouseEvent.MOUSE_PRESSED, clickHandler);
                        strokePath.removeEventHandler(MouseEvent.MOUSE_PRESSED, clickHandler);
                        strokePaths.remove(strokePath);
                    });
                }
            }
        };

        // Add selector to chart
        selector = new Circle();
        selector.setFill(_selectorFillColor);
        selector.setStroke(_selectorStrokeColor);
        selector.setOpacity(0);

        selectorTooltip = new Tooltip("");
        Tooltip.install(selector, selectorTooltip);

        final FadeTransition fadeIn = new FadeTransition(Duration.millis(100), selector);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        timeBeforeFadeOut = new PauseTransition(Duration.millis(_tooltipTimeout));

        final FadeTransition fadeOut = new FadeTransition(Duration.millis(100), selector);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        fadeInFadeOut = new SequentialTransition(fadeIn, timeBeforeFadeOut, fadeOut);
        fadeInFadeOut.setOnFinished(endOfTransformationHandler);

        chartPlotBackground = getChartPlotBackground();
        chartPlotBackground.widthProperty().addListener(o -> resizeSelector());
        chartPlotBackground.heightProperty().addListener(o -> resizeSelector());
        chartPlotBackground.layoutYProperty().addListener(o -> resizeSelector());

        final Path horizontalGridLines = getHorizontalGridLines();
        if (horizontalGridLines != null) {
            horizontalGridLines.setMouseTransparent(true);
        }

        final Path verticalGridLines = getVerticalGridLines();
        if (verticalGridLines != null) {
            verticalGridLines.setMouseTransparent(true);
        }

        getChartChildren().addAll(selector);
    }

    private void registerListeners() {
        getData().addListener(seriesListener);
    }

    // ******************** Public Methods ************************************
    public boolean isSmoothed() {
        return smoothed == null ? _smoothed : smoothed.get();
    }

    public void setSmoothed(final boolean SMOOTHED) {
        if (smoothed == null) {
            _smoothed = SMOOTHED;
            layoutPlotChildren();
        } else {
            smoothed.set(SMOOTHED);
        }
    }

    public BooleanProperty smoothedProperty() {
        if (smoothed == null) {
            smoothed = new BooleanPropertyBase(_smoothed) {
                @Override
                protected void invalidated() {
                    layoutPlotChildren();
                }

                @Override
                public Object getBean() {
                    return SmoothedChart.this;
                }

                @Override
                public String getName() {
                    return "smoothed";
                }
            };
        }
        return smoothed;
    }

    public ChartType getChartType() {
        return chartType == null ? _chartType : chartType.get();
    }

    public void setChartType(final ChartType TYPE) {
        if (chartType == null) {
            _chartType = TYPE;
            layoutPlotChildren();
        } else {
            chartType.set(TYPE);
        }
    }

    public ObjectProperty<ChartType> chartTypeProperty() {
        if (chartType == null) {
            chartType = new ObjectPropertyBase<ChartType>(_chartType) {
                @Override
                protected void invalidated() {
                    layoutPlotChildren();
                }

                @Override
                public Object getBean() {
                    return SmoothedChart.this;
                }

                @Override
                public String getName() {
                    return "chartType";
                }
            };
            _chartType = null;
        }
        return chartType;
    }

    public int getSubDivisions() {
        return subDivisions == null ? _subDivisions : subDivisions.get();
    }

    public void setSubDivisions(final int SUB_DIVISIONS) {
        if (subDivisions == null) {
            _subDivisions = Helper.clamp(1, MAX_SUBDIVISIONS, SUB_DIVISIONS);
            layoutPlotChildren();
        } else {
            subDivisions.set(SUB_DIVISIONS);
        }
    }

    public IntegerProperty subDivisionsProperty() {
        if (subDivisions == null) {
            subDivisions = new IntegerPropertyBase(_subDivisions) {
                @Override
                protected void invalidated() {
                    set(Helper.clamp(1, MAX_SUBDIVISIONS, get()));
                    layoutPlotChildren();
                }

                @Override
                public Object getBean() {
                    return SmoothedChart.this;
                }

                @Override
                public String getName() {
                    return "subDivisions";
                }
            };
        }
        return subDivisions;
    }

    public boolean isSnapToTicks() {
        return snapToTicks == null ? _snapToTicks : snapToTicks.get();
    }

    public void setSnapToTicks(final boolean SNAP) {
        if (snapToTicks == null) {
            _snapToTicks = SNAP;
        } else {
            snapToTicks.set(SNAP);
        }
    }

    public BooleanProperty snapToTicksProperty() {
        if (snapToTicks == null) {
            snapToTicks = new BooleanPropertyBase(_snapToTicks) {
                @Override
                protected void invalidated() {
                }

                @Override
                public Object getBean() {
                    return SmoothedChart.this;
                }

                @Override
                public String getName() {
                    return "snapToTicks";
                }
            };
        }
        return snapToTicks;
    }

    public boolean getSymbolsVisible() {
        return symbolsVisible == null ? _symbolsVisible : symbolsVisible.get();
    }

    public void setSymbolsVisible(final boolean VISIBLE) {
        if (symbolsVisible == null) {
            _symbolsVisible = VISIBLE;
            getData().forEach(series -> setSymbolsVisible(series, _symbolsVisible));
        } else {
            symbolsVisible.set(VISIBLE);
        }
    }

    public BooleanProperty symbolsVisibleProperty() {
        if (symbolsVisible == null) {
            symbolsVisible = new BooleanPropertyBase(_symbolsVisible) {
                @Override
                protected void invalidated() {
                    getData().forEach(series -> setSymbolsVisible(series, _symbolsVisible));
                }

                @Override
                public Object getBean() {
                    return SmoothedChart.this;
                }

                @Override
                public String getName() {
                    return "symbolsVisible";
                }
            };
        }
        return symbolsVisible;
    }

    public Color getSelectorFillColor() {
        return selectorFillColor == null ? _selectorFillColor : selectorFillColor.get();
    }

    public void setSelectorFillColor(final Color COLOR) {
        if (selectorFillColor == null) {
            _selectorFillColor = COLOR;
            selector.setFill(_selectorFillColor);
            layoutPlotChildren();
        } else {
            selectorFillColor.set(COLOR);
        }
    }

    public ObjectProperty<Color> selectorFillColorProperty() {
        if (selectorFillColor == null) {
            selectorFillColor = new ObjectPropertyBase<Color>(_selectorFillColor) {
                @Override
                protected void invalidated() {
                    selector.setFill(get());
                    layoutPlotChildren();
                }

                @Override
                public Object getBean() {
                    return SmoothedChart.this;
                }

                @Override
                public String getName() {
                    return "selectorFillColor";
                }
            };
            _selectorFillColor = null;
        }
        return selectorFillColor;
    }

    public Color getSelectorStrokeColor() {
        return selectorStrokeColor == null ? _selectorStrokeColor : selectorStrokeColor.get();
    }

    public void setSelectorStrokeColor(final Color COLOR) {
        if (selectorStrokeColor == null) {
            _selectorStrokeColor = COLOR;
            selector.setStroke(_selectorStrokeColor);
            layoutPlotChildren();
        } else {
            selectorStrokeColor.set(COLOR);
        }
    }

    public ObjectProperty<Color> selectorStrokeColorProperty() {
        if (selectorStrokeColor == null) {
            selectorStrokeColor = new ObjectPropertyBase<Color>(_selectorStrokeColor) {
                @Override
                protected void invalidated() {
                    selector.setStroke(get());
                    layoutPlotChildren();
                }

                @Override
                public Object getBean() {
                    return SmoothedChart.this;
                }

                @Override
                public String getName() {
                    return "selectorStrokeColor";
                }
            };
            _selectorStrokeColor = null;
        }
        return selectorStrokeColor;
    }

    public double getSelectorSize() {
        return selectorSize == null ? _selectorSize : selectorSize.get();
    }

    public void setSelectorSize(final double SIZE) {
        if (selectorSize == null) {
            _selectorSize = Helper.clamp(1, 20, SIZE);
        } else {
            selectorSize.set(SIZE);
        }
    }

    public DoubleProperty selectorSizeProperty() {
        if (selectorSize == null) {
            selectorSize = new DoublePropertyBase(_selectorSize) {
                @Override
                protected void invalidated() {
                    set(Helper.clamp(1, 20, get()));
                }

                @Override
                public Object getBean() {
                    return SmoothedChart.this;
                }

                @Override
                public String getName() {
                    return "selectorSize";
                }
            };
        }
        return selectorSize;
    }

    public int getDecimals() {
        return decimals == null ? _decimals : decimals.get();
    }

    public void setDecimals(final int DECIMALS) {
        if (decimals == null) {
            _decimals = Helper.clamp(0, MAX_DECIMALS, DECIMALS);
            formatString = "%." + _decimals + "f";
        } else {
            decimals.set(DECIMALS);
        }
    }

    public IntegerProperty decimalsProperty() {
        if (decimals == null) {
            decimals = new IntegerPropertyBase(_decimals) {
                @Override
                protected void invalidated() {
                    set(Helper.clamp(0, MAX_DECIMALS, get()));
                    formatString = "%." + _decimals + "f";
                }

                @Override
                public Object getBean() {
                    return SmoothedChart.this;
                }

                @Override
                public String getName() {
                    return "decimals";
                }
            };
        }
        return decimals;
    }

    public boolean isInteractive() {
        return interactive == null ? _interactive : interactive.get();
    }

    public void setInteractive(final boolean INTERACTIVE) {
        if (interactive == null) {
            _interactive = INTERACTIVE;
        } else {
            interactive.set(INTERACTIVE);
        }
    }

    public BooleanProperty interactiveProperty() {
        if (interactive == null) {
            interactive = new BooleanPropertyBase(_interactive) {
                @Override
                public Object getBean() {
                    return SmoothedChart.this;
                }

                @Override
                public String getName() {
                    return "interactive";
                }
            };
        }
        return interactive;
    }

    public double getTooltipTimeout() {
        return tooltipTimeout == null ? _tooltipTimeout : tooltipTimeout.get();
    }

    public void setTooltipTimeout(final double TIMEOUT) {
        if (tooltipTimeout == null) {
            _tooltipTimeout = Helper.clamp(0, 10000, TIMEOUT);
            timeBeforeFadeOut.setDuration(Duration.millis(_tooltipTimeout));
        } else {
            tooltipTimeout.set(TIMEOUT);
        }
    }

    public DoubleProperty tooltipTimeoutProperty() {
        if (tooltipTimeout == null) {
            tooltipTimeout = new DoublePropertyBase(_tooltipTimeout) {
                @Override
                protected void invalidated() {
                    set(Helper.clamp(0, 10000, get()));
                    timeBeforeFadeOut.setDuration(Duration.millis(get()));
                }

                @Override
                public Object getBean() {
                    return SmoothedChart.this;
                }

                @Override
                public String getName() {
                    return "tootipTimeout";
                }
            };
        }
        return tooltipTimeout;
    }

    public void setSymbolsVisible(final XYChart.Series<X, Y> SERIES, final boolean VISIBLE) {
        if (!getData().contains(SERIES)) {
            return;
        }
        for (final XYChart.Data<X, Y> data : SERIES.getData()) {
            final StackPane stackPane = (StackPane) data.getNode();
            if (stackPane == null) {
                continue;
            }
            stackPane.setVisible(VISIBLE);
        }
    }

    public void setSeriesColor(final XYChart.Series<X, Y> SERIES, final Paint COLOR) {
        final Background symbolBackground = new Background(new BackgroundFill(COLOR, new CornerRadii(5), Insets.EMPTY), new BackgroundFill(Color.WHITE, new CornerRadii(5), new Insets(2)));
        setSeriesColor(SERIES, COLOR, COLOR, symbolBackground, COLOR);
    }

    public void setSeriesColor(final XYChart.Series<X, Y> SERIES, final Paint STROKE, final Paint FILL) {
        final Background symbolBackground = new Background(new BackgroundFill(STROKE, new CornerRadii(1024), Insets.EMPTY), new BackgroundFill(Color.WHITE, new CornerRadii(1024), new Insets(2)));
        setSeriesColor(SERIES, STROKE, FILL, symbolBackground, STROKE);
    }

    public void setSeriesColor(final XYChart.Series<X, Y> SERIES, final Paint STROKE, final Paint FILL, final Paint LEGEND_SYMBOL_FILL) {
        final Background symbolBackground = new Background(new BackgroundFill(STROKE, new CornerRadii(1024), Insets.EMPTY), new BackgroundFill(Color.WHITE, new CornerRadii(1024), new Insets(2)));
        setSeriesColor(SERIES, STROKE, FILL, symbolBackground, LEGEND_SYMBOL_FILL);
    }

    public void setSeriesColor(final XYChart.Series<X, Y> SERIES, final Paint STROKE, final Paint FILL, final Background SYMBOL_BACKGROUND) {
        setSeriesColor(SERIES, STROKE, FILL, SYMBOL_BACKGROUND, STROKE);
    }

    public void setSeriesColor(final XYChart.Series<X, Y> SERIES, final Paint STROKE, final Paint FILL, final BackgroundFill SYMBOL_STROKE, final BackgroundFill SYMBOL_Fill) {
        setSeriesColor(SERIES, STROKE, FILL, new Background(SYMBOL_STROKE, SYMBOL_Fill), STROKE);
    }

    public void setSeriesColor(final XYChart.Series<X, Y> SERIES, final Paint STROKE, final Paint FILL, final Background SYMBOL_BACKGROUND, final Paint LEGEND_SYMBOL_FILL) {
        if (getData().isEmpty()) {
            return;
        }
        if (!getData().contains(SERIES)) {
            return;
        }
        if (FILL != null) {
            ((Path) ((Group) SERIES.getNode()).getChildren().get(0)).setFill(FILL);
        }
        if (STROKE != null) {
            ((Path) ((Group) SERIES.getNode()).getChildren().get(1)).setStroke(STROKE);
        }
        if (SYMBOL_BACKGROUND != null) {
            setSymbolFill(SERIES, SYMBOL_BACKGROUND);
        }
        if (LEGEND_SYMBOL_FILL != null) {
            setLegendSymbolFill(SERIES, LEGEND_SYMBOL_FILL);
        }
    }

    public Dimension2D getSymbolSize(final Series<X, Y> SERIES) {
        if (!getData().contains(SERIES)) {
            return new Dimension2D(0, 0);
        }
        if (SERIES.getData().isEmpty()) {
            return new Dimension2D(0, 0);
        }
        for (final XYChart.Data<X, Y> data : SERIES.getData()) {
            final StackPane stackPane = (StackPane) data.getNode();
            if (stackPane == null) {
                continue;
            } else {
                return new Dimension2D(stackPane.getLayoutBounds().getWidth(), stackPane.getLayoutBounds().getHeight());
            }
        }
        return new Dimension2D(0, 0);
    }

    public void setSymbolSize(final Series<X, Y> SERIES, final double SIZE) {
        if (!getData().contains(SERIES)) {
            return;
        }
        if (SERIES.getData().isEmpty()) {
            return;
        }
        final double symbolSize = Helper.clamp(0, 30, SIZE);
        for (final XYChart.Data<X, Y> data : SERIES.getData()) {
            final StackPane stackPane = (StackPane) data.getNode();
            if (stackPane == null) {
                continue;
            }
            stackPane.setPrefSize(symbolSize, symbolSize);
        }
    }

    public void setSymbolFill(final Series<X, Y> SERIES, final Background SYMBOL_BACKGROUND) {
        if (!getData().contains(SERIES)) {
            return;
        }
        for (final XYChart.Data<X, Y> data : SERIES.getData()) {
            final StackPane stackPane = (StackPane) data.getNode();
            if (stackPane == null) {
                continue;
            }
            stackPane.setBackground(SYMBOL_BACKGROUND);
        }
    }

    public Region getChartPlotBackground() {
        if (chartPlotBackground == null) {
            for (final Node node : lookupAll(".chart-plot-background")) {
                if (node instanceof Region) {
                    chartPlotBackground = (Region) node;
                    break;
                }
            }
        }
        return chartPlotBackground;
    }

    public void setChartPlotBackground(final Paint FILL) {
        setChartPlotBackground(new Background(new BackgroundFill(FILL, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    public void setChartPlotBackground(final Background BACKGROUND) {
        getChartPlotBackground().setBackground(BACKGROUND);
    }

    public Path getHorizontalGridLines() {
        if (horizontalGridLines == null) {
            for (final Node node : lookupAll(".chart-horizontal-grid-lines")) {
                if (node instanceof Path) {
                    horizontalGridLines = (Path) node;
                    break;
                }
            }
        }
        return horizontalGridLines;
    }

    public Path getVerticalGridLines() {
        if (verticalGridLines == null) {
            for (final Node node : lookupAll(".chart-vertical-grid-lines")) {
                if (node instanceof Path) {
                    verticalGridLines = (Path) node;
                    break;
                }
            }
        }
        return verticalGridLines;
    }

    public Line getHorizontalZeroLine() {
        if (horizontalZeroLine == null) {
            for (final Node node : lookupAll(".chart-horizontal-zero-line")) {
                if (node instanceof Line) {
                    horizontalZeroLine = (Line) node;
                    break;
                }
            }
        }
        return horizontalZeroLine;
    }

    public Line getVerticalZeroLine() {
        if (verticalZeroLine == null) {
            for (final Node node : lookupAll(".chart-vertical-zero-line")) {
                if (node instanceof Line) {
                    verticalZeroLine = (Line) node;
                    break;
                }
            }
        }
        return verticalZeroLine;
    }

    public Group getChartPlotContent() {
        for (final Node node : lookupAll(".plot-content")) {
            if (node instanceof Group) {
                return ((Group) node);
            }
        }
        return null;
    }

    public void setLegendBackground(final Paint FILL) {
        setLegendBackground(new Background(new BackgroundFill(FILL, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    public void setLegendBackground(final Background BACKGROUND) {
        final Legend legend = (Legend) getLegend();
        if (legend == null) {
            return;
        }
        legend.setBackground(BACKGROUND);
    }

    public void setLegendTextFill(final Paint FILL) {
        getData().forEach(series -> setLegendTextFill(series, FILL));
    }

    public void setLegendTextFill(final Series<X, Y> SERIES, final Paint FILL) {
        if (getData().isEmpty()) {
            return;
        }
        if (!getData().contains(SERIES)) {
            return;
        }

        final int seriesIndex = getData().indexOf(SERIES);
        if (seriesIndex == -1) {
            return;
        }

        final Legend legend = (Legend) getLegend();
        if (legend == null) {
            return;
        }

        final LegendItem item = legend.getItems().get(seriesIndex);
        if (item == null) {
            return;
        }

        final String itemText = item.getText();
        for (final Node node : legend.lookupAll(".chart-legend-item")) {
            if (node instanceof final Label label) {
                if (label.getText().equals(itemText)) {
                    label.setTextFill(FILL);
                }
            }
        }
    }

    public void setLegendSymbolFill(final Series<X, Y> SERIES, final Paint LEGEND_SYMBOL_FILL) {
        if (getData().isEmpty()) {
            return;
        }
        if (!getData().contains(SERIES)) {
            return;
        }

        final int seriesIndex = getData().indexOf(SERIES);
        if (seriesIndex == -1) {
            return;
        }

        final Legend legend = (Legend) getLegend();
        if (legend == null) {
            return;
        }

        final LegendItem item = legend.getItems().get(seriesIndex);
        if (item == null) {
            return;
        }

        final Region symbol = (Region) item.getSymbol();
        if (symbol == null) {
            return;
        }

        symbol.setBackground(new Background(new BackgroundFill(LEGEND_SYMBOL_FILL, new CornerRadii(6), Insets.EMPTY)));
    }

    public void setXAxisTickMarkFill(final Paint FILL) {
        for (final Node node : getXAxis().lookupAll(".axis-tick-mark")) {
            if (node instanceof Path) {
                ((Path) node).setStroke(FILL);
                break;
            }
        }
    }

    public void setYAxisTickMarkFill(final Paint FILL) {
        for (final Node node : getYAxis().lookupAll(".axis-tick-mark")) {
            if (node instanceof Path) {
                ((Path) node).setStroke(FILL);
                break;
            }
        }
        for (final Node node : getYAxis().lookupAll(".axis-minor-tick-mark")) {
            if (node instanceof Path) {
                ((Path) node).setStroke(FILL);
                break;
            }
        }
    }

    public void setAxisTickMarkFill(final Paint FILL) {
        setXAxisTickMarkFill(FILL);
        setYAxisTickMarkFill(FILL);
    }

    public void setXAxisTickLabelFill(final Paint FILL) {
        getXAxis().setTickLabelFill(FILL);
    }

    public void setYAxisTickLabelFill(final Paint FILL) {
        getYAxis().setTickLabelFill(FILL);
    }

    public void setTickLabelFill(final Paint FILL) {
        setXAxisTickLabelFill(FILL);
        setYAxisTickLabelFill(FILL);
    }

    public void setXAxisBorderColor(final Paint FILL) {
        if (getXAxis().getSide() == Side.BOTTOM) {
            getXAxis().setBorder(new Border(
              new BorderStroke(FILL, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                               BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.DEFAULT_WIDTHS, Insets.EMPTY
              )));
        } else {
            getXAxis().setBorder(new Border(
              new BorderStroke(Color.TRANSPARENT, Color.TRANSPARENT, FILL, Color.TRANSPARENT, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                               BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.DEFAULT_WIDTHS, Insets.EMPTY
              )));
        }
    }

    public void setYAxisBorderColor(final Paint FILL) {
        if (getYAxis().getSide() == Side.LEFT) {
            getYAxis().setBorder(new Border(
              new BorderStroke(Color.TRANSPARENT, FILL, Color.TRANSPARENT, Color.TRANSPARENT, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                               BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.DEFAULT_WIDTHS, Insets.EMPTY
              )));
        } else {
            getYAxis().setBorder(new Border(
              new BorderStroke(Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, FILL, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                               BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.DEFAULT_WIDTHS, Insets.EMPTY
              )));
        }
    }

    public Path getFillPath(final Series<X, Y> SERIES) {
        return getPaths(SERIES)[0];
    }

    public Path getStrokePath(final Series<X, Y> SERIES) {
        return getPaths(SERIES)[1];
    }

    public List<StackPane> getSymbols(final Series<X, Y> SERIES) {
        return SERIES.getData().stream().map(node -> (StackPane) node.getNode()).collect(Collectors.toList());
    }

    public void dispose() {
        getData().removeListener(seriesListener);
    }

    // ******************** Internal Methods **********************************
    @Override
    public String getUserAgentStylesheet() {
        return SmoothedChart.class.getResource("smoothed-chart.css").toExternalForm();
    }

    @Override
    protected void layoutPlotChildren() {
        super.layoutPlotChildren();

        final double height = getLayoutBounds().getHeight();
        getData().forEach(series -> {
            final Path[] paths = getPaths(series);
            if (paths == null) {
                return;
            }
            if (isSmoothed()) {
                smooth(paths[1].getElements(), paths[0].getElements(), height);
            }
            paths[0].setVisible(getChartType() == ChartType.AREA);
            paths[0].setManaged(getChartType() == ChartType.AREA);
        });
    }

    /**
     * Returns an array of paths where the first entry represents the fill path
     * and the second entry represents the stroke path
     *
     * @return an array of paths where [0] == FillPath and [1] == StrokePath
     */
    private Path[] getPaths(final Series<X, Y> SERIES) {
        if (!getData().contains(SERIES)) {
            return null;
        }

        final Node seriesNode = SERIES.getNode();
        if (seriesNode == null) {
            return null;
        }

        final Group seriesGroup = (Group) seriesNode;
        if (seriesGroup.getChildren().isEmpty() || seriesGroup.getChildren().size() < 2) {
            return null;
        }

        return new Path[] { /* FillPath   */ (Path) (seriesGroup).getChildren().get(0),
                            /* StrokePath */ (Path) (seriesGroup).getChildren().get(1)};
    }

    private void resizeSelector() {
        selectorTooltip.hide();
        selector.setVisible(false);
        selector.setRadius(getSelectorSize() * 0.5);
        selector.setStrokeWidth(getSelectorSize() * 0.25);
    }

    private void select(final MouseEvent EVT) {
        if (!isInteractive()) {
            return;
        }

        final double EVENT_X = EVT.getX();
        final double EVENT_Y = EVT.getY();
        final double CHART_X = chartPlotBackground.getBoundsInParent().getMinX();
        final double CHART_MIN_Y = chartPlotBackground.getBoundsInParent().getMinY();
        final double CHART_HEIGHT = chartPlotBackground.getBoundsInParent().getHeight();

        if (!(getYAxis() instanceof NumberAxis)) {
            return;
        }

        final double upperBound = ((NumberAxis) getYAxis()).getUpperBound();
        final double lowerBound = ((NumberAxis) getYAxis()).getLowerBound();
        final double range = upperBound - lowerBound;
        final double factor = range / getYAxis().getLayoutBounds().getHeight();
        List<PathElement> elements = null;
        int noOfElements = 0;
        Bounds pathBounds = null;
        double pathMinX = 0;
        double pathWidth = 0;
        PathElement lastElement = null;

        Series<X, Y> series = null;
        for (final Series<X, Y> s : getData()) {
            final Path[] paths = getPaths(s);
            final int type = getChartType().ordinal(); // AREA == 0, LINE == 1 in ChartType enum
            if (paths[type].contains(EVENT_X, EVENT_Y)) {
                series = s;
                elements = paths[type].getElements();
                noOfElements = elements.size();
                lastElement = elements.get(0);
                pathBounds = paths[1].getLayoutBounds();
                pathMinX = pathBounds.getMinX();
                pathWidth = pathBounds.getWidth();
                break;
            }
        }

        if (series == null || series.getData().isEmpty()) {
            return;
        }

        if (isSnapToTicks()) {
            final double reverseFactor = CHART_HEIGHT / range;
            final int noOfDataElements = series.getData().size();
            final double interval = pathWidth / (double) (noOfDataElements - 1);
            final int selectedIndex = Helper.roundDoubleToInt((EVENT_X - pathMinX) / interval);
            final Data<X, Y> selectedData = series.getData().get(selectedIndex);
            final Y selectedYValue = selectedData.getYValue();

            if (!(selectedYValue instanceof Number)) {
                return;
            }
            final double selectedValue = ((Number) selectedYValue).doubleValue();

            selector.setCenterX(pathMinX + CHART_X + interval * selectedIndex);
            selector.setCenterY((CHART_MIN_Y + CHART_HEIGHT) - (selectedValue * reverseFactor));
            selector.setVisible(true);
            fadeInFadeOut.playFrom(Duration.millis(0));

            final Point2D tooltipLocation = selector.localToScreen(selector.getCenterX(), selector.getCenterY());
            final String tooltipText = selectedData.getXValue().toString() + "\n" + selectedData.getYValue();
            selectorTooltip.setText(tooltipText);
            selectorTooltip.setX(tooltipLocation.getX());
            selectorTooltip.setY(tooltipLocation.getY());
            selectorTooltip.show(getScene().getWindow());

            fireEvent(new SmoothedChartEvent(this, null, SmoothedChartEvent.DATA_SELECTED, selectedValue));
        } else {
            for (int i = 1; i < noOfElements; i++) {
                final PathElement element = elements.get(i);

                final double[] xy = getXYFromPathElement(lastElement);
                final double[] xy1 = getXYFromPathElement(element);
                if (xy[0] < 0 || xy[1] < 0 || xy1[0] < 0 || xy1[1] < 0) {
                    continue;
                }

                if (EVENT_X > xy[0] && EVENT_X < xy1[0]) {
                    final double deltaX = xy1[0] - xy[0];
                    final double deltaY = xy1[1] - xy[1];
                    final double m = deltaY / deltaX;
                    final double y = m * (EVT.getX() - xy[0]) + xy[1];
                    final double selectedValue = ((getYAxis().getLayoutBounds().getHeight() - y) * factor + lowerBound);

                    selector.setCenterX(CHART_X + EVT.getX());
                    selector.setCenterY(CHART_MIN_Y + y);
                    selector.setVisible(true);
                    fadeInFadeOut.playFrom(Duration.millis(0));

                    final Point2D tooltipLocation = selector.localToScreen(selector.getCenterX(), selector.getCenterY());
                    final String tooltipText = String.format(Locale.US, formatString, selectedValue);
                    selectorTooltip.setText(tooltipText);
                    selectorTooltip.setX(tooltipLocation.getX());
                    selectorTooltip.setY(tooltipLocation.getY());
                    selectorTooltip.show(getScene().getWindow());

                    fireEvent(new SmoothedChartEvent(this, null, SmoothedChartEvent.DATA_SELECTED, selectedValue));
                    break;
                }
                lastElement = element;
            }
        }
    }

    private void smooth(final ObservableList<PathElement> strokeElements, final ObservableList<PathElement> fillElements, final double HEIGHT) {
        if (fillElements.isEmpty()) {
            return;
        }
        // as we do not have direct access to the data, first recreate the list of all the data points we have
        final Point2D[] dataPoints = new Point2D[strokeElements.size()];
        for (int i = 0; i < strokeElements.size(); i++) {
            final PathElement element = strokeElements.get(i);
            if (element instanceof final MoveTo move) {
                dataPoints[i] = new Point2D(move.getX(), move.getY());
            } else if (element instanceof final LineTo line) {
                final double x = line.getX(), y = line.getY();
                dataPoints[i] = new Point2D(x, y);
            }
        }
        final double firstX = dataPoints[0].getX();
        final double lastX = dataPoints[dataPoints.length - 1].getX();

        final Point2D[] points = Helper.subdividePoints(dataPoints, getSubDivisions());

        fillElements.clear();
        fillElements.add(new MoveTo(firstX, HEIGHT));

        strokeElements.clear();
        strokeElements.add(new MoveTo(points[0].getX(), points[0].getY()));

        for (final Point2D p : points) {
            if (Double.compare(p.getX(), firstX) >= 0) {
                fillElements.add(new LineTo(p.getX(), p.getY()));
                strokeElements.add(new LineTo(p.getX(), p.getY()));
            }
        }

        fillElements.add(new LineTo(lastX, HEIGHT));
        fillElements.add(new LineTo(0, HEIGHT));
        fillElements.add(new ClosePath());
    }

    private double[] getXYFromPathElement(final PathElement ELEMENT) {
        if (ELEMENT instanceof MoveTo) {
            return new double[] {((MoveTo) ELEMENT).getX(), ((MoveTo) ELEMENT).getY()};
        } else if (ELEMENT instanceof LineTo) {
            return new double[] {((LineTo) ELEMENT).getX(), ((LineTo) ELEMENT).getY()};
        } else {
            return new double[] {-1, -1};
        }
    }

    public enum ChartType {AREA, LINE}
}
