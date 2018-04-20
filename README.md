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
* [Bulk Update API](#bulk-import-api)
  * [List of supported field update operations](#bulk-update-list-update-operations)
  * [Configurable parameters](#bulk-update-configurations)
  * [Bulk update response details](#bulk-updatjavae-response)
  * [Getting started with bulk update](#bulk-update-getting-started)
  * [Performance of bulk update sample](bulk-update-performance)
  * [API implementation details](bulk-update-client-side)  
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
  <version>2.0.0</version>
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

The bulk import API accepts a collection of JSON-serialized documents:

```java
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
BulkImportResponse bulkImportResponse = bulkExecutor.importAll(documents, false, true, null);
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

------------------------------------------
## Bulk Update API

The bulk update (a.k.a patch) API accepts a collection of update items - each update item specifies the list of field update operations to be performed on a document identified by an id and parititon key value.

```java
public BulkUpdateResponse updateAll(
        Collection<UpdateItem> updateItems,
        Integer maxConcurrencyPerPartitionRange) throws DocumentClientException;
```

* Definition of UpdateItem
```java
public class UpdateItem
{
    private String id;

    private Object partitionKeyValue;

    private List<UpdateOperationBase> updateOperations;

    public UpdateItem(String id, Object partitionKeyValue, List<UpdateOperationBase> list)
    {
        this.id = id;
        this.partitionKeyValue = partitionKeyValue;
        this.updateOperations = list;
    }

    public String getId()
    {
        return this.id;
    }
        
    public Object getPartitionKeyValue()
    {
        return this.partitionKeyValue;
    }
        
    public List<UpdateOperationBase> getUpdateOperations()
    {
        return this.updateOperations;
    }
}
```

### List of supported field update operations

* Increment

Supports incrementing any numeric document field by a specific value
```java
public class IncUpdateOperation
{
    public IncUpdateOperation(String field, Double value)
}
```

* Set

Supports setting any document field to a specific value
```java
public class SetUpdateOperation<TValue>
{
    public SetUpdateOperation(String field, TValue value)
}
```

* Unset

Supports removing a specific document field along with all children fields
```java
public class UnsetUpdateOperation
{
    public UnsetUpdateOperation(String field)
}
```

* Array push

Supports appending an array of values to a document field which contains an array
```java
public class PushUpdateOperation
{
    public PushUpdateOperation(String field, Object[] value)
}
```

* Array remove

Supports removing a specific value (if present) from a document field which contains an array
```java
public class RemoveUpdateOperation<TValue>
{
    public RemoveUpdateOperation(String field, TValue value)
}
```

**Note**: For nested fields, use '.' as the nesting separtor. For example, if you wish to set the '/address/city' field to 'Seattle', express as shown:
```java
SetUpdateOperation<String> nestedPropertySetUpdate = new SetUpdateOperation<String>("address.city", "Seattle");
```

### Configurable parameters

* *maxConcurrencyPerPartitionRange* : The maximum degree of concurrency per partition key range, default value is 20.

### Bulk update response details

The result of the bulk update API call contains the getter functions:
* Gets the total number of documents which were successfully updated.
```java
public int getNumberOfDocumentsUpdated();
```
* Gets the total request units (RU) consumed by the bulk update API call.
```java
public double getTotalRequestUnitsConsumed();
```
* Gets total time taken by the bulk update API call to complete execution.
```java
public Duration getTotalTimeTaken();
```
* Gets the list of errors if some documents out of the batch supplied to the bulk import API call failed to get inserted.
```java
public List<Exception> getErrors();
```

### Getting started with bulk update

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

* Define the update items along with corresponding field update operations
```java
SetUpdateOperation<String> nameUpdate = new SetUpdateOperation<>("Name","UpdatedDocValue");
UnsetUpdateOperation descriptionUpdate = new UnsetUpdateOperation("description");

ArrayList<UpdateOperationBase> updateOperations = new ArrayList<>();
updateOperations.add(nameUpdate);
updateOperations.add(descriptionUpdate);

List<UpdateItem> updateItems = new ArrayList<>(cfg.getNumberOfDocumentsForEachCheckpoint());
IntStream.range(0, cfg.getNumberOfDocumentsForEachCheckpoint()).mapToObj(j -> {						
    return new UpdateItem(Long.toString(prefix + j), Long.toString(prefix + j), updateOperations);
}).collect(Collectors.toCollection(() -> updateItems));
```

* Call updateAll API
```java
BulkUpdateResponse bulkUpdateResponse = bulkExecutor.updateAll(updateItems, null)
```

You can find the complete sample command line tool consuming the bulk update API [here](https://github.com/Azure/azure-cosmosdb-bulkexecutor-java-getting-started/blob/master/samples/bulkexecutor-sample/src/main/java/com/microsoft/azure/cosmosdb/bulkexecutor/App.java)
 - which generates random documents to be then bulk imported into an Azure Cosmos DB collection. You can configure the command line configurations to be passed in *CmdLineConfiguration* [here](https://github.com/Azure/azure-cosmosdb-bulkexecutor-java-getting-started/blob/master/samples/bulkexecutor-sample/src/main/java/com/microsoft/azure/cosmosdb/bulkexecutor/CmdLineConfiguration.java).

To build the command line tool from source (jar can be found in *target* folder):
```console
mvn clean package
```

Here is a sample command line invocation for bulk update:
```console
java -Xmx12G -jar bulkexecutor-sample-1.0-SNAPSHOT-jar-with-dependencies.jar -serviceEndpoint *** -masterKey *** -databaseId bulkUpdateDb -collectionId bulkUpdateColl -operation update -collectionThroughput 1000000 -partitionKey /profileid -maxConnectionPoolSize 6000 -numberOfDocumentsForEachCheckpoint 1000000 -numberOfCheckpoints 10
```

Prior to running the above bulk update, ensure sample documents have been imported using:
```console
java -Xmx12G -jar bulkexecutor-sample-1.0-SNAPSHOT-jar-with-dependencies.jar -serviceEndpoint *** -masterKey *** -databaseId bulkUpdateDb -collectionId bulkUpdateColl -operation import -shouldCreateCollection -collectionThroughput 1000000 -partitionKey /profileid -maxConnectionPoolSize 6000 -numberOfDocumentsForEachCheckpoint 1000000 -numberOfCheckpoints 10
```

### Performance of bulk update sample

When the given sample command line tool is run (to bulk update **10 million** documents) on a standard D16s v3 Azure Ubuntu VM in East US against a Cosmos DB collection in East US with **1 million RU/s** allocated throughput - with command line configs *numberOfDocumentsForEachCheckpoint* set to 1000000 and *numberOfCheckpoints* set to 10, we observe the following performance for bulk update:

```java
Total Number of documents updated : 10000000
Average RUs/second : 564108
Average #Updates/second : 61244
```

### API implementation details

The bulk update API is designed similar to bulk import - look at the implementation details of bulk import API for more details.

------------------------------------------
## Performance tips

* For best performance, run your application **from an Azure VM in the same region as your Cosmos DB account write region**.
* For achieving higher throughput:
    - Set JVM heap size to a large enough number to avoid any memory issue in handling large number of documents. Suggested heap size: max(3GB, 3 * sizeof(all documents passed to bulk import API in one batch)) 
    - There is a preprocessing and warm up time; due that you will get higher throughput for bulks with larger number of documents. So, if you want to import 10,000,000 documents, running bulk import 10 times on 10 bulk of documents each of size 1,000,000 is more preferable than running bulk import 100 times on 100 bulk of documents each of size 100,000 documents. 
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