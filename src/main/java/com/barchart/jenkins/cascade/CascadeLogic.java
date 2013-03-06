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
import hudson.Launcher;
import hudson.maven.ModuleName;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.queue.QueueTaskFuture;
import hudson.scm.SCM;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;

/**
 * Release logic.
 * 
 * @author Andrei Pozolotin
 */
public class CascadeLogic {

	static final String RELEASE = "release:prepare release:perform --define localCheckout=true --define resume=false";

	static final String SCM_CHECKIN = "scm:checkin --define includes=**/pom.xml --define message=cascade";

	static final String SCM_CHECKOUT = "scm:checkout";

	static final String SNAPSHOT = "-SNAPSHOT";

	static final String VALIDATE = "validate";

	static final String VERSION_DEPENDENCY = "versions:use-latest-versions "
			+ " --define  excludeReactor=false";
	// "versions:use-releases"
	// " --define allowMajorUpdates=false --define allowMinorUpdates=false --define allowIncrementalUpdates=true";

	/**
	 * <pre>
	 * if release is 1.0.25:
	 * and parent is 1.0.26-SNAPSHOT
	 * 	mvn versions:update-parent
	 * will revert to 1.0.25
	 * 
	 * workaround:
	 * 	mvn versions:update-parent -DparentVersion="[1.0.26,)"
	 * will either use 1.0.26+ or will keep SNAPSHOT
	 * </pre>
	 */
	static final String VERSION_PARENT = "versions:update-parent";

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

	public static boolean checkout(final BuildContext context,
			final MavenModuleSet project) throws Exception {

		final AbstractBuild<?, ?> build = context.build();
		final Launcher launcher = new Launcher.LocalLauncher(context.listener());
		final FilePath workspace = project.getWorkspace();
		final BuildListener listener = context.listener();
		final File changelogFile = new File(build.getRootDir(), "changelog.xml");

		final SCM scm = project.getScm();

		return scm
				.checkout(build, launcher, workspace, listener, changelogFile);

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
			context.log("\t" + action);
		}
	}

	public static void logDependency(final BuildContext<CascadeBuild> context,
			final List<Dependency> dependencyList) {
		for (final Dependency dependency : dependencyList) {
			context.log("\t" + dependency);
		}
	}

	public static List<Action> mavenAnyGoals(final String... options) {
		final List<Action> list = new ArrayList<Action>();
		final MavenInterceptorAction goals = new MavenInterceptorAction();
		goals.append(options);
		return list;

	}

	public static List<Action> mavenDependencyGoals(final String... options) {
		final List<Action> list = new ArrayList<Action>();
		final MavenInterceptorAction goals = new MavenInterceptorAction();
		goals.append(VERSION_DEPENDENCY);
		goals.append(SCM_CHECKIN);
		goals.append(options);
		return list;
	}

	public static List<Action> mavenParentGoals(final String... options) {
		final List<Action> list = new ArrayList<Action>();
		final MavenInterceptorAction goals = new MavenInterceptorAction();
		goals.append(VERSION_PARENT);
		goals.append(SCM_CHECKIN);
		goals.append(options);
		list.add(goals);
		return list;

	}

	public static List<Action> mavenReleaseGoals(final String... options) {
		final List<Action> list = new ArrayList<Action>();
		final MavenInterceptorAction goals = new MavenInterceptorAction();
		goals.append(RELEASE);
		goals.append(options);
		list.add(goals);
		return list;
	}

	public static List<Action> mavenValidateGoals(final String... options) {
		final List<Action> list = new ArrayList<Action>();
		final MavenInterceptorAction goals = new MavenInterceptorAction();
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

	public static String parentVersion(final Parent parent) {
		String version = parent.getVersion();
		version = version.replaceAll(SNAPSHOT, "");
		return "--define parentVersion=[" + version + ",)";
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

		final Result result = process(context, memberName);

		context.log("Cascade finished: " + result);

		return result;

	}

	/**
	 * Recursively release projects.
	 */
	public static Result process(final BuildContext<CascadeBuild> context,
			final ModuleName memberName) throws Exception {

		context.log("---");
		context.log("Maven Module: " + memberName);

		final MavenModuleSet memberProject = project(context, memberName);

		if (memberProject == null) {
			context.err("Jenkins project not found.");
			return Result.FAILURE;
		}

		context.log("Jenkins project: " + memberProject.getName());

		if (isRelease(mavenModel(memberProject))) {
			context.err("Project is a release.");
			return Result.FAILURE;
		}

		/** Update jenkins/maven module state. */
		CHECKOUT: {
			process(context, memberName, mavenValidateGoals());
		}

		/** Process parent. */
		PARENT: {

			/** Update to next release, if present. */
			{
				final Parent parent = mavenParent(memberProject);
				if (parent == null) {
					break PARENT;
				}
				if (isRelease(parent)) {
					break PARENT;
				}
				context.log("Parent needs an update: " + parent);
				if (isFailure(process(context, memberName,
						mavenParentGoals(parentVersion(parent))))) {
					return Result.FAILURE;
				}
			}

			/** Need to release a parent, do it now. */
			{
				final Parent parent = mavenParent(memberProject);
				if (isRelease(parent)) {
					break PARENT;
				}
				context.log("Parent needs a release: " + parent);
				final ModuleName parentName = moduleName(parent);
				if (isFailure(process(context, parentName))) {
					return Result.FAILURE;
				}
			}

			/** Refresh parent after the release. */
			{
				final Parent parent = mavenParent(memberProject);
				if (isRelease(parent)) {
					break PARENT;
				}
				context.log("Parent needs a refresh: " + parent);
				if (isFailure(process(context, memberName,
						mavenParentGoals(parentVersion(parent))))) {
					return Result.FAILURE;
				}
			}

			/** Verify parent version after release/update. */
			{
				final Parent parent = mavenParent(memberProject);
				if (isRelease(parent)) {
					break PARENT;
				}
				context.err("Can not release parent.");
				return Result.FAILURE;
			}

		}

		/** Process dependencies. */
		DEPENDENCY: {

			/** Dependency update. */
			{
				final List<Dependency> snapshots = mavenDependencies(
						memberProject, MATCH_SNAPSHOT);
				if (snapshots.isEmpty()) {
					break DEPENDENCY;
				}
				context.log("Dependency needs an update: " + snapshots.size());
				logDependency(context, snapshots);
				if (isFailure(process(context, memberName,
						mavenDependencyGoals()))) {
					return Result.FAILURE;
				}
			}

			/** Dependency release. */
			{
				final List<Dependency> snapshots = mavenDependencies(
						memberProject, MATCH_SNAPSHOT);
				if (snapshots.isEmpty()) {
					break DEPENDENCY;
				}
				for (final Dependency dependency : snapshots) {
					context.log("Dependency needs a release: " + dependency);
					final ModuleName dependencyName = moduleName(dependency);
					if (isFailure(process(context, dependencyName))) {
						return Result.FAILURE;
					}
				}
			}

			/** Dependency refresh. */
			{
				final List<Dependency> snapshots = mavenDependencies(
						memberProject, MATCH_SNAPSHOT);
				if (snapshots.isEmpty()) {
					break DEPENDENCY;
				}
				context.log("Dependency needs a refresh: " + snapshots.size());
				logDependency(context, snapshots);
				if (isFailure(process(context, memberName,
						mavenDependencyGoals()))) {
					return Result.FAILURE;
				}
			}

			/** Verify dependency. */
			{
				final List<Dependency> snapshots = mavenDependencies(
						memberProject, MATCH_SNAPSHOT);
				if (snapshots.isEmpty()) {
					break DEPENDENCY;
				}
				context.err("Failed to release dependency: " + snapshots.size());
				logDependency(context, snapshots);
				return Result.FAILURE;
			}

		}

		/** Process artifact. */
		{

			if (isFailure(process(context, memberName, mavenReleaseGoals()))) {
				return Result.FAILURE;
			}

			context.log("Project released: " + memberName);

			return Result.SUCCESS;

		}

	}

	/**
	 * Invoke maven build.
	 */
	public static Result process(final BuildContext<CascadeBuild> context,
			final ModuleName memberName, final List<Action> goals)
			throws Exception {

		context.log("===");
		context.log("Maven module build: " + memberName);
		logActions(context, goals);

		final MavenModuleSet memberProject = project(context, memberName);

		if (memberProject == null) {
			context.err("Jenkins project not found.");
			return Result.FAILURE;
		}

		context.log("Jenkins project: " + memberProject.getAbsoluteUrl());

		final MemberUserCause cause = cascadeCause(context);

		final QueueTaskFuture<MavenModuleSetBuild> buildFuture = memberProject
				.scheduleBuild2(0, cause, goals);

		final Future<MavenModuleSetBuild> startFuture = buildFuture
				.getStartCondition();

		/** Block till build started. */
		final MavenModuleSetBuild memberBuild = startFuture.get();

		context.log("Jenkins console: " + memberBuild.getAbsoluteUrl()
				+ "console");

		/** Block till build complete. */
		buildFuture.get();

		final Result result = memberBuild.getResult();

		context.log("Maven module result: " + result);

		return result;

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
