/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.clientapi.agent.discovery;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceType;

/**
 * The interface to a RHQ Agent's Resource discovery subsystem.
 */
public interface DiscoveryAgentService {
    /**
     * This will update the plugin configuration for the resource with the given ID. This effectively will change the
     * way the resource component connects to/communicates with the resource, so it will require the resource component
     * to be restarted.
     *
     * @param  resourceId             identifies the resource whose plugin configuration is to be updated
     * @param  newPluginConfiguration the new plugin configuration
     *
     * @throws InvalidPluginConfigurationClientException if failed to update the plugin configuration or failed to
     *                                                   restart the component due to a bad plugin configuration
     * @throws PluginContainerException                  if failed for some other reason
     */
    void updatePluginConfiguration(int resourceId, Configuration newPluginConfiguration)
        throws InvalidPluginConfigurationClientException, PluginContainerException;

    /**
     * Called to inform the agent of a status change for the resource represented by syncInfo. The agent processes the syncInfo for the resource and initiates a status update for its sub-tree.
     *
     * @param syncInfo for the root of the tree to be updated.
     */
    void synchronizeInventory(ResourceSyncInfo syncInfo);

    /**
     * Access to the current inventory managed by the plugin container.
     *
     * @return the platform that is managed by this plugin container
     */
    Resource getPlatform();

    /**
     * Executes an immediate plugin discovery scan for top-level servers. This looks for servers not yet in inventory.
     *
     * @return the inventory report
     *
     * @throws PluginContainerException if the server scan fails
     */
    @NotNull
    InventoryReport executeServerScanImmediately() throws PluginContainerException;

    /**
     * Executes an immediate plugin discovery scan for services and non-top-level servers. This looks for platform
     * services and for servers and services that are children of servers or services already in inventory.
     *
     * @return the inventory report
     *
     * @throws PluginContainerException if the service scan fails
     */
    @NotNull
    InventoryReport executeServiceScanImmediately() throws PluginContainerException;

    /**
     * This method asks that a service scan be performed, but it does not wait for the results of that scan.
     */
    void executeServiceScanDeferred();

    /**
     * Checks the availability of all resources and returns a report on their availability statuses. This method blocks
     * until all availabilities have been checked at which point in time the report is built and returned.
     *
     * @param  changedOnlyReport if <code>true</code>, the report returned will only contain statuses for those
     *                           resources that have changed availability status from their last known state. If <code>
     *                           false</code>, the report will contain information on all known resources (which will
     *                           make the report much larger than had <code>true</code> been passed in).
     *
     * @return the report
     */
    AvailabilityReport executeAvailabilityScanImmediately(boolean changedOnlyReport);

    /**
     * Returns the current availablity for the specified Resource if no other thread currently possesses a write lock
     * on that Resource's ResourceComponent, or otherwise, returns the last-collected availablity for the Resource. If
     * the availability is not known, "unknown" will be returned (that is, the {@link AvailabilityType} in the returned
     * Availablity will be set to <code>null</code>).
     *
     * @param  resource a Resource
     *
     * @return the current availablity for the specified Resource if no other thread currently possesses a write lock
     *         on that Resource's ResourceComponent, or otherwise, the last-collected availablity for the Resource; if
     *         the availability is not known, "unknown" will be returned (that is, the {@link AvailabilityType} in the
     *         returned Availablity will be set to <code>null</code>).
     */
    @NotNull
    Availability getCurrentAvailability(Resource resource);

    /**
     * This call will request that the agent produce a full availability report on its next availability scan. 
     */
    void requestFullAvailabilityReport();

    /**
     * Manually discover the resource of the specified type using the specified plugin configuration (i.e. connection
     * properties). This will not only create a new resource, but it will also ensure the resource component is
     * activated (and thus connects to the managed resource). If an error occurs, but the caller can still process the
     * results, the returned object will contain a ResourceError that is associated with the new resource (this occurs
     * when the new resource was created but its component could not be activated).
     *
     * @param  resourceType        the type of resource to be manually added
     * @param  parentResourceId    the id of the resource that will be the parent of the manually discovered resource
     * @param  pluginConfiguration the properties that should be used to connect to the underlying managed resource
     * @param  creatorSubjectId    the {@link org.rhq.core.domain.auth.Subject} id of the JON user that requested the
     *                             addition of the resource
     *
     * @return the newly discovered resource with any associated {@link ResourceError} that might have occurred during
     *         the activation of the resource
     *
     * @throws InvalidPluginConfigurationClientException if connecting to the underlying managed resource failed due to
     *                                                   an invalid plugin configuration.
     * @throws PluginContainerException                  if the manual discovery fails for any other reason
     */
    MergeResourceResponse manuallyAddResource(ResourceType resourceType, int parentResourceId,
        Configuration pluginConfiguration, int creatorSubjectId) throws InvalidPluginConfigurationClientException,
        PluginContainerException;

    /**
     * Shuts down and removes a Resource and its descendents from the PC's inventory.
     *
     * @param resourceId the id of the Resource to remove
     */
    void uninventoryResource(int resourceId);

    // TODO GH: Everything below here is not used yet, nor implemented
    /**
     * Enable periodic scans for new services for the specified server, using the specified discovery configuration. If
     * service scans were already enabled, the server's discovery configuration is updated on the agent.
     *
     * @param serverResourceId resource id of the server
     * @param config           discovery configuration for the server
     */
    void enableServiceScans(int serverResourceId, Configuration config);

    /**
     * Disable periodic scans for new services for the specified server. If service scans were not enabled, this method
     * is a no-op.
     *
     * @param serverResourceId resource id of the server
     */
    void disableServiceScans(int serverResourceId);
}