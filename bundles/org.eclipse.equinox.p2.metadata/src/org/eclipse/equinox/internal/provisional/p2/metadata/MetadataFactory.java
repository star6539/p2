/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC
 *		EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata;

import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.internal.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;

/**
 * A factory class for instantiating various p2 metadata objects.
 */
public class MetadataFactory {
	/**
	 * A description containing information about an installable unit. Once created,
	 * installable units are immutable. This description class allows a client to build
	 * up the state for an installable unit incrementally, and then finally product
	 * the resulting immutable unit.
	 */
	public static class InstallableUnitDescription {
		protected InstallableUnit unit;

		public InstallableUnitDescription() {
			super();
		}

		public void addProvidedCapabilities(Collection additional) {
			if (additional == null || additional.size() == 0)
				return;
			IProvidedCapability[] current = unit().getProvidedCapabilities();
			IProvidedCapability[] result = new IProvidedCapability[additional.size() + current.length];
			System.arraycopy(current, 0, result, 0, current.length);
			int j = current.length;
			for (Iterator i = additional.iterator(); i.hasNext();)
				result[j++] = (IProvidedCapability) i.next();
			unit().setCapabilities(result);
		}

		public void addRequiredCapabilities(Collection additional) {
			if (additional == null || additional.size() == 0)
				return;
			IRequiredCapability[] current = unit().getRequiredCapabilities();
			IRequiredCapability[] result = new IRequiredCapability[additional.size() + current.length];
			System.arraycopy(current, 0, result, 0, current.length);
			int j = current.length;
			for (Iterator i = additional.iterator(); i.hasNext();)
				result[j++] = (IRequiredCapability) i.next();
			unit().setRequiredCapabilities(result);
		}

		public void addTouchpointData(ITouchpointData data) {
			Assert.isNotNull(data);
			unit().addTouchpointData(data);
		}

		public String getId() {
			return unit().getId();
		}

		public IProvidedCapability[] getProvidedCapabilities() {
			return unit().getProvidedCapabilities();
		}

		public IRequiredCapability[] getRequiredCapabilities() {
			return unit().getRequiredCapabilities();
		}

		public IRequiredCapability[] getMetaRequiredCapabilities() {
			return unit().getMetaRequiredCapabilities();
		}

		/**
		 * Returns the current touchpoint data on this installable unit description. The
		 * touchpoint data may change if further data is added to the description.
		 * 
		 * @return The current touchpoint data on this description
		 */
		public ITouchpointData[] getTouchpointData() {
			return unit().getTouchpointData();

		}

		public Version getVersion() {
			return unit().getVersion();
		}

		public void setArtifacts(IArtifactKey[] value) {
			unit().setArtifacts(value);
		}

		public void setCapabilities(IProvidedCapability[] exportedCapabilities) {
			unit().setCapabilities(exportedCapabilities);
		}

		public void setCopyright(ICopyright copyright) {
			unit().setCopyright(copyright);
		}

		public void setFilter(String filter) {
			unit().setFilter(filter);
		}

		public void setId(String id) {
			unit().setId(id);
		}

		public void setLicense(ILicense license) {
			unit().setLicense(license);
		}

		public void setProperty(String key, String value) {
			unit().setProperty(key, value);
		}

		public void setRequiredCapabilities(IRequiredCapability[] requirements) {
			unit().setRequiredCapabilities(requirements);
		}

		public void setMetaRequiredCapabilities(IRequiredCapability[] metaRequirements) {
			unit().setMetaRequiredCapabilities(metaRequirements);
		}

		public void setSingleton(boolean singleton) {
			unit().setSingleton(singleton);
		}

		public void setTouchpointType(ITouchpointType type) {
			unit().setTouchpointType(type);
		}

		public void setUpdateDescriptor(IUpdateDescriptor updateInfo) {
			unit().setUpdateDescriptor(updateInfo);
		}

		public void setVersion(Version newVersion) {
			unit().setVersion(newVersion);
		}

		InstallableUnit unit() {
			if (unit == null) {
				unit = new InstallableUnit();
				unit.setArtifacts(new IArtifactKey[0]);
			}
			return unit;
		}

		IInstallableUnit unitCreate() {
			IInstallableUnit result = unit();
			this.unit = null;
			return result;
		}
	}

	/**
	 * Description of an installable unit patch. The description will automatically have
	 * the {@link IInstallableUnit#PROP_TYPE_FRAGMENT} set to <code>true</code>.
	 */
	public static class InstallableUnitFragmentDescription extends InstallableUnitDescription {
		public InstallableUnitFragmentDescription() {
			super();
			setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		}

		public void setHost(IRequiredCapability[] hostRequirements) {
			((InstallableUnitFragment) unit()).setHost(hostRequirements);
		}

		InstallableUnit unit() {
			if (unit == null)
				unit = new InstallableUnitFragment();
			return unit;
		}
	}

	/**
	 * Description of an installable unit patch. The description will automatically have
	 * the {@link IInstallableUnit#PROP_TYPE_PATCH} set to <code>true</code>.
	 */
	public static class InstallableUnitPatchDescription extends InstallableUnitDescription {

		public InstallableUnitPatchDescription() {
			super();
			setProperty(IInstallableUnit.PROP_TYPE_PATCH, Boolean.TRUE.toString());
		}

		public void setApplicabilityScope(IRequiredCapability[][] applyTo) {
			if (applyTo == null)
				throw new IllegalArgumentException("A patch scope can not be null"); //$NON-NLS-1$
			((InstallableUnitPatch) unit()).setApplicabilityScope(applyTo);
		}

		public void setLifeCycle(IRequiredCapability lifeCycle) {
			((InstallableUnitPatch) unit()).setLifeCycle(lifeCycle);
		}

		public void setRequirementChanges(IRequirementChange[] changes) {
			((InstallableUnitPatch) unit()).setRequirementsChange(changes);
		}

		InstallableUnit unit() {
			if (unit == null) {
				unit = new InstallableUnitPatch();
				((InstallableUnitPatch) unit()).setApplicabilityScope(new IRequiredCapability[0][0]);
			}
			return unit;
		}
	}

	/**
	 * Singleton touchpoint data for a touchpoint with no instructions.
	 */
	private static final ITouchpointData EMPTY_TOUCHPOINT_DATA = new TouchpointData(Collections.EMPTY_MAP);

	private static ITouchpointType[] typeCache = new ITouchpointType[5];

	private static int typeCacheOffset;

	/**
	 * Returns an {@link IInstallableUnit} based on the given 
	 * description.  Once the installable unit has been created, the information is 
	 * discarded from the description object.
	 * 
	 * @param description The description of the unit to create
	 * @return The created installable unit
	 */
	public static IInstallableUnit createInstallableUnit(InstallableUnitDescription description) {
		Assert.isNotNull(description);
		return description.unitCreate();
	}

	/**
	 * Returns an {@link IInstallableUnitFragment} based on the given 
	 * description.  Once the fragment has been created, the information is 
	 * discarded from the description object.
	 * 
	 * @param description The description of the unit to create
	 * @return The created installable unit fragment
	 */
	public static IInstallableUnitFragment createInstallableUnitFragment(InstallableUnitFragmentDescription description) {
		Assert.isNotNull(description);
		return (IInstallableUnitFragment) description.unitCreate();
	}

	/**
	 * Returns an {@link IInstallableUnitPatch} based on the given 
	 * description.  Once the patch installable unit has been created, the information is 
	 * discarded from the description object.
	 * 
	 * @param description The description of the unit to create
	 * @return The created installable unit patch
	 */
	public static IInstallableUnitPatch createInstallableUnitPatch(InstallableUnitPatchDescription description) {
		Assert.isNotNull(description);
		return (IInstallableUnitPatch) description.unitCreate();
	}

	/**
	 * Returns a {@link IProvidedCapability} with the given values.
	 * 
	 * @param namespace The capability namespace
	 * @param name The capability name
	 * @param version The capability version
	 */
	public static IProvidedCapability createProvidedCapability(String namespace, String name, Version version) {
		return new ProvidedCapability(namespace, name, version);
	}

	/**
	 * Returns a {@link IRequiredCapability} with the given values.
	 * 
	 * @param namespace The capability namespace
	 * @param name The required capability name
	 * @param range The range of versions that are required, or <code>null</code>
	 * to indicate that any version will do.
	 * @param filter The filter used to evaluate whether this capability is applicable in the
	 * current environment, or <code>null</code> to indicate this capability is always applicable
	 * @param optional <code>true</code> if this required capability is optional,
	 * and <code>false</code> otherwise.
	 * @param multiple <code>true</code> if this capability can be satisfied by multiple provided capabilities, or it requires exactly one match
	 */
	public static IRequiredCapability createRequiredCapability(String namespace, String name, VersionRange range, String filter, boolean optional, boolean multiple) {
		return new RequiredCapability(namespace, name, range, filter, optional, multiple);
	}

	public static IRequiredCapability createRequiredCapability(String namespace, String name, VersionRange range, String filter, boolean optional, boolean multiple, boolean greedy) {
		return new RequiredCapability(namespace, name, range, filter, optional, multiple, greedy);
	}

	/**
	 * Returns a new requirement change.
	 * @param applyOn The source of the requirement change - the kind of requirement to apply the change to
	 * @param newValue The result of the requirement change - the requirement to replace the source requirement with
	 * @return a requirement change
	 */
	public static IRequirementChange createRequirementChange(IRequiredCapability applyOn, IRequiredCapability newValue) {
		return new RequirementChange(applyOn, newValue);
	}

	/**
	 * Returns a new {@link ICopyright}.
	 * @param location the location of a document containing the copyright notice, or <code>null</code>
	 * @param body the copyright body, cannot be <code>null</code>
	 * @throws IllegalArgumentException when the <code>body</code> is <code>null</code>
	 */
	public static ICopyright createCopyright(URI location, String body) {
		return new Copyright(location, body);
	}

	/**
	 * Return a new {@link ILicense}
	 * The body should contain either the full text of the license or an summary for a license
	 * fully specified in the given location.
	 * 
	 * @param location the location of a document containing the full license, or <code>null</code>
	 * @param body the license body, cannot be <code>null</code>
	 * @throws IllegalArgumentException when the <code>body</code> is <code>null</code>
	 */
	public static ILicense createLicense(URI location, String body) {
		return new License(location, body);
	}

	/**
	 * Returns an {@link IInstallableUnit} that represents the given
	 * unit bound to the given fragments.
	 * 
	 * @see IInstallableUnit#isResolved()
	 * @param unit The unit to be bound
	 * @param fragments The fragments to be bound
	 * @return A resolved installable unit
	 */
	public static IInstallableUnit createResolvedInstallableUnit(IInstallableUnit unit, IInstallableUnitFragment[] fragments) {
		if (unit.isResolved())
			return unit;
		Assert.isNotNull(unit);
		Assert.isNotNull(fragments);
		return new ResolvedInstallableUnit(unit, fragments);

	}

	/**
	 * Returns an instance of {@link ITouchpointData} with the given instructions.
	 * 
	 * @param instructions The instructions for the touchpoint data.
	 * @return The created touchpoint data
	 */
	public static ITouchpointData createTouchpointData(Map instructions) {
		Assert.isNotNull(instructions);
		//copy the map to protect against subsequent change by caller
		if (instructions.isEmpty())
			return EMPTY_TOUCHPOINT_DATA;

		Map result = new LinkedHashMap(instructions.size());
		for (Iterator iterator = instructions.entrySet().iterator(); iterator.hasNext();) {
			Entry entry = (Entry) iterator.next();
			Object value = entry.getValue();
			if (value == null || value instanceof String)
				value = createTouchpointInstruction((String) value, null);

			result.put(entry.getKey(), value);
		}
		return new TouchpointData(result);
	}

	public static ITouchpointInstruction createTouchpointInstruction(String body, String importAttribute) {
		return new TouchpointInstruction(body, importAttribute);
	}

	/**
	 * Returns a {@link TouchpointType} with the given id and version.
	 * 
	 * @param id The touchpoint id
	 * @param version The touchpoint version
	 * @return A touchpoint type instance with the given id and version
	 */
	public static ITouchpointType createTouchpointType(String id, Version version) {
		Assert.isNotNull(id);
		Assert.isNotNull(version);

		if (id.equals(ITouchpointType.NONE.getId()) && version.equals(ITouchpointType.NONE.getVersion()))
			return ITouchpointType.NONE;

		synchronized (typeCache) {
			ITouchpointType result = getCachedTouchpointType(id, version);
			if (result != null)
				return result;
			result = new TouchpointType(id, version);
			putCachedTouchpointType(result);
			return result;
		}
	}

	public static IUpdateDescriptor createUpdateDescriptor(String id, VersionRange range, int severity, String description) {
		return new UpdateDescriptor(id, range, severity, description);
	}

	private static ITouchpointType getCachedTouchpointType(String id, Version version) {
		for (int i = 0; i < typeCache.length; i++) {
			if (typeCache[i] != null && typeCache[i].getId().equals(id) && typeCache[i].getVersion().equals(version))
				return typeCache[i];
		}
		return null;
	}

	private static void putCachedTouchpointType(ITouchpointType result) {
		//simple rotating buffer
		typeCache[typeCacheOffset] = result;
		typeCacheOffset = (typeCacheOffset + 1) % typeCache.length;
	}
}