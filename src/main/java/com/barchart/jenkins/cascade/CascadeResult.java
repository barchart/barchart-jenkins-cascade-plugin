/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import org.apache.maven.artifact.Artifact;

/**
 * Persisted cascade result.
 * 
 * @author Andrei Pozolotin
 */
public class CascadeResult implements Comparable<CascadeResult> {

	private final Artifact artifact;

	private final String buildURL;

	public CascadeResult(final Artifact artifact, final String buildURL) {
		this.artifact = artifact;
		this.buildURL = buildURL;
	}

	/**
	 * Artifact which was released.
	 */
	@Jelly
	public Artifact getArtifact() {
		return artifact;
	}

	/**
	 * Member project build URL which released this artifact.
	 */
	@Jelly
	public String getBuildURL() {
		return buildURL;
	}

	@Override
	public String toString() {
		return getArtifact() + " @ " + getBuildURL();
	}

	public int compareTo(final CascadeResult that) {
		return this.getArtifact().compareTo(that.getArtifact());
	}

}
