/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.osgi.service.environment.EnvironmentInfo;

/**
 * Factory class that can create a new profile with the correct
 * default values.
 * 
 * @since 3.4
 *
 */
public class ProfileFactory {

	static private String FLAVOR_DEFAULT = "tooling"; //$NON-NLS-1$
	static private String EMPTY = ""; //$NON-NLS-1$
	static private EnvironmentInfo info;

	public static IProfile makeProfile(String profileId) {
		Map profileProperties = new HashMap();
		profileProperties.put(IProfile.PROP_INSTALL_FOLDER, getDefaultLocation());
		profileProperties.put(IProfile.PROP_FLAVOR, getDefaultFlavor());
		profileProperties.put(IProfile.PROP_ENVIRONMENTS, getDefaultEnvironments());
		profileProperties.put(IProfile.PROP_NL, getDefaultNL());

		try {
			return ProvisioningUtil.addProfile(profileId, profileProperties, null);
		} catch (ProvisionException e) {
			// log
		}
		return null;
	}

	public static String getDefaultLocation() {
		return Platform.getUserLocation().getURL().getPath();
	}

	public static String getDefaultFlavor() {
		return FLAVOR_DEFAULT;
	}

	private static EnvironmentInfo getEnvironmentInfo() {
		if (info == null) {
			info = (EnvironmentInfo) ServiceHelper.getService(ProvUIActivator.getContext(), EnvironmentInfo.class.getName());
		}
		return info;
	}

	public static String getDefaultNL() {
		if (getEnvironmentInfo() != null) {
			return info.getNL();
		}
		return EMPTY;
	}

	public static String getDefaultEnvironments() {
		if (getEnvironmentInfo() != null) {
			return "osgi.os=" //$NON-NLS-1$
					+ info.getOS() + ",osgi.ws=" + info.getWS() //$NON-NLS-1$
					+ ",osgi.arch=" + info.getOSArch(); //$NON-NLS-1$
		}
		return EMPTY;
	}
}