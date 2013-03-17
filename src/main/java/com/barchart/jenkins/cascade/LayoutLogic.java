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
import hudson.FilePath;
import hudson.Util;
import hudson.maven.ModuleName;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.Action;
import hudson.model.TopLevelItem;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.ListView;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.scm.SubversionSCM;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;
import hudson.util.VariableResolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jenkins.model.Jenkins;
import jenkins.scm.SCMCheckoutStrategy;

import org.apache.maven.model.Model;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.joda.time.DateTime;

import com.barchart.jenkins.cascade.PluginUtilities.JenkinsTask;
import com.barchart.jenkins.cascade.ProjectIdentity.Mode;

/**
 * Layout build logic.
 * 
 * @author Andrei Pozolotin
 */
public class LayoutLogic {

	/**
	 * Generate cascade project name.
	 */
	public static String cascadeName(
			final BuildContext<MavenModuleSetBuild> context,
			final MavenModuleSet layoutProject) throws IOException {

		final LayoutBuildWrapper wrapper = layoutProject.getBuildWrappersList()
				.get(LayoutBuildWrapper.class);

		final String cascadePattern = wrapper.getLayoutOptions()
				.getCascadeProjectName();

		try {

			final String cascadeName = TokenMacro.expandAll(context.build(),
					context.listener(), cascadePattern);

			return cascadeName;

		} catch (final Exception e) {
			throw new IOException(e);
		}

	}

	/**
	 * Verify plug-in maven module nesting convention:
	 * <p>
	 * 1) Layout project must have modules.
	 * <p>
	 * 2) Do not permit modules for member projects.
	 */
	public static boolean checkModuleNesting(
			final BuildContext<MavenModuleSetBuild> context,
			final MavenModuleSet layoutProject) throws IOException {

		final Model layoutModel = mavenModel(layoutProject);

		if (layoutModel.getModules().isEmpty()) {
			context.logErr("Layout project has no modules: " + layoutModel);
			context.logErr("Cascade member projects must be defined in layout project as <module/> entries.");
			return false;
		}

		/** Layout project wokrspace. */
		final FilePath workspace = context.build().getWorkspace();

		final MavenModule layoutModule = layoutProject.getRootModule();

		/** Topologically sorted list of modules. */
		final List<MavenModule> moduleList = layoutProject
				.getDisabledModules(false);

		for (final MavenModule module : moduleList) {
			if (isSameModuleName(layoutModule, module)) {
				/** Layout project module */
				continue;
			} else {
				/** Member project module */
				final String modulePath = module.getRelativePath();
				final String moduleFolder = workspace.child(modulePath)
						.getRemote();
				final File pomFile = new File(moduleFolder, "pom.xml");
				if (!pomFile.exists()) {
					context.logErr("Project pom.xml is missing: " + pomFile);
					return false;
				}
				final Model moduleModel = mavenModel(pomFile);
				if (moduleModel.getModules().isEmpty()) {
					continue;
				}
				context.logErr("Project contains <module/>: " + moduleModel);
				context.logErr("Cascade member projects must not be using  <module/> entries.");
				return false;
			}
		}

		return true;

	}

	/**
	 * Create view if missing and add project to the view.
	 */
	public static void ensureProjectView(
			final BuildContext<MavenModuleSetBuild> context,
			final TopLevelItem project) throws IOException {

		final String viewName = context.layoutOptions().getLayoutViewName();

		final ListView view = ensureListView(viewName);

		view.add(project);

		context.logTab("Project view: " + view.getAbsoluteUrl());

	}

	/**
	 * Update maven and jenkins metadata.
	 */
	public static List<Action> mavenValidateGoals(
			final BuildContext<MavenModuleSetBuild> context,
			final String... options) {
		final LayoutOptions layoutOptions = new LayoutOptions();
		final MavenGoalsIntercept goals = new MavenGoalsIntercept();
		goals.append(layoutOptions.getMavenValidateGoals());
		goals.append(options);
		final List<Action> list = new ArrayList<Action>();
		list.add(new DoLayoutBadge());
		list.add(new DoValidateBadge());
		list.add(goals);
		return list;
	}

	/**
	 * Generate member project name.
	 */
	public static String memberName(
			final BuildContext<MavenModuleSetBuild> context,
			final MavenModuleSet layoutProject, final MavenModule module)
			throws IOException {

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

		final String memberPattern = context.layoutOptions()
				.getMemberProjectName();

		final String memberName = Util.replaceMacro(memberPattern, resolver);

		return memberName;

	}

	/**
	 * Layout build entry point.
	 */
	public static boolean process(
			final BuildContext<MavenModuleSetBuild> context) throws IOException {

		final MavenModuleSet layoutProject = mavenProject(context.build());

		final LayoutArgumentsAction action = context.build().getAction(
				LayoutArgumentsAction.class);

		final ProjectIdentity layoutIdentity = ProjectIdentity
				.ensureLayoutIdentity(layoutProject);

		final String layoutName = layoutProject.getName();

		context.log("");
		context.log("Layout action: " + action);
		context.log("Layout project: " + layoutName);
		context.logTab("Project identity: " + layoutIdentity);

		if (!checkModuleNesting(context, layoutProject)) {
			return false;
		}

		ensureProjectView(context, layoutProject);

		processCascade(context, layoutProject, action);

		processMember(context, layoutProject, action);

		return true;
	}

	/**
	 * Handle cascade project create/update/delete.
	 */
	public static void processCascade( //
			final BuildContext<MavenModuleSetBuild> context, //
			final MavenModuleSet layoutProject, //
			final LayoutArgumentsAction action //
	) throws IOException {

		final Jenkins jenkins = Jenkins.getInstance();

		final String layoutName = layoutProject.getName();
		final String cascadeName = cascadeName(context, layoutProject);

		context.log("");
		context.log("Layout project: " + layoutName);
		context.log("Cascade project: " + cascadeName);

		/**
		 * Create using name as distinction.
		 */
		final JenkinsTask projectCreate = new JenkinsTask() {
			public void run() throws IOException {
				if (isProjectExists(cascadeName)) {

					context.logErr("Cascade project exist, skip create.");

				} else {

					context.logTab("Creating cascade project.");

					final CascadeProject cascadeProject = jenkins
							.createProject(CascadeProject.class, cascadeName);

					final ProjectIdentity cascadeIdentity = ProjectIdentity
							.ensureCascadeIdentity(layoutProject,
									cascadeProject);

					context.logTab("Project identity: " + cascadeIdentity);

					context.logTab("Provide description.");
					{
						final StringBuilder text = new StringBuilder();
						text.append("Generated on:");
						text.append("<br>\n");
						text.append("<b>");
						text.append(new DateTime());
						text.append("</b>");
						text.append("<p>\n");
						cascadeProject.setDescription(text.toString());
					}

					context.logTab("Persist project.");
					{
						cascadeProject.save();
					}

					ensureProjectView(context, cascadeProject);

					context.logTab("Project created.");

				}
			}
		};

		/**
		 * Delete using identity as distinction.
		 */
		final JenkinsTask projectDelete = new JenkinsTask() {
			public void run() throws IOException {

				final ProjectRole role = ProjectRole.CASCADE;
				final String cascadeID = ProjectIdentity
						.familyID(layoutProject);
				final String projectID = "unused";

				final AbstractProject cascadeProject = ProjectIdentity
						.abstractProject(role, cascadeID, projectID,
								Mode.ROLE_FAMILY);

				if (cascadeProject == null) {

					context.logErr("Cascade project missing, skip delete.");

				} else {

					context.logTab("Project identity: "
							+ ProjectIdentity.identity(cascadeProject));

					context.logTab("Deleting cascade project.");

					try {
						cascadeProject.delete();
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}

					context.logTab("Project deleted.");

				}
			}
		};

		switch (action.getConfigAction()) {
		default:
			context.logErr("Unexpected config action, ignore: "
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
	 * Handle member projects create/update/delete.
	 */
	public static boolean processMember(//
			final BuildContext<MavenModuleSetBuild> context,//
			final MavenModuleSet layoutProject,//
			final LayoutArgumentsAction action //
	) throws IOException {

		switch (action.getConfigAction()) {
		default:
			context.logErr("Unexpected config action, ignore: "
					+ action.getConfigAction());
			break;
		case CREATE:
			processMemberCreate(context, layoutProject, action);
			break;
		case DELETE:
			processMemberDelete(context, layoutProject, action);
			break;
		case UPDATE:
			processMemberDelete(context, layoutProject, action);
			processMemberCreate(context, layoutProject, action);
			break;
		}

		return true;

	}

	/**
	 * Update details of created member project.
	 */
	public static void processMemberCreate(//
			final BuildContext<MavenModuleSetBuild> context,//
			final MavenModule module, //
			final MavenModuleSet memberProject, //
			final MavenModuleSet layoutProject//
	) throws IOException {

		context.logTab("Update SCM paths.");
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

			throw new IllegalStateException("Unsupported SCM");

		}

		context.logTab("Update Maven paths.");
		{
			/** Member project is nested in the layout project. */
			final String rootPOM = module.getRelativePath() + "/pom.xml";
			memberProject.setRootPOM(rootPOM);

			if (context.layoutOptions().getUseSharedWorkspace()) {
				final String nodeRoot = Computer.currentComputer().getNode()
						.getRootPath().getRemote();
				final String layoutWorkspace = context.build().getWorkspace()
						.getRemote();
				final String memberWorkspace = relativePath(nodeRoot,
						layoutWorkspace);
				memberProject.setCustomWorkspace(memberWorkspace);
				context.logTab("Member is sharing workspace with layout.");
			} else {
				context.logTab("Member is using its own private workspace.");
			}
		}

		context.logTab("Configure build wrappers.");
		{
			final DescribableList<BuildWrapper, Descriptor<BuildWrapper>> buildWrapperList = memberProject
					.getBuildWrappersList();

			buildWrapperList.remove(LayoutBuildWrapper.class);

			// BuildWrapper item = null;
			// buildWrapperList.add(item);
		}

		context.logTab("Ensure project identity.");
		{
			final ProjectIdentity memberdentity = ProjectIdentity
					.ensureMemberIdentity(layoutProject, memberProject);

			context.logTab("identity: " + memberdentity);
		}

		context.logTab("Provide project description.");
		{
			final StringBuilder text = new StringBuilder();
			text.append("Generated on:");
			text.append("<br>\n");
			text.append("<b>");
			text.append(new DateTime());
			text.append("</b>");
			text.append("<p>\n");
			memberProject.setDescription(text.toString());
		}

		context.logTab("Use custom checkout strategy.");
		{
			final SCMCheckoutStrategy strategy = new CheckoutStrategySCM();
			memberProject.setScmCheckoutStrategy(strategy);
		}

		context.logTab("Persist project changes.");
		{
			memberProject.save();
		}

	}

	/**
	 * Create using name as distinction.
	 */
	public static boolean processMemberCreate(//
			final BuildContext<MavenModuleSetBuild> context,//
			final MavenModuleSet layoutProject,//
			final LayoutArgumentsAction action //
	) throws IOException {

		final Jenkins jenkins = Jenkins.getInstance();

		/** Topologically sorted list of modules. */
		final List<MavenModule> moduleList = layoutProject
				.getDisabledModules(false);

		for (final MavenModule module : moduleList) {

			final ModuleName moduleName = module.getModuleName();

			/**
			 * Module-to-Project naming convention.
			 */
			final String memberName = memberName(context, layoutProject, module);

			context.log("");
			context.log("Module name: " + moduleName);
			context.log("Member project: " + memberName);

			if (isSameModuleName(layoutProject.getRootModule(), module)) {
				context.logTab("This is a root module project, managed by user, skip.");
				continue;
			}

			if (isProjectExists(memberName)) {

				context.logErr("Project exists, create skipped: " + memberName);

			} else {

				context.logTab("Creating project: " + memberName);

				/** Clone project via XML. */
				final TopLevelItem item = jenkins.copy(
						(TopLevelItem) layoutProject, memberName);

				final MavenModuleSet memberProject = (MavenModuleSet) item;

				processMemberCreate(context, module, memberProject,
						layoutProject);

				processMemberValidate(context, memberProject);

				ensureProjectView(context, memberProject);

				context.logTab("Project created: " + memberName);

			}

		}

		return true;
	}

	/**
	 * Delete using identity as distinction.
	 */
	public static boolean processMemberDelete(//
			final BuildContext<MavenModuleSetBuild> context,//
			final MavenModuleSet layoutProject,//
			final LayoutArgumentsAction action //
	) throws IOException {

		final String familyID = ProjectIdentity.familyID(layoutProject);

		final List<MavenModuleSet> memberProjectList = ProjectIdentity
				.memberProjectList(familyID);

		if (memberProjectList.isEmpty()) {
			context.logErr("No member projects in the family: " + familyID);
			return false;
		}

		for (final MavenModuleSet memberProject : memberProjectList) {

			context.log("");
			context.log("Member project: " + memberProject.getName());

			context.logTab("Project identity: "
					+ ProjectIdentity.identity(memberProject));

			try {
				memberProject.delete();
				context.logTab("Project deleted.");
			} catch (final Exception e) {
				context.logExc(e);
				context.logErr("Failed to delete project.");
			}

		}

		return true;

	}

	/**
	 * Validate newly created member projects.
	 * <p>
	 * Build maven module, do not wait for completion.
	 */
	public static void processMemberValidate( //
			final BuildContext<MavenModuleSetBuild> context, //
			final MavenModuleSet project //
	) {

		context.logTab("project: " + project.getAbsoluteUrl());

		final LayoutOptions options = context.layoutOptions();

		if (options.getBuildAfterLayout()) {

			final Cause cause = context.build().getCauses().get(0);

			final List<Action> actionList = mavenValidateGoals(context);

			if (options.getUseSharedWorkspace()) {
				actionList.add(new CheckoutSkipAction());
			}

			actionList.add(new LayoutLogicAction());

			project.scheduleBuild2(0, cause, actionList);

			context.logTab("building now");

		}

	}

	private LayoutLogic() {

	}

}
