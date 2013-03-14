package com.barchart.jenkins.cascade;

import static com.barchart.jenkins.cascade.PluginUtilities.*;
import hudson.maven.MavenModuleSet;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.Node;
import hudson.plugins.git.GitException;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.opt.PreBuildMergeOptions;
import hudson.scm.SCM;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.RemoteConfig;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;

public class PluginUtilitiesSCM {

	public static File gitDir(final BuildContext<CascadeBuild> context,
			final MavenModuleSet project) {
		final File workDir = new File(project.getWorkspace().getRemote());
		return workDir;
	}

	public static String gitExe(final BuildContext<CascadeBuild> context,
			final MavenModuleSet project) {
		final SCM scm = project.getScm();
		final GitSCM gitScm = (GitSCM) scm;
		final Node builtOn = context.build().getBuiltOn();
		final TaskListener listener = context.listener();
		return gitScm.getGitExe(builtOn, listener);
	}

	public static String gitPull(final BuildContext<CascadeBuild> context,
			final MavenModuleSet project) throws IOException,
			InterruptedException {
		final String git = gitExe(context, project);
		final File workDir = gitDir(context, project);
		final String result = executeResult(workDir, git, "pull");
		return result;
	}

	public static String gitPush(final BuildContext<CascadeBuild> context,
			final MavenModuleSet project) throws IOException,
			InterruptedException {
		final String git = gitExe(context, project);
		final File workDir = gitDir(context, project);
		final String result = executeResult(workDir, git, "push");
		return result;
	}

	public static Result scmCommit(final BuildContext<CascadeBuild> context,
			final MavenModuleSet project) throws IOException,
			InterruptedException {
		final SCM scm = project.getScm();
		if (scm instanceof GitSCM) {
			final String result = gitPush(context, project);
			context.logTab(result);
			return Result.SUCCESS;
		}
		throw new IllegalStateException("Unsupported SCM");
	}

	public static Result scmUpdate(final BuildContext<CascadeBuild> context,
			final MavenModuleSet project) throws IOException,
			InterruptedException {
		final SCM scm = project.getScm();
		context.logTab("### scm:" + scm);
		if (scm instanceof GitSCM) {
			final String result = gitPull(context, project);
			context.logTab(result);
			return Result.SUCCESS;
		}
		throw new IllegalStateException("Unsupported SCM");
	}

	public static Result scmCommit2(final BuildContext<CascadeBuild> context,
			final MavenModuleSet project) {

		final SCM scm = project.getScm();

		final GitSCM gitScm = (GitSCM) scm;

		final File workspace = gitDir(context, project);

		final String gitExe = gitExe(context, project);

		final GitClient git = Git.with(null, null).in(workspace).using(gitExe)
				.getClient();

		final PreBuildMergeOptions mergeOptions = gitScm.getMergeOptions();

		final RemoteConfig remote = mergeOptions.getMergeRemote();

		final String refspec = Constants.HEAD + ":"
				+ mergeOptions.getMergeTarget();

		final String remoteName = remote.getName();

		git.push(remoteName, refspec);

		return Result.SUCCESS;

	}

	public static Result scmUpdate2(final BuildContext<CascadeBuild> context,
			final MavenModuleSet project) throws GitException, IOException {

		final SCM scm = project.getScm();

		final GitSCM gitScm = (GitSCM) scm;

		// final PreBuildMergeOptions mergeOptions = gitScm.getMergeOptions();
		// final RemoteConfig remote = mergeOptions.getMergeRemote();

		final File workspace = gitDir(context, project);
		final String gitExe = gitExe(context, project);
		final GitClient git = Git.with(null, null).in(workspace).using(gitExe)
				.getClient();

		git.fetch(null, null);

		// git.merge(null);

		return Result.SUCCESS;

	}

}
