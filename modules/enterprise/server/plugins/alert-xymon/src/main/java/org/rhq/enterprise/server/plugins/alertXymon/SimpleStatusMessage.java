package org.rhq.enterprise.server.plugins.alertXymon;

import org.rhq.core.domain.alert.Alert;

/**
 * Created by IntelliJ IDEA.
 * User: torben
 * Date: 1/19/11
 * Time: 1:26 AM
 * To change this template use File | Settings | File Templates.
 */
class SimpleStatusMessage extends StatusMessage {

    public SimpleStatusMessage(Alert alert) {
        this.alert = alert;
    }

    public String getTestname() {
        return alert.getAlertDefinition().getResource().getName();
    }

    public String getColor() {
        return XymonColorMapping.valueOf(alert.getAlertDefinition().getPriority().getName()).getColor();
    }

    @Override
    String getDetails() {
        return new HtmlBuilder().toString();
    }

    class HtmlBuilder {

        @Override
        public String toString() {
            StringBuilder status = new StringBuilder();
            status.append(getTableStartTag());
            status.append(getRowTag());
            status.append(getTableEndTag());
            return status.toString();
        }

        private String getRowTag() {
            StringBuilder sb = new StringBuilder();
            sb.append("<tr>");
            sb.append("<td valign=top>&");
            sb.append(XymonColorMapping.valueOf(alert.getAlertDefinition().getPriority().getName()).getColor());
            sb.append("</td>");
            sb.append("<td valign=top>");
            sb.append(alert.getCtime());
            sb.append("</td>");
            sb.append("<td>");
            sb.append(alert.getAlertDefinition().getName());
            sb.append("<br/>Resource: ");
            sb.append(alert.getAlertDefinition().getResource().getName());
            sb.append("</td>");
            sb.append("</tr>");

            return sb.toString();
        }


        @SuppressWarnings({"SameReturnValue"})
        private String getTableEndTag() {
            return "</table>";
        }

        @SuppressWarnings({"SameReturnValue"})
        private String getTableStartTag() {
            return "<table summary='Status summary' border=1>";
        }

    }

}
