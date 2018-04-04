package com.microsoft.azure.documentdb.bulkexecutor.bulkimport;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.base.Preconditions;
import com.microsoft.azure.documentdb.PartitionKeyDefinition;

public class DataMigrationDocumentSource {

    /**
     * Creates a collection of documents.
     *
     * @param numberOfDocuments
     * @param partitionKeyDefinition
     * @return collection of documents.
     */
    public static Collection<String> loadDocuments(int numberOfDocuments, PartitionKeyDefinition partitionKeyDefinition) {

        Preconditions.checkArgument(partitionKeyDefinition != null &&
                partitionKeyDefinition.getPaths().size() > 0, "there is no partition key definition");

        Collection<String> partitionKeyPath = partitionKeyDefinition.getPaths();
        Preconditions.checkArgument(partitionKeyPath.size() == 1,
                "the command line benchmark tool only support simple partition key path");

        String partitionKeyName = partitionKeyPath.iterator().next().replaceFirst("^/", "");

        // the size of each document is approximately 1KB

        ArrayList<String> allDocs = new ArrayList<>(numberOfDocuments);

        // return documents to be bulk imported
        // if you are reading documents from disk you can change this to read documents from disk
        return IntStream.range(0, numberOfDocuments).mapToObj(i ->
        {
            String partitionKeyValue = UUID.randomUUID().toString();
            return generateDocument(partitionKeyName, partitionKeyValue);
        }).collect(Collectors.toCollection(() -> allDocs));
    }

    private static String generateDocument(String partitionKeyName, String partitionKeyValue) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(UUID.randomUUID().toString()).append("abc\"");

        String data = UUID.randomUUID().toString();
        data = data + data + "0123456789012";

        for(int j = 0; j < 10;j++) {
            sb.append(",").append("\"f").append(j).append("\":\"").append(data).append("\"");
        }

        // partition key
        sb.append(",\"").append(partitionKeyName).append("\":\"").append(partitionKeyValue).append("\"");

        sb.append("}");

        return sb.toString();
    }
}
