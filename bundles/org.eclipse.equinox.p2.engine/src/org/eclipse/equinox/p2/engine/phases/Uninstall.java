/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.engine.phases;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.internal.p2.engine.TouchpointManager;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.core.helpers.MultiStatus;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class Uninstall extends IUPhase {

	private static final String PHASE_ID = "uninstall"; //$NON-NLS-1$

	public Uninstall(int weight) {
		super(weight, Messages.Engine_Uninstall_Phase);
	}

	protected IStatus performOperand(EngineSession session, Profile profile, Operand operand, IProgressMonitor monitor) {
		IInstallableUnit unit = operand.first();

		monitor.subTask(NLS.bind(Messages.Engine_Uninstalling_IU, unit.getId()));

		ITouchpoint touchpoint = TouchpointManager.getInstance().getTouchpoint(unit.getTouchpointType());
		if (!touchpoint.supports(PHASE_ID))
			return Status.OK_STATUS;

		((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(PHASE_ID, true, profile, operand, InstallableUnitEvent.UNINSTALL, touchpoint));

		//TODO need to protect the actual operation on a try / catch to ensure the delivery of event. 
		ITouchpointAction[] actions = touchpoint.getActions(PHASE_ID, profile, operand);
		MultiStatus result = new MultiStatus();
		for (int i = 0; i < actions.length; i++) {
			result.add((IStatus) actions[i].execute());
			session.record(actions[i]);
		}

		((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(PHASE_ID, false, profile, operand, InstallableUnitEvent.UNINSTALL, touchpoint, result));
		return result;
	}

	protected boolean isApplicable(Operand op) {
		if (op.first() != null)
			return true;
		return false;
	}
}