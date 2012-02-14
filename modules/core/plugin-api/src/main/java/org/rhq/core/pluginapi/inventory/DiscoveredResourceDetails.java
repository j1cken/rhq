/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.pluginapi.inventory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.system.ProcessInfo;

/**
 * This contains all the details for a resource that was discovered by a {@link ResourceDiscoveryComponent}.
 *
 * <p>Note that any newly discovered resource must have a unique resource key as compared to other sibling resources.
 * That is to say, a parent resource's children must have unique resource keys (the children cannot share keys). A
 * corollary to this is that if a discovery component "re-discovers" a resource, the discovery component must ensure
 * that it assigns the same key to that re-discovered resource (i.e. resource keys must be consistent and stable across
 * multiple discoveries of that same resource). As an example, if you discover a resource "foo" and assign it a resource
 * key of "fooKey"; then the next time that "foo" resource is discovered again, the discovery component must ensure that
 * it discovers it with the resource key "fooKey" again.</p>
 *
 * @author John Mazzitelli
 */
public class DiscoveredResourceDetails {
    private static final int RESOURCE_KEY_MAX_LENGTH = 500;
    private static final int RESOURCE_NAME_MAX_LENGTH = 500;
    private static final int RESOURCE_VERSION_MAX_LENGTH = 100;
    private static final int RESOURCE_DESCRIPTION_MAX_LENGTH = 1000;

    private final Log log = LogFactory.getLog(this.getClass());

    private ResourceType resourceType;
    private String resourceKey;
    private String resourceName;
    private String resourceVersion;
    private String resourceDescription;
    private Configuration pluginConfiguration;
    private ProcessInfo processInfo;

    /**
     * This creates a new instance that provides details for a newly discovered resource.
     *
     * <p>The Resource type, key, and name must not be <code>null</code>. Additionally the key and name must not be "",
     * and the key must not be longer than 500 characters. If any of these requirements are not met, an exception will
     * be thrown.</p>
     *
     * @param  resourceType        the type of resource that was discovered (must not be <code>null</code>)
     * @param  resourceKey         the discovered resource's key where the key must be unique among the resource's
     *                             sibling resources; that is, a parent's direct child resources must all have unique
     *                             resource keys, but those keys need not be unique across the entire inventory of all
     *                             resources (must not be <code>null</code>)
     * @param  resourceName        the name of the discovered resource, used mainly for UI display purposes (must not be
     *                             <code>null</code>)
     * @param  resourceVersion     the discovered resource's version string (which may have any form or syntax
     *                             appropriate for the resource and may be <code>null</code>)
     * @param  resourceDescription a simple description of the discovered resource, which may or may not be an
     *                             internationalized string (may be <code>null</code>)
     * @param  pluginConfiguration the discovered resource's plugin configuration that will be used by the plugin to
     *                             connect to it (may be <code>null</code>, which means the new resource will just use
     *                             the default plugin configuration as defined by its resource type)
     * @param  processInfo         information on the process in which the newly discovered resource is running (this
     *                             may be <code>null</code> if unknown or not applicable)
     *
     * @throws IllegalArgumentException if the resource type, key, or name is <code>null</code>; the key or name is "";
     *                                  or the key is longer than 500 characters
     */
    public DiscoveredResourceDetails(ResourceType resourceType, String resourceKey, String resourceName,
        String resourceVersion, String resourceDescription, Configuration pluginConfiguration, ProcessInfo processInfo) {
        if (resourceType == null) {
            throw new IllegalArgumentException("resourceType==null");
        }

        this.resourceType = resourceType;
        this.processInfo = processInfo;

        setResourceKey(resourceKey);
        setResourceName(resourceName);
        setResourceVersion(resourceVersion);
        setResourceDescription(resourceDescription);
        setPluginConfiguration(pluginConfiguration);

        return;
    }

    /**
     * The type of resource that was discovered.
     *
     * @return new resource's type (will not be <code>null</code>)
     */
    public ResourceType getResourceType() {
        return resourceType;
    }

    /**
     * The discovered resource's key where the key must be unique among the resource's sibling resources; that is, a
     * parent's direct child resources must all have unique resource keys, but those keys need not be unique across the
     * entire inventory of all resources.
     *
     * @return resource's unique key (will not be <code>null</code>)
     */
    public String getResourceKey() {
        return resourceKey;
    }

    /**
     * Sets the discovered resource's unique key. The key must be unique among the resource's sibling resources; that
     * is, a parent's direct child resources must all have unique resource keys, but those keys need not be unique
     * across the entire inventory of all resources.
     *
     * @param  resourceKey the discovered resource's key (must not be <code>null</code> or "")
     *
     * @throws IllegalArgumentException if <code>resourceKey</code> is <code>null</code>, "", or more than 500
     *                                  characters long
     */
    public void setResourceKey(String resourceKey) {
        if (resourceKey == null) {
            throw new IllegalArgumentException("resourceKey==null");
        }
        if (resourceKey.length() == 0) {
            throw new IllegalArgumentException("resourceKey==\"\"");
        }

        if (resourceKey.length() > RESOURCE_KEY_MAX_LENGTH) {
            throw new IllegalArgumentException("Plugin error: Resource key [" + resourceKey + "] specified by ["
                + this.resourceType + "] discovery component is longer than the maximum length ("
                + RESOURCE_KEY_MAX_LENGTH + ").");
        }

        this.resourceKey = resourceKey;
    }

    /**
     * The name of the discovered resource, which is used mainly for UI display purposes. This has no uniqueness
     * requirements (that is, resources can have the same names; this is true even for sibling resources).
     *
     * @return resource's name (will not be <code>null</code>)
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * Sets the name of the discovered resource, which is used mainly for UI display purposes. The name can be anything
     * (other than <code>null</code> or ""); it has no uniqueness requirements (that is, even sibling resources can have
     * the same name). If the name is longer than 500 characters, it will automatically be truncated to 500 characters.
     *
     * @param  resourceName the discovered resource's name (must not be <code>null</code> or "")
     *
     * @throws IllegalArgumentException if <code>resourceName</code> is <code>null</code> or ""
     */
    public void setResourceName(String resourceName) {
        if (resourceName == null) {
            throw new IllegalArgumentException("resourceName==null");
        }
        if (resourceName.length() == 0) {
            throw new IllegalArgumentException("resourceName==\"\"");
        }

        if (resourceName.length() > RESOURCE_NAME_MAX_LENGTH) {
            log.warn("Plugin error: Resource name [" + resourceName + "] specified by [" + this.resourceType
                + "] discovery component is longer than the maximum length (" + RESOURCE_NAME_MAX_LENGTH
                + " - truncating it to " + RESOURCE_NAME_MAX_LENGTH + " characters...");
            this.resourceName = resourceName.substring(0, RESOURCE_NAME_MAX_LENGTH);
        } else {
            this.resourceName = resourceName;
        }
    }

    /**
     * The discovered resource's version string (which may have any form or syntax that is appropriate for the resource
     * and may be <code>null</code>)
     *
     * @return resource version string
     */
    public String getResourceVersion() {
        return resourceVersion;
    }

    /**
     * Sets the discovered resource's version string (which may have any form or syntax that is appropriate for the
     * resource and may be <code>null</code> which correlates to an empty version string). If the version is longer than
     * 100 characters, it will automatically be truncated to 100 characters.
     *
     * @param  resourceVersion the discovered resource's version string; may be <code>null</code>, if the Resource
     *                         does not have a version or if its version could not be determined
     */
    public void setResourceVersion(String resourceVersion) {
        if (resourceVersion != null && (resourceVersion.length() == 0)) {
            this.resourceVersion = null;
        } else {
            if (resourceVersion != null && (resourceVersion.length() > RESOURCE_VERSION_MAX_LENGTH)) {
                log.warn("Plugin error: Resource version [" + resourceVersion + "] specified by [" + this.resourceType
                    + "] discovery component is longer than the maximum length (" + RESOURCE_VERSION_MAX_LENGTH
                    + ") - truncating it to " + RESOURCE_VERSION_MAX_LENGTH + " characters...");
                this.resourceVersion = resourceVersion.substring(0, RESOURCE_VERSION_MAX_LENGTH);
            } else {
                this.resourceVersion = resourceVersion;
            }
        }
    }

    /**
     * Gets the simple description of the resource, which may or may not be an internationalized string
     *
     * @return discovered resource's simple description string (may be <code>null</code>)
     */
    public String getResourceDescription() {
        return resourceDescription;
    }

    /**
     * Sets a simple description of the resource, which may or may not be an internationalized string. If the
     * description is longer than 1000 characters, it will automatically be truncated to 1000 characters.
     *
     * @param  resourceDescription the discovered resource's description; may be <code>null</code>
     */
    public void setResourceDescription(String resourceDescription) {
        if ((resourceDescription != null) && (resourceDescription.length() == 0)) {
            this.resourceDescription = null;
        } else {
            if ((resourceDescription != null) && (resourceDescription.length() > RESOURCE_DESCRIPTION_MAX_LENGTH)) {
                log.warn("Plugin error: Resource description [" + resourceDescription + "] specified by [" + this.resourceType
                    + "] discovery component is longer than the maximum length (" + RESOURCE_DESCRIPTION_MAX_LENGTH
                    + " - truncating it to " + RESOURCE_DESCRIPTION_MAX_LENGTH + " characters...");
                this.resourceDescription = resourceDescription.substring(0, RESOURCE_DESCRIPTION_MAX_LENGTH);
            } else {
                this.resourceDescription = resourceDescription;
            }
        }
    }

    /**
     * Returns the discovered resource's plugin configuration. If this was never
     * {@link #setPluginConfiguration(Configuration) set} before, a copy of the default plugin configuration, as defined
     * in the {@link #getResourceType() resource type}'s default template, is returned.
     *
     * @return copy of the resource's default plugin configuration
     */
    public Configuration getPluginConfiguration() {
        if (pluginConfiguration == null) {
            pluginConfiguration = createDefaultPluginConfiguration();
        }

        return pluginConfiguration;
    }

    /**
     * Returns the information on the operating system process in which the resource is running.
     *
     * @return resource's process information or <code>null</code> if not known or not applicable (e.g. a platform
     *         resource)
     */
    public ProcessInfo getProcessInfo() {
        return processInfo;
    }

    /**
     * Defines the discovered resource's plugin configuration. You normally call {@link #getPluginConfiguration()} first
     * to get a copy of the resource's default plugin configuration, and then modify that default configuration with
     * custom values.
     *
     * <p>If you never need to customize or change a discovered resource's plugin configuration, you will not have to
     * call this method. The plugin container will simply use the default plugin configuration from the resource's
     * {@link #getResourceType() type}.</p>
     *
     * @param pluginConfiguration the discovered resource's new plugin configuration
     *
     * @see   #setPluginConfiguration(Configuration)
     */
    public void setPluginConfiguration(Configuration pluginConfiguration) {
        this.pluginConfiguration = pluginConfiguration;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append("key=");
        buf.append(getResourceKey());
        buf.append(",name=");
        buf.append(getResourceName());
        buf.append(",type=");
        buf.append(getResourceType().getName());
        buf.append(",version=");
        buf.append(getResourceVersion());
        buf.append(",description=");
        buf.append(getResourceDescription());

        return buf.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        DiscoveredResourceDetails that = (DiscoveredResourceDetails) o;

        if (!resourceKey.equals(that.resourceKey)) {
            return false;
        }

        if (!resourceType.equals(that.resourceType)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = resourceType.hashCode();
        result = (31 * result) + resourceKey.hashCode();
        return result;
    }

    /**
     * Returns a copy of the {@link #getResourceType() resource type}'s default plugin configuration.
     *
     * @return copy of the resource's default plugin configuration
     */
    private Configuration createDefaultPluginConfiguration() {
        ConfigurationDefinition definition = resourceType.getPluginConfigurationDefinition();
        if (definition != null) {
            ConfigurationTemplate template = definition.getDefaultTemplate();
            if (template != null) {
                return template.getConfiguration().deepCopy();
            }
        }

        // There is no default plugin config template defined - return an empty one.
        return new Configuration();
    }
}