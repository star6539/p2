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
package org.eclipse.equinox.internal.p2.artifact.repository;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRequest;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;

/**
 * Base class for all requests on an {@link IArtifactRepository}.
 */
public abstract class ArtifactRequest implements IArtifactRequest {
	private static final Status DEFAULT_STATUS = new Status(IStatus.ERROR, Activator.ID, "default"); //$NON-NLS-1$
	protected IArtifactKey artifact;
	protected String resolvedKey;
	protected IArtifactRepository source;
	protected IStatus result = DEFAULT_STATUS;

	public ArtifactRequest(IArtifactKey key) {
		artifact = key;
		// TODO do we need to make this configurable? for now set default request handler to ECF
	}

	public IArtifactKey getArtifactKey() {
		return artifact;
	}

	/**
	 * Returns the result of the previous call to {@link #perform(IProgressMonitor)},
	 * or <code>null</code> if perform has never been called.
	 * 
	 * @return The result of the previous perform call.
	 */
	public IStatus getResult() {
		if (result == DEFAULT_STATUS)
			return new Status(IStatus.ERROR, Activator.ID, "No repository found containing: " + getArtifactKey().toString());

		return result;
	}

	protected IArtifactRepository getSourceRepository() {
		return source;
	}

	/**
	 * Performs the artifact request, and sets the result status.
	 * 
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 */
	abstract public void perform(IProgressMonitor monitor);

	/**
	 * Sets the result of an invocation of {@link #perform(IProgressMonitor)}.
	 * This method is called by subclasses to set the result status for
	 * this request.
	 * 
	 * @param value The result status
	 */
	protected void setResult(IStatus value) {
		result = value;
	}

	public void setSourceRepository(IArtifactRepository value) {
		source = value;
	}
}