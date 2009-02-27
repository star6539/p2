/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;

public class AdviceFileParser {

	private static final String QUALIFIER_SUBSTITUTION = "$qualifier$"; //$NON-NLS-1$
	private static final String VERSION_SUBSTITUTION = "$version$"; //$NON-NLS-1$

	private static final String UPDATE_DESCRIPTION = "update.description"; //$NON-NLS-1$
	private static final String UPDATE_SEVERITY = "update.severity"; //$NON-NLS-1$
	private static final String UPDATE_RANGE = "update.range"; //$NON-NLS-1$
	private static final String UPDATE_ID = "update.id"; //$NON-NLS-1$
	private static final String CLASSIFIER = "classifier"; //$NON-NLS-1$
	private static final String TOUCHPOINT_VERSION = "touchpoint.version"; //$NON-NLS-1$
	private static final String TOUCHPOINT_ID = "touchpoint.id"; //$NON-NLS-1$
	private static final String COPYRIGHT_LOCATION = "copyright.location"; //$NON-NLS-1$
	private static final String COPYRIGHT = "copyright"; //$NON-NLS-1$
	private static final String ID = "id"; //$NON-NLS-1$
	private static final String SINGLETON = "singleton"; //$NON-NLS-1$
	private static final String IMPORT = "import"; //$NON-NLS-1$
	private static final String RANGE = "range"; //$NON-NLS-1$
	private static final String FILTER = "filter"; //$NON-NLS-1$
	private static final String MULTIPLE = "multiple"; //$NON-NLS-1$
	private static final String OPTIONAL = "optional"; //$NON-NLS-1$
	private static final String GREEDY = "greedy"; //$NON-NLS-1$
	private static final String VERSION = "version"; //$NON-NLS-1$
	private static final String NAMESPACE = "namespace"; //$NON-NLS-1$
	private static final String NAME = "name"; //$NON-NLS-1$
	private static final String LOCATION = "location"; //$NON-NLS-1$
	private static final String VALUE = "value"; //$NON-NLS-1$

	private static final String UNITS_PREFIX = "units."; //$NON-NLS-1$
	private static final String INSTRUCTIONS_PREFIX = "instructions."; //$NON-NLS-1$
	private static final String REQUIRES_PREFIX = "requires."; //$NON-NLS-1$
	private static final String PROVIDES_PREFIX = "provides."; //$NON-NLS-1$
	private static final String PROPERTIES_PREFIX = "properties."; //$NON-NLS-1$
	private static final String LICENSES_PREFIX = "licenses."; //$NON-NLS-1$
	private static final String ARTIFACTS_PREFIX = "artifacts."; //$NON-NLS-1$
	private static final String HOST_REQUIREMENTS_PREFIX = "hostRequirements."; //$NON-NLS-1$

	private Properties adviceProperties = new Properties();
	private List adviceProvides = new ArrayList();
	private List adviceRequires = new ArrayList();
	private Map adviceInstructions = new HashMap();
	private List adviceOtherIUs = new ArrayList();

	private final Map advice;
	private Iterator keysIterator;
	private String current;
	//	private String hostId; not currently used
	private Version hostVersion;

	public AdviceFileParser(String id, Version version, Map advice) {
		// this.hostId = id; not currently used
		this.hostVersion = version;
		this.advice = advice;
	}

	public void parse() {
		List keys = new ArrayList(advice.keySet());
		Collections.sort(keys);

		keysIterator = keys.iterator();
		next();

		while (current != null) {
			if (current.startsWith(PROPERTIES_PREFIX))
				parseProperties(PROPERTIES_PREFIX, adviceProperties);
			else if (current.startsWith(PROVIDES_PREFIX))
				parseProvides(PROVIDES_PREFIX, adviceProvides);
			else if (current.startsWith(REQUIRES_PREFIX))
				parseRequires(REQUIRES_PREFIX, adviceRequires);
			else if (current.startsWith(INSTRUCTIONS_PREFIX))
				parseInstructions(INSTRUCTIONS_PREFIX, adviceInstructions);
			else if (current.startsWith(UNITS_PREFIX))
				parseUnits(UNITS_PREFIX, adviceOtherIUs);
			else {
				// we ignore elements we do not understand
				next();
			}
		}
	}

	private void next() {
		current = (String) (keysIterator.hasNext() ? keysIterator.next() : null);
	}

	private String currentValue() {
		return ((String) advice.get(current)).trim();
	}

	private void parseProperties(String prefix, Map properties) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex == -1)
				throw new IllegalStateException("bad token: " + current); //$NON-NLS-1$

			parseProperty(current.substring(0, dotIndex + 1), properties);
		}
	}

	private void parseProperty(String prefix, Map properties) {
		String propertyName = null;
		String propertyValue = null;
		while (current != null && current.startsWith(prefix)) {
			String token = current.substring(prefix.length());
			if (token.equals(NAME)) {
				propertyName = currentValue();
			} else if (token.equals(VALUE)) {
				propertyValue = currentValue();
			} else {
				// we ignore elements we do not understand
			}
			next();
		}

		properties.put(propertyName, propertyValue);
	}

	private void parseProvides(String prefix, List provides) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex == -1)
				throw new IllegalStateException("bad token: " + current); //$NON-NLS-1$

			parseProvided(current.substring(0, dotIndex + 1), provides);
		}
	}

	private void parseProvided(String prefix, List provides) {
		String namespace = null;
		String name = null;
		Version capabilityVersion = null;
		while (current != null && current.startsWith(prefix)) {
			String token = current.substring(prefix.length());
			if (token.equals(NAME)) {
				name = currentValue();
			} else if (token.equals(NAMESPACE)) {
				namespace = currentValue();
			} else if (token.equals(VERSION)) {
				capabilityVersion = new Version(substituteVersionAndQualifier(currentValue()));
			} else {
				// we ignore elements we do not understand
			}
			next();
		}

		IProvidedCapability capability = MetadataFactory.createProvidedCapability(namespace, name, capabilityVersion);
		provides.add(capability);
	}

	private void parseRequires(String prefix, List requires) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex == -1)
				throw new IllegalStateException("bad token: " + current); //$NON-NLS-1$

			parseRequired(current.substring(0, dotIndex + 1), requires);
		}
	}

	private void parseRequired(String prefix, List requires) {

		String namespace = null;
		String name = null;
		VersionRange range = null;
		String filter = null;
		boolean optional = false;
		boolean multiple = false;
		boolean greedy = false;

		while (current != null && current.startsWith(prefix)) {
			String token = current.substring(prefix.length());
			if (token.equals(GREEDY)) {
				greedy = Boolean.valueOf(currentValue()).booleanValue();
			} else if (token.equals(OPTIONAL)) {
				optional = Boolean.valueOf(currentValue()).booleanValue();
			} else if (token.equals(MULTIPLE)) {
				multiple = Boolean.valueOf(currentValue()).booleanValue();
			} else if (token.equals(FILTER)) {
				filter = currentValue();
			} else if (token.equals(NAME)) {
				name = currentValue();
			} else if (token.equals(NAMESPACE)) {
				namespace = currentValue();
			} else if (token.equals(RANGE)) {
				range = new VersionRange(substituteVersionAndQualifier(currentValue()));
			} else {
				// we ignore elements we do not understand
			}
			next();
		}
		IRequiredCapability capability = MetadataFactory.createRequiredCapability(namespace, name, range, filter, optional, multiple, greedy);
		requires.add(capability);
	}

	private void parseInstructions(String prefix, Map instructions) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex != -1)
				throw new IllegalStateException("bad token: " + current); //$NON-NLS-1$

			parseInstruction(current, instructions);
		}
	}

	private void parseInstruction(String prefix, Map instructions) {
		String phase = current.substring(current.lastIndexOf('.') + 1);
		String body = currentValue();
		next();

		prefix += '.';
		String importAttribute = null;
		if (current != null && current.startsWith(prefix)) {
			if (current.substring(prefix.length()).equals(IMPORT)) {
				importAttribute = currentValue();
			} else {
				// we ignore elements we do not understand
			}
			next();
		}
		ITouchpointInstruction instruction = MetadataFactory.createTouchpointInstruction(body, importAttribute);
		instructions.put(phase, instruction);
	}

	private void parseUnits(String prefix, List ius) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex == -1)
				throw new IllegalStateException("bad token: " + current + " = " + currentValue()); //$NON-NLS-1$ //$NON-NLS-2$

			parseUnit(current.substring(0, dotIndex + 1), ius);
		}
	}

	private void parseUnit(String prefix, List units) {
		String unitId = null;
		Version unitVersion = null;
		boolean unitSingleton = false;
		String unitFilter = null;
		String unitCopyright = null;
		String unitCopyrightLocation = null;
		String unitTouchpointId = null;
		Version unitTouchpointVersion = null;

		String unitUpdateId = null;
		VersionRange unitUpdateRange = null;
		int unitUpdateSeverity = 0;
		String unitUpdateDescription = null;

		List unitArtifacts = new ArrayList();
		Properties unitProperties = new Properties();
		List unitHostRequirements = new ArrayList();
		List unitProvides = new ArrayList();
		List unitRequires = new ArrayList();
		List unitLicenses = new ArrayList();
		Map unitInstructions = new HashMap();
		//		updatedescriptor ??

		while (current != null && current.startsWith(prefix)) {
			String token = current.substring(prefix.length());
			if (token.equals(ID)) {
				unitId = currentValue();
				next();
			} else if (token.equals(VERSION)) {
				unitVersion = new Version(substituteVersionAndQualifier(currentValue()));
				next();
			} else if (token.equals(SINGLETON)) {
				unitSingleton = Boolean.valueOf(currentValue()).booleanValue();
				next();
			} else if (token.equals(FILTER)) {
				unitFilter = currentValue();
				next();
			} else if (token.equals(COPYRIGHT)) {
				unitCopyright = currentValue();
				next();
			} else if (token.equals(COPYRIGHT_LOCATION)) {
				unitCopyrightLocation = currentValue();
				next();
			} else if (token.equals(TOUCHPOINT_ID)) {
				unitTouchpointId = currentValue();
				next();
			} else if (token.equals(TOUCHPOINT_VERSION)) {
				unitTouchpointVersion = new Version(substituteVersionAndQualifier(currentValue()));
				next();
			} else if (token.equals(UPDATE_ID)) {
				unitUpdateId = currentValue();
				next();
			} else if (token.equals(UPDATE_RANGE)) {
				unitUpdateRange = new VersionRange(substituteVersionAndQualifier(currentValue()));
				next();
			} else if (token.equals(UPDATE_SEVERITY)) {
				unitUpdateSeverity = Integer.parseInt(currentValue());
				next();
			} else if (token.equals(UPDATE_DESCRIPTION)) {
				unitUpdateDescription = currentValue();
				next();
			} else if (token.startsWith(HOST_REQUIREMENTS_PREFIX))
				parseHostRequirements(prefix + HOST_REQUIREMENTS_PREFIX, unitHostRequirements);
			else if (token.startsWith(ARTIFACTS_PREFIX))
				parseArtifacts(prefix + ARTIFACTS_PREFIX, unitArtifacts);
			else if (token.startsWith(LICENSES_PREFIX))
				parseLicenses(prefix + LICENSES_PREFIX, unitLicenses);
			else if (token.startsWith(PROPERTIES_PREFIX))
				parseProperties(prefix + PROPERTIES_PREFIX, unitProperties);
			else if (token.startsWith(PROVIDES_PREFIX))
				parseProvides(prefix + PROVIDES_PREFIX, unitProvides);
			else if (token.startsWith(REQUIRES_PREFIX))
				parseRequires(prefix + REQUIRES_PREFIX, unitRequires);
			else if (token.startsWith(INSTRUCTIONS_PREFIX))
				parseInstructions(prefix + INSTRUCTIONS_PREFIX, unitInstructions);
			else {
				// we ignore elements we do not understand
				next();
			}
		}

		InstallableUnitDescription description = unitHostRequirements.isEmpty() ? new InstallableUnitDescription() : new InstallableUnitFragmentDescription();
		description.setId(unitId);
		description.setVersion(unitVersion);
		description.setSingleton(unitSingleton);
		description.setFilter(unitFilter);
		if (unitCopyright != null || unitCopyrightLocation != null) {
			try {
				URI uri = unitCopyrightLocation != null ? new URI(unitCopyrightLocation) : null;
				description.setCopyright(MetadataFactory.createCopyright(uri, unitCopyright));
			} catch (URISyntaxException e) {
				throw new IllegalStateException("bad copyright URI at token: " + current + ", " + currentValue()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		if (unitTouchpointId != null)
			description.setTouchpointType(MetadataFactory.createTouchpointType(unitTouchpointId, unitTouchpointVersion));

		if (unitUpdateId != null)
			description.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(unitUpdateId, unitUpdateRange, unitUpdateSeverity, unitUpdateDescription));

		if (!unitLicenses.isEmpty())
			description.setLicense((ILicense) unitLicenses.get(0));

		if (!unitArtifacts.isEmpty())
			description.setArtifacts((IArtifactKey[]) unitArtifacts.toArray(new IArtifactKey[unitArtifacts.size()]));

		if (!unitHostRequirements.isEmpty())
			((InstallableUnitFragmentDescription) description).setHost((IRequiredCapability[]) unitHostRequirements.toArray(new IRequiredCapability[unitHostRequirements.size()]));

		if (!unitProperties.isEmpty()) {
			for (Iterator iterator = unitProperties.entrySet().iterator(); iterator.hasNext();) {
				Entry entry = (Entry) iterator.next();
				description.setProperty((String) entry.getKey(), (String) entry.getValue());
			}
		}

		if (!unitProvides.isEmpty())
			description.setCapabilities((IProvidedCapability[]) unitProvides.toArray(new IProvidedCapability[unitProvides.size()]));

		if (!unitRequires.isEmpty())
			description.setRequiredCapabilities((IRequiredCapability[]) unitRequires.toArray(new IRequiredCapability[unitRequires.size()]));

		if (!unitInstructions.isEmpty())
			description.addTouchpointData(MetadataFactory.createTouchpointData(unitInstructions));

		adviceOtherIUs.add(description);
	}

	private void parseLicenses(String prefix, List licenses) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex != -1)
				throw new IllegalStateException("bad token: " + current + " = " + currentValue()); //$NON-NLS-1$ //$NON-NLS-2$

			parseLicense(current, licenses);
		}
	}

	private void parseLicense(String prefix, List licenses) {
		String body = currentValue();
		next();

		prefix += '.';
		String location = null;
		if (current != null && current.startsWith(prefix)) {
			if (current.substring(prefix.length()).equals(LOCATION)) {
				location = currentValue();
			} else {
				// we ignore elements we do not understand
			}
			next();
		}

		try {
			URI uri = location != null ? new URI(location) : null;
			ILicense license = MetadataFactory.createLicense(uri, body);
			licenses.add(license);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("bad license URI at token: " + current + ", " + currentValue()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void parseArtifacts(String prefix, List artifacts) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex == -1)
				throw new IllegalStateException("bad token: " + current + " = " + currentValue()); //$NON-NLS-1$ //$NON-NLS-2$

			parseArtifact(current.substring(0, dotIndex + 1), artifacts);
		}
	}

	private void parseArtifact(String prefix, List artifacts) {
		String artifactClassifier = null;
		String artifactId = null;
		Version artifactVersion = null;
		while (current != null && current.startsWith(prefix)) {
			String token = current.substring(prefix.length());
			if (token.equals(CLASSIFIER)) {
				artifactClassifier = currentValue();
			} else if (token.equals(ID)) {
				artifactId = currentValue();
			} else if (token.equals(VERSION)) {
				artifactVersion = new Version(substituteVersionAndQualifier(currentValue()));
			} else {
				// we ignore elements we do not understand
			}

			next();
		}
		IArtifactKey artifactKey = new ArtifactKey(artifactClassifier, artifactId, artifactVersion);
		artifacts.add(artifactKey);
	}

	private void parseHostRequirements(String prefix, List hostRequirements) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex == -1)
				throw new IllegalStateException("bad token: " + current + " = " + currentValue()); //$NON-NLS-1$ //$NON-NLS-2$

			parseRequired(current.substring(0, dotIndex + 1), hostRequirements);
		}
	}

	private String substituteVersionAndQualifier(String version) {
		if (version.indexOf(VERSION_SUBSTITUTION) != -1) {
			version = replace(version, VERSION_SUBSTITUTION, hostVersion.toString());
		}

		if (version.indexOf(QUALIFIER_SUBSTITUTION) != -1) {
			String qualifier = hostVersion.getQualifier();
			if (qualifier == null)
				qualifier = ""; //$NON-NLS-1$
			if (qualifier.length() == 0) {
				// Note: this works only for OSGi versions and version ranges
				// where the qualifier if present must be at the end of a version string
				version = replace(version, "." + QUALIFIER_SUBSTITUTION, ""); //$NON-NLS-1$ //$NON-NLS-2$
			}
			version = replace(version, QUALIFIER_SUBSTITUTION, qualifier);
		}
		return version;
	}

	// originally from org.eclipse.core.internal.net.StringUtil
	public static String replace(String source, String from, String to) {
		if (from.length() == 0)
			return source;
		StringBuffer buffer = new StringBuffer();
		int current = 0;
		int pos = 0;
		while (pos != -1) {
			pos = source.indexOf(from, current);
			if (pos == -1) {
				buffer.append(source.substring(current));
			} else {
				buffer.append(source.substring(current, pos));
				buffer.append(to);
				current = pos + from.length();
			}
		}
		return buffer.toString();
	}

	public Properties getProperties() {
		if (adviceProperties.isEmpty())
			return null;
		return adviceProperties;
	}

	public IRequiredCapability[] getRequiredCapabilities() {
		if (adviceRequires.isEmpty())
			return null;

		return (IRequiredCapability[]) adviceRequires.toArray(new IRequiredCapability[adviceRequires.size()]);
	}

	public IProvidedCapability[] getProvidedCapabilities() {
		if (adviceProvides.isEmpty())
			return null;

		return (IProvidedCapability[]) adviceProvides.toArray(new IProvidedCapability[adviceProvides.size()]);
	}

	public Map getTouchpointInstructions() {
		if (adviceInstructions.isEmpty())
			return null;

		return adviceInstructions;
	}

	public InstallableUnitDescription[] getAdditionalInstallableUnitDescriptions() {
		if (adviceOtherIUs.isEmpty())
			return null;

		return (InstallableUnitDescription[]) adviceOtherIUs.toArray(new InstallableUnitDescription[adviceOtherIUs.size()]);
	}
}