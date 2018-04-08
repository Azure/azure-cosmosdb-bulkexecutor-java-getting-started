<img src="https://raw.githubusercontent.com/dennyglee/azure-cosmosdb-spark/master/docs/images/azure-cosmos-db-icon.png" width="75">  &nbsp; Azure Cosmos DB BulkExecutor library for Java
==========================================

The Azure Cosmos DB BulkExecutor library for Java acts as an extension library to the [Cosmos DB Java SDK](https://docs.microsoft.com/en-us/azure/cosmos-db/sql-api-sdk-java) and provides developers out-of-the-box functionality to perform bulk operations in [Azure Cosmos DB](http://cosmosdb.com).

<details>
<summary><strong><em>Table of Contents</em></strong></summary>

* [Consuming the Microsoft Azure Cosmos DB BulkExecutor Java library](#maven)
* [DocumentBulkExecutor builder interface](#builder)
* [Bulk Import API](#bulk-import-api)
  * [Configurable parameters](#bulk-import-configurations)
  * [Bulk import response details](#bulk-import-response)
  * [Getting started with bulk import](#bulk-import-getting-started)
  * [Performance of bulk import sample](bulk-import-performance)
  * [API implementation details](bulk-import-client-side)
* [Performance tips](#additional-pointers)
* [Contributing & Feedback](#contributing--feedback)
* [Legal Notices](#legal-notice)
* [Other relevant projects](#relevant-projects)

</details>

## Consuming the Microsoft Azure Cosmos DB BulkExecutor Java library

This project includes samples, documentation and performance tips for consuming the BulkExecutor library. You can download the official public maven package from [here](https://search.maven.org/#search%7Cga%7C1%7Cdocumentdb-bulkexecutor).

For example, using maven, you can add the following dependency to your maven pom file:
```xml
<dependency>
  <groupId>com.microsoft.azure</groupId>
  <artifactId>documentdb-bulkexecutor</artifactId>
  <version>1.0.6</version>
</dependency>
```
------------------------------------------

## DocumentBulkExecutor builder interface

```java
/**
* Use the instance of {@link DocumentClient} to perform bulk operations in target {@link DocumentCollection} instance at specified allocated throughput.
* @param client an instance of {@link DocumentClient}
* @param partitionKeyDef specifies the {@link PartitionKeyDefinition} of the collection
* @param databaseName name of the database
* @param collectionName name of the collection
* @param offerThroughput specifies the throughput you want to allocate for bulk operations out of the collection's total throughput
* @return an instance of {@link Builder}
*/
public Builder from(DocumentClient client,
        String databaseName, 
        String collectionName,
        PartitionKeyDefinition partitionKeyDef,
        int offerThroughput)

/**
* Use the given size to configure max mini-batch size (specific to bulk import API).
* If not specified will use the default value of 200 KB.
* @param size specifies the maximum size of a mini-batch used in bulk import API.
* @return {@link Builder}
*/
public Builder withMaxMiniBatchSize(int size)

/**
* Use the given count to configure max update mini-batch count (specific to bulk update API).
* If not specified will use the default value of 500.
* @param count specifies the maximum count of update items in a mini-batch used in bulk update API.
* @return {@link Builder}
*/
public Builder withMaxUpdateMiniBatchCount(int count)

/**
* Use the given retry options to apply to {@link DocumentClient} used in initialization of {@link DocumentBulkExecutor}.
* @param options an instance of {@link RetryOptions}
* @return {@link Builder}
*/
public Builder withInitializationRetryOptions(RetryOptions options)

/**
* Instantiates {@link DocumentBulkExecutor} given the configured {@link Builder}.
* @return the newly instantiated instance of {@link DocumentBulkExecutor}
* @throws Exception if there is any failure
*/
public DocumentBulkExecutor build() throws Exception
```

------------------------------------------
## Bulk Import API

We provide two overloads of the bulk import API which accept a list of JSON-serialized documents:

```java
public BulkImportResponse importAll(
        Collection<String> documents,
        boolean isUpsert) throws DocumentClientException;

public BulkImportResponse importAll(
        Collection<String> documents,
        boolean isUpsert,
        boolean disableAutomaticIdGeneration,
        Integer maxConcurrencyPerPartitionRange) throws DocumentClientException;   
```

### Configurable parameters

* *isUpsert* : A flag to enable upsert of the documents if document with given id already exists.
* *disableAutomaticIdGeneration* : A flag to disable automatic generation of id if absent in the document - default value is true.
* *maxConcurrencyPerPartitionRange* : The maximum degree of concurrency per partition key range, default value is 20.

### Bulk import response details

The result of the bulk import API call contains the getter functions:
* Gets the total number of documents which were successfully imported out of the documents supplied to the bulk import API call.
```java
public int getNumberOfDocumentsImported();
```
* Gets the total request units (RU) consumed by the bulk import API call.
```java
public double getTotalRequestUnitsConsumed();
```
* Gets total time taken by the bulk import API call to complete execution.
```java
public Duration getTotalTimeTaken();
```
* Gets the list of errors if some documents out of the batch supplied to the bulk import API call failed to get inserted.
```java
public List<Exception> getErrors();
```
* Gets the list of bad-format documents which were not successfully imported in the bulk import API call. User needs to fix the documents returned and retry import. Bad-format documents include documents whose *id* value is not a string (null or any other datatype is considered invalid).
```java
public List<Object> getBadInputDocuments();
```

### Getting started with bulk import

* Initialize DocumentClient
```java
ConnectionPolicy connectionPolicy = new ConnectionPolicy();
connectionPolicy.setMaxPoolSize(1000);
DocumentClient client = new DocumentClient(
    HOST,
    MASTER_KEY, 
    connectionPolicy,
    ConsistencyLevel.Session)
```

* Initialize DocumentBulkExecutor with high retry option values for the client SDK and then set to 0 to pass congestion control to DocumentBulkExecutor for its lifetime
```java
// Set client's retry options high for initialization
client.getConnectionPolicy().getRetryOptions().setMaxRetryWaitTimeInSeconds(30);
client.getConnectionPolicy().getRetryOptions().setMaxRetryAttemptsOnThrottledRequests(9);

// Builder pattern
Builder bulkExecutorBuilder = DocumentBulkExecutor.builder().from(
    client,
    DATABASE_NAME,
    COLLECTION_NAME,
    collection.getPartitionKey(),
    offerThroughput) // throughput you want to allocate for bulk import out of the collection's total throughput

// Instantiate DocumentBulkExecutor
DocumentBulkExecutor bulkExecutor = bulkExecutorBuilder.build()

// Set retries to 0 to pass complete control to bulk executor
client.getConnectionPolicy().getRetryOptions().setMaxRetryWaitTimeInSeconds(0);
client.getConnectionPolicy().getRetryOptions().setMaxRetryAttemptsOnThrottledRequests(0);
```

* Call importAll API
```java
BulkImportResponse bulkImportResponse = bulkExecutor.importAll(documents, false);
```

You can find the complete sample command line tool consuming the bulk import API [here](https://github.com/Azure/azure-cosmosdb-bulkexecutor-java-getting-started/blob/master/samples/bulkexecutor-sample/src/main/java/com/microsoft/azure/cosmosdb/bulkexecutor/App.java)
 - which generates random documents to be then bulk imported into an Azure Cosmos DB collection. You can configure the command line configurations to be passed in *CmdLineConfiguration* [here](https://github.com/Azure/azure-cosmosdb-bulkexecutor-java-getting-started/blob/master/samples/bulkexecutor-sample/src/main/java/com/microsoft/azure/cosmosdb/bulkexecutor/CmdLineConfiguration.java).

To build the command line tool from source (jar can be found in *target* folder):
```console
mvn clean package
```

Here is a sample command line invocation for bulk import:
```console
java -Xmx12G -jar bulkexecutor-sample-1.0-SNAPSHOT-jar-with-dependencies.jar -serviceEndpoint *** -masterKey *** -databaseId bulkImportDb -collectionId bulkImportColl -operation import -shouldCreateCollection -collectionThroughput 1000000 -partitionKey /profileid -maxConnectionPoolSize 6000 -numberOfDocumentsForEachCheckpoint 1000000 -numberOfCheckpoints 10
```

### Performance of bulk import sample

When the given sample command line tool is run (to bulk import **10 million** documents of ~1KB) on a standard D16s v3 Azure Ubuntu VM in East US against a Cosmos DB collection in East US with **1 million RU/s** allocated throughput - with command line configs *numberOfDocumentsForEachCheckpoint* set to 1000000 and *numberOfCheckpoints* set to 10, we observe the following performance for bulk import:

```java
Total Number of documents inserted : 10000000
Average RUs/second : 628386
Average #Inserts/second : 108340
```

As seen, we observe **>10x** improvement in the write throughput using the bulk import API while providing out-of-the-box efficient handling of throttling, timeouts and transient exceptions - allowing easier scale-out by adding additional *DocumentBulkExecutor* client instances on individual VMs to achieve even greater write throughputs.

### API implementation details

When a bulk import API is triggered with a batch of documents, on the client-side, they are first shuffled into buckets corresponding to their target Cosmos DB partition key range. Within each partiton key range bucket, they are broken down into mini-batches and each mini-batch of documents acts as a payload that is committed transactionally.

We have built in optimizations for the concurrent execution of these mini-batches both within and across partition key ranges to maximally utilize the allocated collection throughput. We have designed an [AIMD-style congestion control](https://academic.microsoft.com/#/detail/2158700277?FORM=DACADP) mechanism for each Cosmos DB partition key range **to efficiently handle throttling and timeouts**.

These client-side optimizations augment server-side features specific to the DocumentBulkExecutor library which together make maximal consumption of available throughput possible.

------------------------------------------
## Performance tips

* For best performance, run your application **from an Azure VM in the same region as your Cosmos DB account write region**.
* For achieving higher throughput:
1) Set JVM heap size to a large enough number to avoid any memory issue in handling large number of documents. 
   Suggested heap size: max(3GB, 3 * sizeof(all documents passed to bulk import API in one batch)) 
2) There is a preprocessing and warm up time; due that you will get higher throughput for bulks with larger number of documents. So, if you want to import 10,000,000 documents, running bulk import 10 times on 10 bulk of documents each of size 1,000,000 is more preferable than running bulk import 100 times on 100 bulk of documents each of size 100,000 documents. 
* It is advised to instantiate a single *DocumentBulkExecutor* object for the entirety of the application within a single VM corresponding to a specific Cosmos DB collection.
* Since a single bulk operation API execution consumes a large chunk of the client machine's CPU and network IO by spawning multiple tasks internally, avoid spawning multiple concurrent tasks within your application process each executing bulk operation API calls. If a single bulk operation API call running on a single VM is unable to consume your entire collection's throughput (if your collection's throughput > 1 million RU/s), preferably spin up separate VMs to concurrently execute bulk operation API calls.

------------------------------------------
## Contributing & Feedback

This project welcomes contributions and suggestions.  Most contributions require you to agree to a
Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us
the rights to use your contribution. For details, visit https://cla.microsoft.com.

When you submit a pull request, a CLA-bot will automatically determine whether you need to provide
a CLA and decorate the PR appropriately (e.g., label, comment). Simply follow the instructions
provided by the bot. You will only need to do this once across all repos using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/).
For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or
contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

To give feedback and/or report an issue, open a [GitHub
Issue](https://help.github.com/articles/creating-an-issue/).

------------------------------------------
## Legal Notices

Microsoft and any contributors grant you a license to the Microsoft documentation and other content
in this repository under the [Creative Commons Attribution 4.0 International Public License](https://creativecommons.org/licenses/by/4.0/legalcode),
see the [LICENSE](LICENSE) file, and grant you a license to any code in the repository under the [MIT License](https://opensource.org/licenses/MIT), see the
[LICENSE-CODE](LICENSE-CODE) file.

Microsoft, Windows, Microsoft Azure and/or other Microsoft products and services referenced in the documentation
may be either trademarks or registered trademarks of Microsoft in the United States and/or other countries.
The licenses for this project do not grant you rights to use any Microsoft names, logos, or trademarks.
Microsoft's general trademark guidelines can be found at http://go.microsoft.com/fwlink/?LinkID=254653.

Privacy information can be found at https://privacy.microsoft.com/en-us/

Microsoft and any contributors reserve all others rights, whether under their respective copyrights, patents,
or trademarks, whether by implication, estoppel or otherwise.

------------------------------------------
## Other relevant projects

* [Cosmos DB BulkExecutor library for .NET](https://github.com/Azure/azure-cosmosdb-bulkexecutor-dotnet-getting-started)