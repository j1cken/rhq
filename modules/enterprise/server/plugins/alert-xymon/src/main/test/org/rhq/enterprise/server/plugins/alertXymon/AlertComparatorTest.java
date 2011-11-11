package org.rhq.enterprise.server.plugins.alertXymon;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: torben
 * Date: 1/19/11
 * Time: 4:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class AlertComparatorTest {
    static List<Alert> alerts = new ArrayList();

    static HashMap<Integer, AlertPriority> results = new HashMap<Integer, AlertPriority>();
    @BeforeClass
    public static void setup() {
        AlertDefinition low = new AlertDefinition();
        low.setPriority(AlertPriority.LOW);
        AlertDefinition med = new AlertDefinition();
        med.setPriority(AlertPriority.MEDIUM);
        AlertDefinition high = new AlertDefinition();
        high.setPriority(AlertPriority.HIGH);

        alerts.add(new Alert(low, new Date().getTime()));
        alerts.add(new Alert(high, new Date().getTime()));
        alerts.add(new Alert(low, new Date().getTime()));
        alerts.add(new Alert(high, new Date().getTime()));
        alerts.add(new Alert(med, new Date().getTime()));

        results.put(1, AlertPriority.LOW);
        results.put(2, AlertPriority.LOW);
        results.put(3, AlertPriority.MEDIUM);
        results.put(4, AlertPriority.HIGH);
        results.put(5, AlertPriority.HIGH);
    }

    @Test
    public void compare() {
        Collections.sort(alerts, GroupedStatusMessage.AlertComparator.getInstance());

        int i = 1;
        for (Alert alert : alerts) {
            Assert.assertEquals("wrong prio", results.get(i), alert.getAlertDefinition().getPriority());
            i++;
        }
    }
}
