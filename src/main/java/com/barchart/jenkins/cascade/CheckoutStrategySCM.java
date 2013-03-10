/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.Extension;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractBuild.AbstractBuildExecution;
import hudson.model.AbstractProject;

import java.io.IOException;

import jenkins.scm.SCMCheckoutStrategyDescriptor;
import jenkins.scm.SCMCheckoutStrategy;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Checkout strategy that pays attention to custom actions.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class CheckoutStrategySCM extends SCMCheckoutStrategy {

	@Extension
	public static class TheDescriptor extends SCMCheckoutStrategyDescriptor {
		@Override
		public String getDisplayName() {
			return "Cascade Release Stragegy";
		}

		@Override
		public boolean isApplicable(final AbstractProject project) {
			return true;
		}
	}

	@DataBoundConstructor
	public CheckoutStrategySCM() {
	}

	@Override
	public void checkout(final AbstractBuildExecution execution)
			throws IOException, InterruptedException {

		final AbstractBuild build = (AbstractBuild) execution.getBuild();
		final BuildListener listener = execution.getListener();

		final BuildContext context = new BuildContext(build, listener);

		final CheckoutSkipAction skipAction = build
				.getAction(CheckoutSkipAction.class);

		if (skipAction == null) {
			context.log("Normal checkout.");
			super.checkout(execution);
		} else {
			context.log("Ignore checkout.");
		}

	}

}
