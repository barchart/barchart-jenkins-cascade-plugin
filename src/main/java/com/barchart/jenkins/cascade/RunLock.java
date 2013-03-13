/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import java.util.EnumMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents mutual exclusion between different project type builds.
 * <p>
 * Singleton per cascade project family during the life of jenkins instance.
 * 
 * @author Andrei Pozolotin
 */
public class RunLock {

	/***
	 * [ familyID : runLock ]
	 */
	private static final ConcurrentMap<String, RunLock> lockMap = new ConcurrentHashMap<String, RunLock>();

	/**
	 * Produce existing or create new lock.
	 */
	public static RunLock ensure(final String familyID) {
		RunLock lock = lockMap.get(familyID);
		if (lock == null) {
			lock = new RunLock(familyID);
			lockMap.putIfAbsent(familyID, lock);
			lock = lockMap.get(familyID);
		}
		return lock;
	}

	/**
	 * Ensure class loading and initialization.
	 */
	public static void init() {
	}

	private final String familyID;

	/**
	 * Number of active projects of a given role.
	 */
	private final EnumMap<ProjectRole, AtomicInteger> roleCountMap = new EnumMap<ProjectRole, AtomicInteger>(
			ProjectRole.class);

	public RunLock(final String familyID) {
		this.familyID = familyID;
		for (final ProjectRole role : ProjectRole.values()) {
			roleCountMap.put(role, new AtomicInteger(0));
		}
	}

	public String familyID() {
		return familyID;
	}

	/** Check if have any running cascade projects in the family. */
	public boolean hasCascade() {
		return isActive(ProjectRole.CASCADE);
	}

	/** Check if have any running layout projects in the family. */
	public boolean hasLayout() {
		return isActive(ProjectRole.LAYOUT);
	}

	/** Check if have any running member projects in the family. */
	public boolean hasMember() {
		return isActive(ProjectRole.MEMBER);
	}

	/** Check if have running projects with the role. */
	public boolean isActive(final ProjectRole role) {
		return roleCountMap.get(role).get() > 0;
	}

	/** Change number of running projects with the role. */
	public void setActive(final ProjectRole role, final boolean on) {
		final AtomicInteger count = roleCountMap.get(role);
		if (on) {
			count.incrementAndGet();
		} else {
			count.decrementAndGet();
		}
	}

}
