/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.db.ant;

import mazz.i18n.Logger;
import mazz.i18n.LoggerFactory;
import mazz.i18n.LoggerLocale;
import mazz.i18n.Msg;

/**
 * Convenience utility that creates I18N {@link Logger loggers} and {@link Msg messages} for the ant tools. These
 * factory methods are used to create I18N objects that access the proper resource bundle for the ant tools.
 *
 * @author John Mazzitelli
 *
 */
public class DbAntI18NFactory {
    private static final Msg.BundleBaseName s_bundleBaseName = new Msg.BundleBaseName("dbutils-ant-messages");

    /**
     * Creates the logger and uses the {@link LoggerLocale}.
     *
     * @param  clazz the class that owns the logger - identifies the resource bundle
     *
     * @return the logger
     */
    public static Logger getLogger(Class clazz) {
        return LoggerFactory.getLogger(clazz, s_bundleBaseName);
    }

    /**
     * Creates a {@link Msg} that uses the messages resource bundle and the VM's default locale.
     *
     * @return object that can be used to look up I18N messages
     */
    public static Msg getMsg() {
        return new Msg(s_bundleBaseName);
    }

    /**
     * Creates a {@link Msg} that uses the messages resource bundle and {@link LoggerLocale}, which is used by the
     * {@link #getLogger(Class) loggers created by this class}. This is useful if you want to set messages in exceptions
     * that are simply to be logged and not necessarily be bubbled up to a user interface.
     *
     * @return object that can be used to look up I18N messages
     */
    public static Msg getMsgWithLoggerLocale() {
        return new Msg(s_bundleBaseName, LoggerLocale.getLogLocale());
    }
}