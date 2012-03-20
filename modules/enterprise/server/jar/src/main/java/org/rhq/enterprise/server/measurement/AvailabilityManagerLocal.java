/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.measurement;

import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;

/**
 * Manager that is used to determine a resource's availability over a span of time.
 *
 * @author Heiko W. Rupp
 * @author John Mazzitelli
 */
@Local
public interface AvailabilityManagerLocal extends AvailabilityManagerRemote {
    /**
     * Purges all availabilities that are old. The <code>oldest</code> time is the epoch milliseconds of the oldest
     * availability that is to be retained. The {@link Availability#getEndTime() end time} is the time that is examined.
     * No availability row with a <code>null</code> {@link Availability#getEndTime() end time} will ever be purged.
     *
     * @param  oldest oldest time (in epoch milliseconds) to retain; older records get purged
     *
     * @return the number of availabilities that were purged
     */
    int purgeAvailabilities(long oldest);

    /**
     * Indicates if the given resource is currently up (i.e. available) or down.
     *
     * @param  subject
     * @param  resourceId
     *
     * @return the current status of the resource
     */
    AvailabilityType getCurrentAvailabilityTypeForResource(Subject subject, int resourceId);

    /**
     * Get the individual availability data points for the given resource.
     *
     * @param  subject
     * @param  resourceId              PK of the resource wanted
     * @param  begin                   start time for data we are interested in
     * @param  end                     end time for data we are interested in
     * @param  points                  number of data points to return
     * @param  withCurrentAvailability if true, the last data point in the range will match the resource's current
     *                                 availability no matter what
     *
     * @return the availabilities over the given time span in a list
     */
    List<AvailabilityPoint> findAvailabilitiesForResource(Subject subject, int resourceId, long begin, long end,
        int points, boolean withCurrentAvailability);

    /**
     * Get the individual availability data points for the given resource group.
     *
     * @param  subject
     * @param  groupId                 PK of the resource group wanted
     * @param  begin                   start time for data we are interested in
     * @param  end                     end time for data we are interested in
     * @param  points                  number of data points to return
     * @param  withCurrentAvailability if true, the last data point in the range will match the resource group's current
     *                                 availability no matter what
     *
     * @return the availabilities over the given time span in a list
     */
    List<AvailabilityPoint> findAvailabilitiesForResourceGroup(Subject subject, int groupId, long begin, long end,
        int points, boolean withCurrentAvailability);

    /**
     * Get the individual availability data points for the given auto group.
     *
     * @param  subject
     * @param  parentResourceId        PK of the parent resource of the auto group wanted
     * @param  resourceTypeId          PK of the resource type of the auto group wanted
     * @param  begin                   start time for data we are interested in
     * @param  end                     end time for data we are interested in
     * @param  points                  number of data points to return
     * @param  withCurrentAvailability if true, the last data point in the range will match the autogroup's current 
     *                                 availability no matter what
     *
     * @return the availabilities over the given time span in a list
     */
    List<AvailabilityPoint> findAvailabilitiesForAutoGroup(Subject subject, int parentResourceId, int resourceTypeId,
        long begin, long end, int points, boolean withCurrentAvailability);

    /**
     * Merge an {@link AvailabilityReport} that has been received from an agent. A report will only contain those
     * availabilities that have changed since the agent's last sent report. Note that if an agent has been restarted, it
     * will always send a full report as its first. An agent is obliged to sent at least one availability record in the
     * report in order for the server to determine which agent is sending the report (since a record has a Resource in
     * it and from any Resource we can dsetermine the Agent).
     *
     * @param  report the report containing 1 or more availabilities for 1 or more resources.
     *
     * @return If <code>true</code>, this indicates everything seems OK - the server merged everything successfully and
     *         the server and agent seem to be in sync with each. If <code>false</code>, the server thinks something
     *         isn't right and it may be out of sync with the agent. When <code>false</code> is returned, the caller
     *         should send in a <i>full</i> availability report the next time in order to ensure the server and agent
     *         are in sync. <code>true</code> should always be returned if the given availability report is already a
     *         full report.
     */
    boolean mergeAvailabilityReport(AvailabilityReport report);

    /**
     * Executing this method will update the given agent's lastAvailabilityReport time
     * in a new transaction
     *  
     * @param agentId the id of the agent
     */
    void updateLastAvailabilityReport(int agentId);

    /**
     * Update availabilities for all resources managed by the given agent to the given availability type (which may be
     * <code>null</code> to indicate unknown).  NOTE: This does not include the top-level platform resource for
     * the agent. To update a single resource avail see {@link #updateResourceAvailability(Subject, Availability)}.
     *
     * @param agentId all resources managed by this agent will have their availabilities changed
     * @param platformAvailabilityType the type that the agent's top level platform resource will have 
     * @param childAvailabilityType the type that the agent's child resources will have
     */
    void updateAgentResourceAvailabilities(int agentId, AvailabilityType platformAvailabilityType,
        AvailabilityType childAvailabilityType);

    /**
     * @Deprecated use {@link #findAvailabilityByCriteria(Subject, org.rhq.core.domain.criteria.AvailabilityCriteria)}
     */
    @Deprecated
    List<Availability> findAvailabilityWithinInterval(int resourceId, Date startDate, Date endDate);
}