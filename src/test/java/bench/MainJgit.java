/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package bench;

import java.io.File;

import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.util.FileUtils;

import com.barchart.jenkins.cascade.PluginScmGit;

public class MainJgit {

	/**
	 */
	public static void main(final String[] args) throws Exception {

		final File baseDir = new File(".");

		final File folder = new File(baseDir, "target");

		final File workspace = new File(folder, "workspace");

		FileUtils.delete(workspace, FileUtils.IGNORE_ERRORS
				| FileUtils.RECURSIVE);

		FileUtils.mkdirs(workspace, true);

		final String remoteURI = "git@github.com:barchart/barchart-jenkins-tester.git";
		final String remoteName = "archon";
		final String remoteBranch = "master";

		final String localBranch = "cascade";

		{
			final Git git = PluginScmGit.doClone(workspace, remoteURI,
					remoteName);
			final Repository repo = git.getRepository();
			System.out.println("repo " + repo);
		}

		{
			final CheckoutResult result = PluginScmGit.doCheckout(workspace,
					localBranch, remoteName, remoteBranch);
			System.out.println("checkout " + result.getStatus());
		}

		{
			final CheckoutResult result = PluginScmGit.doCheckout(workspace,
					localBranch, remoteName, remoteBranch);
			System.out.println("checkout " + result.getStatus());
		}

		{
			final RefSpec fetchSpec = PluginScmGit.refFetch(remoteBranch,
					remoteName, remoteBranch);

			final FetchResult fetchResult = PluginScmGit.doFetch(workspace,
					remoteName, fetchSpec);
			System.out.println("fetch satus: "
					+ fetchResult.getAdvertisedRefs());

			final String refHead = PluginScmGit.refHeads(remoteBranch);

			final Ref remoteHead = fetchResult.getAdvertisedRef(refHead);

			final ObjectId commit = remoteHead.getObjectId();

			final MergeResult mergeResult = PluginScmGit.doMerge(workspace,
					commit);

			final MergeStatus mergeStatus = mergeResult.getMergeStatus();
			System.out.println("merge status: " + mergeStatus);

		}

	}

}
