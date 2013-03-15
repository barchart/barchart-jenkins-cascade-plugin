package com.barchart.jenkins.cascade;

import static com.barchart.jenkins.cascade.PluginUtilities.*;
import hudson.EnvVars;
import hudson.maven.MavenModuleSet;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.Node;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitException;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;

public class PluginScm {

	public static Result checkSCM(final BuildContext<?> context,
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

	/**
	 * Commit to local.
	 */
	public static Result scmCommit(final BuildContext<?> context,
			final MavenModuleSet project) {

		if (isFailure(checkSCM(context, project))) {
			return Result.FAILURE;
		}

		final GitSCM gitScm = (GitSCM) project.getScm();

		final RemoteConfig repository = gitScm.getRepositories().get(0);
		final BranchSpec remoteRranch = gitScm.getBranches().get(0);
		final String localBranchName = gitScm.getLocalBranch();

		final String remoteName = repository.getName();
		final String remoteBranchName = remoteRranch.getName();

		final File workspace = workspace(context, project);
		final String pomFile = project.getRootPOM(null);

		final Status status = PluginScmGit.doStatus(workspace);
		final Set<String> modifiedSet = status.getModified();

		context.logTab("modifiedSet: " + modifiedSet);

		if (!modifiedSet.contains(pomFile)) {
			context.logTab("no change: " + pomFile);
			return Result.SUCCESS;
		}

		final DirCache addResult = PluginScmGit.doAdd(workspace, pomFile);
		context.logTab("added: " + pomFile);

		final String message = "[cascade-process]";
		final PersonIdent person = PluginScmGit.person(gitScm);

		final RevCommit commitResult = PluginScmGit.doCommit(workspace, person,
				message);
		context.logTab("commit: " + commitResult);

		return Result.SUCCESS;

		// final RefSpec refSpec = PluginScmGit.refSpec(localBranchName,
		// remoteName, remoteBranchName);
		//
		// final Iterable<PushResult> pushResultList = PluginScmGit.doPush(
		// workspace, refSpec);
		//
		// final PushResult pushResult = pushResultList.iterator().next();
		//
		// final String refRemotes = PluginScmGit.refRemotes(remoteName,
		// remoteBranchName);
		//
		// final RemoteRefUpdate remoteUpdate = pushResult
		// .getRemoteUpdate(refRemotes);
		//
		// final RemoteRefUpdate.Status pushStatus = remoteUpdate.getStatus();
		//
		// context.logTab("push status: " + pushStatus);
		//
		// if (pushStatus == RemoteRefUpdate.Status.OK) {
		// return Result.SUCCESS;
		// }
		//
		// return Result.FAILURE;

	}

	/**
	 * Update to remote.
	 */
	public static Result scmUpdate(final BuildContext<CascadeBuild> context,
			final MavenModuleSet project) throws GitException, IOException {

		if (isFailure(checkSCM(context, project))) {
			return Result.FAILURE;
		}

		final GitSCM gitScm = (GitSCM) project.getScm();

		final RemoteConfig repository = gitScm.getRepositories().get(0);
		final BranchSpec remoteRranch = gitScm.getBranches().get(0);
		final String localBranchName = gitScm.getLocalBranch();

		final String remoteName = repository.getName();
		final String remoteBranchName = remoteRranch.getName();

		final RefSpec refSpec = PluginScmGit.refSpec(localBranchName,
				remoteName, remoteBranchName);

		final File workspace = workspace(context, project);

		final FetchResult fetchResult = PluginScmGit
				.doFetch(workspace, refSpec);

		final String refRemote = PluginScmGit.refRemotes(remoteName,
				remoteBranchName);

		final Ref ref = fetchResult.getAdvertisedRef(refRemote);

		// for (final Ref r : fetchResult.getAdvertisedRefs()) {
		// context.logTab("### ref: " + r.getName());
		// }
		//
		// final TrackingRefUpdate trackingUpdate = fetchResult
		// .getTrackingRefUpdate(refRemote);
		//
		// if (trackingUpdate == null) {
		// context.logTab("fetch status: " + "no change");
		// return Result.SUCCESS;
		// }
		//
		// final RefUpdate.Result fetchStatus = trackingUpdate.getResult();
		// context.logTab("fetch status: " + fetchStatus);
		//
		// if (fetchStatus == RefUpdate.Result.NO_CHANGE) {
		// return Result.SUCCESS;
		// }

		final ObjectId commit = ref.getObjectId();
		final MergeResult mergeResult = PluginScmGit.doMerge(workspace, commit);

		final MergeStatus mergeStatus = mergeResult.getMergeStatus();
		context.logTab("merge status: " + mergeStatus);

		if (mergeStatus.isSuccessful()) {
			return Result.SUCCESS;
		}

		return Result.FAILURE;

	}

	public static File workspace(final BuildContext<?> context,
			final MavenModuleSet project) {
		final String path = project.getWorkspace().getRemote();
		final File file = new File(path);
		return file;
	}

}
