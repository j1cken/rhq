/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.dashboard;

import java.util.Arrays;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.EventHandler;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.DragResizeStartEvent;
import com.smartgwt.client.widgets.events.DragResizeStartHandler;
import com.smartgwt.client.widgets.events.DropEvent;
import com.smartgwt.client.widgets.events.DropHandler;
import com.smartgwt.client.widgets.events.ResizedEvent;
import com.smartgwt.client.widgets.events.ResizedHandler;

import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Greg Hinkle
 * @author Jay Shaughnessy
 */
public class PortalLayout extends LocatableHLayout {

    private DashboardView dashboardView;
    private Integer dragResizeColumnCount;

    /**
     * @param locatorId
     * @param dashboardView
     * @param numColumns
     * @param columnWidths Currently only the first column width is set, others are ignored and share the remaining space
     * evenly. If null column 0 defaults to 30%.
     */
    public PortalLayout(String locatorId, DashboardView dashboardView, int numColumns, String[] columnWidths) {
        super(locatorId);

        if (numColumns < 1) {
            throw new IllegalArgumentException("Invalid number of columns [" + numColumns + "]");
        }
        if ((null != columnWidths && columnWidths.length > numColumns)) {
            throw new IllegalArgumentException("Invalid column widths (more widths than columns) "
                + Arrays.toString(columnWidths));
        }

        this.dashboardView = dashboardView;
        setMargin(4);
        setMembersMargin(4);

        for (int i = 0; i < numColumns; i++) {
            final PortalColumn column = new PortalColumn();
            if (null != columnWidths && i < columnWidths.length) {
                column.setWidth(columnWidths[i]);
            } else {
                column.setWidth("*");
            }

            final int columnNumber = i;
            column.addDropHandler(new DropHandler() {
                public void onDrop(DropEvent dropEvent) {

                    int dropPosition = column.getDropPosition();
                    int dropColumn = columnNumber;

                    final PortletWindow target = (PortletWindow) EventHandler.getDragTarget();
                    target.getStoredPortlet().setIndex(dropPosition);
                    target.getStoredPortlet().setColumn(dropColumn);

                    int colNum = 0;
                    for (Canvas pcc : getMembers()) {

                        PortalColumn pc = (PortalColumn) pcc;

                        int i = 0;
                        for (Canvas c : pc.getMembers()) {
                            if (colNum == dropColumn && i == dropPosition) {
                                i++;
                            }

                            if (c instanceof PortletWindow) {
                                DashboardPortlet dp = ((PortletWindow) c).getStoredPortlet();
                                dp.setIndex(i++);
                            }
                        }

                        colNum++;
                    }

                    // drop means the portlet location has changed. The selenium testing locators include positioning
                    // info. So, in this case we have to take the hit and completely rebuild the dash.
                    AsyncCallback<Dashboard> callback = SeleniumUtility.getUseDefaultIds() ? null
                        : new AsyncCallback<Dashboard>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                rebuild();
                            }

                            @Override
                            public void onSuccess(Dashboard result) {
                                // for some reason the drag drop operation is leaving the target widget (the
                                // portlet window) in the DOM, detached from its original parent (the PortalLayout),
                                // and therefore not destroyed in the redraw().  So, kill it off manually to
                                // avoid ID conflicts if the portlet is again dragged back its original position. 
                                target.removeFromParent();
                                target.destroy();

                                rebuild();
                            }
                        };
                    save(callback);

                    Log.info("Rearranged column indexes");
                }
            });

            column.addDragResizeStartHandler(new DragResizeStartHandler() {

                @Override
                public void onDragResizeStart(DragResizeStartEvent event) {

                    // When a drag resize starts activate a counter keeping track of the number of columns that
                    // have been resized.  A resize of one column will force a resize of all columns. After the
                    // last column resize completes, persist the new column widths to the database.
                    dragResizeColumnCount = 0;
                }
            });

            // This handler is called when the resizing is complete (the DragResizeStopHandler is called
            // immediately on stop but before all resizing is complete.)
            column.addResizedHandler(new ResizedHandler() {

                @Override
                public void onResized(ResizedEvent event) {

                    // ignore resizing not related to drag resize (presumable initial draw)
                    if (null == dragResizeColumnCount) {
                        return;
                    }

                    ++dragResizeColumnCount;
                    Canvas[] members = getMembers();

                    // ignore resizing prior to all columns being resized as a result of the drag operation
                    if (dragResizeColumnCount != members.length) {
                        return;
                    }

                    // one drag operation results in a save for each column, as they all get resized
                    // now that they are all resized, save the column widths (save updates these automatically)
                    save();

                    // reset the flag
                    dragResizeColumnCount = null;
                }
            });

            addMember(column);
        }
    }

    public PortalColumn getPortalColumn(int column) {
        return ((PortalColumn) getMember(column));
    }

    public PortalColumn addPortletWindow(PortletWindow portletWindow, int column) {

        PortalColumn portalColumn = (PortalColumn) getMember(column);
        portalColumn.addMember(portletWindow);

        return portalColumn;
    }

    public void save() {
        this.dashboardView.save();
    }

    public void save(AsyncCallback<Dashboard> callback) {
        this.dashboardView.save(callback);
    }

    public void rebuild() {
        this.dashboardView.rebuild();
    }

    public void resize() {
        for (Canvas c : getMembers()) {
            PortalColumn column = (PortalColumn) c;

            for (Canvas p : column.getMembers()) {
                if (p instanceof PortletWindow) {
                    PortletWindow portlet = (PortletWindow) p;

                    portlet.setWidth(column.getWidth());
                }
            }
        }
    }

}