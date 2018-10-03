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
package com.microsoft.azure.cosmosdb.bulkdelete;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.microsoft.azure.cosmosdb.bulkexecutor.CmdLineConfiguration;
import com.microsoft.azure.cosmosdb.bulkexecutor.Utilities;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.documentdb.DocumentCollection;
import com.microsoft.azure.documentdb.PartitionKey;
import com.microsoft.azure.documentdb.RequestOptions;
import com.microsoft.azure.documentdb.bulkexecutor.BulkDeleteResponse;
import com.microsoft.azure.documentdb.bulkexecutor.DocumentBulkExecutor;
import com.microsoft.azure.documentdb.bulkexecutor.DocumentBulkExecutor.Builder;

public class BulkDeleter {

    public static final Logger LOGGER = LoggerFactory.getLogger(BulkDeleter.class);

    /**
     * In this sample, we assume documents have been loaded into the collection using BulkImporter with the schema mentioned 
     * in DataMigrationDocumentSource
     * 
     * @param cfg Command line configuration settings passed
     * @throws Exception
     */
    public void executeBulkDelete(CmdLineConfiguration cfg) throws Exception {
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
                
                Stopwatch totalWatch = Stopwatch.createUnstarted();

                // Execute bulk delete API  
                String query = "select * from c where c." + cfg.getPartitionKey().replaceFirst("^/", "") + " = \"2\""; 
                System.out.println(query);
                
                // If a partition key is present in the query above, adding RequestOptions also containing the value of the partition key
                // for which the query is being issued to delete documents, will optimize performance.
                // If a partition key filter is not present in the query, only including the query in the 'deleteAll' call below, will suffice.
                RequestOptions requestOptions = new RequestOptions();
                requestOptions.setPartitionKey(new PartitionKey("2"));
                
                totalWatch.start();
                BulkDeleteResponse bulkDeleteResponse = bulkExecutor.deleteAll(query, requestOptions);
                totalWatch.stop();
                
                // Print statistics for bulk delete operation             
                System.out.println(
                        "##########################################################################################");
                System.out.println("Number of documents deleted: "
                                 + bulkDeleteResponse.getNumberOfDocumentsDeleted());
                System.out.println("Time taken to delete documents specified by the query: "
                        + bulkDeleteResponse.getTotalTimeTaken().toMillis());
                System.out.println("Time request units consumed by the bulk delete operation: "
                        + bulkDeleteResponse.getTotalRequestUnitsConsumed());
                
                System.out.println("Average RUs/second consumed by bulk delete batch: "
                        + bulkDeleteResponse.getTotalRequestUnitsConsumed()
                                / (0.001 * bulkDeleteResponse.getTotalTimeTaken().toMillis()));
                System.out.println("Average #Deletes/second in this checkpoint: "
                        + bulkDeleteResponse.getNumberOfDocumentsDeleted()
                                / (0.001 * bulkDeleteResponse.getTotalTimeTaken().toMillis()));
                System.out.println(
                        "##########################################################################################");
                
            } 
        }
    }
}
