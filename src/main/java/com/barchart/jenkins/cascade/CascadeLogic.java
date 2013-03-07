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
import hudson.model.queue.QueueTaskFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;

/**
 * Release build logic.
 * 
 * @author Andrei Pozolotin
 */
public class CascadeLogic {

	/**
	 * Perform maven release.
	 */
	static final String RELEASE = "release:prepare release:perform --define localCheckout=true --define resume=false";

	/**
	 * Perform SCM:
	 * 
	 * <pre>
	 * git add pom.xml
	 * git commit -m "cascade"
	 * git push
	 * </pre>
	 * 
	 * Do not use wildcards in SCM.
	 */
	static final String SCM_CHECKIN = "scm:checkin --define includes=pom.xml --define message=cascade";

	/**
	 * Perform SCM:
	 * 
	 * <pre>
	 * git checkout
	 * </pre>
	 */
	static final String SCM_CHECKOUT = "scm:checkout";

	/**
	 * Perform SCM:
	 * 
	 * <pre>
	 * git pull
	 * </pre>
	 */
	static final String SCM_UPDATE = "scm:update";

	/**
	 * Perform maven validateion.
	 */
	static final String VALIDATE = "validate";

	/**
	 * Maven dependency version update goals.
	 */
	static final String VERSION_DEPENDENCY = "versions:use-latest-versions "
			+ "--define generateBackupPoms=false "
			+ "--define excludeReactor=false "
			+ "--define allowMajorUpdates=false "
			+ "--define allowMinorUpdates=false "
			+ "--define allowIncrementalUpdates=true ";

	/**
	 * Maven parent version update goals.
	 */
	static final String VERSION_PARENT = "versions:update-parent"
			+ "--define generateBackupPoms=false ";

	public static MemberUserCause cascadeCause(
			final BuildContext<CascadeBuild> context) {
		final CascadeBuild build = context.build();
		final MemberUserCause cause = build.getCause(MemberUserCause.class);
		return cause;
	}

	public static String cascadeProjectName(
			final BuildContext<CascadeBuild> context) {
		final CascadeBuild build = context.build();
		final MemberBuildAction action = build
				.getAction(MemberBuildAction.class);
		return action.getCascadeName();
	}

	public static boolean hasCascadeCause(
			final BuildContext<CascadeBuild> context) {
		return null != cascadeCause(context);
	}

	public static boolean isFailure(final Result result) {
		return Result.SUCCESS != result;
	}

	public static boolean isSuccess(final Result result) {
		return Result.SUCCESS == result;
	}

	public static String layoutProjectName(
			final BuildContext<CascadeBuild> context) {
		final CascadeBuild build = context.build();
		final MemberBuildAction action = build
				.getAction(MemberBuildAction.class);
		return action.getLayoutName();
	}

	public static void logActions(final BuildContext<CascadeBuild> context,
			final List<Action> actionList) {
		for (final Action action : actionList) {
			context.log("\t" + action.getClass().getName());
			context.log("\t\t" + action.toString());
		}
	}

	public static void logDependency(final BuildContext<CascadeBuild> context,
			final List<Dependency> dependencyList) {
		for (final Dependency dependency : dependencyList) {
			context.log("\t" + dependency);
		}
	}

	/**
	 * Commit pom.xml
	 */
	public static List<Action> mavenCommitGoals(final String... options) {
		final MavenGoalsAction goals = new MavenGoalsAction();
		/** Pull */
		goals.append(SCM_UPDATE);
		/** Push */
		goals.append(SCM_CHECKIN);
		goals.append(options);
		final List<Action> list = new ArrayList<Action>();
		list.add(new CheckoutSkipAction());
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
	public static List<Action> mavenDependencyGoals(final String... options) {
		final MavenGoalsAction goals = new MavenGoalsAction();
		goals.append(VERSION_DEPENDENCY);
		goals.append(options);
		final List<Action> list = new ArrayList<Action>();
		list.add(new CheckoutSkipAction());
		list.add(goals);
		return list;
	}

	/**
	 * Update parent version with lower bound of snapshot.
	 * 
	 * See <a href=
	 * "http://mojo.codehaus.org/versions-maven-plugin/update-parent-mojo.html#parentVersion"
	 * >parentVersion</a>
	 */
	public static String mavenParentFilter(final Parent item) {
		String version = item.getVersion();
		version = version.replaceAll(PluginUtilities.SNAPSHOT, "");
		return "--define parentVersion=[" + version + ",)";
	}

	/**
	 * Update parent version in pom.xml.
	 */
	public static List<Action> mavenParentGoals(final String... options) {
		final MavenGoalsAction goals = new MavenGoalsAction();
		goals.append(VERSION_PARENT);
		goals.append(options);
		final List<Action> list = new ArrayList<Action>();
		list.add(new CheckoutSkipAction());
		list.add(goals);
		return list;
	}

	/**
	 * Release maven artifact.
	 */
	public static List<Action> mavenReleaseGoals(final String... options) {
		final List<Action> list = new ArrayList<Action>();
		final MavenGoalsAction goals = new MavenGoalsAction();
		goals.append(RELEASE);
		goals.append(options);
		list.add(goals);
		return list;
	}

	/**
	 * Update maven and jenkins metadata.
	 */
	public static List<Action> mavenValidateGoals(final String... options) {
		final List<Action> list = new ArrayList<Action>();
		final MavenGoalsAction goals = new MavenGoalsAction();
		goals.append(VALIDATE);
		goals.append(options);
		list.add(goals);
		return list;
	}

	public static String memberProjectName(
			final BuildContext<CascadeBuild> context) {
		final CascadeBuild build = context.build();
		final MemberBuildAction action = build
				.getAction(MemberBuildAction.class);
		return action.getMemberName();
	}

	/**
	 * Cascade entry point.
	 */
	public static Result process(final BuildContext<CascadeBuild> context)
			throws Exception {

		if (!hasCascadeCause(context)) {
			context.err("Unknown build cause.");
			context.err("Cascade builds expect invocation form member projects.");
			return Result.NOT_BUILT;
		}

		final String projectName = memberProjectName(context);

		context.log("Cascade started: " + projectName);

		final MavenModuleSet memberProject = mavenProject(projectName);

		final MavenModule rootModule = memberProject.getRootModule();

		if (rootModule == null) {
			context.err("Maven module undefined.");
			context.err("This happens when a new project is created but is never built.");
			return Result.NOT_BUILT;
		}

		final ModuleName memberName = rootModule.getModuleName();

		final Result result = process(0, context, memberName);

		context.log("Cascade finished: " + result);

		return result;

	}

	/**
	 * Build maven module , wait for completion.
	 */
	public static Result process(final BuildContext<CascadeBuild> context,
			final ModuleName moduleName, final List<Action> goals)
			throws Exception {

		context.log("=> module: " + moduleName);
		logActions(context, goals);

		final MavenModuleSet project = project(context, moduleName);

		if (project == null) {
			context.err("Project not found.");
			return Result.FAILURE;
		}

		final MemberUserCause cause = cascadeCause(context);

		final QueueTaskFuture<MavenModuleSetBuild> buildFuture = project
				.scheduleBuild2(0, cause, goals);

		final Future<MavenModuleSetBuild> startFuture = buildFuture
				.getStartCondition();

		/** Block till build started. */
		final MavenModuleSetBuild moduleBuild = startFuture.get();

		context.log("=> console: " + moduleBuild.getAbsoluteUrl() + "console");

		/** Block till build complete. */
		buildFuture.get();

		final Result result = moduleBuild.getResult();

		context.log("=> result: " + result);

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

		final MavenModuleSet project = project(context, moduleName);

		if (project == null) {
			context.err("Project not found.");
			return Result.FAILURE;
		}

		context.log("Project: " + project.getAbsoluteUrl());

		context.log("Update jenkins/maven metadata.");
		if (isFailure(process(context, moduleName, mavenValidateGoals()))) {
			return Result.FAILURE;
		}

		context.log("Verify project is a snapshot.");
		if (isRelease(mavenModel(project))) {
			context.err("Project is a release.");
			context.err("Please update project version to a snapshot.");
			return Result.FAILURE;
		}

		context.log("Process parent.");
		PARENT: {

			/** Update to next release, if present. */
			{
				final Parent parent = mavenParent(project);
				if (parent == null) {
					context.log("Project has no parent.");
					break PARENT;
				}
				if (isRelease(parent)) {
					context.log("Parent is a release: " + parent);
					break PARENT;
				}
				context.log("Parent needs an update: " + parent);
				if (isFailure(process(context, moduleName,
						mavenParentGoals(mavenParentFilter(parent))))) {
					return Result.FAILURE;
				}
			}

			/** Need to release a parent, do it now. */
			{
				final Parent parent = mavenParent(project);
				if (isRelease(parent)) {
					context.log("Parent updated: " + parent);
					break PARENT;
				}
				context.log("Parent needs a release: " + parent);
				final ModuleName parentName = moduleName(parent);
				if (isFailure(process(level + 1, context, parentName))) {
					return Result.FAILURE;
				}
			}

			/** Refresh parent after the release. */
			{
				final Parent parent = mavenParent(project);
				if (isRelease(parent)) {
					context.log("Parent released: " + parent);
					break PARENT;
				}
				context.log("Parent needs a refresh: " + parent);
				if (isFailure(process(context, moduleName,
						mavenParentGoals(mavenParentFilter(parent))))) {
					return Result.FAILURE;
				}
			}

			/** Verify parent version after release/update. */
			{
				final Parent parent = mavenParent(project);
				if (isRelease(parent)) {
					context.log("Parent released: " + parent);
					break PARENT;
				}
				context.err("Can not release parent:" + parent);
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
					context.log("Project has no snapshot dependencies.");
					break DEPENDENCY;
				}
				context.log("Dependencies need update: " + snapshots.size());
				logDependency(context, snapshots);
				if (isFailure(process(context, moduleName,
						mavenDependencyGoals(mavenDependencyFilter(snapshots))))) {
					return Result.FAILURE;
				}
			}

			/** Dependency release. */
			{
				final List<Dependency> snapshots = mavenDependencies(project,
						MATCH_SNAPSHOT);
				if (snapshots.isEmpty()) {
					context.log("Dependencies are updated.");
					break DEPENDENCY;
				}
				context.log("Dependencies need release: " + snapshots.size());
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
					context.log("Dependencies are released.");
					break DEPENDENCY;
				}
				context.log("Dependencies needs refresh: " + snapshots.size());
				logDependency(context, snapshots);
				if (isFailure(process(context, moduleName,
						mavenDependencyGoals(mavenDependencyFilter(snapshots))))) {
					return Result.FAILURE;
				}
			}

			/** Verify dependency. */
			{
				final List<Dependency> snapshots = mavenDependencies(project,
						MATCH_SNAPSHOT);
				if (snapshots.isEmpty()) {
					context.log("Dependencies are released.");
					break DEPENDENCY;
				}
				context.err("Failed to release dependency: " + snapshots.size());
				logDependency(context, snapshots);
				return Result.FAILURE;
			}

		}

		context.log("Commit project pom.xml changes.");
		if (isFailure(process(context, moduleName, mavenCommitGoals()))) {
			return Result.FAILURE;
		}

		context.log("Release project.");
		if (isFailure(process(context, moduleName, mavenReleaseGoals()))) {
			return Result.FAILURE;
		}

		context.log("Project released: " + moduleName);
		return Result.SUCCESS;

	}

	/**
	 * Find member project of a cascade.
	 */
	public static MavenModuleSet project(
			final BuildContext<CascadeBuild> context,
			final ModuleName moudleName) {

		for (final MavenModuleSet project : mavenProjectList()) {

			final MemberProjectProperty property = project
					.getProperty(MemberProjectProperty.class);

			if (property == null) {
				continue;
			}

			final boolean isCascadeMatch = context.build().getProject()
					.getName().equals(property.getCascadeName());

			final MavenModule rootModule = project.getRootModule();

			if (rootModule == null) {
				continue;
			}

			final boolean isModuleMatch = rootModule.getModuleName().equals(
					moudleName);

			if (isCascadeMatch && isModuleMatch) {
				return project;
			}

		}

		return null;
	}

	private CascadeLogic() {

	}

}
