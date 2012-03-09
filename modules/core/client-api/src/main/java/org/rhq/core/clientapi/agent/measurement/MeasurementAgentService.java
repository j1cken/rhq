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
package org.rhq.core.clientapi.agent.measurement;

import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;

/**
 * The interface to a JON Agent's measurement (i.e. metric collection) subsystem.
 */
public interface MeasurementAgentService {
    /**
     * Schedules a group of measurements on the agent at specified interval. This method should not fail if it cannot
     * schedule any individual measurement(s). The only errors which should occur are protocol or connection errors.
     *
     * @param resourceSchedules list of the schedules that are to be added to the agent
     */
    void scheduleCollection(Set<ResourceMeasurementScheduleRequest> resourceSchedules);

    /**
     * Updates the schedule for a group of measurements on the agent. Like scheduleCollection(), this method should not
     * fail if it cannot schedule an individual measurement. The only errors which should occur are protocol or
     * connection errors.
     *
     * @param resourceSchedules list of schedules that are to be updated on the agent
     */
    void updateCollection(Set<ResourceMeasurementScheduleRequest> resourceSchedules);

    /**
     * Unschedule previously scheduled measurements for the resources with the specified id's . An attempt will be made
     * to unschedule all measurements; however, if one measurement is unable to be unscheduled, an exception will not be
     * thrown until the end of the operation. Therefore, it is safe to assume that measurements will be unscheduled (or
     * at least attempted to be unscheduled) for all specified resources, regardless of exceptions.
     *
     * @param resourceIds list of the resources whose measurements are to be unscheduled from collection
     */
    void unscheduleCollection(Set<Integer> resourceIds);

    /**
     * This method is a way for the caller to ask for measurement collections to occur "now". In other words, you can
     * obtain real-time, current values of the given measurements for the given resource, even if those measurements are
     * not even scheduled for collection.
     *
     * <p>Measurement data collected via this call will have its non-persistent "name" field set the name of the
     * measurement, but will not have scheduleIds set except for per minute metrics. Requests for per minute
     * metrics must specify the schedule id. This is because of the way a per minute metric is calculated which
     * involved the previously collected value. That value is obtained from a cache which is keyed by schedule id.</p>
     *
     * @param resourceId id of resource to collect from
     * @param requests Each request specifies a metric to be collected along with its corresponding data type
     * @return the set of collected measurements with their data values collected
     */
    Set<MeasurementData> getRealTimeMeasurementValue(int resourceId, Set<MeasurementScheduleRequest> requests);

    Map<String, Object> getMeasurementScheduleInfoForResource(int resourceId);
}