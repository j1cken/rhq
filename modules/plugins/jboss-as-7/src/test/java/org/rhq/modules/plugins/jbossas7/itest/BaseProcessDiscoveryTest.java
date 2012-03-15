/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7.itest;

import java.io.File;
import java.util.Collection;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.util.maven.MavenArtifactNotFoundException;
import org.rhq.core.util.maven.MavenArtifactProperties;
import org.rhq.test.arquillian.BeforeDiscovery;
import org.rhq.test.arquillian.DiscoveredResources;
import org.rhq.test.arquillian.FakeServerInventory;
import org.rhq.test.arquillian.MockingServerServices;
import org.rhq.test.arquillian.RunDiscovery;
import org.rhq.test.shrinkwrap.RhqAgentPluginArchive;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * An integration test for {@link org.rhq.modules.plugins.jbossas7.BaseProcessDiscovery}.
 *
 * @author Ian Springer
 */
@Test(groups = "arquillian")
public class BaseProcessDiscoveryTest extends Arquillian {

    @Deployment(name = "platform")
    public static RhqAgentPluginArchive getPlatformPlugin() throws Exception {
        MavenDependencyResolver mavenDependencyResolver = DependencyResolvers.use(MavenDependencyResolver.class);
        String platformPluginArtifact = "org.rhq:rhq-platform-plugin:jar:" + getRhqVersion();
        Collection<RhqAgentPluginArchive> plugins = mavenDependencyResolver.loadEffectivePom("pom.xml")
            .artifact(platformPluginArtifact).resolveAs(RhqAgentPluginArchive.class);
        return plugins.iterator().next();
    }

    @Deployment(name = "jboss-as-7")
    public static RhqAgentPluginArchive getJBossAS7Plugin() throws Exception {
        // This is the jar that was just built during the Maven package phase, just prior to this test getting run
        // during the Maven integration-test phase. This is exactly what we want, because it's the real Maven-produced
        // jar, freshly assembled from the classes being tested.
        File pluginJarFile = new File("target/rhq-jboss-as-7-plugin-" + getRhqVersion() + ".jar");
        MavenDependencyResolver mavenDependencyResolver = DependencyResolvers.use(MavenDependencyResolver.class);
        // Pull in any required plugins from our pom's dependencies.
        Collection<RhqAgentPluginArchive> requiredPlugins = mavenDependencyResolver.loadEffectivePom("pom.xml")
            .importAllDependencies().resolveAs(RhqAgentPluginArchive.class);
        return ShrinkWrap.create(ZipImporter.class, pluginJarFile.getName()).importFrom(pluginJarFile)
            .as(RhqAgentPluginArchive.class).withRequiredPluginsFrom(requiredPlugins);
    }

    @ArquillianResource
    private MockingServerServices serverServices;

    @ArquillianResource
    private PluginContainerConfiguration pluginContainerConfiguration;

    @ArquillianResource
    private PluginContainer pluginContainer;

    @ArquillianResource
    private Deployer pluginDeployer;

    private FakeServerInventory fakeServerInventory;

    @DiscoveredResources(plugin = "jboss-as-7", resourceType = "JBossAS7 Host Controller")
    private Set<Resource> discoveredHostControllers;

    @DiscoveredResources(plugin = "jboss-as-7", resourceType = "JBossAS7 Standalone Server")
    private Set<Resource> discoveredStandaloneServers;

    /**
     * Set up our fake server discovery ServerService, which will auto-import all Resources in reports it receives.
     *
     * @throws Exception if an error occurs
     */
    @BeforeDiscovery
    public void resetServerServices() throws Exception {
        this.serverServices.resetMocks();
        this.fakeServerInventory = new FakeServerInventory();
        when(this.serverServices.getDiscoveryServerService().mergeInventoryReport(any(InventoryReport.class))).then(
            this.fakeServerInventory.mergeInventoryReport(InventoryStatus.COMMITTED));
    }

    /**
     * Tests that autodiscovery of "JBossAS Server" Resources works.
     *
     * @throws Exception if an error occurs
     */
    @RunDiscovery
    public void testAutoDiscovery() throws Exception {
        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        Assert.assertNotNull(platform);
        Assert.assertEquals(platform.getInventoryStatus(), InventoryStatus.COMMITTED);
        // The key of an AS7 Standalone Server Resource is its JBOSS_HOME dir.
        String jboss7Home = new File(System.getProperty("jboss7.home")).getCanonicalPath();
        boolean foundTestServer = containsResourceWithKey(this.discoveredStandaloneServers, jboss7Home);
        Assert.assertTrue(foundTestServer,
                "JBossAS7 Standalone Server Resource with key [" + jboss7Home + "] was not discovered.");
    }

    private boolean containsResourceWithKey(Set<Resource> resources, String key) {
        boolean result = false;
        for (Resource standaloneServer : resources) {
            if (standaloneServer.getResourceKey().equals(key)) {
                result = true;
                break;
            }
        }
        return result;
    }

    private static String getRhqVersion() throws MavenArtifactNotFoundException {
        MavenArtifactProperties rhqPluginContainerPom = MavenArtifactProperties.getInstance("org.rhq",
            "rhq-core-plugin-container");
        return rhqPluginContainerPom.getVersion();
    }

}
