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

import jenkins.model.Jenkins;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

/**
 * Release logic.
 * 
 * @author Andrei Pozolotin
 */
public class CascadeLogic {

	public static Result process(final BuildContext<CascadeBuild> context)
			throws Exception {

		context.log("Build started.");

		final Jenkins jenkins = Jenkins.getInstance();
		final CascadeBuild build = context.build();
		final CascadeProject project = build.getProject();

		final MemberUserCause cause = build.getCause(MemberUserCause.class);

		if (cause == null) {
			context.log("Unknown build cause.");
			context.log("Cascade builds expect invocation form member projects.");
			context.log("Build finished.");
			return Result.NOT_BUILT;
		}

		final MemberBuildAction action = build
				.getAction(MemberBuildAction.class);

		final String memberName = action.getMemberName();

		context.log("Member build cause.");

		context.log("---");
		context.log("Member Project: " + action.getMemberName());

		context.log("Build: " + build);
		context.log("Result: " + build.getResult());

		context.log("Build finished.");
		return Result.SUCCESS;

	}

	/**
	 * Recursively release projects.
	 */
	public static Result processRelease(
			final BuildContext<CascadeBuild> context,
			final ModuleName memberName) throws Exception {

		context.log("---");
		context.log("Release Project: " + memberName);

		final MavenModuleSet memberProject = mavenProject(memberName);

		if (memberProject == null) {
			context.err("Project not found.");
			return Result.FAILURE;
		}

		final Model model = mavenModel(memberProject);

		if (!isSnapshot(model)) {
			context.err("Project is a release.");
			return Result.FAILURE;
		}

		final Parent parent = model.getParent();

		if (isSnapshot(parent)) {

			context.log("Parent needs release: " + parent);

			final ModuleName parentName = moduleName(parent);

			final Result result = processRelease(context, parentName);

			if (result != Result.SUCCESS) {
				return Result.FAILURE;
			}

		}

		final List<Dependency> snapshots = mavenDependencies(memberProject,
				MATCH_SNAPSHOT);

		for (final Dependency dependency : snapshots) {

			context.log("Dependency needs release: " + dependency);

			final ModuleName dependencyName = moduleName(dependency);

			final Result result = processRelease(context, dependencyName);

			if (result != Result.SUCCESS) {
				return Result.FAILURE;
			}

		}

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

		}

		context.log("Project released: " + memberName);

		return Result.SUCCESS;

	}

	static final String RELEASE = "release:prepare release:perform --define localCheckout=true --define pushChanges=true --define resume=false";

	static final String UPDATE_PARENT = "versions:update-parent";

	static final String USE_RELEASES = "versions:use-releases";

	static final String SCM_CHECKING = "scm:checkin --define includes=pom.xml --define message=cascade-release";

	/**
	 * Update project versions.
	 */
	public static Result processUpdate(
			final BuildContext<CascadeBuild> context,
			final ModuleName memberName) throws Exception {

		context.log("---");
		context.log("Update Project: " + memberName);

		final MavenModuleSet memberProject = mavenProject(memberName);

		if (memberProject == null) {
			context.err("Project not found.");
			return Result.FAILURE;
		}

		final MavenInterceptorAction goals = new MavenInterceptorAction(
				USE_RELEASES + " " + SCM_CHECKING);

		final MemberUserCause cause = new MemberUserCause();

		final MemberBadgeAction badge = new MemberBadgeAction();

		final MavenModuleSetBuild memberBuild = memberProject.scheduleBuild2(0,
				cause, goals, badge).get();

		return Result.SUCCESS;

	}

	private CascadeLogic() {

	}

}
