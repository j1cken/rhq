package org.rhq.enterprise.server.plugins.alertXymon;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.util.LookupUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: torben
 * Date: 1/19/11
 * Time: 1:29 AM
 * To change this template use File | Settings | File Templates.
 * <p/>
 * todo: null bei availability notif
 */
public class GroupedStatusMessage extends StatusMessage {

    private final Subject overlord = LookupUtil.getSubjectManager().getOverlord();

    public GroupedStatusMessage(Alert alert) {
        this.alert = alert;
    }

    public String getTestname() {
        return alert.getAlertDefinition().getResource().getResourceType().getName();
    }

    public String getColor() {
        List<Alert> lastAlertList = getLastAlertList(true);
        Collections.sort(lastAlertList, new AlertComparator());
        Alert lowestPrioAlert = lastAlertList.get(lastAlertList.size() - 1);
        return XymonColorMapping.valueOf(lowestPrioAlert.getAlertDefinition().getPriority().getName()).getColor();
    }

    @Override
    String getDetails() {
        return new HtmlBuilder().toString();
    }

    private List getLastAlertList(boolean includeOriginalAlert) {
        List<ResourceWithAvailability> childResources = LookupUtil.getResourceManager().
                findResourcesByParentAndType(overlord,
                        alert.getAlertDefinition().getResource().getParentResource(),
                        alert.getAlertDefinition().getResource().getResourceType());

        ArrayList<Alert> alertList = new ArrayList<Alert>(childResources.size());
            for (ResourceWithAvailability childResource : childResources) {
                int id = childResource.getResource().getId();
                if (alert.getAlertDefinition().getResource().getId() != id || includeOriginalAlert) {
                    alertList.add(getLastAlertFor(childResource.getResource()));
                }
            }
        return alertList;
    }

    private Alert getLastAlertFor(Resource resource) {
        AlertCriteria alertCriteria = new AlertCriteria();
        alertCriteria.addFilterResourceIds(resource.getId());
        alertCriteria.addSortCtime(PageOrdering.DESC);
        alertCriteria.fetchAlertDefinition(true);
        alertCriteria.setPaging(0, 1);
        PageList<Alert> alerts = LookupUtil.getAlertManager().findAlertsByCriteria(overlord, alertCriteria);

        if (alerts.isEmpty()) {
            return new MyNullAlert(resource);
        } else {
            return alerts.get(0);
        }
    }

    class HtmlBuilder {

        @Override
        public String toString() {
            StringBuilder status = new StringBuilder().append(getTableStartTag());
            status.append(getRowTag());

            List<Alert> lastAlertList = getLastAlertList(false);
            for (Alert alert : lastAlertList) {
                status.append(getRowTag(alert));
            }

            status.append(getTableEndTag());
            return status.toString();
        }

        private String getRowTag() {
            return getRowTag(alert);
        }

        private String getRowTag(Alert _alert) {
            StringBuilder sb = new StringBuilder();
            sb.append("<tr>");
            sb.append("<td valign=top>&");
            if (!(_alert instanceof MyNullAlert)) {
                sb.append(XymonColorMapping.valueOf(_alert.getAlertDefinition().getPriority().getName()).getColor());
            } else {
                sb.append("clear");
            }
            sb.append("</td>");
            sb.append("<td valign=top>");
            sb.append(getDate(_alert));
            sb.append("</td>");
            sb.append("<td>");
            sb.append(_alert.getAlertDefinition().getName());
            if (!(_alert instanceof MyNullAlert)) {
                sb.append("<br/>");
            }
            sb.append("Resource: ");
            sb.append(_alert.getAlertDefinition().getResource().getName());
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

    public class MyNullAlert extends Alert {
        private final Resource _resource;

        public MyNullAlert(Resource resource) {
            this._resource = resource;
        }

        @Override
        public AlertDefinition getAlertDefinition() {
            return new MyAlertDefinition(_resource);
        }
    }

    private class MyAlertDefinition extends AlertDefinition {
        private final Resource _resource;

        public MyAlertDefinition(Resource resource) {
            _resource = resource;
        }

        @Override
        public Resource getResource() {
            return _resource;
        }

//        @Override
//        public AlertPriority getPriority() {
//            return MyAlertPriority.UNDEF;
//        }

        @Override
        public String getName() {
            return "";
        }
    }


    public static class AlertComparator implements Comparator<Alert> {
        private AlertComparator() {

        }

        public int compare(Alert o1, Alert o2) {
            if (o1 instanceof MyNullAlert && o2 instanceof MyNullAlert) {
                return 0;
            } else if (o1 instanceof MyNullAlert) {
                return -1;
            } else if (o2 instanceof MyNullAlert) {
                return 1;
            }
            int prio1 = XymonColorMapping.valueOf(o1.getAlertDefinition().getPriority().getName()).getPrio();
            int prio2 = XymonColorMapping.valueOf(o2.getAlertDefinition().getPriority().getName()).getPrio();

            if (prio1 == prio2) {
                return 0;
            } else if (prio1 < prio2) {
                return -1;
            } else {
                return 1;
            }
        }

        public static AlertComparator getInstance() {
            return new AlertComparator();
        }
    }
}
