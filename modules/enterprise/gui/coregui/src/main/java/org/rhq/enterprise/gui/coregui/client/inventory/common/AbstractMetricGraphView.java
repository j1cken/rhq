/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.coregui.client.inventory.common;

import java.util.Date;
import java.util.List;

import ca.nanometrics.gflot.client.Axis;
import ca.nanometrics.gflot.client.DataPoint;
import ca.nanometrics.gflot.client.PlotItem;
import ca.nanometrics.gflot.client.PlotModel;
import ca.nanometrics.gflot.client.PlotPosition;
import ca.nanometrics.gflot.client.SeriesHandler;
import ca.nanometrics.gflot.client.SimplePlot;
import ca.nanometrics.gflot.client.event.PlotHoverListener;
import ca.nanometrics.gflot.client.jsni.Plot;
import ca.nanometrics.gflot.client.options.AxisOptions;
import ca.nanometrics.gflot.client.options.GridOptions;
import ca.nanometrics.gflot.client.options.LineSeriesOptions;
import ca.nanometrics.gflot.client.options.PlotOptions;
import ca.nanometrics.gflot.client.options.PointsSeriesOptions;
import ca.nanometrics.gflot.client.options.TickFormatter;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.smartgwt.client.types.AnimationEffect;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.MouseOutEvent;
import com.smartgwt.client.widgets.events.MouseOutHandler;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementConverterClient;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableImg;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWidgetCanvas;

/**
 * @author Greg Hinkle
 * @author Jay Shaughnessy
 */
public abstract class AbstractMetricGraphView extends LocatableVLayout {

    private static final String INSTRUCTIONS = MSG.view_resource_monitor_graph_instructions();

    /*
    private static final String[] MONTH_NAMES = { MSG.common_calendar_january_short(),
        MSG.common_calendar_february_short(), MSG.common_calendar_march_short(), MSG.common_calendar_april_short(),
        MSG.common_calendar_may_short(), MSG.common_calendar_june_short(), MSG.common_calendar_july_short(),
        MSG.common_calendar_august_short(), MSG.common_calendar_september_short(), MSG.common_calendar_october_short(),
        MSG.common_calendar_november_short(), MSG.common_calendar_december_short() };
        */

    private final Label selectedPointLabel = new Label(INSTRUCTIONS);
    private final Label positionLabel = new Label();
    private final Label hoverLabel = new Label();

    private int entityId;
    private int definitionId;

    private MeasurementDefinition definition;
    private List<MeasurementDataNumericHighLowComposite> data;

    public AbstractMetricGraphView(String locatorId) {
        super(locatorId);
    }

    public AbstractMetricGraphView(String locatorId, int entityId, int definitionId) {
        this(locatorId);

        this.entityId = entityId;
        this.definitionId = definitionId;
    }

    public AbstractMetricGraphView(String locatorId, int entityId, MeasurementDefinition def,
        List<MeasurementDataNumericHighLowComposite> data) {
        this(locatorId);

        this.entityId = entityId;
        this.definition = def;
        this.data = data;
        setHeight100();
        setWidth100();
    }

    public abstract AbstractMetricGraphView getInstance(String locatorId, int entityId, MeasurementDefinition def,
        List<MeasurementDataNumericHighLowComposite> data);

    protected abstract void renderGraph();

    protected abstract HTMLFlow getEntityTitle();

    public int getEntityId() {
        return this.entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
        this.definition = null;
    }

    public int getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(int definitionId) {
        this.definitionId = definitionId;
        this.definition = null;
    }

    public MeasurementDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(MeasurementDefinition definition) {
        this.definition = definition;
    }

    public List<MeasurementDataNumericHighLowComposite> getData() {
        return data;
    }

    public void setData(List<MeasurementDataNumericHighLowComposite> data) {
        this.data = data;
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        removeMembers(getMembers());
        renderGraph();
    }

    @Override
    public void parentResized() {
        super.parentResized();
        removeMembers(getMembers());
        renderGraph();
    }

    protected void drawGraph() {

        HLayout titleHLayout = new LocatableHLayout(extendLocatorId("HTitle"));

        if (definition != null) {
            titleHLayout.setAutoHeight();
            titleHLayout.setWidth100();

            HTMLFlow entityTitle = getEntityTitle();
            if (null != entityTitle) {
                entityTitle.setWidth("*");
                titleHLayout.addMember(entityTitle);
            }

            if (supportsLiveGraphViewDialog()) {
                Img liveGraph = new LocatableImg(extendLocatorId("Live"), "subsystems/monitor/Monitor_16.png", 16, 16);
                liveGraph.setTooltip(MSG.view_resource_monitor_graph_live_tooltip());

                liveGraph.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent clickEvent) {
                        displayLiveGraphViewDialog();
                    }
                });
                titleHLayout.addMember(liveGraph);
            }

            addMember(titleHLayout);

            HTMLFlow title = new HTMLFlow("<b>" + definition.getDisplayName() + "</b> " + definition.getDescription());
            title.setWidth100();
            title.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    displayAsDialog(extendLocatorId("Dialog"));
                }
            });
            addMember(title);
        }

        PlotModel model = new PlotModel();
        PlotOptions plotOptions = new PlotOptions();
        plotOptions.setDefaultLineSeriesOptions(new LineSeriesOptions().setLineWidth(1).setShow(true));
        plotOptions.setDefaultPointsOptions(new PointsSeriesOptions().setRadius(2).setShow(true));
        plotOptions.setDefaultShadowSize(0);

        // You need make the grid hoverable <<<<<<<<<
        plotOptions
            .setGridOptions(new GridOptions().setHoverable(true).setMouseActiveRadius(10).setAutoHighlight(true));

        // create a series
        if (definition != null && data != null) {
            loadData(model, plotOptions);
        }

        // create the plot
        SimplePlot plot = new SimplePlot(model, plotOptions);
        plot.setSize(String.valueOf(getInnerContentWidth()),
            String.valueOf(getInnerContentHeight() - titleHLayout.getHeight() - 50));
        //                "80%","80%");

        // add hover listener
        plot.addHoverListener(new PlotHoverListener() {
            public void onPlotHover(Plot plot, PlotPosition position, PlotItem item) {
                if (position != null) {
                    positionLabel.setContents("position: (" + position.getX() + "," + position.getY() + ")");
                }
                if (item != null) {
                    hoverLabel.setContents(getHover(item));

                    hoverLabel.animateShow(AnimationEffect.FADE);
                    if (hoverLabel.getLeft() > 0 || hoverLabel.getTop() > 0) {
                        hoverLabel.animateMove(item.getPageX() + 10, item.getPageY() - 35);
                    } else {
                        hoverLabel.moveTo(item.getPageX() + 10, item.getPageY() - 35);
                    }
                    hoverLabel.redraw();

                    selectedPointLabel.setContents("x: " + item.getDataPoint().getX() + ", y: "
                        + item.getDataPoint().getY());
                } else {
                    hoverLabel.animateHide(AnimationEffect.FADE);
                    selectedPointLabel.setContents(INSTRUCTIONS);
                }
            }
        }, false);

        addMouseOutHandler(new MouseOutHandler() {
            public void onMouseOut(MouseOutEvent mouseOutEvent) {
                hoverLabel.animateHide(AnimationEffect.FADE);
            }
        });

        hoverLabel.setOpacity(80);
        hoverLabel.setWrap(false);
        hoverLabel.setHeight(25);
        hoverLabel.setBackgroundColor("yellow");
        hoverLabel.setBorder("1px solid orange");
        hoverLabel.hide();

        if (hoverLabel.isDrawn())
            hoverLabel.redraw();
        else
            hoverLabel.draw();

        // put it on a panel

        addMember(new LocatableWidgetCanvas(extendLocatorId("Plot"), plot));

        plot.setSize(String.valueOf(getInnerContentWidth()),
            String.valueOf(getInnerContentHeight() - titleHLayout.getHeight() - 50));

    }

    protected boolean supportsLiveGraphViewDialog() {
        return false;
    }

    protected void displayLiveGraphViewDialog() {
        return;
    }

    @Override
    public void destroy() {
        hoverLabel.destroy();
        super.destroy();
    }

    @Override
    public void hide() {
        super.hide();
        hoverLabel.hide();
    }

    protected String getHover(PlotItem item) {
        if (definition != null) {
            com.google.gwt.i18n.client.DateTimeFormat df = DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_MEDIUM);
            return definition.getDisplayName() + ": "
                + MeasurementConverterClient.format(item.getDataPoint().getY(), definition.getUnits(), true) + "<br/>"
                + df.format(new Date((long) item.getDataPoint().getX()));
        } else {
            return "x: " + item.getDataPoint().getX() + ", y: " + item.getDataPoint().getY();
        }
    }

    protected void loadData(PlotModel model, PlotOptions plotOptions) {
        SeriesHandler handler = model.addSeries(definition.getDisplayName(), "#007f00");

        for (MeasurementDataNumericHighLowComposite d : data) {
            handler.add(new DataPoint(d.getTimestamp(), d.getValue()));
        }

        plotOptions.setYAxisOptions(new AxisOptions().setTicks(5).setLabelWidth(70)
            .setTickFormatter(new TickFormatter() {
                public String formatTickValue(double v, Axis axis) {
                    return MeasurementConverterClient.format(v, definition.getUnits(), true);
                }
            }));

        long max = System.currentTimeMillis();
        long min = max - (1000L * 60 * 60 * 8);

        int xTicks = getDefaultWidth() / 140;

        plotOptions.setXAxisOptions(new AxisOptions().setTicks(xTicks).setMinimum(min).setMaximum(max)
            .setTickFormatter(new TickFormatter() {
                public String formatTickValue(double tickValue, Axis axis) {
                    com.google.gwt.i18n.client.DateTimeFormat dateFormat = DateTimeFormat
                        .getFormat(PredefinedFormat.DATE_TIME_SHORT);
                    return dateFormat.format(new Date((long) tickValue));
                    //                return String.valueOf(new Date((long) tickValue));
                    //                return MONTH_NAMES[(int) (tickValue - 1)];
                }
            }));

    }

    private void displayAsDialog(String locatorId) {
        AbstractMetricGraphView graph = getInstance(locatorId, entityId, definition, data);
        Window graphPopup = new Window();
        graphPopup.setTitle(MSG.view_resource_monitor_detailed_graph_label());
        graphPopup.setWidth(800);
        graphPopup.setHeight(400);
        graphPopup.setIsModal(true);
        graphPopup.setShowModalMask(true);
        graphPopup.setCanDragResize(true);
        graphPopup.centerInPage();
        graphPopup.addItem(graph);
        graphPopup.show();
    }

}
