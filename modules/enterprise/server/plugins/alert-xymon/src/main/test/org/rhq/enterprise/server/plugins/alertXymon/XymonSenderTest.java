package org.rhq.enterprise.server.plugins.alertXymon;

import org.junit.BeforeClass;
import org.junit.Test;
import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.resource.Resource;

import static org.junit.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA.
 * User: torben
 * Date: 12/10/10
 * Time: 2:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class XymonSenderTest {

    static Alert alert;

    static XymonSender sender;

    @BeforeClass
    public static void setup() {
        alert = new Alert();
        AlertDefinition alertDef = new AlertDefinition();
        alertDef.setName("MyAlertDef");
        alertDef.setPriority(AlertPriority.LOW);

        Resource resource = new Resource();
        resource.setName("MyResource");
        alertDef.setResource(resource);

        sender = new XymonSender();
//        sender.alertParameters = new

    }

    @Test
    public void testEnum() {
        assertEquals("valueOf does not return the correct color.", "red", XymonColorMapping.valueOf(AlertPriority.HIGH.getName()).getColor());
        assertEquals("valueOf does not return the correct color.", "yellow", XymonColorMapping.valueOf(AlertPriority.MEDIUM.getName()).getColor());
        assertEquals("valueOf does not return the correct color.", "green", XymonColorMapping.valueOf(AlertPriority.LOW.getName()).getColor());
    }

    public void testSendWithoutInvoking() {
        System.out.println(sender.send(alert));
    }

}
