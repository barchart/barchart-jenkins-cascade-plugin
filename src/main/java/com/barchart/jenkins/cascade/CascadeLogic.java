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
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.Result;

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

	static final String VERSION_PARENT = "versions:update-parent";

	static final String VERSION_DEPENDENCY = "versions:use-releases";

	public static String cascadeProjectName(
			final BuildContext<CascadeBuild> context) {
		final CascadeBuild build = context.build();
		final MemberBuildAction action = build
				.getAction(MemberBuildAction.class);
		return action.getCascadeName();
	}

	public static String layoutProjectName(
			final BuildContext<CascadeBuild> context) {
		final CascadeBuild build = context.build();
		final MemberBuildAction action = build
				.getAction(MemberBuildAction.class);
		return action.getLayoutName();
	}

	public static String memberProjectName(
			final BuildContext<CascadeBuild> context) {
		final CascadeBuild build = context.build();
		final MemberBuildAction action = build
				.getAction(MemberBuildAction.class);
		return action.getMemberName();
	}

	public static MavenModuleSet memberProjectOfCascade(
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

			final boolean isModuleMatch = project.getRootModule()
					.getModuleName().equals(moudleName);

			if (isCascadeMatch && isModuleMatch) {
				return project;
			}

		}

		return null;
	}

	public static boolean hasProperCause(
			final BuildContext<CascadeBuild> context) {
		final CascadeBuild build = context.build();
		final MemberUserCause cause = build.getCause(MemberUserCause.class);
		return cause != null;
	}

	public static Result process(final BuildContext<CascadeBuild> context)
			throws Exception {

		context.log("Build started.");

		if (!hasProperCause(context)) {
			context.log("Unknown build cause.");
			context.log("Cascade builds expect invocation form member projects.");
			context.log("Build finished.");
			return Result.NOT_BUILT;
		}

		context.log("Member build cause, proceed.");

		context.log("Build finished.");
		return Result.SUCCESS;

	}

	public static boolean isFailure(final Result result) {
		return Result.SUCCESS != result;
	}

	/**
	 * Recursively release projects.
	 */
	public static Result processRelease(
			final BuildContext<CascadeBuild> context,
			final ModuleName memberName) throws Exception {

		context.log("---");
		context.log("Release module: " + memberName);

		final MavenModuleSet memberProject = memberProjectOfCascade(context,
				memberName);

		if (memberProject == null) {
			context.err("Project not found.");
			return Result.FAILURE;
		}

		if (isRelease(mavenModel(memberProject))) {
			context.err("Project is a release.");
			return Result.FAILURE;
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
				if (isFailure(updateParent(context, memberName))) {
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
				if (isFailure(updateParent(context, memberName))) {
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
				if (isFailure(updateDependency(context, memberName))) {
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
				if (isFailure(updateDependency(context, memberName))) {
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

			final MavenInterceptorAction goals = new MavenInterceptorAction(
					"clean package");

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
	 * Update project version of a dependency.
	 */
	public static Result updateDependency(
			final BuildContext<CascadeBuild> context,
			final ModuleName memberName) throws Exception {

		return Result.SUCCESS;

	}

	/**
	 * Update project version of a parent.
	 */
	public static Result updateParent(final BuildContext<CascadeBuild> context,
			final ModuleName memberName) throws Exception {

		context.log("---");
		context.log("Update Project: " + memberName);

		final MavenModuleSet memberProject = mavenProject(memberName);

		if (memberProject == null) {
			context.err("Project not found.");
			return Result.FAILURE;
		}

		final MavenInterceptorAction goals = new MavenInterceptorAction(
				VERSION_DEPENDENCY + " " + SCM_CHECKIN);

		final MemberUserCause cause = new MemberUserCause();

		final MemberBadgeAction badge = new MemberBadgeAction();

		final MavenModuleSetBuild memberBuild = memberProject.scheduleBuild2(0,
				cause, goals, badge).get();

		return Result.SUCCESS;

	}

	private CascadeLogic() {

	}

}
