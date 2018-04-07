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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.microsoft.azure.documentdb.ConnectionPolicy;
import com.microsoft.azure.documentdb.Database;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.documentdb.DocumentClientException;
import com.microsoft.azure.documentdb.DocumentCollection;
import com.microsoft.azure.documentdb.FeedResponse;
import com.microsoft.azure.documentdb.Offer;
import com.microsoft.azure.documentdb.PartitionKeyDefinition;
import com.microsoft.azure.documentdb.RequestOptions;
import com.microsoft.azure.documentdb.ResourceResponse;
import com.microsoft.azure.documentdb.RetryOptions;

public class Utilities {

	/*
	 * Create an empty collection at said throughput if not exists.
	 */
	public static DocumentCollection createEmptyCollectionIfNotExists(DocumentClient client, String databaseId, String collectionId,
			String partitionKeyDef, int collectionThroughput) throws DocumentClientException {

		String databaseLink = String.format("/dbs/%s", databaseId);
		String collectionLink = String.format("/dbs/%s/colls/%s", databaseId, collectionId);

		ResourceResponse<Database> databaseResponse = null;
		Database readDatabase = null;

		while (readDatabase == null) {
			try {
				databaseResponse = client.readDatabase(databaseLink, null);
				readDatabase = databaseResponse.getResource();

				System.out.println("Database already exists...");
			} catch (DocumentClientException dce) {
				if (dce.getStatusCode() == 404) {
					System.out.println("Attempting to create database since non-existent...");

					Database databaseDefinition = new Database();
					databaseDefinition.setId(databaseId);

					client.createDatabase(databaseDefinition, null);

					databaseResponse = client.readDatabase(databaseLink, null);
					readDatabase = databaseResponse.getResource();
				} else {
					throw dce;
				}
			}
		}

		ResourceResponse<DocumentCollection> collectionResponse = null;
		DocumentCollection readCollection = null;

		while (readCollection == null) {
			try {
				collectionResponse = client.readCollection(collectionLink, null);
				readCollection = collectionResponse.getResource();

				System.out.println("Collection already exists...");
			} catch (DocumentClientException dce) {
				if (dce.getStatusCode() == 404) {
					System.out.println("Attempting to create collection since non-existent...");

					DocumentCollection collectionDefinition = new DocumentCollection();
					collectionDefinition.setId(collectionId);

					PartitionKeyDefinition partitionKeyDefinition = new PartitionKeyDefinition();
					Collection<String> paths = new ArrayList<String>();
					paths.add(partitionKeyDef);
					partitionKeyDefinition.setPaths(paths);
					collectionDefinition.setPartitionKey(partitionKeyDefinition);

					RequestOptions options = new RequestOptions();
					options.setOfferThroughput(1000000);

					// create a collection
					client.createCollection(databaseLink, collectionDefinition, options);

					collectionResponse = client.readCollection(collectionLink, null);
					readCollection = collectionResponse.getResource();
				} else {
					throw dce;
				}
			}
		}

		// Find offer associated with this collection
		Iterator<Offer> it = client.queryOffers(
				String.format("SELECT * FROM r where r.offerResourceId = '%s'", readCollection.getResourceId()), null)
				.getQueryIterator();
		Offer offer = it.next();

		// Update the offer
		System.out.println("Attempting to modify collection throughput...");

		offer.getContent().put("offerThroughput", collectionThroughput);
		client.replaceOffer(offer);

		return readCollection;
	}

	/*
	 * Create a document client from specified configuration.
	 */
	public static DocumentClient documentClientFrom(CmdLineConfiguration cfg) throws DocumentClientException {

		ConnectionPolicy policy = new ConnectionPolicy();
		RetryOptions retryOptions = new RetryOptions();
		retryOptions.setMaxRetryAttemptsOnThrottledRequests(0);
		policy.setRetryOptions(retryOptions);
		policy.setConnectionMode(cfg.getConnectionMode());
		policy.setMaxPoolSize(cfg.getMaxConnectionPoolSize());

		return new DocumentClient(cfg.getServiceEndpoint(), cfg.getMasterKey(), policy, cfg.getConsistencyLevel());
	}

	/*
	 * Get the specified collection's allocated throughput.
	 */
	public static int getOfferThroughput(DocumentClient client, DocumentCollection collection) {
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