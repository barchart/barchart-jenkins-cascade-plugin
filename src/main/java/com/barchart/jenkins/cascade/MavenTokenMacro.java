/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.Extension;
import hudson.maven.ModuleName;
import hudson.maven.MavenModule;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;

import java.io.IOException;

import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

/**
 * Evaluator for maven build parameters.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class MavenTokenMacro extends DataBoundTokenMacro {

	/**
	 * Maven project id as ${groupId}:${artifactId}
	 */
	public static final String TOKEN_PROJECT_ID = "MAVEN_PROJECT_ID";

	/**
	 * Maven ${groupId}
	 */
	public static final String TOKEN_GROUP_ID = "MAVEN_GROUP_ID";

	/**
	 * Maven ${artifactId}
	 */
	public static final String TOKEN_ARTIFACT_ID = "MAVEN_ARTIFACT_ID";

	@Override
	public boolean acceptsMacroName(final String macroName) {
		if (TOKEN_PROJECT_ID.equals(macroName)) {
			return true;
		}
		if (TOKEN_GROUP_ID.equals(macroName)) {
			return true;
		}
		if (TOKEN_ARTIFACT_ID.equals(macroName)) {
			return true;
		}
		return false;
	}

	@Override
	public String evaluate(final AbstractBuild<?, ?> build,
			final TaskListener listener, final String macroName)
			throws MacroEvaluationException, IOException, InterruptedException {

		final PluginLogger log = new PluginLogger(listener);

		final MavenModule module = PluginUtilities.mavenModule(build);

		if (module == null) {
			return "";
		}

		final ModuleName moduleName = module.getModuleName();

		if (TOKEN_PROJECT_ID.equals(macroName)) {
			return moduleName.toString();
		}
		if (TOKEN_GROUP_ID.equals(macroName)) {
			return moduleName.groupId;
		}
		if (TOKEN_ARTIFACT_ID.equals(macroName)) {
			return moduleName.artifactId;
		}
		return "";

	}

}
