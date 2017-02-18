package me.anon.grow.updater;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Tests {@link me.anon.grow.updater.CheckUpdateReceiver.Version} class methods
 */
public class VersionTest
{
	@Test
	public void testVersions()
	{
		CheckUpdateReceiver.Version version1 = CheckUpdateReceiver.Version.parse("v1.0-alpha1");
		CheckUpdateReceiver.Version version2 = CheckUpdateReceiver.Version.parse("v1.0-alpha2");
		CheckUpdateReceiver.Version version3 = CheckUpdateReceiver.Version.parse("v1.0-beta1");
		CheckUpdateReceiver.Version version4 = CheckUpdateReceiver.Version.parse("v1.0-beta2");
		CheckUpdateReceiver.Version version5 = CheckUpdateReceiver.Version.parse("v1.0");
		CheckUpdateReceiver.Version version6 = CheckUpdateReceiver.Version.parse("v1.0.1");
		CheckUpdateReceiver.Version version7 = CheckUpdateReceiver.Version.parse("v1.0.2");
		CheckUpdateReceiver.Version version8 = CheckUpdateReceiver.Version.parse("v1.1-alpha1");
		CheckUpdateReceiver.Version version9 = CheckUpdateReceiver.Version.parse("v1.1-alpha2");
		CheckUpdateReceiver.Version version10 = CheckUpdateReceiver.Version.parse("1.1");
		CheckUpdateReceiver.Version version11 = CheckUpdateReceiver.Version.parse("2.0-alpha1");
		CheckUpdateReceiver.Version version12 = CheckUpdateReceiver.Version.parse("2.0-alpha2");
		CheckUpdateReceiver.Version version13 = CheckUpdateReceiver.Version.parse("2.0-beta1");
		CheckUpdateReceiver.Version version14 = CheckUpdateReceiver.Version.parse("2.0-beta2");
		CheckUpdateReceiver.Version version15 = CheckUpdateReceiver.Version.parse("2.0");
		CheckUpdateReceiver.Version version16 = CheckUpdateReceiver.Version.parse("2.0.1");
		CheckUpdateReceiver.Version version17 = CheckUpdateReceiver.Version.parse("2.1");

		// test newers
		Assert.assertTrue(version2.newerThan(version1));
		Assert.assertTrue(version3.newerThan(version2));
		Assert.assertTrue(version4.newerThan(version3));
		Assert.assertTrue(version5.newerThan(version4));
		Assert.assertTrue(version6.newerThan(version5));
		Assert.assertTrue(version7.newerThan(version6));
		Assert.assertTrue(version8.newerThan(version7));
		Assert.assertTrue(version9.newerThan(version8));
		Assert.assertTrue(version10.newerThan(version9));
		Assert.assertTrue(version11.newerThan(version10));
		Assert.assertTrue(version12.newerThan(version11));
		Assert.assertTrue(version13.newerThan(version12));
		Assert.assertTrue(version14.newerThan(version13));
		Assert.assertTrue(version15.newerThan(version14));
		Assert.assertTrue(version16.newerThan(version15));
		Assert.assertTrue(version17.newerThan(version16));

		Assert.assertTrue(version17.newerThan(version1));
	}
}
