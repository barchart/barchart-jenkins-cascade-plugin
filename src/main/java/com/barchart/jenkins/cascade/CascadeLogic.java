/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import static com.barchart.jenkins.cascade.PluginUtilities.*;
import hudson.maven.ModuleName;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.Action;
import hudson.model.Result;
import hudson.model.AbstractProject;
import hudson.model.Actionable;
import hudson.model.Queue;
import hudson.model.queue.QueueTaskFuture;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import jenkins.model.Jenkins;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;

/**
 * Release build logic.
 * 
 * @author Andrei Pozolotin
 */
public class CascadeLogic {

	/**
	 * Cascade build cause.
	 */
	public static MemberBuildCause cascadeCause(
			final BuildContext<CascadeBuild> context) {
		final CascadeBuild build = context.build();
		return build.getCause(MemberBuildCause.class);
	}

	/**
	 * Extract cascade options from layout build wrapper.
	 */
	public static CascadeOptions cascadeOptions(
			final BuildContext<CascadeBuild> context) {
		final MavenModuleSet layoutProject = layoutProject(context);
		final LayoutBuildWrapper wrapper = LayoutBuildWrapper
				.wrapper(layoutProject);
		return wrapper.getCascadeOptions();
	}

	/**
	 * Find layout project form any cascade family project.
	 */
	public static MavenModuleSet layoutProject(final BuildContext<?> context) {
		final AbstractProject<?, ?> currentProject = context.build()
				.getProject();
		final ProjectIdentity property = ProjectIdentity
				.identity(currentProject);
		final MavenModuleSet layoutProject = property.layoutProject();
		return layoutProject;
	}

	/**
	 * Extract layout options from layout build wrapper.
	 */
	public static LayoutOptions layoutOptions(final BuildContext<?> context) {
		final MavenModuleSet layoutProject = layoutProject(context);
		final LayoutBuildWrapper wrapper = LayoutBuildWrapper
				.wrapper(layoutProject);
		return wrapper.getLayoutOptions();
	}

	/**
	 * Verify presence of a build cause.
	 */
	public static boolean hasCascadeCause(
			final BuildContext<CascadeBuild> context) {
		return null != cascadeCause(context);
	}

	/**
	 * Verify if module was already included in the results.
	 */
	public static boolean hasModuleResult(
			final BuildContext<CascadeBuild> context,
			final ModuleName moduleName) {
		for (final CascadeResult result : context.build().resultSet()) {
			if (moduleName(result.getArtifact()).equals(moduleName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Verify presence of a release badge.
	 */
	public static boolean hasReleaseAction(final Actionable item) {
		return null != item.getAction(DoReleaseBadge.class);
	}

	/**
	 * Show build actions in the log.
	 */
	public static void logActions(final BuildContext<CascadeBuild> context,
			final List<Action> actionList) {
		if (!cascadeOptions(context).getShouldLogActions()) {
			return;
		}
		for (final Action action : actionList) {
			context.logTab(action.getClass().getName());
			context.logTab("\t" + action.toString());
		}
	}

	/**
	 * Show project dependencies in the log.
	 */
	public static void logDependency(final BuildContext<CascadeBuild> context,
			final List<Dependency> dependencyList) {
		if (!cascadeOptions(context).getShouldLogDependency()) {
			return;
		}
		for (final Dependency dependency : dependencyList) {
			context.logTab("\t" + dependency);
		}
	}

	public static void logResult(final BuildContext<CascadeBuild> context) {

		context.log("Cascade result: ");

		for (final CascadeResult result : context.build().resultSet()) {
			context.logTab(result.toString());
		}

	}

	/**
	 * Commit pom.xml to SCM.
	 */
	public static List<Action> mavenCommitGoals(
			final BuildContext<CascadeBuild> context, final String... options) {
		final CascadeOptions cascadeOptions = cascadeOptions(context);
		final MavenGoalsIntercept goals = new MavenGoalsIntercept();
		goals.append(cascadeOptions.getMavenCommitGoals());
		goals.append(options);
		final List<Action> list = new ArrayList<Action>();
		list.add(new CheckoutSkipAction());
		list.add(new DoCascadeBadge());
		list.add(new DoCommitBadge());
		list.add(goals);
		return list;
	}

	/**
	 * Update selected dependency only.
	 * 
	 * See <a href=
	 * "http://mojo.codehaus.org/versions-maven-plugin/use-latest-versions-mojo.html#includes"
	 * >includes</a>
	 */
	public static String mavenDependencyFilter(final List<Dependency> list) {
		final StringBuilder text = new StringBuilder();
		for (final Dependency item : list) {
			final String groupId = item.getGroupId();
			final String artifactId = item.getArtifactId();
			final String expression = groupId + ":" + artifactId;
			if (text.length() == 0) {
				text.append(expression);
			} else {
				text.append(",");
				text.append(expression);
			}
		}
		return "--define includes=" + text;
	}

	/**
	 * Update dependency version in pom.xml.
	 */
	public static List<Action> mavenDependencyGoals(
			final BuildContext<CascadeBuild> context, final String... options) {
		final CascadeOptions cascadeOptions = cascadeOptions(context);
		final MavenGoalsIntercept goals = new MavenGoalsIntercept();
		goals.append(cascadeOptions.getMavenDependencyGoals());
		goals.append(options);
		final List<Action> list = new ArrayList<Action>();
		list.add(new CheckoutSkipAction());
		list.add(new DoCascadeBadge());
		list.add(new DoDependencyBadge());
		list.add(goals);
		return list;
	}

	/**
	 * Update parent version with lower bound of current snapshot.
	 * 
	 * See <a href=
	 * "http://mojo.codehaus.org/versions-maven-plugin/update-parent-mojo.html#parentVersion"
	 * >parentVersion</a>
	 */
	public static String mavenParentFilter(final Parent item) {
		String version = item.getVersion();
		version = version.replaceAll(SNAPSHOT, "");
		return "--define parentVersion=[" + version + ",)";
	}

	/**
	 * Update parent version in pom.xml.
	 */
	public static List<Action> mavenParentGoals(
			final BuildContext<CascadeBuild> context, final String... options) {
		final CascadeOptions cascadeOptions = cascadeOptions(context);
		final MavenGoalsIntercept goals = new MavenGoalsIntercept();
		goals.append(cascadeOptions.getMavenParentGoals());
		goals.append(options);
		final List<Action> list = new ArrayList<Action>();
		list.add(new CheckoutSkipAction());
		list.add(new DoCascadeBadge());
		list.add(new DoParentBadge());
		list.add(goals);
		return list;
	}

	/**
	 * Release maven artifact.
	 */
	public static List<Action> mavenReleaseGoals(
			final BuildContext<CascadeBuild> context, final String... options) {
		final CascadeOptions cascadeOptions = cascadeOptions(context);
		final MavenGoalsIntercept goals = new MavenGoalsIntercept();
		goals.append(cascadeOptions.getMavenReleaseGoals());
		goals.append(options);
		final List<Action> list = new ArrayList<Action>();
		list.add(new CheckoutSkipAction()); // XXX
		list.add(new DoCascadeBadge());
		list.add(new DoReleaseBadge());
		list.add(goals);
		return list;
	}

	/**
	 * Update maven and jenkins metadata.
	 */
	public static List<Action> mavenValidateGoals(
			final BuildContext<CascadeBuild> context, final String... options) {
		final CascadeOptions cascadeOptions = cascadeOptions(context);
		final MavenGoalsIntercept goals = new MavenGoalsIntercept();
		goals.append(cascadeOptions.getMavenValidateGoals());
		goals.append(options);
		final List<Action> list = new ArrayList<Action>();
		list.add(new DoCascadeBadge());
		list.add(new DoValidateBadge());
		list.add(goals);
		return list;
	}

	/**
	 * Find initial member project.
	 */
	public static MavenModuleSet memberProject(
			final BuildContext<CascadeBuild> context) {
		final CascadeBuild build = context.build();
		final MemberBuildAction action = build
				.getAction(MemberBuildAction.class);
		return action.getIdentity().memberProject();
	}

	/**
	 * Find member project of a cascade by maven module name.
	 */
	public static MavenModuleSet memberProject(
			final BuildContext<CascadeBuild> context,
			final ModuleName sourceName) {

		final CascadeProject cacadeProject = context.build().getProject();

		final String sourceID = ProjectIdentity.familyID(cacadeProject);

		for (final MavenModuleSet project : mavenProjectList()) {

			final String targetID = ProjectIdentity.familyID(project);

			if (targetID == null) {
				continue;
			}

			final boolean isFamilyMatch = sourceID.equals(targetID);

			final MavenModule rootModule = project.getRootModule();

			if (rootModule == null) {
				continue;
			}

			final ModuleName targetName = rootModule.getModuleName();

			final boolean isModuleMatch = sourceName.equals(targetName);

			if (isFamilyMatch && isModuleMatch) {
				return project;
			}

		}

		return null;
	}

	/**
	 * Initial member release version.
	 */
	public static String memberReleaseVersion(
			final BuildContext<CascadeBuild> context) {
		final CascadeBuild build = context.build();
		final MemberBuildAction action = build
				.getAction(MemberBuildAction.class);
		return action.getReleaseVersion();
	}

	/**
	 * Initial member development version.
	 */
	public static String memberSnapshotVersion(
			final BuildContext<CascadeBuild> context) {
		final CascadeBuild build = context.build();
		final MemberBuildAction action = build
				.getAction(MemberBuildAction.class);
		return action.getSnapshotVersion();
	}

	/**
	 * Cascade entry point.
	 */
	public static Result process(final BuildContext<CascadeBuild> context)
			throws Exception {

		if (!hasCascadeCause(context)) {
			context.logErr("Unknown build cause.");
			context.logErr("Cascade builds expect invocation form member projects.");
			return Result.NOT_BUILT;
		}

		final MavenModuleSet memberProject = memberProject(context);

		if (memberProject == null) {
			context.logErr("Project not found.");
			return Result.FAILURE;
		}

		final String projectName = memberProject.getName();

		context.log("Cascade started: " + projectName);

		final MavenModule rootModule = memberProject.getRootModule();

		if (rootModule == null) {
			context.logErr("Maven module undefined.");
			context.logErr("This happens when a new project is created but is never built.");
			return Result.NOT_BUILT;
		}

		final ModuleName memberName = rootModule.getModuleName();

		final int level = 0;

		final Result result = process(level + 1, context, memberName);

		context.log("Cascade finished: " + result);

		logResult(context);

		return result;

	}

	/**
	 * Build maven module, wait for completion.
	 */
	public static Result process(final BuildContext<CascadeBuild> context,
			final ModuleName moduleName, final List<Action> goals)
			throws Exception {

		context.logTab("module: " + moduleName);
		logActions(context, goals);

		final MavenModuleSet project = memberProject(context, moduleName);

		if (project == null) {
			context.logErr("Project not found.");
			return Result.FAILURE;
		}

		final MemberBuildCause cause = cascadeCause(context);

		/** Ensure empty project build queue. */
		final Queue queue = Jenkins.getInstance().getQueue();
		while (queue.cancel(project)) {
			context.logTab("removed non-cascade pending build");
		}

		final QueueTaskFuture<MavenModuleSetBuild> buildFuture = project
				.scheduleBuild2(0, cause, goals);

		if (buildFuture == null) {
			context.logErr("Logic error: can not schedule build.");
			return Result.FAILURE;
		}

		final Future<MavenModuleSetBuild> startFuture = buildFuture
				.getStartCondition();

		/** Block till build started. */
		final MavenModuleSetBuild build = startFuture.get();

		context.logTab("console: " + build.getAbsoluteUrl() + "console");

		/** Block till build complete. */
		buildFuture.get();

		final Result result = build.getResult();

		context.logTab("result: " + result);

		if (isSuccess(result)) {
			storeBuildResult(context, build);
		}

		return result;

	}

	/**
	 * Recursively release projects.
	 */
	public static Result process(final int level,
			final BuildContext<CascadeBuild> context,
			final ModuleName moduleName) throws Exception {

		context.log("---------------------");
		context.log("Level: " + level);
		context.log("Module: " + moduleName);

		final MavenModuleSet project = memberProject(context, moduleName);

		if (project == null) {
			context.logErr("Project not found.");
			context.logErr("Please ensure cascade layout contains this module.");
			return Result.FAILURE;
		}

		context.logTab("project: " + project.getAbsoluteUrl());

		final String pomFile = project.getRootPOM(null);

		if (hasModuleResult(context, moduleName)) {
			context.logTab("Module already released: " + moduleName);
			return Result.SUCCESS;
		}

		context.log("Update metadata before release.");
		UPDATE: {

			if (layoutOptions(context).getUseSharedWorkspace() && level > 1) {
				context.logTab("using shared workspace - skip");
				break UPDATE;
			}

			if (isFailure(PluginScm.scmUpdate(context, project))) {
				return Result.FAILURE;
			}

		}

		context.log("Verify project.");
		if (isRelease(mavenModel(project))) {
			context.logErr("Project is a release.");
			context.logErr("Please update project version to appropriate snapshot.");
			return Result.FAILURE;
		} else {
			context.logTab("Project is a snapshot.");
		}

		context.log("Process parent.");
		PARENT: {

			/** Update to next release, if present. */
			{
				final Parent parent = mavenParent(project);
				if (parent == null) {
					context.logTab("Project has no parent.");
					break PARENT;
				}
				if (isRelease(parent)) {
					context.logTab("Parent is a release: " + parent);
					break PARENT;
				}
				context.logTab("Parent needs an update: " + parent);
				if (isFailure(process(context, moduleName,
						mavenParentGoals(context, mavenParentFilter(parent))))) {
					return Result.FAILURE;
				}
			}

			/** Need to release a parent, do it now. */
			{
				final Parent parent = mavenParent(project);
				if (isRelease(parent)) {
					context.logTab("Parent updated: " + parent);
					break PARENT;
				}
				context.logTab("Parent needs a release: " + parent);
				final ModuleName parentName = moduleName(parent);
				if (isFailure(process(level + 1, context, parentName))) {
					return Result.FAILURE;
				}
			}

			/** Refresh parent after the release. */
			{
				final Parent parent = mavenParent(project);
				if (isRelease(parent)) {
					context.logTab("Parent refreshed: " + parent);
					break PARENT;
				}
				context.logTab("Parent needs a refresh: " + parent);
				if (isFailure(process(context, moduleName,
						mavenParentGoals(context, mavenParentFilter(parent))))) {
					return Result.FAILURE;
				}
			}

			/** Verify parent version after release/update. */
			{
				final Parent parent = mavenParent(project);
				if (isRelease(parent)) {
					context.logTab("Parent verified: " + parent);
					break PARENT;
				}
				context.logErr("Can not verify parent:" + parent);
				return Result.FAILURE;
			}

		}

		context.log("Commit (parent update): " + pomFile);
		if (isFailure(PluginScm.scmCommit(context, project))) {
			return Result.FAILURE;
		}

		context.log("Process dependencies.");
		DEPENDENCY: {

			/** Dependency update. */
			{
				final List<Dependency> snapshots = mavenDependencies(project,
						MATCH_SNAPSHOT);
				if (snapshots.isEmpty()) {
					context.logTab("Project has no snapshot dependencies.");
					break DEPENDENCY;
				}
				context.logTab("Dependencies need update: " + snapshots.size());
				logDependency(context, snapshots);
				if (isFailure(process(
						context,
						moduleName,
						mavenDependencyGoals(context,
								mavenDependencyFilter(snapshots))))) {
					return Result.FAILURE;
				}
			}

			/** Dependency release. */
			{
				final List<Dependency> snapshots = mavenDependencies(project,
						MATCH_SNAPSHOT);
				if (snapshots.isEmpty()) {
					context.logTab("Dependencies are updated.");
					break DEPENDENCY;
				}
				context.logTab("Dependencies need release: " + snapshots.size());
				for (final Dependency dependency : snapshots) {
					final ModuleName dependencyName = moduleName(dependency);
					if (isFailure(process(level + 1, context, dependencyName))) {
						return Result.FAILURE;
					}
				}
			}

			/** Dependency refresh. */
			{
				final List<Dependency> snapshots = mavenDependencies(project,
						MATCH_SNAPSHOT);
				if (snapshots.isEmpty()) {
					context.logTab("Dependencies are released.");
					break DEPENDENCY;
				}
				context.logTab("Dependencies needs refresh: "
						+ snapshots.size());
				logDependency(context, snapshots);
				if (isFailure(process(
						context,
						moduleName,
						mavenDependencyGoals(context,
								mavenDependencyFilter(snapshots))))) {
					return Result.FAILURE;
				}
			}

			/** Verify dependency. */
			{
				final List<Dependency> snapshots = mavenDependencies(project,
						MATCH_SNAPSHOT);
				if (snapshots.isEmpty()) {
					context.logTab("Dependencies are verified.");
					break DEPENDENCY;
				}
				context.logErr("Failed to verify dependency: "
						+ snapshots.size());
				logDependency(context, snapshots);
				return Result.FAILURE;
			}

		}

		context.log("Commit (dependency update):" + pomFile);
		if (isFailure(PluginScm.scmCommit(context, project))) {
			return Result.FAILURE;
		}

		context.log("Release project.");
		if (isFailure(process(context, moduleName, mavenReleaseGoals(context)))) {
			return Result.FAILURE;
		}

		/**
		 * Ensure next non-cascade release will pick up the change.
		 * 
		 * TODO to it once, optionally, at the end of cascade, for all released
		 * modules.
		 */
		// context.log("Update metadata after release.");
		// if (isFailure(process(context, moduleName,
		// mavenValidateGoals(context)))) {
		// return Result.FAILURE;
		// }

		context.log("Project released: " + moduleName);
		return Result.SUCCESS;

	}

	/**
	 * Store build result in the build context.
	 */
	public static void storeBuildResult(
			final BuildContext<CascadeBuild> context,
			final MavenModuleSetBuild build) throws Exception {

		if (!hasReleaseAction(build)) {
			return;
		}

		/** Project which was released. */
		final MavenModuleSet project = build.getProject();

		/** Relative path of this project in SCM repository. */
		final String modulePath = project.getRootModule().getRelativePath();

		/** Location of SCM checkout repository during the release. */
		final String releaseRepo = "target/checkout";

		/** Absolute path of this project during the release. */
		final String releaseFolder = build.getWorkspace().child(modulePath)
				.child(releaseRepo).child(modulePath).getRemote();

		/** Maven pom.xml which was released in this build. */
		final File pomFile = new File(releaseFolder, "pom.xml");

		if (!pomFile.exists()) {
			context.logErr("Can not locate release result: " + pomFile);
			return;
		}

		final Artifact artifact = mavenArtifact(mavenModel(pomFile));
		final String buildURL = build.getAbsoluteUrl();

		final CascadeResult result = new CascadeResult(artifact, buildURL);

		context.build().resultSet().add(result);

	}

	private CascadeLogic() {

	}

}
