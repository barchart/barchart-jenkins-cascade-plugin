/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import static com.barchart.jenkins.cascade.MavenTokenMacro.*;
import static com.barchart.jenkins.cascade.PluginUtilities.*;
import hudson.Util;
import hudson.maven.ModuleName;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.model.TopLevelItem;
import hudson.model.Descriptor;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.scm.SubversionSCM;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;
import hudson.util.VariableResolver;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jenkins.model.Jenkins;

import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.joda.time.DateTime;

import com.barchart.jenkins.cascade.PluginUtilities.JenkinsTask;

/**
 * Layout build logic.
 * 
 * @author Andrei Pozolotin
 */
public class LayoutLogic {

	/**
	 * Provide cascade project name.
	 */
	public static String cascadeName(final BuildContext context,
			final MavenModuleSet layoutProject) throws IOException {

		final LayoutBuildWrapper wrapper = layoutProject.getBuildWrappersList()
				.get(LayoutBuildWrapper.class);

		final String cascadePattern = wrapper.getCascadePattern();

		try {

			final String cascadeName = TokenMacro.expandAll(context.build(),
					context.listener(), cascadePattern);

			return cascadeName;

		} catch (final Exception e) {
			throw new IOException(e);
		}

	}

	/**
	 * Provide member project name.
	 */
	public static String memberName(final BuildContext context,
			final MavenModuleSet layoutProject, final MavenModule module)
			throws IOException {

		final LayoutBuildWrapper wrapper = layoutProject.getBuildWrappersList()
				.get(LayoutBuildWrapper.class);

		final ModuleName moduleName = module.getModuleName();

		final Map<String, String> moduleTokens = new HashMap<String, String>();
		moduleTokens.put(TOKEN_PROJECT_ID, moduleName.toString());
		moduleTokens.put(TOKEN_GROUP_ID, moduleName.groupId);
		moduleTokens.put(TOKEN_ARTIFACT_ID, moduleName.artifactId);

		final VariableResolver<String> moduleResolver = new VariableResolver.ByMap<String>(
				moduleTokens);

		final VariableResolver<String> buildResolver = context.build()
				.getBuildVariableResolver();

		@SuppressWarnings("unchecked")
		final VariableResolver<String> resolver = new VariableResolver.Union<String>(
				moduleResolver, buildResolver);

		final String memberPattern = wrapper.getMemberPattern();

		final String memberName = Util.replaceMacro(memberPattern, resolver);

		return memberName;

	}

	/**
	 * Process layout build action.
	 */
	public static boolean process(final BuildContext context)
			throws IOException {

		final Jenkins jenkins = Jenkins.getInstance();

		final MavenModuleSet layoutProject = mavenModuleSet(context.build());

		final LayoutArgumentsAction action = context.build().getAction(
				LayoutArgumentsAction.class);

		processCascade(context, layoutProject, action);

		final Collection<MavenModule> moduleList = layoutProject.getModules();

		for (final MavenModule module : moduleList) {

			final ModuleName moduleName = module.getModuleName();

			/**
			 * Module-to-Project naming convention.
			 */
			final String memberName = memberName(context, layoutProject, module);

			context.log("---");
			context.log("Module name: " + moduleName);
			context.log("Project name: " + memberName);

			if (isSameModuleName(layoutProject.getRootModule(), module)) {
				context.log("This is a root module project, managed by user, skip.");
				continue;
			}

			final JenkinsTask projectCreate = new JenkinsTask() {
				public void run() throws IOException {
					if (isProjectExists(memberName)) {
						context.log("Project exists, create skipped: "
								+ memberName);
					} else {
						context.log("Creating project: " + memberName);

						/** Clone project via XML. */
						final TopLevelItem item = jenkins.copy(
								(TopLevelItem) layoutProject, memberName);

						final MavenModuleSet memberProject = (MavenModuleSet) item;

						processMember(context, module, memberProject,
								layoutProject);

						context.log("Project created: " + memberName);
					}
				}
			};

			final JenkinsTask projectDelete = new JenkinsTask() {
				public void run() throws IOException {
					if (!isProjectExists(memberName)) {
						context.log("Project not present, delete skipped: "
								+ memberName);
					} else {
						final TopLevelItem item = jenkins.getItem(memberName);
						context.log("Deleting project : " + memberName);
						try {
							item.delete();
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}
						context.log("Project deleted: " + memberName);
					}
				}
			};

			switch (action.getConfigAction()) {
			default:
				context.log("Unexpected config action, ignore: "
						+ action.getConfigAction());
				break;
			case CREATE:
				projectCreate.run();
				break;
			case DELETE:
				projectDelete.run();
				break;
			case UPDATE:
				projectDelete.run();
				projectCreate.run();
				break;
			}

		}

		return true;
	}

	/** Handle cascade project. */
	public static void processCascade(final BuildContext context,
			final MavenModuleSet layoutProject,
			final LayoutArgumentsAction action) throws IOException {

		final Jenkins jenkins = Jenkins.getInstance();

		final String layoutName = layoutProject.getName();
		final String cascadeName = cascadeName(context, layoutProject);

		context.log("---");
		context.log("Root project: " + layoutName);
		context.log("Cascade project: " + cascadeName);

		final MemberProjectProperty layoutProperty = new MemberProjectProperty(
				ProjectRole.LAYOUT.code(), //
				cascadeName, //
				layoutName, //
				"" //
		);

		final JenkinsTask projectCreate = new JenkinsTask() {
			public void run() throws IOException {
				if (isProjectExists(cascadeName)) {
					context.log("Cascade project exist, skip create.");
				} else {
					context.log("Creating cascade project.");
					final CascadeProject cascadeProject = jenkins
							.createProject(CascadeProject.class, cascadeName);
					ensureProperty(layoutProject, layoutProperty);
					/** Provide description */
					{
						final StringBuilder text = new StringBuilder();
						text.append("Generated on:");
						text.append("<br>\n");
						text.append("<b>");
						text.append(new DateTime());
						text.append("</b>");
						text.append("<p>\n");
						text.append("Cascade layout project:");
						text.append("<br>\n");
						text.append("<b>");
						text.append(layoutProject.getName());
						text.append("</b>");
						text.append("<p>\n");
						cascadeProject.setDescription(text.toString());
					}
					/** Persist project. */
					{
						cascadeProject.save();
					}
					context.log("Cascade project created.");
				}
			}
		};
		final JenkinsTask projectDelete = new JenkinsTask() {
			public void run() throws IOException {
				if (!isProjectExists(cascadeName)) {
					context.log("Cascade project missing, skip delete.");
				} else {
					context.log("Deleting cascade project.");
					final TopLevelItem cascadeProject = jenkins
							.getItem(cascadeName);
					try {
						cascadeProject.delete();
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}
					context.log("Cascade project deleted.");
				}
			}
		};

		switch (action.getConfigAction()) {
		default:
			context.log("Unexpected config action, ignore: "
					+ action.getConfigAction());
			break;
		case CREATE:
			projectCreate.run();
			break;
		case DELETE:
			projectDelete.run();
			break;
		case UPDATE:
			projectDelete.run();
			projectCreate.run();
			break;
		}

	}

	/**
	 * Update details of created member project.
	 */
	public static void processMember(final BuildContext context,
			final MavenModule module, final MavenModuleSet memberProject,
			final MavenModuleSet layoutProject) throws IOException {

		/** Update SCM paths. */
		SCM: {

			final SCM scm = memberProject.getScm();

			if (scm instanceof GitSCM) {

				final GitSCM gitScm = (GitSCM) scm;

				final String includedRegions = module.getRelativePath() + "/.*";

				changeField(gitScm, "includedRegions", includedRegions);

				break SCM;

			}

			if (scm instanceof SubversionSCM) {

				final SubversionSCM svnScm = (SubversionSCM) scm;

				/** TODO */

			}

			context.log("###################################");
			context.log("WARNING: YOU ARE USING UNTESTED SCM");
			context.log("###################################");
		}

		/** Update Maven paths. */
		{
			final String rootPOM = module.getRelativePath() + "/pom.xml";

			memberProject.setRootPOM(rootPOM);

		}

		/** Disable cascade layout action. */
		{
			final DescribableList<BuildWrapper, Descriptor<BuildWrapper>> buildWrapperList = memberProject
					.getBuildWrappersList();

			buildWrapperList.remove(LayoutBuildWrapper.class);
		}

		/** Enable cascade release action. */
		{

			final MemberProjectProperty memberProperty = new MemberProjectProperty(
					ProjectRole.MEMBER.code(), //
					cascadeName(context, layoutProject), //
					layoutProject.getName(), //
					memberProject.getName() //
			);

			ensureProperty(memberProject, memberProperty);

		}

		/** Provide description. */
		{
			final StringBuilder text = new StringBuilder();
			text.append("Generated on:");
			text.append("<br>\n");
			text.append("<b>");
			text.append(new DateTime());
			text.append("</b>");
			text.append("<p>\n");
			text.append("Cascade layout project:");
			text.append("<br>\n");
			text.append("<b>");
			text.append(layoutProject.getName());
			text.append("</b>");
			text.append("<p>\n");
			text.append("Cascade Member project:");
			text.append("<br>\n");
			text.append("<b>");
			text.append(memberProject.getName());
			text.append("</b>");
			text.append("<p>\n");
			memberProject.setDescription(text.toString());
		}

		/** Persist changes. */
		{
			memberProject.save();
		}

	}

	private LayoutLogic() {

	}

}
