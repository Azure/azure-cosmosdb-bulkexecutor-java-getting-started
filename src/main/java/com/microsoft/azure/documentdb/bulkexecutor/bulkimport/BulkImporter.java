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
package com.microsoft.azure.documentdb.bulkexecutor.bulkimport;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.microsoft.azure.documentdb.ConnectionPolicy;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.documentdb.DocumentClientException;
import com.microsoft.azure.documentdb.DocumentCollection;
import com.microsoft.azure.documentdb.FeedResponse;
import com.microsoft.azure.documentdb.Offer;
import com.microsoft.azure.documentdb.RetryOptions;
import com.microsoft.azure.documentdb.bulkexecutor.CmdLineConfiguration;
import com.microsoft.azure.documentdb.bulkexecutor.DocumentBulkExecutor;
import com.microsoft.azure.documentdb.bulkexecutor.Main;
import com.microsoft.azure.documentdb.bulkexecutor.DocumentBulkExecutor.Builder;
import com.microsoft.azure.documentdb.bulkexecutor.bulkimport.BulkImportResponse;

public class BulkImporter {

	public static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	public void executeBulkImport(CmdLineConfiguration cfg) throws Exception {
		try (DocumentClient client = documentClientFrom(cfg)) {

			// Set retry options high for initialization
			client.getConnectionPolicy().getRetryOptions().setMaxRetryWaitTimeInSeconds(120);
			client.getConnectionPolicy().getRetryOptions().setMaxRetryAttemptsOnThrottledRequests(100);

			// Also it is a good idea to set your connection pool size to be equal to the
			// number of partitions serving your collection

			// This assumes database and collection already exist
			String collectionLink = String.format("/dbs/%s/colls/%s", cfg.getDatabaseId(), cfg.getCollectionId());

			DocumentCollection collection = client.readCollection(collectionLink, null).getResource();

			// You can specify the maximum throughput (out of entire collection's
			// throughput) that you wish the bulk import API to consume here
			int offerThroughput = getOfferThroughput(client, collection);

			Builder bulkExecutorBuilder = DocumentBulkExecutor.builder().from(client, cfg.getDatabaseId(),
					cfg.getCollectionId(), collection.getPartitionKey(), offerThroughput);

			// Instantiate bulk executor
			try (DocumentBulkExecutor bulkExecutor = bulkExecutorBuilder.build()) {

				// Set retries to 0 to pass control to bulk executor
				client.getConnectionPolicy().getRetryOptions().setMaxRetryWaitTimeInSeconds(0);
				client.getConnectionPolicy().getRetryOptions().setMaxRetryAttemptsOnThrottledRequests(0);

				Stopwatch fromStartToEnd = Stopwatch.createStarted();

				Stopwatch totalWatch = Stopwatch.createUnstarted();

				double totalRequestCharge = 0;
				long totalTimeInMillis = 0;
				long totalNumberOfDocumentsImported = 0;

				for (int i = 0; i < cfg.getNumberOfCheckpoints(); i++) {

					BulkImportResponse bulkImportResponse;

					Collection<String> documents = DataMigrationDocumentSource
							.loadDocuments(cfg.getNumberOfDocumentsForEachCheckpoint(), collection.getPartitionKey());

					if (documents.size() != cfg.getNumberOfDocumentsForEachCheckpoint()) {
						throw new RuntimeException("not enough documents generated");
					}

					// Interested in the bulk import time, loading/generating documents is out of
					// the scope of bulk executor and so has to be excluded
					totalWatch.start();
					bulkImportResponse = bulkExecutor.importAll(documents, false);
					totalWatch.stop();

					System.out.println(
							"##########################################################################################");

					totalNumberOfDocumentsImported += bulkImportResponse.getNumberOfDocumentsImported();
					totalTimeInMillis += bulkImportResponse.getTotalTimeTaken().toMillis();
					totalRequestCharge += bulkImportResponse.getTotalRequestUnitsConsumed();

					// Print statistics for current checkpoint
					System.out.println("Number of documents inserted in this checkpoint: "
							+ bulkImportResponse.getNumberOfDocumentsImported());
					System.out.println("Import time for this checkpoint in milli seconds "
							+ bulkImportResponse.getTotalTimeTaken().toMillis());
					System.out.println("Total request unit consumed in this checkpoint: "
							+ bulkImportResponse.getTotalRequestUnitsConsumed());

					System.out.println("Average RUs/second in this checkpoint: "
							+ bulkImportResponse.getTotalRequestUnitsConsumed()
									/ (0.001 * bulkImportResponse.getTotalTimeTaken().toMillis()));
					System.out.println("Average #Inserts/second in this checkpoint: "
							+ bulkImportResponse.getNumberOfDocumentsImported()
									/ (0.001 * bulkImportResponse.getTotalTimeTaken().toMillis()));
					System.out.println(
							"##########################################################################################");

					// Check the number of imported documents to ensure everything is successfully
					// imported
					if (bulkImportResponse.getNumberOfDocumentsImported() != cfg
							.getNumberOfDocumentsForEachCheckpoint()) {
						System.err.println(
								"Some documents failed to get inserted in this checkpoint. This checkpoint has to get retried with upsert enabled");
						System.err.println("Number of surfaced failures: " + bulkImportResponse.getErrors().size());
						for (int j = 0; j < bulkImportResponse.getErrors().size(); j++) {
							bulkImportResponse.getErrors().get(j).printStackTrace();
						}
						break;
					}
				}

				fromStartToEnd.stop();

				// Print average statistics across checkpoints
				System.out.println(
						"##########################################################################################");
				System.out
						.println("Total import time including data generation: " + fromStartToEnd.elapsed().toMillis());
				System.out.println(
						"Total import time in milli seconds measured by stopWatch: " + totalWatch.elapsed().toMillis());
				System.out.println("Total import time in milli seconds measured by api : " + totalTimeInMillis);
				System.out.println("Total Number of documents inserted " + totalNumberOfDocumentsImported);
				System.out.println("Total request unit consumed: " + totalRequestCharge);
				System.out.println(
						"Average RUs/second:" + totalRequestCharge / (totalWatch.elapsed().toMillis() * 0.001));
				System.out.println("Average #Inserts/second: "
						+ totalNumberOfDocumentsImported / (totalWatch.elapsed().toMillis() * 0.001));

			}
		}

	}

	private static DocumentClient documentClientFrom(CmdLineConfiguration cfg) throws DocumentClientException {

		ConnectionPolicy policy = new ConnectionPolicy();
		RetryOptions retryOptions = new RetryOptions();
		retryOptions.setMaxRetryAttemptsOnThrottledRequests(0);
		policy.setRetryOptions(retryOptions);
		policy.setConnectionMode(cfg.getConnectionMode());
		policy.setMaxPoolSize(cfg.getMaxConnectionPoolSize());

		return new DocumentClient(cfg.getServiceEndpoint(), cfg.getMasterKey(), policy, cfg.getConsistencyLevel());
	}

	private static int getOfferThroughput(DocumentClient client, DocumentCollection collection) {
		FeedResponse<Offer> offers = client.queryOffers(
				String.format("SELECT * FROM c where c.offerResourceId = '%s'", collection.getResourceId()), null);

		List<Offer> offerAsList = offers.getQueryIterable().toList();
		if (offerAsList.isEmpty()) {
			throw new IllegalStateException("Cannot find Collection's corresponding offer");
		}

		Offer offer = offerAsList.get(0);
		return offer.getContent().getInt("offerThroughput");
	}
}
