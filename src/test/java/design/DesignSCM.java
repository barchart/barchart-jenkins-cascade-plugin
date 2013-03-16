/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package design;

import static com.barchart.jenkins.cascade.PluginUtilities.*;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.plugins.git.GitPublisher;
import hudson.plugins.git.GitPublisher.BranchToPush;
import hudson.plugins.git.GitPublisher.NoteToPush;
import hudson.plugins.git.GitPublisher.TagToPush;
import hudson.plugins.git.GitSCM;
import hudson.remoting.VirtualChannel;
import hudson.scm.SCM;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.RefSpec;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;

import com.barchart.jenkins.cascade.BuildContext;

public class DesignSCM {

	public static void scmCommit() {

		final BranchToPush branch = new BranchToPush("origin", "integration");

		final List<BranchToPush> branchList = Collections.singletonList(branch);
		final List<TagToPush> tagList = Collections.emptyList();
		final List<NoteToPush> noteList = Collections.emptyList();

		new GitPublisher(tagList, branchList, noteList, true, true);

	}

	public static void scmUpdate() {

		final FileCallable<Boolean> actor = new FileCallable<Boolean>() {

			private static final long serialVersionUID = 1L;

			public Boolean invoke(final File file, final VirtualChannel channel)
					throws IOException, InterruptedException {

				final BuildListener listener = null;
				final FilePath workspacePath = null;
				final EnvVars environment = null;

				final File workspace = null;

				final String gitExe = "";

				final GitClient git = Git.with(listener, environment)
						.in(workspace).using(gitExe).getClient();

				return null;
			}

		};

	}

	public static void scmTest() throws Exception {

		final BuildListener listener = null;
		final FilePath workspacePath = null;
		final EnvVars environment = null;

		final File workspace = null;

		final String gitExe = "";

		final GitClient git = Git.with(listener, environment).in(workspace)
				.using(gitExe).getClient();

		final String remoteName = "";
		final RefSpec refspec = null;

		git.fetch(remoteName, refspec);

		final ObjectId rev = null;

		git.merge(rev);

	}

	public static boolean checkin(final BuildContext context,
			final MavenModuleSet project) throws Exception {

		final SCM scm = project.getScm();

		if (scm instanceof GitSCM) {

			final GitSCM gitScm = (GitSCM) scm;

			final String gitExe = gitScm.getGitExe(
					context.build().getBuiltOn(), context.listener());

		}

		return false;
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

	public static Result scmCommit1(final BuildContext<?> context,
			final MavenModuleSet project) throws IOException,
			InterruptedException {
		final SCM scm = project.getScm();
		if (scm instanceof GitSCM) {
			final String result = DesignSCM.gitPush(context, project);
			context.logTab(result);
			return Result.SUCCESS;
		}
		throw new IllegalStateException("Unsupported SCM");
	}

	public static Result scmUpdate1(final BuildContext<?> context,
			final MavenModuleSet project) throws IOException,
			InterruptedException {
		final SCM scm = project.getScm();
		context.logTab("### scm:" + scm);
		if (scm instanceof GitSCM) {
			final String result = DesignSCM.gitPull(context, project);
			context.logTab(result);
			return Result.SUCCESS;
		}
		throw new IllegalStateException("Unsupported SCM");
	}

	public static String gitPull(final BuildContext<?> context,
			final MavenModuleSet project) throws IOException,
			InterruptedException {
		final String git = DesignSCM.gitExe(context, project);
		final File workDir = DesignSCM.gitDir(context, project);
		final String result = executeResult(workDir, git, "pull");
		return result;
	}

	public static String gitPush(final BuildContext<?> context,
			final MavenModuleSet project) throws IOException,
			InterruptedException {
		final String git = DesignSCM.gitExe(context, project);
		final File workDir = DesignSCM.gitDir(context, project);
		final String result = executeResult(workDir, git, "push");
		return result;
	}

	public static GitClient gitClient(final BuildContext<?> context,
			final MavenModuleSet project) {
	
		final String gitExe = gitExe(context, project);
	
		final File workspace = gitDir(context, project);
	
		final EnvVars environment = new EnvVars();
	
		final GitClient gitClient = Git.with(context.listener(), environment)
				.in(workspace).using(gitExe).getClient();
	
		return gitClient;
	
	}

	public static File gitDir(final BuildContext<?> context,
			final MavenModuleSet project) {
		final File workDir = new File(project.getWorkspace().getRemote());
		return workDir;
	}

	public static String gitExe(final BuildContext<?> context,
			final MavenModuleSet project) {
		final SCM scm = project.getScm();
		final GitSCM gitScm = (GitSCM) scm;
		final Node builtOn = context.build().getBuiltOn();
		final TaskListener listener = context.listener();
		return gitScm.getGitExe(builtOn, listener);
	}

	public static void log(final String title, final BuildContext<?> context,
			final Collection<?> list) {
		if (title != null) {
			context.logTab("### " + title);
		}
		for (final Object item : list) {
			context.logTab("### " + item);
		}
	}

}
