/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package design;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.plugins.git.GitPublisher;
import hudson.plugins.git.GitPublisher.BranchToPush;
import hudson.plugins.git.GitPublisher.NoteToPush;
import hudson.plugins.git.GitPublisher.TagToPush;
import hudson.plugins.git.GitSCM;
import hudson.remoting.VirtualChannel;
import hudson.scm.SCM;

import java.io.File;
import java.io.IOException;
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

}
