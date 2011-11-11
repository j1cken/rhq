package org.rhq.enterprise.server.plugins.alertXymon;

import org.rhq.core.domain.alert.Alert;

/**
 * Created by IntelliJ IDEA.
 * User: torben
 * Date: 1/19/11
 * Time: 1:25 AM
 * To change this template use File | Settings | File Templates.
 */
final class StatusMessageFactory {
    private StatusMessageFactory() {

    }

    public static StatusMessage getInstance(XymonInfo info, Alert alert) {
        if (info.isGroupResources()) {
            return new GroupedStatusMessage(alert);
        } else {
            return new SimpleStatusMessage(alert);
        }
    }

}
