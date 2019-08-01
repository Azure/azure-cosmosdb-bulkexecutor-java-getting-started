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
package com.microsoft.azure.cosmosdb.bulkexecutor.bulkupdate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.microsoft.azure.cosmosdb.bulkexecutor.CmdLineConfiguration;
import com.microsoft.azure.cosmosdb.bulkexecutor.Utilities;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.documentdb.DocumentCollection;
import com.microsoft.azure.documentdb.bulkexecutor.BulkImportFailure;
import com.microsoft.azure.documentdb.bulkexecutor.BulkUpdateFailure;
import com.microsoft.azure.documentdb.bulkexecutor.BulkUpdateResponse;
import com.microsoft.azure.documentdb.bulkexecutor.DocumentBulkExecutor;
import com.microsoft.azure.documentdb.bulkexecutor.DocumentBulkExecutor.Builder;
import com.microsoft.azure.documentdb.bulkexecutor.SetUpdateOperation;
import com.microsoft.azure.documentdb.bulkexecutor.UnsetUpdateOperation;
import com.microsoft.azure.documentdb.bulkexecutor.UpdateItem;
import com.microsoft.azure.documentdb.bulkexecutor.UpdateOperationBase;

public class BulkUpdater {
	
	public static final Logger LOGGER = LoggerFactory.getLogger(BulkUpdater.class);

	/**
	 * In this sample, we assume documents have been loaded into the collection using BulkImporter with the schema mentioned 
	 * in DataMigrationDocumentSource, and we perform the following updates to each document:
	 * - We set the 'f0' property to a new value
	 * - We unset the 'f1' property
	 * 
	 * You can modify the sample to perform other update operations such as increment, array push/remove
	 * 
	 * @param cfg Command line configuration settings passed
	 * @throws Exception
	 */
	public void executeBulkUpdate(CmdLineConfiguration cfg) throws Exception {
		try (DocumentClient client = Utilities.documentClientFrom(cfg)) {

			// Tip: It is a good idea to set your connection pool size to be equal to the
			// number of partitions serving your collection

			// Set client's retry options high for initialization
			client.getConnectionPolicy().getRetryOptions().setMaxRetryWaitTimeInSeconds(120);
			client.getConnectionPolicy().getRetryOptions().setMaxRetryAttemptsOnThrottledRequests(100);
			
			DocumentCollection collection = null;
			if(cfg.getShouldCreateCollection()) {
				collection = Utilities.createEmptyCollectionIfNotExists(client, cfg.getDatabaseId(), cfg.getCollectionId(),
						cfg.getPartitionKey(), cfg.getCollectionThroughput());
			}
			else {
				// This assumes database and collection already exist
				String collectionLink = String.format("/dbs/%s/colls/%s", cfg.getDatabaseId(), cfg.getCollectionId());
				collection = client.readCollection(collectionLink, null).getResource();
			}
			
			// You can specify the maximum throughput (out of entire collection's throughput) that you wish the bulk import API to consume here
			int offerThroughput = Utilities.getOfferThroughput(client, collection);

			Builder bulkExecutorBuilder = DocumentBulkExecutor.builder().from(client, cfg.getDatabaseId(),
					cfg.getCollectionId(), collection.getPartitionKey(), offerThroughput);

			// Instantiate bulk executor
			try (DocumentBulkExecutor bulkExecutor = bulkExecutorBuilder.build()) {

				// Set retries to 0 to pass control to bulk executor
				client.getConnectionPolicy().getRetryOptions().setMaxRetryWaitTimeInSeconds(0);
				client.getConnectionPolicy().getRetryOptions().setMaxRetryAttemptsOnThrottledRequests(0);

				// Create the update operations list
				
	            // Set operation
	            SetUpdateOperation<String> f0Update = new SetUpdateOperation<>("f0","UpdatedDocValue");
	            
	            // Unset Operation
	            UnsetUpdateOperation f1Update = new UnsetUpdateOperation("f1");
	            
	            // Note: Add other update operations if needed.
	            
	            ArrayList<UpdateOperationBase> updateOperations = new ArrayList<>();
	            updateOperations.add(f0Update);
	            updateOperations.add(f1Update);
				
				Stopwatch totalWatch = Stopwatch.createUnstarted();

				double totalRequestCharge = 0;
				long totalTimeInMillis = 0;
				long totalNumberOfDocumentsUpdated = 0;
	            
				for (int i = 0; i < cfg.getNumberOfCheckpoints(); i++) {

					// Generate update items				
					long prefix = i * cfg.getNumberOfDocumentsForEachCheckpoint();
					
					List<UpdateItem> updateItems = new ArrayList<>(cfg.getNumberOfDocumentsForEachCheckpoint());
					IntStream.range(0, cfg.getNumberOfDocumentsForEachCheckpoint()).mapToObj(j -> {
						
						return new UpdateItem(Long.toString(prefix + j), Long.toString(prefix + j), updateOperations);
					}).collect(Collectors.toCollection(() -> updateItems));
					
					// Execute bulk update API				
					totalWatch.start();
					BulkUpdateResponse bulkUpdateResponse = bulkExecutor.updateAll(updateItems, null);
					totalWatch.stop();

					// Print statistics for this checkpoint				
					System.out.println(
							"##########################################################################################");

					totalNumberOfDocumentsUpdated += bulkUpdateResponse.getNumberOfDocumentsUpdated();
					totalTimeInMillis += bulkUpdateResponse.getTotalTimeTaken().toMillis();
					totalRequestCharge += bulkUpdateResponse.getTotalRequestUnitsConsumed();

					// Print statistics for current checkpoint
					System.out.println("Number of documents updated in this checkpoint: "
							+ bulkUpdateResponse.getNumberOfDocumentsUpdated());
					System.out.println("Update time for this checkpoint in milli seconds "
							+ bulkUpdateResponse.getTotalTimeTaken().toMillis());
					System.out.println("Total request unit consumed in this checkpoint: "
							+ bulkUpdateResponse.getTotalRequestUnitsConsumed());

					System.out.println("Average RUs/second in this checkpoint: "
							+ bulkUpdateResponse.getTotalRequestUnitsConsumed()
									/ (0.001 * bulkUpdateResponse.getTotalTimeTaken().toMillis()));
					System.out.println("Average #Inserts/second in this checkpoint: "
							+ bulkUpdateResponse.getNumberOfDocumentsUpdated()
									/ (0.001 * bulkUpdateResponse.getTotalTimeTaken().toMillis()));
					System.out.println(
							"##########################################################################################");

					// Check the number of updated documents to ensure everything is successfully updated
					if (bulkUpdateResponse.getNumberOfDocumentsUpdated() != cfg.getNumberOfDocumentsForEachCheckpoint()) {
						System.err.println(
								"Some documents failed to get updated in this checkpoint.");
						
						System.out.println("Number of bulk update failures = " + bulkUpdateResponse.getFailedUpdates().size());
                        for (BulkUpdateFailure eachBulkUpdateFailure : bulkUpdateResponse.getFailedUpdates()) {
                            System.out.println(
                                "Number of failures corresponding to exception of type: " + 
                                eachBulkUpdateFailure.getBulkUpdateFailureException().getClass().getName() + 
                                " = " + 
                                eachBulkUpdateFailure.getFailedUpdateItems().size());
                        }
						break;
					}
				}

				// Print average statistics across checkpoints			
				System.out.println(
						"##########################################################################################");
				System.out.println(
						"Total update time in milli seconds measured by stopWatch: " + totalWatch.elapsed().toMillis());
				System.out.println("Total update time in milli seconds measured by api : " + totalTimeInMillis);
				System.out.println("Total Number of documents updated " + totalNumberOfDocumentsUpdated);
				System.out.println("Total request unit consumed: " + totalRequestCharge);
				System.out.println(
						"Average RUs/second:" + totalRequestCharge / (totalWatch.elapsed().toMillis() * 0.001));
				System.out.println("Average #Updates/second: "
						+ totalNumberOfDocumentsUpdated / (totalWatch.elapsed().toMillis() * 0.001));
			}
		}
	}
}
