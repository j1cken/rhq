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
package org.rhq.core.domain.measurement;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * This contains a set of measurement schedules for one resource.
 *
 * @author <a href="mailto:heiko.rupp@redhat.com">Heiko W. Rupp</a>
 */
public class ResourceMeasurementScheduleRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Set<MeasurementScheduleRequest> measurementSchedules = new HashSet<MeasurementScheduleRequest>();
    private final int resourceId;

    // The avail schedule is stripped out on set and must be explicitly requested.  This allows the avail
    // schedule to piggyback on all other standard schedule sync/updates while protecting all standard
    // handling of measurement schedules.
    private MeasurementScheduleRequest availabilitySchedule;

    /**
     * Creates a {@link ResourceMeasurementScheduleRequest} object that will contain measurement schedules for the given
     * resource.
     *
     * @param resourceId ID of the resource that is associated with the encapsulated schedules
     */
    public ResourceMeasurementScheduleRequest(int resourceId) {
        this.resourceId = resourceId;
    }

    public void addMeasurementScheduleRequest(MeasurementScheduleRequest scheduleRequest) {
        if (MeasurementDefinition.AVAILABILITY_NAME.equals(scheduleRequest.getName())) {
            this.availabilitySchedule = scheduleRequest;
        } else {
            this.measurementSchedules.add(scheduleRequest);
        }
    }

    public Set<MeasurementScheduleRequest> getMeasurementSchedules() {
        return measurementSchedules;
    }

    public MeasurementScheduleRequest getAvailabilitySchedule() {
        return availabilitySchedule;
    }

    public void setMeasurementSchedules(Set<MeasurementScheduleRequest> measurementSchedules) {
        this.measurementSchedules = new HashSet<MeasurementScheduleRequest>(measurementSchedules.size());
        for (MeasurementScheduleRequest scheduleRequest : measurementSchedules) {
            if (MeasurementDefinition.AVAILABILITY_NAME.equals(scheduleRequest.getName())) {
                this.availabilitySchedule = scheduleRequest;
            } else {
                this.measurementSchedules.add(scheduleRequest);
            }
        }
    }

    public int getResourceId() {
        return resourceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (!(o instanceof ResourceMeasurementScheduleRequest))) {
            return false;
        }

        final ResourceMeasurementScheduleRequest rmsr = (ResourceMeasurementScheduleRequest) o;
        return (this.resourceId == rmsr.resourceId);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.resourceId;

        return result;
    }

}