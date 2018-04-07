/**
 * The MIT License (MIT)
 * Copyright (c) 2017 Microsoft Corporation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.documentdb.bulkexecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.microsoft.azure.documentdb.bulkexecutor.bulkimport.BulkImporter;

public class Main {

	public static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws Exception {

		CmdLineConfiguration cfg = parseCommandLineArgs(args);

		if (cfg.getOperation().equalsIgnoreCase("import")) {
			
			BulkImporter bulkImporter = new BulkImporter();
			bulkImporter.executeBulkImport(cfg);			
		}
	}

	private static CmdLineConfiguration parseCommandLineArgs(String[] args) {
		
		LOGGER.debug("Parsing the arguments ...");
		CmdLineConfiguration cfg = new CmdLineConfiguration();
		JCommander jcommander = null;
		
		try {
			
			jcommander = new JCommander(cfg, args);
		} catch (Exception e) {
			
			// invalid command line args
			System.err.println(e.getMessage());
			jcommander = new JCommander(cfg);
			jcommander.usage();
			System.exit(-1);
			return null;
		}

		if (cfg.isHelp()) {
			
			// prints out the usage help
			jcommander.usage();
			System.exit(0);
			return null;
		}
		return cfg;
	}
}
