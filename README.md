<img src="https://raw.githubusercontent.com/dennyglee/azure-cosmosdb-spark/master/docs/images/azure-cosmos-db-icon.png" width="75">  &nbsp; Azure Cosmos DB BulkExecutor library for Java
==========================================

The Azure Cosmos DB BulkExecutor library for Java acts as an extension library to the [Cosmos DB Java SDK](https://docs.microsoft.com/en-us/azure/cosmos-db/sql-api-sdk-java) and provides developers out-of-the-box functionality to perform bulk operations in [Azure Cosmos DB](http://cosmosdb.com).

<details>
<summary><strong><em>Table of Contents</em></strong></summary>

* [Consuming the Microsoft Azure Cosmos DB BulkExecutor Java library](#maven)
* [Bulk Import API](#bulk-import-api)
  * [Configurable parameters](#bulk-import-configurations)
  * [Bulk import response object definition](#bulk-import-response)
  * [Getting started with bulk import](#bulk-import-getting-started)
  * [Performance of bulk import sample](bulk-import-performance)
  * [API implementation details](bulk-import-client-side)
* [Bulk Update API](#bulk-update-api)
  * [List of supported field update operations](#field-update-operations)
  * [Configurable parameters](#bulk-update-configurations)
  * [Bulk update response object definition](#bulk-update-response)
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
  <version>1.0.6</version>
</dependency>
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