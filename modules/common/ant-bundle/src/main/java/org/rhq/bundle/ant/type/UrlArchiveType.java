/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.bundle.ant.type;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;

/**
 * An archive file to be exploded during the bundle deployment (it could remain compressed if exploded="false" is specified)
 * Can optionally contain a rhq:replace child element that specifies the set of files that contain
 * template variables (e.g. @@http.port@@) which need to be replaced with the value of the corresponding property.
 *
 * This archive file is located at a remote location specified by a URL.
 * 
 * @author Ian Springer
 * @author John Mazzitelli
 */
public class UrlArchiveType extends AbstractUrlFileType {
    private Pattern replacePattern;
    private String exploded = Boolean.TRUE.toString();

    public void addConfigured(ReplaceType replace) {
        List<FileSet> fileSets = replace.getFileSets();
        this.replacePattern = getPattern(fileSets);
    }

    public Pattern getReplacePattern() {
        return replacePattern;
    }

    public String getExploded() {
        return exploded;
    }

    public void setExploded(String exploded) {
        if (!Boolean.TRUE.toString().equalsIgnoreCase(exploded) && !Boolean.FALSE.toString().equalsIgnoreCase(exploded)) {
            throw new BuildException("'exploded' attribute must be 'true' or 'false': " + exploded);
        }
        this.exploded = exploded;
    }
}