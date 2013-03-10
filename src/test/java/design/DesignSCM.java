/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package design;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.LocalLauncher;
import hudson.maven.MavenModuleSet;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;

import java.io.File;

import com.barchart.jenkins.cascade.BuildContext;

public class DesignSCM {

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
