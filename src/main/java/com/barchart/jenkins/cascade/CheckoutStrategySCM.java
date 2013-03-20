/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.Extension;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractBuild.AbstractBuildExecution;
import hudson.model.AbstractProject;

import java.io.IOException;

import jenkins.scm.SCMCheckoutStrategyDescriptor;
import jenkins.scm.SCMCheckoutStrategy;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Checkout strategy for cascade workflow?
 * <p>
 * Pays attention to custom actions.
 * <p>
 * Performs synchronization.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class CheckoutStrategySCM extends SCMCheckoutStrategy {

	public static class TheDescriptor extends SCMCheckoutStrategyDescriptor {
		@Override
		public String getDisplayName() {
			return PluginConstants.PLUGIN_NAME + " Checkout Strategy";
		}

		@Override
		public boolean isApplicable(final AbstractProject project) {
			return ProjectIdentity.identity(project) != null;
		}
	}

	@Extension
	public static final TheDescriptor META = new TheDescriptor();

	@DataBoundConstructor
	public CheckoutStrategySCM() {
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void checkout(final AbstractBuildExecution execution)
			throws IOException, InterruptedException {

		final BuildContext context = new BuildContext(execution);

		context.log(META.getDisplayName());

		final ProjectIdentity identity = context.identity();

		if (identity == null) {
			context.log("Non-cascade project:");
			context.log("Perform default checkout.");
			super.checkout(execution);
			return;
		}

		context.log("Cascade family project.");

		if (CheckoutSkipAction.hasAction(context.build())) {
			context.log("Found " + CheckoutSkipAction.class.getSimpleName());
			context.log("Do not to perform checkout.");
			return;
		}

		final MavenModuleSet layoutProject = identity.layoutProject();
		if (layoutProject == null) {
			context.log("No layout project.");
			context.log("Perform default checkout.");
			super.checkout(execution);
			return;
		}

		if (context.layoutOptions().getUseSharedWorkspace()) {
			context.log("Using shared workspace.");
			context.log("Perform synchronized checkout.");
			synchronized (layoutProject) {
				super.checkout(execution);
			}
			return;
		} else {
			context.log("Using individual workspace.");
			context.log("Perform default checkout.");
			super.checkout(execution);
			return;
		}

	}

	@Override
	public TheDescriptor getDescriptor() {
		return META;
	}
}
