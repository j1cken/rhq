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

import java.lang.annotation.Annotation;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import org.rhq.core.pc.ServerServices;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class ServerServicesProvider implements ResourceProvider {

    @Inject
    private Instance<RhqAgentPluginContainer> rhqAgentPluginContainer;
    
    @Override
    public boolean canProvide(Class<?> type) {
        ServerServices ss = getServerServices();
        
        if (ss == null) {
            return false;
        }
        
        return type.isAssignableFrom(ss.getClass());        
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return getServerServices();
    }

    private ServerServices getServerServices() {
        RhqAgentPluginContainer rpc = rhqAgentPluginContainer.get();
        if (rpc == null) {
            return null;
        }
        
        return rpc.getConfiguration().getServerServices();
    }
}
