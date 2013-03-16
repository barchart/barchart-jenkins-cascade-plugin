/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import java.io.File;

import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;

/**
 * Plugin SCM GIT utilities.
 * 
 * @author Andrei Pozolotin
 */
public class PluginScmGit {

	/**
	 * See {@link Git#add()}
	 */
	public static DirCache doAdd(final File workspace, final String pattern) {
		try {
			final Git git = Git.open(workspace);
			return git.add().addFilepattern(pattern).call();
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * See {@link Git#checkout()}
	 */
	public static Ref doCheckout(final File workspace, final String branch) {
		try {
			final Git git = Git.open(workspace);
			return git.checkout().setName(branch).call();
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * See {@link Git#commit()}
	 */
	public static RevCommit doCommit(final File workspace,
			final PersonIdent person, final String message) {
		try {
			final Git git = Git.open(workspace);
			final CommitCommand command = git.commit();
			if (person != null) {
				command.setAuthor(person).setCommitter(person);
			}
			return command.setMessage(message).call();
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * See {@link Git#fetch()}
	 */
	public static FetchResult doFetch(final File workspace,
			final String remote, final RefSpec spec) {
		try {
			final Git git = Git.open(workspace);
			return git.fetch().setRemote(remote).setRefSpecs(spec).call();
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * See {@link Git#merge()}
	 */
	public static MergeResult doMerge(final File workspace,
			final ObjectId commit) {
		try {
			final Git git = Git.open(workspace);
			return git.merge().include(commit).call();
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * See {@link Git#pull()}
	 */
	public static PullResult doPull(final File workspace) {
		try {
			final Git git = Git.open(workspace);
			return git.pull().call();
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * See {@link Git#push()}
	 */
	public static Iterable<PushResult> doPush(final File workspace,
			final String remote, final RefSpec spec) {
		try {
			final Git git = Git.open(workspace);
			return git.push().setRemote(remote).setRefSpecs(spec).call();
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * See {@link Git#status()}
	 */
	public static Status doStatus(final File workspace) {
		try {
			final Git git = Git.open(workspace);
			return git.status().call();
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Short name of current branch.
	 */
	public static String branch(final File workspace) {
		try {
			final Git git = Git.open(workspace);
			return git.getRepository().getBranch();
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Spec name of current branch.
	 */
	public static String branchSpec(final File workspace) {
		try {
			final Git git = Git.open(workspace);
			return git.getRepository().getFullBranch();
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Find reference in repository.
	 */
	public static Ref findRef(final File workspace, final String name) {
		try {
			final Git git = Git.open(workspace);
			return git.getRepository().getRef(name);
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Source:Target branch reference for push.
	 * <p>
	 * Example: refs/heads/master:refs/heads/master
	 * */
	public static String ref(final boolean fast, final String localBranchName,
			final String remoteBranchName) {
		String ref = "";
		if (fast) {
			ref += "+";
		}
		return ref + refHeads(localBranchName) + ":"
				+ refHeads(remoteBranchName);
	}

	/**
	 * Source:Target branch reference for fetch.
	 * <p>
	 * Example: refs/heads/master:refs/remotes/origin/master
	 * */
	public static String ref(final boolean fast, final String remoteBranchName,
			final String remoteName, final String remoteTrackingBranchName) {
		String ref = "";
		if (fast) {
			ref += "+";
		}
		return ref + refHeads(remoteBranchName) + ":"
				+ refRemotes(remoteName, remoteTrackingBranchName);
	}

	/**
	 * Head branch reference.
	 * <p>
	 * Example: refs/heads/master
	 */
	public static String refHeads(final String branchName) {
		return Constants.R_HEADS + branchName;
	}

	/**
	 * Local remote tracking branch reference.
	 * <p>
	 * Example: refs/remotes/origin/master
	 */
	public static String refRemotes(final String remoteName,
			final String remoteBranchName) {
		return Constants.R_REMOTES + remoteName + "/" + remoteBranchName;
	}

	/**
	 * Source:Target branch reference for push w/o fast-forward.
	 * <p>
	 * Example: refs/heads/master:refs/heads/master
	 */
	public static RefSpec refSpec(final String localBranchName,
			final String remoteBranchName) {
		return new RefSpec(ref(false, localBranchName, remoteBranchName));
	}

	/**
	 * Source:Target branch reference for fetch with fast-forward.
	 * <p>
	 * Example: +refs/heads/master:refs/remotes/origin/master
	 */
	public static RefSpec refSpec(final String remoteBranchName,
			final String remoteName, final String remoteTrackingBranchName) {
		return new RefSpec(ref(true, remoteBranchName, remoteName,
				remoteTrackingBranchName));
	}

}
