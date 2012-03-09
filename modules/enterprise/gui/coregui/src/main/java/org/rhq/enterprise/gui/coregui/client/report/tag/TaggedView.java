/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.report.tag;

import java.util.ArrayList;
import java.util.Set;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.events.TabSelectedEvent;
import com.smartgwt.client.widgets.tab.events.TabSelectedHandler;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.enterprise.gui.coregui.client.*;
import org.rhq.enterprise.gui.coregui.client.bundle.deployment.BundleDeploymentListView;
import org.rhq.enterprise.gui.coregui.client.bundle.destination.BundleDestinationListView;
import org.rhq.enterprise.gui.coregui.client.bundle.list.BundlesListView;
import org.rhq.enterprise.gui.coregui.client.bundle.version.BundleVersionListView;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.view.HasViewName;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTab;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTabSet;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * The main tag cloud view which will show the actual tag cloud with a tabbed view
 * underneath containing all the tables of data for the selected tag.
 * 
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class TaggedView extends LocatableVLayout implements BookmarkableView, HasViewName {

    public static final ViewName VIEW_ID = new ViewName("Tags", MSG.view_tags_tags(), IconEnum.TAGS);

    private TagCloudView tagCloudView;
    private LocatableTabSet container;
    private ArrayList<Table> viewsWithTags = new ArrayList<Table>();
    private int selectedTabNum = 0;
    private Set<Permission> globalPermissions;

    public TaggedView(String locatorId) {
        super(locatorId);
        setWidth100();
        setHeight100();
    }

    @Override
    protected void onInit() {
        super.onInit();

//        TitleBar titleBar = new TitleBar(this, TaggedView.VIEW_ID.getTitle(), "global/Tag_24.png");
//        titleBar.setExtraSpace(10);
//        addMember(titleBar);

        tagCloudView = new TagCloudView(extendLocatorId("TagCloud"));
        tagCloudView.setAutoHeight();
        tagCloudView.setExtraSpace(10);
        addMember(tagCloudView);
    }

    private void viewTag(String tagString) {

        // if we are switching tags, remove all our tables - we need to recreate them with new criteria
        if (tagString != null && !tagString.equals(tagCloudView.getSelectedTag())) {
            destroyTagTables();
        }

        tagCloudView.setSelectedTag(tagString);

        if (container == null) {

            Tag tag = new Tag(tagString);

            Criteria criteria = new Criteria();
            criteria.addCriteria("tagNamespace", tag.getNamespace());
            criteria.addCriteria("tagSemantic", tag.getSemantic());
            criteria.addCriteria("tagName", tag.getName());

            container = new LocatableTabSet(extendLocatorId("tags"));
            container.setWidth100();
            container.setHeight100();
            container.setTabBarControls(tagCloudView.getDeleteButton());
            container.addTabSelectedHandler(new TabSelectedHandler() {
                public void onTabSelected(TabSelectedEvent event) {
                    selectedTabNum = event.getTabNum();
                }
            });
            container.setSelectedTab(selectedTabNum); // this is so we keep the same tab the user selected between tag selections

            Tab tab;

            BundlesListView bundlesView = new BundlesListView(extendLocatorId("bundleListView"), criteria, null);
            viewsWithTags.add(bundlesView);
            tab = new LocatableTab(extendLocatorId("bundleList"), MSG.view_bundle_bundles());
            tab.setIcon(ImageManager.getBundleIcon());
            tab.setPane(bundlesView);
            container.addTab(tab);

            BundleVersionListView bundleVersionListView = new BundleVersionListView(
                extendLocatorId("bundleVersionListView"), criteria);
            viewsWithTags.add(bundleVersionListView);
            tab = new LocatableTab(extendLocatorId("bundleVersionList"), MSG.view_bundle_bundleVersions());
            tab.setIcon("subsystems/bundle/BundleVersion_16.png");
            tab.setPane(bundleVersionListView);
            container.addTab(tab);

            BundleDeploymentListView bundleDeploymentListView = new BundleDeploymentListView(
                extendLocatorId("bundleDeploymentListView"), criteria,
                    this.globalPermissions.contains(Permission.MANAGE_BUNDLE));
            viewsWithTags.add(bundleDeploymentListView);
            tab = new LocatableTab(extendLocatorId("bundleDeploymentsList"), MSG.view_bundle_bundleDeployments());
            tab.setIcon("subsystems/bundle/BundleDeployment_16.png");
            tab.setPane(bundleDeploymentListView);
            container.addTab(tab);

            BundleDestinationListView bundleDestinationListView = new BundleDestinationListView(
                extendLocatorId("bundleDestinationListView"), criteria);
            viewsWithTags.add(bundleDestinationListView);
            tab = new LocatableTab(extendLocatorId("bundleDestinationsList"), MSG.view_bundle_bundleDestinations());
            tab.setIcon("subsystems/bundle/BundleDestination_16.png");
            tab.setPane(bundleDestinationListView);
            container.addTab(tab);

            ResourceSearchView resourceView = new ResourceSearchView(extendLocatorId("resourceView"), criteria, MSG
                .view_taggedResources_title());
            viewsWithTags.add(resourceView);
            tab = new LocatableTab(extendLocatorId("resourceList"), MSG.view_taggedResources_title());
            tab.setIcon(ImageManager.getResourceIcon(ResourceCategory.SERVICE));
            tab.setPane(resourceView);
            container.addTab(tab);

            for (Table t : viewsWithTags) {
                t.setShowFooter(false);
            }

            addMember(container);
        }

        for (Table t : viewsWithTags) {
            t.refresh();
        }
    }

    private void destroyTagTables() {
        if (this.container != null) {
            this.viewsWithTags.clear();
            removeMember(this.container);
            this.container.destroy();
            this.container = null;
        }
    }

    public void renderView(final ViewPath viewPath) {
        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {
            public void onPermissionsLoaded(Set<Permission> globalPermissions) {
                TaggedView.this.globalPermissions = globalPermissions;
                completeRenderView(viewPath);
            }
        });

    }

    private void completeRenderView(ViewPath viewPath) {
        if (!viewPath.isEnd()) {
            String tagString = viewPath.getCurrent().getPath();
            viewTag(tagString);
        } else {
            destroyTagTables();
            tagCloudView.setSelectedTag(null);
        }
    }


    @Override
    public ViewName getViewName() {
        return VIEW_ID;
    }
}
