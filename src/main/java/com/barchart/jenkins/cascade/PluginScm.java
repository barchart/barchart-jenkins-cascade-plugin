/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractProject;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.remoting.VirtualChannel;
import hudson.scm.SCM;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.TrackingRefUpdate;

/**
 * Plugin SCM utilities.
 * 
 * @author Andrei Pozolotin
 */
public class PluginScm {

	/**
	 * Verify jenkins scm assumptions for cascade to work.
	 */
	public static String checkScm(final AbstractProject<?, ?> project) {

		final SCM scm = project.getScm();

		if (!(scm instanceof GitSCM)) {
			return "Unsupported SCM: " + scm;
		}

		final GitSCM gitScm = (GitSCM) scm;

		final List<RemoteConfig> repositoryList = gitScm.getRepositories();

		if (repositoryList.size() != 1) {
			return "Cascade build needs single remote repository;"
					+ " current count=" + repositoryList.size();
		}

		final List<BranchSpec> branchList = gitScm.getBranches();

		if (branchList.size() != 1) {
			return "Cascade build needs single remote branch;"
					+ " current count=" + branchList.size();
		}

		final String remoteBranchName = branchList.get(0).getName();

		if (remoteBranchName == null || remoteBranchName.length() == 0) {
			return "Cascade build needs remote branch;"
					+ " current remoteBranchName=" + remoteBranchName;
		}

		if (remoteBranchName.contains("*")) {
			return "Cascade remote branch can not be a wildcard;"
					+ " current remoteBranchName=" + remoteBranchName;
		}

		final String localBranchName = gitScm.getLocalBranch();

		if (localBranchName == null || localBranchName.length() == 0) {
			return "Cascade build needs local branch;"
					+ " current localBranchName=" + localBranchName;
		}

		if (localBranchName.contains("*")) {
			return "Cascade local branch can not be a wildcard;"
					+ " current localBranchName=" + localBranchName;
		}

		return null;

	}

	public static String localBranchName(final GitSCM gitScm) {
		return gitScm.getLocalBranch();
	}

	/**
	 * Convert identity from jenkins to jgit.
	 */
	public static PersonIdent person(final GitSCM gitScm) {
		final String name = gitScm.getGitConfigNameToUse();
		if (name == null) {
			return null;
		}
		final String mail = gitScm.getGitConfigEmailToUse();
		if (mail == null) {
			return null;
		}
		final PersonIdent person = new PersonIdent(name, mail);
		return person;

	}

	public static String remoteBranchName(final GitSCM gitScm) {
		return gitScm.getBranches().get(0).getName();
	}

	public static String remoteName(final GitSCM gitScm) {
		return gitScm.getRepositories().get(0).getName();
	}

	public static String remoteURI(final GitSCM gitScm) {
		return gitScm.getRepositories().get(0).getURIs().get(0)
				.toPrivateString();
	}

	/**
	 * Transmit into remote.
	 */
	public static void scmCheckin(final BuildContext<CascadeBuild> context,
			final MavenModuleSet project) throws IOException,
			InterruptedException {

		final String message = checkScm(project);

		if (message != null) {
			throw new IllegalStateException(message);
		}

		final GitSCM gitScm = (GitSCM) project.getScm();
		final FilePath workspace = workspace(context, project);

		/** Remote objects. */
		final BuildLogger logger = context.logger();
		final String localBranch = localBranchName(gitScm);
		final String remoteName = remoteName(gitScm);
		final String remoteBranch = remoteBranchName(gitScm);

		/** Remote operation. */
		final FileCallable<Void> callable = new FileCallable<Void>() {

			private static final long serialVersionUID = 1L;

			public Void invoke(final File basedir, final VirtualChannel channel)
					throws IOException, InterruptedException {

				final RefSpec pushSpec = PluginScmGit.refPush(localBranch,
						remoteBranch);

				final Iterable<PushResult> pushResultList = PluginScmGit
						.doPush(basedir, remoteName, pushSpec);

				final PushResult pushResult = pushResultList.iterator().next();

				final String refHeads = PluginScmGit.refHeads(remoteBranch);

				final RemoteRefUpdate remoteUpdate = pushResult
						.getRemoteUpdate(refHeads);

				final RemoteRefUpdate.Status pushStatus = remoteUpdate
						.getStatus();

				logger.logTab("push status: " + pushStatus);

				if (!PluginScmGit.isSuccess(pushStatus)) {
					throw new IllegalStateException("Unexpected");
				}

				return null;
			}
		};

		workspace.act(callable);

	}

	/**
	 * Copy from remote into local.
	 */
	public static void scmCheckout(final BuildContext<CascadeBuild> context,
			final MavenModuleSet project) throws IOException,
			InterruptedException {

		final String message = checkScm(project);

		if (message != null) {
			throw new IllegalStateException(message);
		}

		final GitSCM gitScm = (GitSCM) project.getScm();
		final FilePath workspace = workspace(context, project);

		/** Remote objects. */
		final BuildLogger logger = context.logger();
		final String localBranch = localBranchName(gitScm);
		final String remoteURI = remoteURI(gitScm);
		final String remoteName = remoteName(gitScm);
		final String remoteBranch = remoteBranchName(gitScm);

		/** Remote operation. */
		final FileCallable<String> callable = new FileCallable<String>() {

			private static final long serialVersionUID = 1L;

			public String invoke(final File basedir,
					final VirtualChannel channel) throws IOException,
					InterruptedException {

				final boolean hasRepo = PluginScmGit.doRepoTest(basedir);

				if (hasRepo) {

					logger.logTab("repository present");

					final Status status = PluginScmGit.doStatus(basedir);

					if (!status.isClean()) {
						logger.logTab("repository needs cleanup");
						PluginScmGit.doReset(basedir);
					}

					/** Spec for the fetch mapping. */
					final RefSpec fetchSpec = PluginScmGit.refFetch(
							remoteBranch, remoteName, remoteBranch);

					final FetchResult fetchResult = PluginScmGit.doFetch(
							basedir, remoteName, fetchSpec);

					logger.logTab("fetch result: "
							+ fetchResult.getTrackingRefUpdates().size());

					/** Spec of the head of the remote branch. */
					final String refHead = PluginScmGit.refHeads(remoteBranch);

					/** Reference to head of the remote branch. */
					final Ref remoteHead = fetchResult
							.getAdvertisedRef(refHead);
					if (remoteHead == null) {
						logger.logErr("remote branch not found: " + refHead);
						throw new IllegalStateException("Unexpected");
					}

					final ObjectId commit = remoteHead.getObjectId();

					final MergeResult mergeResult = PluginScmGit.doMerge(
							basedir, commit);

					final MergeStatus mergeStatus = mergeResult
							.getMergeStatus();

					logger.logTab("merge result: " + mergeStatus);

					if (!mergeStatus.isSuccessful()) {
						logger.logTab("repository needs clone");
						PluginScmGit.doClone(basedir, remoteURI, remoteName);
					}

				} else {

					logger.logTab("repository needs clone");
					PluginScmGit.doClone(basedir, remoteURI, remoteName);

				}

				final CheckoutResult checkoutResult = PluginScmGit.doCheckout(
						basedir, localBranch, remoteName, remoteBranch);

				final CheckoutResult.Status checkoutStatus = checkoutResult
						.getStatus();

				logger.logTab("checkout status: " + checkoutStatus);

				if (!PluginScmGit.isSuccess(checkoutStatus)) {
					throw new IllegalStateException("Unexpected");
				}

				/** FIXME checkout does not work */
				PluginScmGit.doReset(basedir);

				final Ref ref = PluginScmGit.findRef(basedir, localBranch);

				logger.logTab(localBranch + ": " + ref.getObjectId().name());

				/** TODO delete local tags */

				return null;
			}
		};

		workspace.act(callable);

	}

	/**
	 * Commit into local.
	 */
	public static void scmCommit(final BuildContext<?> context,
			final MavenModuleSet project, final String pattern)
			throws IOException, InterruptedException {

		final String message = checkScm(project);

		if (message != null) {
			throw new IllegalStateException(message);
		}

		final GitSCM gitScm = (GitSCM) project.getScm();
		final FilePath workspace = workspace(context, project);

		/** Remote objects. */
		final BuildLogger logger = context.logger();
		final PersonIdent person = person(gitScm);

		/** Remote operation. */
		final FileCallable<Void> callable = new FileCallable<Void>() {

			private static final long serialVersionUID = 1L;

			public Void invoke(final File basedir, final VirtualChannel channel)
					throws IOException, InterruptedException {

				final Status status = PluginScmGit.doStatus(basedir);
				final Set<String> modifiedSet = status.getModified();
				// logger.logTab("modifiedSet: " + modifiedSet);

				if (!modifiedSet.contains(pattern)) {
					logger.logTab("no change: " + pattern);
					return null;
				}

				final DirCache addResult = PluginScmGit.doAdd(basedir, pattern);
				logger.logTab("added: " + pattern);

				final String commitMessage = "[cascade]" + " " + pattern;

				final RevCommit commitResult = PluginScmGit.doCommit(basedir,
						person, commitMessage);
				logger.logTab("commit: " + commitResult.name());

				return null;
			}
		};

		workspace.act(callable);

	}

	/**
	 * Update from remote.
	 */
	public static void scmUpdate(final BuildContext<CascadeBuild> context,
			final MavenModuleSet project) throws IOException,
			InterruptedException {

		final String message = checkScm(project);

		if (message != null) {
			throw new IllegalStateException(message);
		}

		final GitSCM gitScm = (GitSCM) project.getScm();
		final FilePath workspace = workspace(context, project);

		/** Remote objects. */
		final BuildLogger logger = context.logger();
		final String localBranch = localBranchName(gitScm);
		final String remoteName = remoteName(gitScm);
		final String remoteBranch = remoteBranchName(gitScm);

		/** Remote operation. */
		final FileCallable<Void> callable = new FileCallable<Void>() {

			private static final long serialVersionUID = 1L;

			public Void invoke(final File basedir, final VirtualChannel channel)
					throws IOException, InterruptedException {

				final String localBranchCurrent = PluginScmGit.branch(basedir);

				if (!localBranchCurrent.equals(localBranch)) {
					logger.logErr("branch mismatch: " + localBranchCurrent
							+ "/" + localBranch);
					throw new IllegalStateException("Unexpected");
				}

				/** Spec for the fetch mapping. */
				final RefSpec fetchSpec = PluginScmGit.refFetch(remoteBranch,
						remoteName, remoteBranch);

				final FetchResult fetchResult = PluginScmGit.doFetch(basedir,
						remoteName, fetchSpec);

				/** Spec of the head of the remote branch. */
				final String refHead = PluginScmGit.refHeads(remoteBranch);

				/** Spec of the head of the local remote tracking branch. */
				final String refRemote = PluginScmGit.refRemotes(remoteName,
						remoteBranch);

				final TrackingRefUpdate trackingUpdate = fetchResult
						.getTrackingRefUpdate(refRemote);

				if (trackingUpdate == null) {
					logger.logTab("fetch status: " + "no update");
					return null;
				} else {
					final RefUpdate.Result fetchStatus = trackingUpdate
							.getResult();
					logger.logTab("fetch status: " + fetchStatus);
					if (fetchStatus == RefUpdate.Result.NO_CHANGE) {
						return null;
					}
				}

				/** Reference to head of the remote branch. */
				final Ref remoteHead = fetchResult.getAdvertisedRef(refHead);

				final ObjectId commit = remoteHead.getObjectId();
				// logger.logTab("commit: " + commit);

				final MergeResult mergeResult = PluginScmGit.doMerge(basedir,
						commit);

				final MergeStatus mergeStatus = mergeResult.getMergeStatus();
				logger.logTab("merge status: " + mergeStatus);

				if (!mergeStatus.isSuccessful()) {
					throw new IllegalStateException("Unexpected");
				}

				return null;
			}
		};

		workspace.act(callable);

	}

	/**
	 * Find workspace for a project.
	 */
	public static FilePath workspace(final BuildContext<?> context,
			final MavenModuleSet project) {
		return project.getWorkspace();
	}

	private PluginScm() {
	}

}
