# TelemetryViewer

## What is this project about? ##

This project provides the ability to view data from telemetry devices, which measures parameters of the corrosion protection. Devices transmits these parameters to the data collection server, and then this mobile app reads data from the MS SQL database that the server is working with.

## Project features ##

Unlike the most common scheme, when the client works with the server through the HTTP protocol, in this project a different scheme is implemented according to the requirements of the customer. 
This project uses a direct connection to the MS SQL database via Java JTDS library.
This library provides access to the MS SQL database (and to others too) through standard SQL language constructs, as well as the ability to execute stored procedures.
The functionality for this solution is located in the `network` package.  
Loading data is performed by the `serverRequest()` function:  
```
/**
 * This function downloads data from the server
 * and automatically converts it to type T using a lambda expression.
 * 
 * @param T the type of the final objects, must be a data class
 * @param serverRequestInfo contains a string with an SQL query or parameters of a stored procedure
 * @param channel is a Kotlin channel for asynchronously loading chunks of data
 * @return fromJson is a lambda expression that converts JSON objects to type T
 */
suspend fun <T: Any?> serverRequest(serverRequestInfo: ServerRequestInfo, channel: Channel<MutableList<T>>, fromJson: (JSONObject) -> T)
```  
Other libraries and technologies used in the project:  
- Kotlin language with Coroutines
- The Navigation library
- The Paging2 library for viewpaging
- The Room library for storing downloaded data in the cache
- The SQLite library for the ability to create tables which structure
not known at compile time  

## Why is this project on Github? ##

