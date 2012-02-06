<#ftl >
<#--
/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
-->
<#-- @ftlvariable name="var" type="org.rhq.enterprise.server.rest.domain.GroupRest" -->
<html>
    <table>
        <thead>
        <tr>
            <td>Name</td><td>Value</td>
        </tr>
        </thead>
        <tr>
            <td>Name</td><td>${var.name}</td>
        </tr>
        <tr>
            <td>Id</td><td>${var.id}</td>
        </tr>
        <tr>
            <td>Category</td><td>${var.category}</td>
        </tr>
        <#if var.resourceTypeId??>
        <tr>
            <td>ResourceType id</td><td></td>
        </tr>
        </#if>
        <tr>
            <td>Recursive</td><td>${var.recursive?string("Yes","No")}</td>
        </tr>
    </table>
    <a href="/rest/1/group/${var.id?c}/resources.html">Resources</a><br/>
</html>
