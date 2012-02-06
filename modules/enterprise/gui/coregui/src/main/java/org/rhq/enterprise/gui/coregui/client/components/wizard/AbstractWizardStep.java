/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.components.wizard;

import com.smartgwt.client.widgets.Canvas;

import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;

public abstract class AbstractWizardStep implements WizardStep {

    abstract public Canvas getCanvas(Locatable parent);

    abstract public String getName();

    public boolean isNextButtonEnabled() {
        return true;
    }

    public boolean nextPage() {
        return true;
    }

    public boolean previousPage() {
        return true;
    }

}
