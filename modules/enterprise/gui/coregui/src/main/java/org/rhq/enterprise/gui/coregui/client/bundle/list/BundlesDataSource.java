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
package org.rhq.enterprise.gui.coregui.client.bundle.list;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class BundlesDataSource extends RPCDataSource<Bundle, BundleCriteria> {

    private BundleGWTServiceAsync bundleService = GWTServiceLookup.getBundleService();

    public BundlesDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceIntegerField idField = new DataSourceIntegerField("id", MSG.common_title_id());
        idField.setPrimaryKey(true);
        fields.add(idField);

        DataSourceTextField nameField = new DataSourceTextField("name", MSG.common_title_name());
        fields.add(nameField);

        DataSourceTextField descriptionField = new DataSourceTextField("description", MSG.common_title_description());
        fields.add(descriptionField);

        DataSourceTextField bundleTypeDataField = new DataSourceTextField("bundleType", MSG.view_bundle_bundleType());
        fields.add(bundleTypeDataField);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final BundleCriteria criteria) {
        bundleService.findBundlesByCriteria(criteria, new AsyncCallback<PageList<Bundle>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.dataSource_bundle_loadFailed(), caught);
                response.setStatus(DSResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<Bundle> result) {
                response.setData(buildRecords(result));
                response.setTotalRows(result.getTotalSize());
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    protected BundleCriteria getFetchCriteria(final DSRequest request) {
        BundleCriteria criteria = new BundleCriteria();

        if (request.getCriteria().getValues().get("tagNamespace") != null) {
            criteria.addFilterTagNamespace((String) request.getCriteria().getValues().get("tagNamespace"));
        }

        if (request.getCriteria().getValues().get("tagSemantic") != null) {
            criteria.addFilterTagSemantic((String) request.getCriteria().getValues().get("tagSemantic"));
        }

        if (request.getCriteria().getValues().get("tagName") != null) {
            criteria.addFilterTagName((String) request.getCriteria().getValues().get("tagName"));
        }

        if (request.getCriteria().getValues().get("bundleType") != null) {
            criteria.addFilterBundleTypeId(Integer.valueOf(request.getCriteria().getValues().get("bundleType")
                .toString()));
        }

        // TODO: this doesn't work like I think it should, figure out how to search on the name column 
        // if (request.getCriteria().getValues().get("search") != null) {
        //       criteria.setSearchExpression(request.getCriteria().getValues().get("search").toString());
        // }

        return criteria;
    }

    @Override
    public Bundle copyValues(Record from) {
        return (Bundle) from.getAttributeAsObject("object");
    }

    @Override
    public ListGridRecord copyValues(Bundle from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute("id", from.getId());
        record.setAttribute("name", from.getName());
        record.setAttribute("description", from.getDescription());
        record.setAttribute("bundleType", from.getBundleType().getName());
        record.setAttribute("repo", from.getRepo().getName());

        record.setAttribute("object", from);

        return record;
    }
}
