/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import static com.barchart.jenkins.cascade.PluginUtilities.*;
import hudson.maven.MavenModuleSet;
import hudson.model.Result;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitException;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

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
	 * Verify Git SCM assumptions.
	 */
	public static Result checkGitScm(final BuildContext<?> context,
			final MavenModuleSet project) {

		final SCM scm = project.getScm();

		if (!(scm instanceof GitSCM)) {
			context.logErr("Unsupported SCM.");
			return Result.FAILURE;
		}

		final GitSCM gitScm = (GitSCM) scm;

		final List<RemoteConfig> repositoryList = gitScm.getRepositories();
		// log("repositoryList", context, repositoryList);

		if (repositoryList.size() != 1) {
			context.logErr("Cascade build needs single repository.");
			return Result.FAILURE;
		}

		final List<BranchSpec> branchList = gitScm.getBranches();
		// log("branchList", context, branchList);

		if (branchList.size() != 1) {
			context.logErr("Cascade build needs single branch.");
			return Result.FAILURE;
		}

		final String localBranchName = gitScm.getLocalBranch();
		// context.logTab("### localBranchName: " + localBranchName);

		if (localBranchName == null || localBranchName.length() == 0) {
			context.logErr("Cascade build needs local branch.");
			return Result.FAILURE;
		}

		return Result.SUCCESS;

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

	/**
	 * Commit into local.
	 */
	public static Result scmCommit(final BuildContext<?> context,
			final MavenModuleSet project) {

		if (isFailure(checkGitScm(context, project))) {
			return Result.FAILURE;
		}

		final GitSCM gitScm = (GitSCM) project.getScm();

		final RemoteConfig remoteConfig = gitScm.getRepositories().get(0);
		final BranchSpec remoteBranch = gitScm.getBranches().get(0);
		final String localBranchName = gitScm.getLocalBranch();
		final File workspace = workspace(context, project);

		final String remoteName = remoteConfig.getName();
		final String remoteBranchName = remoteBranch.getName();

		final String pomFile = project.getRootPOM(null);

		final Status status = PluginScmGit.doStatus(workspace);
		final Set<String> modifiedSet = status.getModified();
		// context.logTab("modifiedSet: " + modifiedSet);

		if (!modifiedSet.contains(pomFile)) {
			context.logTab("no change: " + pomFile);
			return Result.SUCCESS;
		}

		final DirCache addResult = PluginScmGit.doAdd(workspace, pomFile);
		context.logTab("added: " + pomFile);

		final String message = "[cascade] " + pomFile;
		final PersonIdent person = person(gitScm);

		final RevCommit commitResult = PluginScmGit.doCommit(workspace, person,
				message);
		context.logTab("commit: " + commitResult);

		if (!context.cascadeOptions().getShouldPushUpdates()) {
			return Result.SUCCESS;
		}

		final RefSpec pushSpec = PluginScmGit.refSpec(localBranchName,
				remoteBranchName);

		final Iterable<PushResult> pushResultList = PluginScmGit.doPush(
				workspace, remoteName, pushSpec);

		final PushResult pushResult = pushResultList.iterator().next();

		final String refHeads = PluginScmGit.refHeads(remoteBranchName);

		final RemoteRefUpdate remoteUpdate = pushResult
				.getRemoteUpdate(refHeads);

		final RemoteRefUpdate.Status pushStatus = remoteUpdate.getStatus();

		context.logTab("push status: " + pushStatus);

		if (pushStatus == RemoteRefUpdate.Status.OK) {
			return Result.SUCCESS;
		}

		return Result.FAILURE;

	}

	/**
	 * Update from remote.
	 */
	public static Result scmUpdate(final BuildContext<CascadeBuild> context,
			final MavenModuleSet project) throws GitException, IOException {

		if (isFailure(checkGitScm(context, project))) {
			return Result.FAILURE;
		}

		final GitSCM gitScm = (GitSCM) project.getScm();

		final RemoteConfig remoteConfig = gitScm.getRepositories().get(0);
		final BranchSpec remoteBranch = gitScm.getBranches().get(0);
		final String localBranchName = gitScm.getLocalBranch();
		final File workspace = workspace(context, project);

		final String localBranchCurrent = PluginScmGit.branch(workspace);

		if (!localBranchCurrent.equals(localBranchName)) {
			context.logErr("branch mismatch: " + localBranchCurrent + "/"
					+ localBranchName);
			return Result.FAILURE;
		}

		final String remoteName = remoteConfig.getName();
		final String remoteBranchName = remoteBranch.getName();

		/** Spec for the fetch mapping. */
		final RefSpec fetchSpec = PluginScmGit.refSpec(remoteBranchName,
				remoteName, remoteBranchName);

		final FetchResult fetchResult = PluginScmGit.doFetch(workspace,
				remoteName, fetchSpec);

		/** Spec of the head of the remote branch. */
		final String refHead = PluginScmGit.refHeads(remoteBranchName);

		/** Spec of the head of the local remote tracking branch. */
		final String refRemote = PluginScmGit.refRemotes(remoteName,
				remoteBranchName);

		// context.logTab("TRU");
		// for (final TrackingRefUpdate r : fetchResult.getTrackingRefUpdates())
		// {
		// context.logTab("### tru: " + r);
		// }

		// context.logTab("REF");
		// for (final Ref r : fetchResult.getAdvertisedRefs()) {
		// context.logTab("### ref: " + r);
		// }

		final TrackingRefUpdate trackingUpdate = fetchResult
				.getTrackingRefUpdate(refRemote);

		if (trackingUpdate == null) {
			context.logTab("fetch status: " + "no update");
			return Result.SUCCESS;
		} else {
			final RefUpdate.Result fetchStatus = trackingUpdate.getResult();
			context.logTab("fetch status: " + fetchStatus);
			if (fetchStatus == RefUpdate.Result.NO_CHANGE) {
				return Result.SUCCESS;
			}
		}

		/** Reference to head of the remote branch. */
		final Ref remoteHead = fetchResult.getAdvertisedRef(refHead);

		final ObjectId commit = remoteHead.getObjectId();
		// context.logTab("commit: " + commit);

		final MergeResult mergeResult = PluginScmGit.doMerge(workspace, commit);

		final MergeStatus mergeStatus = mergeResult.getMergeStatus();
		context.logTab("merge status: " + mergeStatus);

		if (mergeStatus.isSuccessful()) {
			return Result.SUCCESS;
		}

		return Result.FAILURE;

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

	public static void scmCheckout(final BuildContext<CascadeBuild> context) {
		// TODO Auto-generated method stub

	}

	public static void scmCheckin(final BuildContext<CascadeBuild> context) {
		// TODO Auto-generated method stub

	}

}
