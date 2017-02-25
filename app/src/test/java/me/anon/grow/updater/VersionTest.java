package me.anon.grow.updater;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Tests {@link me.anon.grow.updater.CheckUpdateReceiver.Version} class methods
 */
public class VersionTest
{
	ArrayList<CheckUpdateReceiver.Version> versions;

	@Test
	public void testVersions()
	{
		CheckUpdateReceiver.Version version2_2_1 = CheckUpdateReceiver.Version.parse("2.2.1");
		CheckUpdateReceiver.Version version2_2_0 = CheckUpdateReceiver.Version.parse("2.2.0");
		CheckUpdateReceiver.Version version2_2 = CheckUpdateReceiver.Version.parse("2.2");
		CheckUpdateReceiver.Version version2_2_beta1 = CheckUpdateReceiver.Version.parse("2.2-beta1");
		CheckUpdateReceiver.Version version2_2_alpha2 = CheckUpdateReceiver.Version.parse("2.2-alpha2");
		CheckUpdateReceiver.Version version2_2_alpha1 = CheckUpdateReceiver.Version.parse("2.2-alpha1");
		CheckUpdateReceiver.Version version2_1 = CheckUpdateReceiver.Version.parse("2.1");
		CheckUpdateReceiver.Version version2_0 = CheckUpdateReceiver.Version.parse("2.0");
		CheckUpdateReceiver.Version version1_4 = CheckUpdateReceiver.Version.parse("1.4");
		CheckUpdateReceiver.Version version1_3 = CheckUpdateReceiver.Version.parse("1.3");
		CheckUpdateReceiver.Version version1_2 = CheckUpdateReceiver.Version.parse("1.2");

		versions = new ArrayList<>();
		versions.add(version2_2_1);
		versions.add(version2_2_0);
		versions.add(version2_2);
		versions.add(version2_2_beta1);
		versions.add(version2_2_alpha2);
		versions.add(version2_2_alpha1);
		versions.add(version2_1);
		versions.add(version2_0);
		versions.add(version1_4);
		versions.add(version1_3);
		versions.add(version1_2);

		Assert.assertFalse(version2_2.equals(version2_2_0));
		Assert.assertFalse(version2_2.equals(version2_1));
		Assert.assertFalse(version2_2.equals(version2_2_alpha1));
		Assert.assertFalse(version2_2.equals(version1_2));
		Assert.assertFalse(version2_2.equals(version1_3));

		Collections.shuffle(versions);
		Collections.sort(versions, new Comparator<CheckUpdateReceiver.Version>()
		{
			@Override public int compare(CheckUpdateReceiver.Version o1, CheckUpdateReceiver.Version o2)
			{
				if (o2.newerThan(o1)) return 1;
				if (o1.newerThan(o2)) return -1;

				return 0;
			}
		});

		Assert.assertTrue(version2_2_alpha2.newerThan(version2_2_alpha1));
		Assert.assertEquals(version2_2_1, versions.get(0));
		Assert.assertEquals(version2_2, versions.get(1));
		Assert.assertEquals(version2_2_0, versions.get(2));
		Assert.assertEquals(version2_2_beta1, versions.get(3));
		Assert.assertEquals(version2_2_alpha2, versions.get(4));
		Assert.assertEquals(version2_2_alpha1, versions.get(5));
		Assert.assertEquals(version2_1, versions.get(6));
		Assert.assertEquals(version2_0, versions.get(7));
		Assert.assertEquals(version1_4, versions.get(8));
		Assert.assertEquals(version1_3, versions.get(9));
		Assert.assertEquals(version1_2, versions.get(10));

		Assert.assertFalse(version2_0.newerThan(version2_2_alpha1));
		Assert.assertFalse(version1_2.newerThan(version2_2));
		Assert.assertTrue(version2_2_1.newerThan(version1_2));
		Assert.assertFalse(version1_4.newerThan(version2_1));
		Assert.assertTrue(version1_4.newerThan(version1_3));
		Assert.assertTrue(version2_1.newerThan(version1_3));
		Assert.assertTrue(version2_1.newerThan(version1_4));
		Assert.assertTrue(version2_2_0.newerThan(version2_1));
		Assert.assertTrue(version2_2_1.newerThan(version2_2));
		Assert.assertTrue(version2_2_alpha1.newerThan(version2_1));
		Assert.assertTrue(version2_2.newerThan(version2_2_alpha1));
		Assert.assertTrue(version2_2.newerThan(version2_2_alpha1));
		Assert.assertTrue(version2_2_alpha2.newerThan(version2_2_alpha1));
		Assert.assertTrue(version2_2_beta1.newerThan(version2_2_alpha2));
		Assert.assertFalse(version2_2_alpha2.newerThan(version2_2_beta1));
	}
}
