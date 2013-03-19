/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.maven.MavenModuleSet;
import hudson.model.AbstractProject;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;

import java.io.File;
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
			final MavenModuleSet project) {

		final String message = checkScm(project);

		if (message != null) {
			throw new IllegalStateException(message);
		}

		final GitSCM gitScm = (GitSCM) project.getScm();
		final String localBranch = localBranchName(gitScm);
		final String remoteName = remoteName(gitScm);
		final String remoteBranch = remoteBranchName(gitScm);

		final File workspace = workspace(context, project);

		final RefSpec pushSpec = PluginScmGit
				.refPush(localBranch, remoteBranch);

		final Iterable<PushResult> pushResultList = PluginScmGit.doPush(
				workspace, remoteName, pushSpec);

		final PushResult pushResult = pushResultList.iterator().next();

		final String refHeads = PluginScmGit.refHeads(remoteBranch);

		final RemoteRefUpdate remoteUpdate = pushResult
				.getRemoteUpdate(refHeads);

		final RemoteRefUpdate.Status pushStatus = remoteUpdate.getStatus();

		context.logTab("push status: " + pushStatus);

		if (!PluginScmGit.isSuccess(pushStatus)) {
			throw new IllegalStateException("Unexpected");
		}

	}

	/**
	 * Copy from remote into local.
	 */
	public static void scmCheckout(final BuildContext<CascadeBuild> context,
			final MavenModuleSet project) {

		final String message = checkScm(project);

		if (message != null) {
			throw new IllegalStateException(message);
		}

		final GitSCM gitScm = (GitSCM) project.getScm();
		final String localBranch = localBranchName(gitScm);
		final String remoteURI = remoteURI(gitScm);
		final String remoteName = remoteName(gitScm);
		final String remoteBranch = remoteBranchName(gitScm);

		final File workspace = workspace(context, project);

		final boolean hasRepo = PluginScmGit.doRepoTest(workspace);

		if (hasRepo) {

			context.logTab("repository present");

			final Status status = PluginScmGit.doStatus(workspace);

			if (!status.isClean()) {
				context.logTab("repository needs cleanup");
				PluginScmGit.doReset(workspace);
			}

			/** Spec for the fetch mapping. */
			final RefSpec fetchSpec = PluginScmGit.refFetch(remoteBranch,
					remoteName, remoteBranch);

			final FetchResult fetchResult = PluginScmGit.doFetch(workspace,
					remoteName, fetchSpec);

			context.logTab("fetch result: "
					+ fetchResult.getTrackingRefUpdates().size());

			/** Spec of the head of the remote branch. */
			final String refHead = PluginScmGit.refHeads(remoteBranch);

			/** Reference to head of the remote branch. */
			final Ref remoteHead = fetchResult.getAdvertisedRef(refHead);
			if (remoteHead == null) {
				context.logErr("remote branch not found: " + refHead);
				throw new IllegalStateException("Unexpected");
			}

			final ObjectId commit = remoteHead.getObjectId();

			final MergeResult mergeResult = PluginScmGit.doMerge(workspace,
					commit);

			final MergeStatus mergeStatus = mergeResult.getMergeStatus();

			context.logTab("merge result: " + mergeStatus);

			if (!mergeStatus.isSuccessful()) {
				context.logTab("repository needs clone");
				PluginScmGit.doClone(workspace, remoteURI, remoteName);
			}

		} else {

			context.logTab("repository needs clone");
			PluginScmGit.doClone(workspace, remoteURI, remoteName);

		}

		final CheckoutResult checkoutResult = PluginScmGit.doCheckout(
				workspace, localBranch, remoteName, remoteBranch);

		final CheckoutResult.Status checkoutStatus = checkoutResult.getStatus();

		context.logTab("checkout status: " + checkoutStatus);

		if (!PluginScmGit.isSuccess(checkoutStatus)) {
			throw new IllegalStateException("Unexpected");
		}

		/** FIXME checkout does not work */
		PluginScmGit.doReset(workspace);

		final Ref ref = PluginScmGit.findRef(workspace, localBranch);

		context.logTab(localBranch + ": " + ref.getObjectId().name());

		/** TODO delete local tags */

	}

	/**
	 * Commit into local.
	 */
	public static void scmCommit(final BuildContext<?> context,
			final MavenModuleSet project, final String pattern) {

		final String message = checkScm(project);

		if (message != null) {
			throw new IllegalStateException(message);
		}

		final GitSCM gitScm = (GitSCM) project.getScm();

		final File workspace = workspace(context, project);

		final Status status = PluginScmGit.doStatus(workspace);
		final Set<String> modifiedSet = status.getModified();
		// context.logTab("modifiedSet: " + modifiedSet);

		if (!modifiedSet.contains(pattern)) {
			context.logTab("no change: " + pattern);
			return;
		}

		final DirCache addResult = PluginScmGit.doAdd(workspace, pattern);
		context.logTab("added: " + pattern);

		final String commitMessage = "[cascade]" + " " + pattern;
		final PersonIdent person = person(gitScm);

		final RevCommit commitResult = PluginScmGit.doCommit(workspace, person,
				commitMessage);
		context.logTab("commit: " + commitResult.name());

	}

	/**
	 * Update from remote.
	 */
	public static void scmUpdate(final BuildContext<CascadeBuild> context,
			final MavenModuleSet project) throws Exception {

		final String message = checkScm(project);

		if (message != null) {
			throw new IllegalStateException(message);
		}

		final GitSCM gitScm = (GitSCM) project.getScm();
		final String localBranch = localBranchName(gitScm);
		final String remoteName = remoteName(gitScm);
		final String remoteBranch = remoteBranchName(gitScm);

		final File workspace = workspace(context, project);

		final String localBranchCurrent = PluginScmGit.branch(workspace);

		if (!localBranchCurrent.equals(localBranch)) {
			context.logErr("branch mismatch: " + localBranchCurrent + "/"
					+ localBranch);
			throw new IllegalStateException("Unexpected");
		}

		/** Spec for the fetch mapping. */
		final RefSpec fetchSpec = PluginScmGit.refFetch(remoteBranch,
				remoteName, remoteBranch);

		final FetchResult fetchResult = PluginScmGit.doFetch(workspace,
				remoteName, fetchSpec);

		/** Spec of the head of the remote branch. */
		final String refHead = PluginScmGit.refHeads(remoteBranch);

		/** Spec of the head of the local remote tracking branch. */
		final String refRemote = PluginScmGit.refRemotes(remoteName,
				remoteBranch);

		final TrackingRefUpdate trackingUpdate = fetchResult
				.getTrackingRefUpdate(refRemote);

		if (trackingUpdate == null) {
			context.logTab("fetch status: " + "no update");
			return;
		} else {
			final RefUpdate.Result fetchStatus = trackingUpdate.getResult();
			context.logTab("fetch status: " + fetchStatus);
			if (fetchStatus == RefUpdate.Result.NO_CHANGE) {
				return;
			}
		}

		/** Reference to head of the remote branch. */
		final Ref remoteHead = fetchResult.getAdvertisedRef(refHead);

		final ObjectId commit = remoteHead.getObjectId();
		// context.logTab("commit: " + commit);

		final MergeResult mergeResult = PluginScmGit.doMerge(workspace, commit);

		final MergeStatus mergeStatus = mergeResult.getMergeStatus();
		context.logTab("merge status: " + mergeStatus);

		if (!mergeStatus.isSuccessful()) {
			throw new IllegalStateException("Unexpected");
		}

	}

	/**
	 * Find workspace for a project.
	 */
	public static File workspace(final BuildContext<?> context,
			final MavenModuleSet project) {
		final String path = project.getWorkspace().getRemote();
		final File file = new File(path);
		return file;
	}

	private PluginScm() {
	}

}
