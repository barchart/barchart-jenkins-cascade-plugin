/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import static com.barchart.jenkins.cascade.PluginUtilities.*;
import hudson.FilePath;
import hudson.maven.ModuleName;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.Action;
import hudson.model.Result;
import hudson.model.Actionable;
import hudson.model.queue.QueueTaskFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.jvnet.hudson.plugins.m2release.M2ReleaseBadgeAction;

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
		for (final CascadeResult result : context.build().getResultSet()) {
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
		if (!context.cascadeOptions().getShouldLogActions()) {
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
		if (!context.cascadeOptions().getShouldLogDependency()) {
			return;
		}
		for (final Dependency dependency : dependencyList) {
			context.logTab("\t" + dependency);
		}
	}

	public static void logResult(final BuildContext<CascadeBuild> context) {

		context.log("Cascade result: ");

		for (final CascadeResult result : context.build().getResultSet()) {
			context.logTab(result.toString());
		}

	}

	/**
	 * Commit pom.xml to SCM.
	 */
	public static List<Action> mavenCommitGoals(
			final BuildContext<CascadeBuild> context, final String... options) {
		final CascadeOptions cascadeOptions = context.cascadeOptions();
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
		final CascadeOptions cascadeOptions = context.cascadeOptions();
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
		final CascadeOptions cascadeOptions = context.cascadeOptions();
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
		final CascadeOptions cascadeOptions = context.cascadeOptions();
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
	 * Update maven and jenkins metadata after release.
	 */
	public static List<Action> mavenUpdateGoals(
			final BuildContext<CascadeBuild> context, final String... options) {
		final CascadeOptions cascadeOptions = context.cascadeOptions();
		// final MavenGoalsIntercept goals = new MavenGoalsIntercept(); // XXX
		// goals.append(cascadeOptions.getMavenReleaseGoals());
		// goals.append(options);
		final List<Action> list = new ArrayList<Action>();
		list.add(new CheckoutSkipAction()); // XXX
		list.add(new DoCascadeBadge());
		list.add(new DoValidateBadge());
		// list.add(goals);
		return list;
	}

	/**
	 * Update maven and jenkins metadata before release.
	 */
	public static List<Action> mavenValidateGoals(
			final BuildContext<CascadeBuild> context, final String... options) {
		final CascadeOptions cascadeOptions = context.cascadeOptions();
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

		final MavenModuleSet project = memberProject(context);

		if (project == null) {
			context.logErr("Project not found.");
			return Result.FAILURE;
		}

		final String projectName = project.getName();

		context.log("Cascade started: " + projectName);

		context.log("Check-out SCM.");
		PluginScm.scmCheckout(context, project);

		final MavenModule rootModule = project.getRootModule();

		if (rootModule == null) {
			context.logErr("maven module undefined.");
			context.logErr("this happens when a new project is created but is never built.");
			return Result.NOT_BUILT;
		}

		final ModuleName memberName = rootModule.getModuleName();

		final int level = 0;

		final Result result = processEntry(level + 1, context, memberName);

		context.log("Cascade finished: " + result);

		logResult(context);

		context.log("Check-in SCM.");
		PluginScm.scmCheckin(context, project);

		return result;

	}

	/**
	 * Recursively release projects.
	 */
	public static Result processEntry(final int level,
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

		if (hasModuleResult(context, moduleName)) {
			context.logTab("module already released: " + moduleName);
			return Result.SUCCESS;
		}

		context.log("Update before release.");
		/** TODO parse poms */
		scmRead(level, context, project);

		context.log("Verify project.");
		if (isRelease(mavenModel(project))) {
			context.logErr("project is a release");
			context.logErr("this is likely due to failed release:perform phase");
			context.logErr("please update project version to the appropriate snapshot");
			context.logErr("you can correct it as follows:");
			context.logErr("1) revert version commit");
			context.logErr("2) wipeout workspace");
			context.logErr("3) restart cascade");
			return Result.FAILURE;
		} else {
			context.logTab("project is a snapshot");
		}

		context.log("Process parent.");
		PARENT: {

			/** Update to next release, if present. */
			{
				final Parent parent = mavenParent(project);
				if (parent == null) {
					context.logTab("project has no parent");
					break PARENT;
				}
				if (isRelease(parent)) {
					context.logTab("parent is a release: " + parent);
					break PARENT;
				}
				context.logTab("parent needs an update: " + parent);
				if (isFailure(processMaven(context, project,
						mavenParentGoals(context, mavenParentFilter(parent)),
						true))) {
					return Result.FAILURE;
				}
			}

			/** Need to release a parent, do it now. */
			{
				final Parent parent = mavenParent(project);
				if (isRelease(parent)) {
					context.logTab("parent updated: " + parent);
					scmWrite(level, context, project);
					break PARENT;
				}
				context.logTab("parent needs a release: " + parent);
				final ModuleName parentName = moduleName(parent);
				if (isFailure(processEntry(level + 1, context, parentName))) {
					return Result.FAILURE;
				}
			}

			/** Refresh parent after the release. */
			{
				final Parent parent = mavenParent(project);
				if (isRelease(parent)) {
					context.logTab("parent refreshed: " + parent);
					scmWrite(level, context, project);
					break PARENT;
				}
				context.logTab("parent needs a refresh: " + parent);
				if (isFailure(processMaven(context, project,
						mavenParentGoals(context, mavenParentFilter(parent)),
						true))) {
					return Result.FAILURE;
				}
			}

			/** Verify parent version after release/update. */
			{
				final Parent parent = mavenParent(project);
				if (isRelease(parent)) {
					context.logTab("parent verified: " + parent);
					scmWrite(level, context, project);
					break PARENT;
				}
				context.logErr("can not verify parent:" + parent);
				return Result.FAILURE;
			}

		}

		context.log("Process dependencies.");
		DEPENDENCY: {

			/** Dependency update. */
			{
				final List<Dependency> snapshots = mavenDependencies(project,
						MATCH_SNAPSHOT);
				if (snapshots.isEmpty()) {
					context.logTab("project has no snapshot dependencies");
					break DEPENDENCY;
				}
				context.logTab("dependencies need update: " + snapshots.size());
				logDependency(context, snapshots);
				if (isFailure(processMaven(
						context,
						project,
						mavenDependencyGoals(context,
								mavenDependencyFilter(snapshots)), true))) {
					return Result.FAILURE;
				}
			}

			/** Dependency release. */
			{
				final List<Dependency> snapshots = mavenDependencies(project,
						MATCH_SNAPSHOT);
				if (snapshots.isEmpty()) {
					context.logTab("dependencies are updated");
					scmWrite(level, context, project);
					break DEPENDENCY;
				}
				scmWrite(level, context, project);
				context.logTab("dependencies need release: " + snapshots.size());
				for (final Dependency dependency : snapshots) {
					final ModuleName dependencyName = moduleName(dependency);
					if (isFailure(processEntry(level + 1, context,
							dependencyName))) {
						return Result.FAILURE;
					}
				}
			}

			/** Dependency refresh. */
			{
				final List<Dependency> snapshots = mavenDependencies(project,
						MATCH_SNAPSHOT);
				if (snapshots.isEmpty()) {
					context.logTab("dependencies are released");
					scmWrite(level, context, project);
					break DEPENDENCY;
				}
				context.logTab("dependencies need refresh: " + snapshots.size());
				logDependency(context, snapshots);
				if (isFailure(processMaven(
						context,
						project,
						mavenDependencyGoals(context,
								mavenDependencyFilter(snapshots)), true))) {
					return Result.FAILURE;
				}
			}

			/** Verify dependency. */
			{
				final List<Dependency> snapshots = mavenDependencies(project,
						MATCH_SNAPSHOT);
				if (snapshots.isEmpty()) {
					context.logTab("dependencies are verified");
					scmWrite(level, context, project);
					break DEPENDENCY;
				}
				context.logErr("failed to verify dependency: "
						+ snapshots.size());
				logDependency(context, snapshots);
				return Result.FAILURE;
			}

		}

		context.log("Release project.");
		if (isFailure(processMaven(context, project,
				mavenReleaseGoals(context), true))) {
			return Result.FAILURE;
		}

		context.log("Update after release.");
		if (isFailure(processMaven(context, project, mavenUpdateGoals(context),
				false))) {
			return Result.FAILURE;
		}

		context.log("Project released: " + moduleName);
		return Result.SUCCESS;

	}

	/**
	 * Build maven module.
	 */
	public static Result processMaven(final BuildContext<CascadeBuild> context,
			final MavenModuleSet project, final List<Action> actionList,
			final boolean isBlocking) throws Exception {

		context.logTab("module: " + project.getRootModule().getName());

		actionList.add(new CascadeLogicAction());

		logActions(context, actionList);

		final MemberBuildCause cause = cascadeCause(context);

		final QueueTaskFuture<MavenModuleSetBuild> buildFuture = project
				.scheduleBuild2(0, cause, actionList);

		if (buildFuture == null) {
			context.logErr("logic error: can not schedule build");
			return Result.FAILURE;
		}

		if (!isBlocking) {
			return Result.SUCCESS;
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
	 * Perform optional update.
	 */
	public static void scmRead(final int level,
			final BuildContext<CascadeBuild> context,
			final MavenModuleSet project) throws Exception {

		if (context.layoutOptions().getUseSharedWorkspace() && level > 1) {
			context.logTab("scm: skip update for shared workspace");
			return;
		}

		PluginScm.scmUpdate(context, project);

	}

	/**
	 * Perform commit and optional check-in.
	 */
	public static void scmWrite(final int level,
			final BuildContext<CascadeBuild> context,
			final MavenModuleSet project) throws Exception {

		final String pattern = project.getRootPOM(null);

		PluginScm.scmCommit(context, project, pattern);

		if (!context.cascadeOptions().getShouldPushUpdates()) {
			context.logTab("scm: skip checkin till cascade finish");
			return;
		}

		PluginScm.scmCheckin(context, project);

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

		/** Relative path of SCM checkout repository during the release. */
		final String releaseRepo = "target/checkout";

		/** Absolute path of module project during the release. */
		final FilePath releaseFolder = build.getWorkspace().child(modulePath)
				.child(releaseRepo).child(modulePath);

		/** Maven pom.xml which was released in this build. */
		final FilePath pomFile = releaseFolder.child("pom.xml");

		final Artifact artifact = mavenArtifact(mavenModel(pomFile));
		final String buildURL = build.getAbsoluteUrl();

		final CascadeResult result = new CascadeResult(artifact, buildURL);

		context.build().getResultSet().add(result);

		/** Provide compatibility with m2release plugin. */
		final String version = artifact.getVersion();
		final Action m2ReleaseBadge = new M2ReleaseBadgeAction(version, false);
		build.addAction(m2ReleaseBadge);
		build.keepLog(true);

	}

	private CascadeLogic() {

	}

}
