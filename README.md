### Optimizing Data Repositories Usage in Java Multithreaded Applications

Data repositories are often the bottleneck of highly demanding system when the number of queries being executed is large. 
DelayedTask is a component to decrease the number of required queries by batching them in Java Multithreaded Applications. 


#### n queries of 1 parameter vs 1 query of n parameters

Let's assume a Java application that executes a query to a relational database to retrieve a Product entity (row) given its unique identifier (id).

The query would look like something like this:

```sql
SELECT * FROM PRODUCT WHERE ID  = <productId>
```
Now, to retrieve n Products there are two ways to proceed:

* Execute n independent queries of one parameter:
```sql
SELECT * FROM PRODUCT WHERE ID   = <productId1>
SELECT * FROM PRODUCT WHERE ID   = <productId2>
...
SELECT * FROM PRODUCT WHERE ID   = <productIdn>
```
* Execute one query of n parameters to retrieve n Products at the same time using the IN operator or a concatenation of ORs

```sql
--  Example using IN OPERATOR
SELECT * FROM PRODUCT WHERE ID IN (<productId1>, <productId2>, ..., <productIdn>)
```
The latter is more efficient in terms of network traffic and database server resources (CPU and Disk) because:
* The number of round-trips to the database is 1 instead of n.
* The database engine optimizes its data traversal process for n parameters, i.e, it may require only one scan per table instead of n.

That is not only true for SELECT operations but also for other operations such as INSERTs, UPDATEs and DELETEs as well, in fact, JDBC API includes batch operations for these operations.

The same applies to NoSQL repositories, most of them provide BULK operations explicitly.

#### DelayedTask 

Java applications like REST Microservices or Asynchronous Message Processors that require retrieving data from the database are usually implemented as Multithreaded applications (\*1) where:
 
 - Each thread executes the same query at some point of its execution (each query with a different parameter).
 - The number of concurrent threads is high (tens or hundreds per second).
 
In this scenario, it is highly likely that the database executes the same query many times within a short interval of time.

If these n queries of 1 parameter were replaced with a single equivalent one with n parameters, as pointed out before, the application would use less database server and network resources. 

The good news is that it can be achieved by a mechanism involving *time windows* as follows:

A *time window* is opened by the first thread that tries to execute the query, so its parameter is stored in a list and that thread is paused. The rest of the threads that execute the same query within the *time window* get their parameters added to the list and they are paused as well. At this point no query has been executed on the database.

As soon as the time window is finished, or the list is full (a *maximum capacity* limit is previously defined), then a single query is executed with all the parameters stored in the list. Finally, once the result of this query is provided by the database, each thread receives its corresponding result and all the threads are resumed automatically.

I've built a simple and light implementation of this mechanism for myself (DelayedTask), which is easy to use in new or existing applications. It is based on the Reactive library *Reactor* and it uses a *Flux* buffered publisher with timeout for the list of parameters .

#### Throughput and Latency analysis using DelayedTask

Let's assume a Rest Microservice for Products that exposes an endpoint for retrieving a Product given its productId.
Without DelayedTask, if there are say 200 hits per second to the endpoint, the database executes 200 queries per second.
If the endpoint were using a DelayedTask configured with a *time window* of 50 ms and *maximum capacity*=10 parameters, the database would execute ONLY 20 queries of 10 parameters each per second at the cost of increasing the latency at most in 50 ms (\*2) for each thread execution.

In other words, for the price of increasing the latency by 50 ms(\*2), the database receives 10 times less queries per second while keeping the overall throughput of the system... Not bad!!.
Other interesting configurations:

* *window time*=100 ms, *max capacity*=20 parameters &rarr; 10 queries of 20 parameters (20 times less queries)
* *window time*=500 ms, *max capacity*=100 parameters &rarr; 2 queries of 100 parameters (100 times less queries)


See full example in `com.vp.delayedTask.ProductDAO` and relative test class

#### Beyond data repositories
Although this article deals with data repositories, DelayedTask could be used as well in other contexts, for instance in requests to REST microservices. Again, it is much more *expensive* to launch n GET requests with one parameter that 1 GET with n parameters.

#### DelayedTask improvements

I created DelayedTask and used for a while as a way of efficiently handling the execution of multiple queries launched by concurrent threads in personal projects. I believe it could be useful for someone else, so I've decided to make it public.

Said that, there is a lot of room to improve and extend the functionality offered by DelayedTask. The most interesting would be to be able to dinamically change the parameters of the DelayedTask (*window time* and *max capacity*) according to the particular conditions of the moment of the execution in order to minimize the latency while taking advantage of having queries with n parameters. Feel free if you feel like to contribute.
___

(\*1) Regardless of whether the application is deployed in a cluster of nodes or not, each (JVM) node runs the application as a Java Multithreaded Application. For REST Microservices, the threads are usually managed by the underlying Servlet engine, and for Asyncronous Messaging Processors the threads are managed by  their Messaging Protocol implementation.

(\*2) Strictly speaking, the  latency would be increased by 50 ms of the window time  plus the extra time required by the database to execute the n parameters query in comparison with when using just one parameter. However, this extra time is very small in most cases (a few milliseconds) where n is not extremely large. You can easily check it in your database. 
