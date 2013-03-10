/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import static com.barchart.jenkins.cascade.PluginUtilities.*;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;

/**
 * Build context bean.
 * 
 * @author Andrei Pozolotin
 */
public class BuildContext<B extends AbstractBuild> {

	private final AbstractBuild build;

	private final BuildListener listener;
	private final Set<Artifact> resultSet = new TreeSet<Artifact>();

	public BuildContext(//
			final AbstractBuild build, //
			final BuildListener listener //
	) {
		this.build = build;
		this.listener = listener;
	}

	public B build() {
		return (B) build;
	}

	public BuildListener listener() {
		return listener;
	}

	/** Log text with plug-in prefix. */
	public void log(final String text) {
		listener.getLogger()
				.println(PluginConstants.LOGGER_PREFIX + " " + text);
	}

	/** Log error with plug-in prefix. */
	public void logErr(final String text) {
		listener.error(PluginConstants.LOGGER_PREFIX + " " + text);
	}

	/** Log collected artifact results. */
	public void logResult(final String title) {
		log(title);
		if (resultSet.isEmpty()) {
			log("\t" + "Empty.");
		} else {
			for (final Artifact artifact : resultSet) {
				log("\t" + artifact);
			}
		}
	}

	/** Log text with plug-in prefix and a tab. */
	public void logTab(final String text) {
		log("\t" + text);
	}

	public void logExc(final Throwable e) {
		e.printStackTrace(listener().getLogger());
	}

	/** Collected artifact results. */
	public Set<Artifact> result() {
		return resultSet;
	}

	/** Store artifact result. */
	public void result(final Artifact artifact) {
		resultSet.add(artifact);
	}

	/** Store model result. */
	public void result(final Model model) {
		ensureFields(model);
		result(mavenArtifact(model));
	}

}
