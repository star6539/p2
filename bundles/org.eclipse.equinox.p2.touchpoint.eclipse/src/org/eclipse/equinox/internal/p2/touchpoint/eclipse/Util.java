/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Incorporated - fix for bug 225145
 *     Code 9 - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.*;
import java.net.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.location.AgentLocation;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class Util {

	/**
	 * TODO "cache" is probably not the right term for this location
	 */
	private static final String REPOSITORY_TYPE = IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY;
	private static final String CACHE_EXTENSIONS = "org.eclipse.equinox.p2.cache.extensions"; //$NON-NLS-1$
	private static final String PIPE = "|"; //$NON-NLS-1$

	/**
	 * Bit-mask value representing this profile's bundle pool
	 */
	public static final int AGGREGATE_CACHE = 0x01;
	/**
	 * Bit-mask value representing the shared profile's bundle pool in a shared install
	 */
	public static final int AGGREGATE_SHARED_CACHE = 0x02;
	/**
	 * Bit-mask value representing the extension locations, such as the dropins folder.
	 */
	public static final int AGGREGATE_CACHE_EXTENSIONS = 0x04;

	public static AgentLocation getAgentLocation() {
		return (AgentLocation) ServiceHelper.getService(Activator.getContext(), AgentLocation.class.getName());
	}

	public static IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
	}

	public static URI getBundlePoolLocation(IProfile profile) {
		String path = profile.getProperty(IProfile.PROP_CACHE);
		if (path != null)
			return new File(path).toURI();
		AgentLocation location = getAgentLocation();
		if (location == null)
			return null;
		try {
			return URIUtil.toURI(location.getDataArea(Activator.ID));
		} catch (URISyntaxException e) {
			// unexpected, URLs should be pre-checked
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, e.getMessage(), e));
			throw new RuntimeException(e);
		}
	}

	public static synchronized IFileArtifactRepository getBundlePoolRepository(IProfile profile) {
		URI location = getBundlePoolLocation(profile);
		if (location == null)
			return null;
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		try {
			return (IFileArtifactRepository) manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			//the repository doesn't exist, so fall through and create a new one
		}
		try {
			String repositoryName = Messages.BundlePool;
			Map properties = new HashMap(1);
			properties.put(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
			return (IFileArtifactRepository) manager.createRepository(location, repositoryName, REPOSITORY_TYPE, properties);
		} catch (ProvisionException e) {
			LogHelper.log(e);
			throw new IllegalArgumentException(NLS.bind(Messages.bundle_pool_not_writeable, location));
		}
	}

	public static IFileArtifactRepository getAggregatedBundleRepository(IProfile profile) {
		return getAggregatedBundleRepository(profile, AGGREGATE_CACHE | AGGREGATE_SHARED_CACHE | AGGREGATE_CACHE_EXTENSIONS);
	}

	public static IFileArtifactRepository getAggregatedBundleRepository(IProfile profile, int repoFilter) {
		List bundleRepositories = new ArrayList();

		// we check for a shared bundle pool first as it should be preferred over the user bundle pool in a shared install
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		if ((repoFilter & AGGREGATE_SHARED_CACHE) != 0) {
			String sharedCache = profile.getProperty(IProfile.PROP_SHARED_CACHE);
			if (sharedCache != null) {
				try {
					URI repoLocation = new File(sharedCache).toURI();
					IArtifactRepository repository = manager.loadRepository(repoLocation, null);
					if (repository != null && repository instanceof IFileArtifactRepository && !bundleRepositories.contains(repository))
						bundleRepositories.add(repository);
				} catch (ProvisionException e) {
					//skip repository if it could not be read
				}
			}
		}

		if ((repoFilter & AGGREGATE_CACHE) != 0) {
			IFileArtifactRepository bundlePool = Util.getBundlePoolRepository(profile);
			if (bundlePool != null)
				bundleRepositories.add(bundlePool);
		}

		if ((repoFilter & AGGREGATE_CACHE_EXTENSIONS) != 0) {
			List repos = getListProfileProperty(profile, CACHE_EXTENSIONS);
			for (Iterator iterator = repos.iterator(); iterator.hasNext();) {
				try {
					String repo = (String) iterator.next();
					URI repoLocation;
					try {
						repoLocation = new URI(repo);
					} catch (URISyntaxException e) {
						//in 1.0 we wrote unencoded URL strings, so try as an unencoded string
						repoLocation = URIUtil.fromString(repo);
					}
					IArtifactRepository repository = manager.loadRepository(repoLocation, null);
					if (repository != null && repository instanceof IFileArtifactRepository && !bundleRepositories.contains(repository))
						bundleRepositories.add(repository);
				} catch (ProvisionException e) {
					//skip repositories that could not be read
				} catch (URISyntaxException e) {
					// unexpected, URLs should be pre-checked
					LogHelper.log(new Status(IStatus.ERROR, Activator.ID, e.getMessage(), e));
				}
			}
		}
		return new AggregatedBundleRepository(bundleRepositories);
	}

	private static List getListProfileProperty(IProfile profile, String key) {
		List listProperty = new ArrayList();
		String dropinRepositories = profile.getProperty(key);
		if (dropinRepositories != null) {
			StringTokenizer tokenizer = new StringTokenizer(dropinRepositories, PIPE);
			while (tokenizer.hasMoreTokens()) {
				listProperty.add(tokenizer.nextToken());
			}
		}
		return listProperty;
	}

	public static BundleInfo createBundleInfo(File bundleFile, String manifest) {
		BundleInfo bundleInfo = new BundleInfo();
		if (bundleFile != null)
			bundleInfo.setLocation(bundleFile.toURI());

		bundleInfo.setManifest(manifest);
		try {
			Map headers = ManifestElement.parseBundleManifest(new ByteArrayInputStream(manifest.getBytes()), new HashMap());
			ManifestElement[] element = ManifestElement.parseHeader("bsn", (String) headers.get(Constants.BUNDLE_SYMBOLICNAME)); //$NON-NLS-1$
			if (element == null || element.length == 0)
				return null;
			bundleInfo.setSymbolicName(element[0].getValue());

			String version = (String) headers.get(Constants.BUNDLE_VERSION);
			if (version == null)
				return null;
			// convert to a Version object first to ensure we are consistent with our version number w.r.t.
			// padding zeros at the end
			bundleInfo.setVersion(new Version(version).toString());

			String fragmentHost = (String) headers.get(Constants.FRAGMENT_HOST);
			if (fragmentHost != null)
				bundleInfo.setFragmentHost(fragmentHost.trim());

		} catch (BundleException e) {
			// unexpected
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, e.getMessage(), e));
			return null;
		} catch (IOException e) {
			// unexpected
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, e.getMessage(), e));
			return null;
		}
		return bundleInfo;
	}

	public static File getArtifactFile(IArtifactKey artifactKey, IProfile profile) {
		IFileArtifactRepository aggregatedView = getAggregatedBundleRepository(profile);
		File bundleJar = aggregatedView.getArtifactFile(artifactKey);
		return bundleJar;
	}

	public static File getConfigurationFolder(IProfile profile) {
		String config = profile.getProperty(IProfile.PROP_CONFIGURATION_FOLDER);
		if (config != null)
			return new File(config);
		return new File(getInstallFolder(profile), "configuration"); //$NON-NLS-1$
	}

	/*
	 * Do a look-up and return the OSGi install area if it is set.
	 */
	public static URL getOSGiInstallArea() {
		Location location = (Location) ServiceHelper.getService(Activator.getContext(), Location.class.getName(), Location.INSTALL_FILTER);
		if (location == null)
			return null;
		if (!location.isSet())
			return null;
		return location.getURL();
	}

	/*
	 * Helper method to return the eclipse.home location. Return
	 * null if it is unavailable.
	 */
	public static File getEclipseHome() {
		Location eclipseHome = (Location) ServiceHelper.getService(Activator.getContext(), Location.class.getName(), Location.ECLIPSE_HOME_FILTER);
		if (eclipseHome == null || !eclipseHome.isSet())
			return null;
		URL url = eclipseHome.getURL();
		if (url == null)
			return null;
		return URLUtil.toFile(url);
	}

	/**
	 * Returns the install folder for the profile, or <code>null</code>
	 * if no install folder is defined.
	 */
	public static File getInstallFolder(IProfile profile) {
		String folder = profile.getProperty(IProfile.PROP_INSTALL_FOLDER);
		return folder == null ? null : new File(folder);
	}

	public static File getLauncherPath(IProfile profile) {
		String name = profile.getProperty(EclipseTouchpoint.PROFILE_PROP_LAUNCHER_NAME);
		if (name == null || name.length() == 0)
			name = "eclipse"; //$NON-NLS-1$
		return new File(getInstallFolder(profile), getLauncherName(name, getOSFromProfile(profile)));
	}

	/**
	 * Returns the name of the Eclipse application launcher.
	 */
	private static String getLauncherName(String name, String os) {
		if (os == null) {
			EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(Activator.getContext(), EnvironmentInfo.class.getName());
			if (info != null)
				os = info.getOS();
		}

		if (os.equals(org.eclipse.osgi.service.environment.Constants.OS_WIN32)) {
			IPath path = new Path(name);
			if ("exe".equals(path.getFileExtension())) //$NON-NLS-1$
				return name;
			return name + ".exe"; //$NON-NLS-1$
		}
		if (os.equals(org.eclipse.osgi.service.environment.Constants.OS_MACOSX)) {
			IPath path = new Path(name);
			if ("app".equals(path.getFileExtension())) //$NON-NLS-1$
				return name;
			StringBuffer buffer = new StringBuffer();
			buffer.append(name.substring(0, 1).toUpperCase());
			buffer.append(name.substring(1));
			buffer.append(".app/Contents/MacOS/"); //$NON-NLS-1$
			buffer.append(name.toLowerCase());
			return buffer.toString();
		}
		return name;
	}

	private static String getOSFromProfile(IProfile profile) {
		String environments = profile.getProperty(IProfile.PROP_ENVIRONMENTS);
		if (environments == null)
			return null;
		for (StringTokenizer tokenizer = new StringTokenizer(environments, ","); tokenizer.hasMoreElements();) { //$NON-NLS-1$
			String entry = tokenizer.nextToken();
			int i = entry.indexOf('=');
			String key = entry.substring(0, i).trim();
			if (!key.equals("osgi.os")) //$NON-NLS-1$
				continue;
			return entry.substring(i + 1).trim();
		}
		return null;
	}

	public static String getManifest(ITouchpointData[] data) {
		for (int i = 0; i < data.length; i++) {
			ITouchpointInstruction manifestInstruction = data[i].getInstruction("manifest"); //$NON-NLS-1$
			if (manifestInstruction == null)
				return null;
			String manifest = manifestInstruction.getBody();
			if (manifest != null && manifest.length() > 0)
				return manifest;
		}
		return null;
	}

	public static IStatus createError(String message) {
		return createError(message, null);
	}

	public static IStatus createError(String message, Exception e) {
		return new Status(IStatus.ERROR, Activator.ID, message, e);
	}

	public static File getLauncherConfigLocation(IProfile profile) {
		String launcherConfig = profile.getProperty(IProfile.PROP_LAUNCHER_CONFIGURATION);
		return launcherConfig == null ? null : new File(launcherConfig);
	}
}