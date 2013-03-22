/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package design.process;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;

public class ProcessUtil {

	/***/
	public static Process executeProcess(final File workDir,
			final String command) throws IOException, InterruptedException {
		final String[] termArray = command.split("\\s+");
		return executeProcess(workDir, termArray);
	}

	/***/
	public static Process executeProcess(final File workDir,
			final String... termArray) throws IOException, InterruptedException {
		final List<String> termList = Arrays.asList(termArray);
		final ProcessBuilder builder = new ProcessBuilder(termList);
		final Process process = builder.directory(workDir).start();
		process.waitFor();
		return process;
	}

	/***/
	public static String executeResult(final File workDir,
			final String... termArray) throws IOException, InterruptedException {
		final Process process = executeProcess(workDir, termArray);
		final String input = IOUtils
				.toString(process.getInputStream(), "UTF-8");
		final String error = IOUtils
				.toString(process.getErrorStream(), "UTF-8");
		final String result = input + error;
		return result;
	}

}
