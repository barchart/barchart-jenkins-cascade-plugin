/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.maven.MavenArgumentInterceptorAction;
import hudson.maven.MavenModuleSetBuild;
import hudson.util.ArgumentListBuilder;

/**
 * Provides replacement arguments for maven builds.
 * 
 * @author Andrei Pozolotin
 */
public class MavenInterceptorAction extends AdapterAction implements
		MavenArgumentInterceptorAction {

	private final StringBuilder text = new StringBuilder();

	public MavenInterceptorAction() {
	}

	public MavenInterceptorAction(final String goals) {
		text.append(goals);
	}

	public String getGoalsAndOptions(final MavenModuleSetBuild build) {
		return text.toString();
	}

	/** Append a goal with separators. */
	public void append(final String goal) {
		text.append(" ");
		text.append(goal);
		text.append(" ");
	}

	public void append(final String... goalArray) {
		for (final String goal : goalArray) {
			append(goal);
		}
	}

	public ArgumentListBuilder intercept(final ArgumentListBuilder mavenargs,
			final MavenModuleSetBuild build) {
		return null;
	}

	@Override
	public String toString() {
		return MavenInterceptorAction.class.getName() + " " + text.toString();
	}

}
