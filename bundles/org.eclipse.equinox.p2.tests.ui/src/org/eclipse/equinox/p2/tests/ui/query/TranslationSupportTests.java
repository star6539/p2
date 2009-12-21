/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - bug fixing
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.query;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.Profile;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.common.TranslationSupport;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.ICopyright;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.IQueryResult;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.IUPropertyUtils;
import org.eclipse.equinox.p2.tests.TestActivator;

/**
 * Tests for {@link IUPropertyUtils}.
 */
public class TranslationSupportTests extends AbstractQueryTest {
	public void testFeatureProperties() {
		TranslationSupport translations = new TranslationSupport();
		IMetadataRepositoryManager repoMan = (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.SERVICE_NAME);
		File site = getTestData("0.1", "/testData/metadataRepo/externalized");
		URI location = site.toURI();
		IMetadataRepository repository;
		try {
			repository = repoMan.loadRepository(location, getMonitor());
		} catch (ProvisionException e) {
			fail("1.99", e);
			return;
		}
		IQueryResult result = repository.query(new InstallableUnitQuery("test.feature.feature.group"), getMonitor());
		assertTrue("1.0", !result.isEmpty());
		IInstallableUnit unit = (IInstallableUnit) result.iterator().next();

		ICopyright copyright = translations.getCopyright(unit);
		assertEquals("1.1", "Test Copyright", copyright.getBody());
		ILicense license = translations.getLicenses(unit)[0];
		assertEquals("1.2", "Test License", license.getBody());
		//		assertEquals("1.3", "license.html", license.getURL().toExternalForm());
		String name = translations.getIUProperty(unit, IInstallableUnit.PROP_NAME);
		assertEquals("1.4", "Test Feature Name", name);
		String description = translations.getIUProperty(unit, IInstallableUnit.PROP_DESCRIPTION);
		assertEquals("1.5", "Test Description", description);
		String provider = translations.getIUProperty(unit, IInstallableUnit.PROP_PROVIDER);
		assertEquals("1.6", "Test Provider Name", provider);
	}

	public void testLocalizedLicense() throws URISyntaxException {
		SimpleProfileRegistry profileRegistry = (SimpleProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.SERVICE_NAME);
		Profile profile = (Profile) profileRegistry.getProfile(IProfileRegistry.SELF);
		profileRegistry.lockProfile(profile);
		String germanLicense = "German License";
		String canadianFRLicense = "Canadian French License";

		// Create a IU that has a license, but the license body is simply %license. This will be filled in by 
		// a fragment
		InstallableUnitDescription iuDescription = new InstallableUnitDescription();
		iuDescription.setId("some IU");
		iuDescription.setVersion(Version.createOSGi(1, 0, 0));
		iuDescription.setLicenses(new ILicense[] {MetadataFactory.createLicense(new URI("http://example.com"), "%license")});
		iuDescription.addProvidedCapabilities(Collections.singleton(MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "some IU", Version.createOSGi(1, 0, 0))));
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(iuDescription);

		// Create a bunch of fragments which spec our IU as their host
		// These fragments don't contribute language information
		for (int i = 0; i < 10; i++) {
			InstallableUnitFragmentDescription installableUnitFragmentDescription = new InstallableUnitFragmentDescription();
			installableUnitFragmentDescription.setId("fragment number: " + i);
			installableUnitFragmentDescription.setVersion(Version.createOSGi(1, 0, 0));
			installableUnitFragmentDescription.setHost(new IRequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "some IU", ANY_VERSION, null, false, false)});
			installableUnitFragmentDescription.setProperty(InstallableUnitDescription.PROP_TYPE_FRAGMENT, "true");
			IInstallableUnitFragment iuFragment = MetadataFactory.createInstallableUnitFragment(installableUnitFragmentDescription);
			profile.addInstallableUnit(iuFragment);
		}

		// Create fragment with a German license
		InstallableUnitFragmentDescription installableUnitFragmentDescription = new InstallableUnitFragmentDescription();
		IProvidedCapability providedCapability = MetadataFactory.createProvidedCapability("org.eclipse.equinox.p2.localization", "de", Version.createOSGi(1, 0, 0));
		ArrayList list = new ArrayList();
		list.add(providedCapability);
		installableUnitFragmentDescription.addProvidedCapabilities(list);
		installableUnitFragmentDescription.setId("german fragment");
		installableUnitFragmentDescription.setVersion(Version.createOSGi(1, 0, 0));
		installableUnitFragmentDescription.setHost(new IRequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "some IU", ANY_VERSION, null, false, false)});
		installableUnitFragmentDescription.setProperty(InstallableUnitDescription.PROP_TYPE_FRAGMENT, "true");
		installableUnitFragmentDescription.setProperty("de.license", germanLicense);
		IInstallableUnitFragment iuFragment = MetadataFactory.createInstallableUnitFragment(installableUnitFragmentDescription);
		profile.addInstallableUnit(iuFragment);

		// Create a French fragment with an fr_CA license
		installableUnitFragmentDescription = new InstallableUnitFragmentDescription();
		providedCapability = MetadataFactory.createProvidedCapability("org.eclipse.equinox.p2.localization", "fr", Version.createOSGi(1, 0, 0));
		list = new ArrayList();
		list.add(providedCapability);
		installableUnitFragmentDescription.addProvidedCapabilities(list);
		installableUnitFragmentDescription.setId("cnd french fragment");
		installableUnitFragmentDescription.setVersion(Version.createOSGi(1, 0, 0));
		installableUnitFragmentDescription.setHost(new IRequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "some IU", ANY_VERSION, null, false, false)});
		installableUnitFragmentDescription.setProperty(InstallableUnitDescription.PROP_TYPE_FRAGMENT, "true");
		installableUnitFragmentDescription.setProperty("fr_CA.license", canadianFRLicense);
		iuFragment = MetadataFactory.createInstallableUnitFragment(installableUnitFragmentDescription);

		profile.addInstallableUnit(iuFragment);
		profile.addInstallableUnit(iu);

		profileRegistry.updateProfile(profile);
		profileRegistry.unlockProfile(profile);
		TranslationSupport german = new TranslationSupport();
		german.setLocale(Locale.GERMAN);
		ILicense license = german.getLicenses(iu)[0];
		assertEquals("1.0", germanLicense, license.getBody());
		TranslationSupport french = new TranslationSupport();
		french.setLocale(Locale.CANADA_FRENCH);
		license = french.getLicenses(iu)[0];
		assertEquals("1.1", canadianFRLicense, license.getBody());
	}

	public void testBasicIU() {
		IInstallableUnit unit = createIU("f1");
		TranslationSupport translations = new TranslationSupport();

		assertNull("1.1", translations.getCopyright(unit));
		assertEquals("1.2", 0, translations.getLicenses(unit).length);;
		assertNull("1.3", translations.getIUProperty(unit, IInstallableUnit.PROP_NAME));
		assertNull("1.4", translations.getIUProperty(unit, IInstallableUnit.PROP_DESCRIPTION));
		assertNull("1.5", translations.getIUProperty(unit, IInstallableUnit.PROP_PROVIDER));
	}
}