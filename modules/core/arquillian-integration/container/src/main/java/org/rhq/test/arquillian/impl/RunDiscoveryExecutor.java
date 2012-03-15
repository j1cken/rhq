/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.test.arquillian.impl;

import org.jboss.arquillian.test.spi.event.suite.TestEvent;

import org.rhq.core.pc.PluginContainer;
import org.rhq.test.arquillian.RunDiscovery;
import org.rhq.test.arquillian.spi.PluginContainerOperation;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class RunDiscoveryExecutor implements PluginContainerOperation {

    public static RunDiscovery getRunDiscoveryForTest(TestEvent event) {
        RunDiscovery runDiscovery = event.getTestMethod().getAnnotation(RunDiscovery.class);

        if (runDiscovery == null) {
            runDiscovery = event.getTestClass().getAnnotation(RunDiscovery.class);
        }
        
        return runDiscovery;
    }
    
    @Override
    public void execute(PluginContainer pluginContainer, TestEvent event) {
        RunDiscovery runDiscovery = getRunDiscoveryForTest(event);
        
        if (runDiscovery != null) {
            runDiscovery(runDiscovery, pluginContainer);
        }
    }
    
    private void runDiscovery(RunDiscovery annotation, PluginContainer pluginContainer) {        
        if (annotation.discoverServers()) {
            pluginContainer.getInventoryManager().executeServerScanImmediately();
        }
        if (annotation.discoverServices()) {
            pluginContainer.getInventoryManager().executeServiceScanImmediately();
        }
    }
}
