/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.report;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.grid.*;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.composite.ResourceInstallCount;
import org.rhq.enterprise.gui.coregui.client.*;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.view.HasViewName;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

import java.util.HashMap;
import java.util.List;

/**
 * A tweaked version of the InventorySummary report that narrows the relevant types to those that support Drift
 * monitoring, and shows the compliance state of each type.
 * 
 * @author Jay Shaughnessy
 */
public class DriftComplianceReport extends LocatableVLayout implements BookmarkableView, HasViewName {

    public static final ViewName VIEW_ID = new ViewName("DriftCompliance", MSG.view_reports_driftCompliance(), IconEnum.DRIFT_COMPLIANCE);

    private ResourceSearchView resourceList;

    public DriftComplianceReport(String locatorId) {
        super(locatorId);

        setHeight100();
        setWidth100();
    }

    @Override
    public void renderView(ViewPath viewPath) {
        if (!viewPath.isEnd()) {
            int resourceTypeId = Integer.parseInt(viewPath.getCurrent().getPath());
            viewPath.next();
            Criteria criteria;
            if (!viewPath.isEnd()) {
                String resourceVersion = viewPath.getCurrent().getPath();
                criteria = createResourceSearchViewCriteria(resourceTypeId, resourceVersion);
            } else {
                criteria = createResourceSearchViewCriteria(resourceTypeId);
            }
            showResourceList(criteria);
        } else {
            hideResourceList();
        }
    }

    @Override
    protected void onInit() {
        super.onInit();

        addMember(new DriftComplianceReportTable(extendLocatorId("table")));
    }

    protected Criteria createResourceSearchViewCriteria(int resourceTypeId) {
        Criteria criteria = new Criteria();
        criteria.addCriteria(ResourceDataSourceField.TYPE.propertyName(), resourceTypeId);
        return criteria;
    }

    protected Criteria createResourceSearchViewCriteria(int resourceTypeId, String resourceVersion) {
        Criteria criteria = new Criteria();
        criteria.addCriteria(ResourceDataSourceField.TYPE.propertyName(), resourceTypeId);
        criteria.addCriteria("version", resourceVersion);
        return criteria;
    }

    private void showResourceList(Criteria criteria) {
        hideResourceList();
        resourceList = new DriftComplianceReportResourceSearchView(extendLocatorId("resourceList"), criteria);
        addMember(resourceList);
        markForRedraw();
    }

    private void hideResourceList() {
        if (resourceList != null) {
            removeMember(resourceList);
            resourceList.destroy();
            resourceList = null;
        }
        markForRedraw();
    }

    @Override
    public ViewName getViewName() {
        return VIEW_ID;
    }

    class DriftComplianceReportTable extends Table<DriftComplianceReportTable.DataSource> {

        public DriftComplianceReportTable(String locatorId) {
            super(locatorId, null);
            setDataSource(new DataSource());
        }

        @Override
        protected void configureTable() {
            ListGridField fieldTypeName = new ListGridField(DataSource.Field.TYPENAME, MSG.common_title_resource_type());
            ListGridField fieldPlugin = new ListGridField(DataSource.Field.TYPEPLUGIN, MSG.common_title_plugin());
            ListGridField fieldCategory = new ListGridField(DataSource.Field.CATEGORY, MSG.common_title_category());
            ListGridField fieldVersion = new ListGridField(DataSource.Field.VERSION, MSG.common_title_version());
            ListGridField fieldCount = new ListGridField(DataSource.Field.COUNT, MSG.common_title_count());

            ListGridField fieldInCompliance = new ListGridField(DataSource.Field.IN_COMPLIANCE, MSG
                .common_title_in_compliance());
            HashMap<String, String> complianceIcons = new HashMap<String, String>();
            complianceIcons.put("true", ImageManager.getAvailabilityIcon(true));
            complianceIcons.put("false", ImageManager.getAvailabilityIcon(false));
            fieldInCompliance.setValueIcons(complianceIcons);
            fieldInCompliance.setType(ListGridFieldType.ICON);
            fieldInCompliance.setCanSortClientOnly(true);

            fieldTypeName.setWidth("35%");
            fieldPlugin.setWidth("10%");
            fieldCategory.setWidth(70);
            fieldVersion.setWidth("*");
            fieldCount.setWidth(60);
            fieldInCompliance.setWidth(100);

            // TODO (ips, 11/11/11): The groupBy functionality is very buggy in SmartGWT 2.4. Once they fix it
            //                       uncomment these lines to allow grouping by the plugin or category fields.
            /*getListGrid().setCanGroupBy(true);
            fieldTypeName.setCanGroupBy(false);
            fieldVersion.setCanGroupBy(false);
            fieldCount.setCanGroupBy(false);
            fieldInCompliance.setCanGroupBy(false);*/

            fieldTypeName.setCellFormatter(new CellFormatter() {
                @Override
                public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                    String url = getResourceTypeTableUrl(record);
                    if (url == null) {
                        return value.toString();
                    }

                    return "<a href=\"" + url + "\">" + value.toString() + "</a>";
                }
            });

            fieldCategory.setType(ListGridFieldType.ICON);
            fieldCategory.setShowValueIconOnly(true);
            HashMap<String, String> categoryIcons = new HashMap<String, String>(3);
            categoryIcons
                .put(ResourceCategory.PLATFORM.name(), ImageManager.getResourceIcon(ResourceCategory.PLATFORM));
            categoryIcons.put(ResourceCategory.SERVER.name(), ImageManager.getResourceIcon(ResourceCategory.SERVER));
            categoryIcons.put(ResourceCategory.SERVICE.name(), ImageManager.getResourceIcon(ResourceCategory.SERVICE));
            fieldCategory.setValueIcons(categoryIcons);
            fieldCategory.setShowHover(true);
            fieldCategory.setHoverCustomizer(new HoverCustomizer() {
                @Override
                public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                    String cat = record.getAttribute(DataSource.Field.CATEGORY);
                    if (ResourceCategory.PLATFORM.name().equals(cat)) {
                        return MSG.common_title_platform();
                    } else if (ResourceCategory.SERVER.name().equals(cat)) {
                        return MSG.common_title_server();
                    } else if (ResourceCategory.SERVICE.name().equals(cat)) {
                        return MSG.common_title_service();
                    }
                    return "";
                }
            });

            setListGridDoubleClickHandler(new DoubleClickHandler() {
                @Override
                public void onDoubleClick(DoubleClickEvent event) {
                    ListGrid lg = (ListGrid) event.getSource();
                    ListGridRecord selected = lg.getSelectedRecord();
                    String url = getResourceTypeTableUrl(selected);
                    if (url != null) {
                        CoreGUI.goToView(url);
                    }
                }
            });

            setListGridFields(fieldTypeName, fieldPlugin, fieldCategory, fieldVersion, fieldCount, fieldInCompliance);
        }

        private String getResourceTypeTableUrl(ListGridRecord selected) {
            String url = null;
            if (selected != null) {
                int resourceTypeId = selected.getAttributeAsInt(DataSource.Field.TYPEID);
                String version = selected.getAttribute(DataSource.Field.VERSION);
                if (version == null) {
                    url = "#Reports/Inventory/DriftCompliance/" + resourceTypeId;
                } else {
                    url = "#Reports/Inventory/DriftCompliance/" + resourceTypeId + "/" + version;
                }
            }
            return url;
        }

        class DataSource extends RPCDataSource<ResourceInstallCount, org.rhq.core.domain.criteria.Criteria> {

            public class Field {
                public static final String COUNT = "count"; // long that we convert to int
                public static final String TYPENAME = "typeName"; // String
                public static final String TYPEPLUGIN = "typePlugin"; // String
                public static final String CATEGORY = "category"; // ResourceCategory
                public static final String TYPEID = "typeId"; // int
                public static final String VERSION = "version"; // String
                public static final String OBJECT = "object";
                public static final String IN_COMPLIANCE = "inCompliance";
            }

            @Override
            public ResourceInstallCount copyValues(Record from) {
                return (ResourceInstallCount) from.getAttributeAsObject(DataSource.Field.OBJECT);
            }

            @Override
            public ListGridRecord copyValues(ResourceInstallCount from) {
                ListGridRecord record = new ListGridRecord();

                record.setAttribute(Field.COUNT, Long.valueOf(from.getCount()).intValue()); // we'll never have over Integer.MAX_VALUE, overflow not a worry
                record.setAttribute(Field.TYPENAME, from.getTypeName());
                record.setAttribute(Field.TYPEPLUGIN, from.getTypePlugin());
                record.setAttribute(Field.CATEGORY, from.getCategory().name());
                record.setAttribute(Field.TYPEID, from.getTypeId());
                record.setAttribute(Field.VERSION, from.getVersion());
                record.setAttribute(Field.OBJECT, from);

                if (from.getNumDriftTemplates() > 0) {
                    record.setAttribute(Field.IN_COMPLIANCE, Boolean.toString(from.isInCompliance()));
                }

                return record;
            }

            @Override
            protected org.rhq.core.domain.criteria.Criteria getFetchCriteria(DSRequest request) {
                // we don't use criterias for this datasource, just return null
                return null;
            }

            @Override
            protected void executeFetch(final DSRequest request, final DSResponse response,
                final org.rhq.core.domain.criteria.Criteria unused) {
                ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

                resourceService.findResourceComplianceCounts(new AsyncCallback<List<ResourceInstallCount>>() {

                    @Override
                    public void onSuccess(List<ResourceInstallCount> result) {
                        response.setData(buildRecords(result));
                        response.setTotalRows(result.size());
                        processResponse(request.getRequestId(), response);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_reports_inventorySummary_failFetch(), caught);
                        response.setStatus(DSResponse.STATUS_FAILURE);
                        processResponse(request.getRequestId(), response);
                    }
                });
            }
        }
    }
}
