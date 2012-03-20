/*
 * RHQ Management Platform
 * Copyright 2010-2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.history;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.history.AbstractOperationHistoryDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;

/**
 * @author Ian Springer
 */
public class ResourceOperationHistoryDataSource extends
    AbstractOperationHistoryDataSource<ResourceOperationHistory, ResourceOperationHistoryCriteria> {

    public static abstract class Field extends AbstractOperationHistoryDataSource.Field {
        public static final String RESOURCE = "resource";
        public static final String GROUP_OPERATION_HISTORY = "groupOperationHistory";
    }

    public static abstract class CriteriaField {
        public static final String RESOURCE_ID = "resourceId";
        public static final String GROUP_OPERATION_HISTORY_ID = "groupOperationHistoryId";
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceTextField resourceField = new DataSourceTextField(Field.RESOURCE);
        fields.add(resourceField);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response,
        final ResourceOperationHistoryCriteria criteria) {
        operationService.findResourceOperationHistoriesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceOperationHistory>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.dataSource_operationHistory_error_fetchFailure(), caught);
                }

                public void onSuccess(PageList<ResourceOperationHistory> result) {
                    dataRetrieved(result, response, request);
                }
            });
    }

    @Override
    protected ResourceOperationHistoryCriteria getFetchCriteria(final DSRequest request) {
        ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();

        if (request.getCriteria().getValues().containsKey(CriteriaField.RESOURCE_ID)) {
            int resourceId = getFilter(request, CriteriaField.RESOURCE_ID, Integer.class);
            criteria.addFilterResourceIds(resourceId);
        }

        if (request.getCriteria().getValues().containsKey(CriteriaField.GROUP_OPERATION_HISTORY_ID)) {
            int groupOperationHistoryId = getFilter(request, CriteriaField.GROUP_OPERATION_HISTORY_ID, Integer.class);
            criteria.addFilterGroupOperationHistoryId(groupOperationHistoryId);
        }

        return criteria;
    }

    @Override
    protected String getSortFieldForColumn(String columnName) {
        if (AncestryUtil.RESOURCE_ANCESTRY.equals(columnName)) {
            return "resource.ancestry";
        }

        return super.getSortFieldForColumn(columnName);
    }

    protected void dataRetrieved(final PageList<ResourceOperationHistory> result, final DSResponse response,
        final DSRequest request) {
        HashSet<Integer> typesSet = new HashSet<Integer>();
        HashSet<String> ancestries = new HashSet<String>();
        for (ResourceOperationHistory history : result) {
            Resource resource = history.getResource();
            typesSet.add(resource.getResourceType().getId());
            ancestries.add(resource.getAncestry());
        }

        // In addition to the types of the result resources, get the types of their ancestry
        // NOTE: this may be too labor intensive in general, but since this datasource is a singleton I couldn't
        //       make it easily optional.
        typesSet.addAll(AncestryUtil.getAncestryTypeIds(ancestries));

        ResourceTypeRepository typeRepo = ResourceTypeRepository.Cache.getInstance();
        typeRepo.getResourceTypes(typesSet.toArray(new Integer[typesSet.size()]), new TypesLoadedCallback() {
            @Override
            public void onTypesLoaded(Map<Integer, ResourceType> types) {
                // Smartgwt has issues storing a Map as a ListGridRecord attribute. Wrap it in a pojo.                
                AncestryUtil.MapWrapper typesWrapper = new AncestryUtil.MapWrapper(types);

                Record[] records = buildRecords(result);
                for (Record record : records) {
                    // To avoid a lot of unnecessary String construction, be lazy about building ancestry hover text.
                    // Store the types map off the records so we can build a detailed hover string as needed.                      
                    record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_TYPES, typesWrapper);

                    // Build the decoded ancestry Strings now for display
                    record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_VALUE, AncestryUtil.getAncestryValue(record));
                }
                response.setData(records);
                response.setTotalRows(result.getTotalSize()); // for paging to work we have to specify size of full result set
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    protected ResourceOperationHistory createOperationHistory() {
        return new ResourceOperationHistory(null, null, null, null, null, null, null);
    }

    @Override
    public ListGridRecord copyValues(ResourceOperationHistory from) {
        ListGridRecord record = super.copyValues(from);

        record.setAttribute(Field.ID, from.getId());
        record.setAttribute(Field.RESOURCE, from.getResource().getName());

        // for ancestry handling       
        record.setAttribute(AncestryUtil.RESOURCE_ID, from.getResource().getId());
        record.setAttribute(AncestryUtil.RESOURCE_NAME, from.getResource().getName());
        record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY, from.getResource().getAncestry());
        record.setAttribute(AncestryUtil.RESOURCE_TYPE_ID, from.getResource().getResourceType().getId());

        return record;
    }

}
