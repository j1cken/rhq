package org.rhq.enterprise.server.plugins.alertXymon;

import org.rhq.core.domain.alert.Alert;
import org.rhq.enterprise.server.util.LookupUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by IntelliJ IDEA.
 * User: torben
 * Date: 1/19/11
 * Time: 1:30 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class StatusMessage {
    Alert alert;

    StatusMessage() {

    }

    abstract String getTestname();

    abstract String getColor();

    abstract String getDetails();

    public String getHostname() {
        //strip domain name
        String machineName = LookupUtil.getResourceManager().getRootResourceForResource(alert.getAlertDefinition().getResource().getId()).getName();

        return machineName.replaceAll("\\.", ",");
    }

    String getDate(Alert alert) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH);
        return sdf.format(new Date(alert.getCtime()));
    }

    public String getDate() {
        return getDate(alert);
    }

}
