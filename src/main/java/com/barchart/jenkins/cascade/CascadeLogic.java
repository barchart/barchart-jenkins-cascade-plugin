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
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.scm.SCM;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;

/**
 * Release logic.
 * 
 * @author Andrei Pozolotin
 */
public class CascadeLogic {

	static final String RELEASE = "release:prepare release:perform --define localCheckout=true --define pushChanges=true --define resume=false";

	static final String SCM_CHECKIN = "scm:checkin --define includes=pom.xml --define message=cascade-release";
	static final String SCM_CHECKOUT = "scm:checkout";

	static final String VERSION_DEPENDENCY = "versions:use-latest-versions --define allowMajorUpdates=false --define allowMinorUpdates=false --define allowIncrementalUpdates=true";

	static final String VERSION_PARENT = "versions:update-parent";

	static final String VALIDATE = "validate";

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

	public static String cascadeProjectName(
			final BuildContext<CascadeBuild> context) {
		final CascadeBuild build = context.build();
		final MemberBuildAction action = build
				.getAction(MemberBuildAction.class);
		return action.getCascadeName();
	}

	public static boolean hasProperCause(
			final BuildContext<CascadeBuild> context) {
		final CascadeBuild build = context.build();
		final MemberUserCause cause = build.getCause(MemberUserCause.class);
		return cause != null;
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

	public static MavenInterceptorAction mavenDependencyGoals() {

		final MavenInterceptorAction goals = new MavenInterceptorAction(
				VERSION_DEPENDENCY + " " + SCM_CHECKIN);

		return goals;

	}

	public static MavenInterceptorAction mavenParentGoals() {

		final MavenInterceptorAction goals = new MavenInterceptorAction(
				VERSION_PARENT + " " + SCM_CHECKIN);

		return goals;

	}

	public static MavenInterceptorAction mavenValidateGoals() {

		final MavenInterceptorAction goals = new MavenInterceptorAction(
				VALIDATE);

		return goals;

	}

	public static MavenInterceptorAction mavenReleaseGoals() {

		final MavenInterceptorAction goals = new MavenInterceptorAction(RELEASE);

		return goals;

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

		if (!hasProperCause(context)) {
			context.err("Unknown build cause.");
			context.err("Cascade builds expect invocation form member projects.");
			return Result.NOT_BUILT;
		}

		final String projectName = memberProjectName(context);

		context.log("Build started: " + projectName);

		final MavenModuleSet memberProject = mavenProject(projectName);

		final ModuleName memberName = memberProject.getRootModule()
				.getModuleName();

		final Result result = processRelease(context, memberName);

		context.log("Build finished: " + result);

		return result;

	}

	/**
	 * Recursively release projects.
	 */
	public static Result processRelease(
			final BuildContext<CascadeBuild> context,
			final ModuleName memberName) throws Exception {

		context.log("---");
		context.log("Release module: " + memberName);

		final MavenModuleSet memberProject = project(context, memberName);

		if (memberProject == null) {
			context.err("Project not found.");
			return Result.FAILURE;
		}

		if (isRelease(mavenModel(memberProject))) {
			context.err("Project is a release.");
			return Result.FAILURE;
		}

		CHECKOUT: {
			processUpdate(context, memberName, mavenValidateGoals());
		}

		/** Process parent. */
		PARENT: {

			/** Parent update on a member. */
			{
				final Parent parent = mavenParent(memberProject);
				if (isRelease(parent)) {
					break PARENT;
				}
				context.log("Parent needs an update: " + parent);
				if (isFailure(processUpdate(context, memberName,
						mavenParentGoals()))) {
					return Result.FAILURE;
				}
			}

			/** Member release on a parent. */
			{
				final Parent parent = mavenParent(memberProject);
				if (isRelease(parent)) {
					break PARENT;
				}
				context.log("Parent needs a release: " + parent);
				final ModuleName parentName = moduleName(parent);
				if (isFailure(processRelease(context, parentName))) {
					return Result.FAILURE;
				}
			}

			/** Parent refresh on a member. */
			{
				final Parent parent = mavenParent(memberProject);
				if (isRelease(parent)) {
					break PARENT;
				}
				context.log("Parent needs a refresh: " + parent);
				if (isFailure(processUpdate(context, memberName,
						mavenParentGoals()))) {
					return Result.FAILURE;
				}
			}

			/** Verify parent. */
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
				if (isFailure(processUpdate(context, memberName,
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
					if (isFailure(processRelease(context, dependencyName))) {
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
				if (isFailure(processUpdate(context, memberName,
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
				context.err("Failed to release dependency.");
				for (final Dependency dependency : snapshots) {
					context.err("\t" + dependency);
				}
				return Result.FAILURE;
			}

		}

		/** Process artifact. */
		{

			final CascadeBuild build = context.build();

			final MemberUserCause cause = build.getCause(MemberUserCause.class);

			final MemberBuildAction action = build
					.getAction(MemberBuildAction.class);

			final MavenInterceptorAction goals = mavenReleaseGoals();

			final MemberBadgeAction badge = new MemberBadgeAction();

			final MavenModuleSetBuild memberBuild = memberProject
					.scheduleBuild2(0, cause, action, goals, badge).get();

			if (isFailure(memberBuild.getResult())) {
				return Result.FAILURE;
			}

			context.log("Project released: " + memberName);

			return Result.SUCCESS;

		}

	}

	/**
	 * Update project version.
	 */
	public static Result processUpdate(
			final BuildContext<CascadeBuild> context,
			final ModuleName memberName, final MavenInterceptorAction goals)
			throws Exception {

		context.log("---");
		context.log("Update project: " + memberName);

		final MavenModuleSet memberProject = project(context, memberName);

		if (memberProject == null) {
			context.err("Project not found.");
			return Result.FAILURE;
		}

		final MemberUserCause cause = new MemberUserCause();

		final MemberBadgeAction badge = new MemberBadgeAction();

		final MavenModuleSetBuild memberBuild = memberProject.scheduleBuild2(0,
				cause, goals, badge).get();

		return memberBuild.getResult();

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
