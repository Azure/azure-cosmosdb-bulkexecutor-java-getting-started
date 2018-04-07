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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.beust.jcommander.Parameter;
import com.microsoft.azure.documentdb.ConnectionMode;
import com.microsoft.azure.documentdb.ConsistencyLevel;

public class CmdLineConfiguration {

    @Parameter(names = "-serviceEndpoint", description = "Service Endpoint", required = true)
    private String serviceEndpoint;

    @Parameter(names = "-masterKey", description = "Master Key", required = true)
    private String masterKey;

    @Parameter(names = "-databaseId", description = "Database ID", required = true)
    private String databaseId;

    @Parameter(names = "-collectionId", description = "Collection ID", required = true)
    private String collectionId;

    @Parameter(names = "-operation", description = "Operation to perform", required = true)
    private String operation;
    
    @Parameter(names = "-shouldCreateCollection", description = "Flag to indicate if empty collection needs to be created on start if not exists. "
    		+ "If false, assumes collection already exists.")
    private boolean shouldCreateCollection = false;
    
    @Parameter(names = "-collectionThroughput", description = "The throughput at which the collection needs to be created if"
    		+ " @shouldCreateCollection is set to true.")
    private int collectionThroughput = 1000000;    
    
    @Parameter(names = "-partitionKey", description = "The partition key with which the collection needs to be created if "
    		+ "shouldCreateCollection is set to true.")
    private String partitionKey = "/partitionKey";
    
    @Parameter(names = "-maxConnectionPoolSize", description = "Max Connection Pool Size")
    private int maxConnectionPoolSize = 1000;

    @Parameter(names = "-consistencyLevel", description = "Consistency Level")
    private ConsistencyLevel consistencyLevel = ConsistencyLevel.Session;

    @Parameter(names = "-connectionMode", description = "Connection Mode")
    private ConnectionMode connectionMode = ConnectionMode.Gateway;

    @Parameter(names = "-numberOfDocumentsForEachCheckpoint", description = "Number of documents in each checkpoint.")
    private int numberOfDocumentsForEachCheckpoint = 500000;

    @Parameter(names = "-numberOfCheckpoints", description = "Number of checkpoints.")
    private int numberOfCheckpoints = 10;
    
    @Parameter(names = {"-h", "-help", "--help"}, description = "Help", help = true)
    private boolean help = false;

    public String getOperation() {
    	return operation;
    }
    
    public boolean getShouldCreateCollection() {
    	return shouldCreateCollection;
    }
    
    public int getCollectionThroughput() {
    	return collectionThroughput;
    }
    
    public String getPartitionKey() {
    	return partitionKey;
    } 
    
    public int getNumberOfCheckpoints() {
        return numberOfCheckpoints;
    }

    public int getNumberOfDocumentsForEachCheckpoint() {
        return numberOfDocumentsForEachCheckpoint;
    }

    public String getServiceEndpoint() {
        return serviceEndpoint;
    }

    public String getMasterKey() {
        return masterKey;
    }

    public boolean isHelp() {
        return help;
    }

    public Integer getMaxConnectionPoolSize() {
        return maxConnectionPoolSize;
    }

    public ConnectionMode getConnectionMode() {
        return connectionMode;
    }

    public ConsistencyLevel getConsistencyLevel() {
        return consistencyLevel;
    }

    public String getDatabaseId() {
        return databaseId;
    }

    public String getCollectionId() {
        return collectionId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}