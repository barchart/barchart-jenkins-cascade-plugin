/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import static com.barchart.jenkins.cascade.PluginUtilities.*;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Layout build options.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class LayoutOptions extends AbstractDescribableImpl<LayoutOptions> {

	public static class TheDescriptor extends Descriptor<LayoutOptions> {

		private LayoutOptions global;

		public TheDescriptor() {
			global = new LayoutOptions();
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

		public LayoutOptions global() {
			return global;
		}

	}

	/**
	 * <a href=
	 * "https://wiki.jenkins-ci.org/display/JENKINS/Building+a+software+project#Buildingasoftwareproject-JenkinsSetEnvironmentVariables"
	 * >Jenkins Build Environment Variables</a>
	 */
	public static final String CASCADE_PROJECT_NAME = //
	tokenVariable("JOB_NAME") + "_CASCADE"//
	;

	public static final String LAYOUT_VIEW_NAME = //
	"cascade" //
	;

	private static final Logger log = Logger.getLogger(LayoutOptions.class
			.getName());

	public static final String MAVEN_VALIDATE_GOALS = //
	"clean validate \n" //
	;

	public static final String MEMBER_PROJECT_NAME = //
	tokenVariable(MavenTokenMacro.TOKEN_ARTIFACT_ID) //
	;

	@Extension
	public final static TheDescriptor META = new TheDescriptor();

	/**
	 * Collect fields of this bean as given JSON object.
	 */
	public static final String NAME = "layoutOptions";

	private String cascadeProjectName = CASCADE_PROJECT_NAME;
	private String layoutViewName = LAYOUT_VIEW_NAME;
	private String mavenValidateGoals = MAVEN_VALIDATE_GOALS;
	private String memberProjectName = MEMBER_PROJECT_NAME;

	private boolean buildAfterLayout = false;
	private boolean useSharedWorkspace = true;

	public LayoutOptions() {
	}

	/**
	 * Jelly form submit.
	 */
	@DataBoundConstructor
	public LayoutOptions(//
			final String mavenValidateGoals, //
			final String memberProjectName, //
			final String layoutViewName, //
			final String cascadeProjectName, //
			final boolean useSharedWorkspace, //
			final boolean buildAfterLayout //
	) {
		this.mavenValidateGoals = mavenValidateGoals;
		this.memberProjectName = memberProjectName;
		this.layoutViewName = layoutViewName;
		this.cascadeProjectName = cascadeProjectName;
		this.useSharedWorkspace = useSharedWorkspace;
		this.buildAfterLayout = buildAfterLayout;
	}

	/**
	 * Build new member projects after layout.
	 */
	public boolean getBuildAfterLayout() {
		return buildAfterLayout;
	}

	/**
	 * Cascade project naming convention.
	 */
	public String getCascadeProjectName() {
		return cascadeProjectName;
	}

	/**
	 * Jenkins view name for the cascade layout. This view will contain
	 * generated cascade and member projects.
	 */
	public String getLayoutViewName() {
		return layoutViewName;
	}

	/**
	 * Maven goals to use for layout validation.
	 */
	public String getMavenValidateGoals() {
		return mavenValidateGoals;
	}

	/**
	 * Member project naming convention.
	 */
	public String getMemberProjectName() {
		return memberProjectName;
	}

	/**
	 * Layout and member projects share work space.
	 */
	public boolean getUseSharedWorkspace() {
		return useSharedWorkspace;
	}

}
