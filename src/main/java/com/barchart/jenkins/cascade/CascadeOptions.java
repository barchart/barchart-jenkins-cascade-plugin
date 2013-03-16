/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Cascade build options.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class CascadeOptions extends AbstractDescribableImpl<CascadeOptions> {

	public static class TheDescriptor extends Descriptor<CascadeOptions> {

		private CascadeOptions global;

		public TheDescriptor() {
			global = new CascadeOptions();
			load();
		}

		@Override
		public boolean configure(final StaplerRequest request,
				final JSONObject json) throws FormException {
			global = newInstance(request, json);
			save();
			return true;
		}

		@Override
		public String getDisplayName() {
			return "";
		}

		public CascadeOptions global() {
			return global;
		}

	}

	private static final Logger log = Logger.getLogger(CascadeOptions.class
			.getName());

	/**
	 * Perform SCM:
	 * 
	 * <pre>
	 * git pull
	 * git add pom.xml
	 * git commit -m "message"
	 * git push
	 * </pre>
	 */
	public static final String MAVEN_COMMIT_GOALS = //
	"scm:update scm:checkin \n" //
			+ "--define includes=pom.xml \n" //
			+ "--define message=[cascade-update] \n" //
	;

	/**
	 * Maven dependency version update goals.
	 */
	public static final String MAVEN_DEPENDENCY_GOALS = //
	"versions:use-latest-versions \n" //
			+ "--define generateBackupPoms=false \n" //
			+ "--define excludeReactor=false \n" //
			+ "--define allowMajorUpdates=false \n" //
			+ "--define allowMinorUpdates=false \n" //
			+ "--define allowIncrementalUpdates=true \n" //
	;

	/**
	 * Maven parent version update goals.
	 */
	public static final String MAVEN_PARENT_GOALS = //
	"versions:update-parent \n" //
			+ "--define generateBackupPoms=false \n";

	/**
	 * Perform maven release.
	 */
	public static final String MAVEN_RELEASE_GOALS = //
	"release:clean release:prepare release:perform \n"
			+ "--define localCheckout=true \n" //
			+ "--define arguments=\"--define skipTests=true\" \n" //
			+ "--define resume=false \n" //
	;

	/**
	 * Perform maven validation.
	 */
	public static final String MAVEN_VALIDATE_GOALS = //
	"validate \n" //
	;

	@Extension
	public final static TheDescriptor META = new TheDescriptor();

	/**
	 * Collect fields of this bean as given JSON object.
	 */
	public static final String NAME = "cascadeOptions";

	private String mavenCommitGoals = MAVEN_COMMIT_GOALS;
	private String mavenDependencyGoals = MAVEN_DEPENDENCY_GOALS;
	private String mavenParentGoals = MAVEN_PARENT_GOALS;
	private String mavenReleaseGoals = MAVEN_RELEASE_GOALS;
	private String mavenValidateGoals = MAVEN_VALIDATE_GOALS;

	private boolean shouldLogActions = false;
	private boolean shouldLogDependency = false;
	private boolean shouldPushUpdates = false;

	public CascadeOptions() {
	}

	/**
	 * Jelly form submit.
	 */
	@DataBoundConstructor
	public CascadeOptions(//
			final String mavenValidateGoals, //
			final String mavenParentGoals, //
			final String mavenDependencyGoals, //
			final String mavenCommitGoals, //
			final String mavenReleaseGoals, //
			final boolean shouldLogActions, //
			final boolean shouldLogDependency, //
			final boolean shouldPushUpdates //
	) {

		this.mavenValidateGoals = mavenValidateGoals;
		this.mavenParentGoals = mavenParentGoals;
		this.mavenDependencyGoals = mavenDependencyGoals;
		this.mavenCommitGoals = mavenCommitGoals;
		this.mavenReleaseGoals = mavenReleaseGoals;

		this.shouldLogActions = shouldLogActions;
		this.shouldLogDependency = shouldLogDependency;
		this.shouldPushUpdates = shouldPushUpdates;

	}

	public String getMavenCommitGoals() {
		return mavenCommitGoals;
	}

	public String getMavenDependencyGoals() {
		return mavenDependencyGoals;
	}

	public String getMavenParentGoals() {
		return mavenParentGoals;
	}

	public String getMavenReleaseGoals() {
		return mavenReleaseGoals;
	}

	public String getMavenValidateGoals() {
		return mavenValidateGoals;
	}

	public boolean getShouldLogActions() {
		return shouldLogActions;
	}

	public boolean getShouldLogDependency() {
		return shouldLogDependency;
	}

	public boolean getShouldPushUpdates() {
		return shouldPushUpdates;
	}

}
