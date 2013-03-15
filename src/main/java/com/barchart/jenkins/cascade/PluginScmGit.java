package com.barchart.jenkins.cascade;

import hudson.plugins.git.GitSCM;

import java.io.File;

import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;

public class PluginScmGit {

	public static DirCache doAdd(final File workspace, final String pattern) {
		try {
			final Git git = Git.open(workspace);
			return git.add().addFilepattern(pattern).call();
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

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

	public static FetchResult doFetch(final File workspace, final RefSpec spec) {
		try {
			final Git git = Git.open(workspace);
			return git.fetch().setRefSpecs(spec).call();
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static MergeResult doMerge(final File workspace,
			final ObjectId commit) {
		try {
			final Git git = Git.open(workspace);
			return git.merge().include(commit).call();
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static Iterable<PushResult> doPush(final File workspace,
			final RefSpec spec) {
		try {
			final Git git = Git.open(workspace);
			return git.push().setRefSpecs(spec).call();
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static Status doStatus(final File workspace) {
		try {
			final Git git = Git.open(workspace);
			return git.status().call();
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

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

	/** refs/head/master:refs/remotes/origin/master */
	public static String ref(final String localBranchName,
			final String remoteName, final String remoteBranchName) {
		return refHeads(localBranchName) + ":"
				+ refRemotes(remoteName, remoteBranchName);
	}

	/** refs/head/master */
	public static String refHeads(final String localBranchName) {
		return Constants.R_HEADS + localBranchName;
	}

	/** refs/remotes/origin/master */
	public static String refRemotes(final String remoteName,
			final String remoteBranchName) {
		return Constants.R_REMOTES + remoteName + "/" + remoteBranchName;
	}

	/** refs/head/master:refs/remotes/origin/master */
	public static RefSpec refSpec(final String localBranchName,
			final String remoteName, final String remoteBranchName) {
		return new RefSpec(ref(localBranchName, remoteName, remoteBranchName));
	}

}
