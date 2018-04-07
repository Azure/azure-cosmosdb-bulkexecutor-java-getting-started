package com.microsoft.azure.documentdb.bulkexecutor.bulkimport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.base.Preconditions;
import com.microsoft.azure.documentdb.PartitionKeyDefinition;

public class DataMigrationDocumentSource {

	/**
	 * Creates a collection of documents.
	 * 
	 * @param numberOfDocuments The number of documents to load
	 * @param partitionKeyDefinition The partition key definition
	 * @param prefix The prefix to start with for partition key and id values
	 * @return The collection of documents to bulk import
	 */
	public static Collection<String> loadDocuments(int numberOfDocuments,
			PartitionKeyDefinition partitionKeyDefinition, long prefix) {

		Preconditions.checkArgument(partitionKeyDefinition != null && partitionKeyDefinition.getPaths().size() > 0,
				"there is no partition key definition");

		Collection<String> partitionKeyPath = partitionKeyDefinition.getPaths();
		Preconditions.checkArgument(partitionKeyPath.size() == 1,
				"the command line benchmark tool only support simple partition key path");

		// Note: This sample assumes a simple (non-nested) partition key. Nested partition keys work with bulk import API too.
		String partitionKeyName = partitionKeyPath.iterator().next().replaceFirst("^/", "");

		ArrayList<String> allDocs = new ArrayList<>(numberOfDocuments);

		// Return documents to be bulk imported
		// If you are reading documents from disk you can change this to read documents from disk
		return IntStream.range(0, numberOfDocuments).mapToObj(i -> {
			
			String partitionKeyValue = Long.toString(prefix + i);
			return generateDocument(partitionKeyName, partitionKeyValue);
		}).collect(Collectors.toCollection(() -> allDocs));
	}

	// The size of each document is approximately 1KB.
	private static String generateDocument(String partitionKeyName, String partitionKeyValue) {
		
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"id\":\"").append(partitionKeyValue).append(UUID.randomUUID().toString()).append("\"");

		String data = UUID.randomUUID().toString();
		data = data + data + "0123456789012";

		for (int j = 0; j < 10; j++) {
			sb.append(",").append("\"f").append(j).append("\":\"").append(data).append("\"");
		}

		// partition key
		sb.append(",\"").append(partitionKeyName).append("\":\"").append(partitionKeyValue).append("\"");

		sb.append("}");

		return sb.toString();
	}
}
