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

package org.rhq.test.arquillian.impl.util;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentScenario;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentTargetDescription;
import org.jboss.arquillian.container.spi.client.deployment.TargetDescription;
import org.jboss.arquillian.container.spi.context.DeploymentContext;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.EventContext;
import org.jboss.arquillian.test.spi.annotation.TestScoped;
import org.jboss.arquillian.test.spi.event.suite.Before;

import org.rhq.core.pc.PluginContainer;
import org.rhq.test.arquillian.impl.RhqAgentPluginContainer;
import org.rhq.test.arquillian.spi.events.PluginContainerDiscovered;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class EnrichmentHook {
    
    @Inject
    private Instance<DeploymentContext> deploymentContext;

    @Inject
    private Instance<DeploymentScenario> deploymentScenario;

    @Inject
    private Instance<ContainerRegistry> containerRegistry;

    @Inject
    @TestScoped
    private InstanceProducer<PluginContainer> pluginContainer;
    
    @Inject
    @TestScoped
    private InstanceProducer<RhqAgentPluginContainer> rhqAgentPluginContainer;
    
    @Inject
    private Event<PluginContainerDiscovered> pluginContainerDiscovered;
    
    public void hookUp(@Observes EventContext<Before> ctx) {
        Before event = ctx.getEvent();
        lookup(event.getTestMethod().getAnnotations());
        ctx.proceed();
        pluginContainerDiscovered.fire(new PluginContainerDiscovered(event.getTestInstance(), event.getTestMethod()));
    }
    
    private void lookup(Annotation... qualifiers) {
        DeploymentScenario scenario = deploymentScenario.get();
        if (scenario == null) {
            return;
        }
        
        boolean contextActivated = false;
        OperateOnDeployment deploymentSpecifier = getOperateOnDeployment(qualifiers);
        Deployment specificDeployment = null;
        if (deploymentSpecifier != null) {
            specificDeployment = scenario.deployment(new DeploymentTargetDescription(deploymentSpecifier.value()));
            if (specificDeployment == null) {
                throw new IllegalArgumentException("@" + OperateOnDeployment.class.getSimpleName() + " specified an unknown deployment '" + deploymentSpecifier.value());
            }
            
            deploymentContext.get().activate(specificDeployment);
            contextActivated = true;
        }
        
        try {            
            TargetDescription target = specificDeployment == null ? checkAndGetCommonTarget(scenario.deployments()) : specificDeployment.getDescription().getTarget();
            lookupAndSetPluginContainer(target);
        } finally {
            if (contextActivated) {
                deploymentContext.get().deactivate();
            }
        }
    }

    private TargetDescription checkAndGetCommonTarget(List<Deployment> deployments) {
        if (deployments == null || deployments.isEmpty()) {
            throw new IllegalStateException("There seem to be no deployments in this deployment scenario. Don't know how to find out the target plugin container");            
        }
        
        Iterator<Deployment> it = deployments.iterator();
        TargetDescription target = it.next().getDescription().getTarget();
        
        while(it.hasNext()) {
            if (!target.equals(it.next().getDescription().getTarget())) {
                throw new IllegalArgumentException("Deployments in this deployment scenario don't share a common target container. Cannot reliably set the PluginContainer to use by the test methods.");
            }
        }
        
        return target;
    }
    
    private void lookupAndSetPluginContainer(TargetDescription target) {
        ContainerRegistry registry = containerRegistry.get();
        if (registry == null) {
            return;
        }
        
        Container container = null;
        
        if (target != null) {
            container = registry.getContainer(target);
        } else {            
        }
        
        if (container == null) {
            throw new IllegalArgumentException("Could not find a PluginContainer called '" + target.getName() + "'");            
        }
        
        try {
            PluginContainer pc = RhqAgentPluginContainer.switchPluginContainer(container.getName());
            pluginContainer.set(pc);
                        
            if (container.getDeployableContainer() instanceof RhqAgentPluginContainer) {
                rhqAgentPluginContainer.set((RhqAgentPluginContainer)container.getDeployableContainer());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not switch to the PluginContainer '" + container.getName() + "'.", e);
        }
    }
    
    private OperateOnDeployment getOperateOnDeployment(Annotation[] annotations) {
        for(Annotation a : annotations) {
            if (a instanceof OperateOnDeployment) {
                return (OperateOnDeployment) a;
            }
        }
        
        return null;
    }    
}
