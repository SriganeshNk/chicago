Getting started with Chicago
============================


## Starting Chicago Servers ##

To build and run Chicago, you need :
 1. Aapache maven 3.0 or above
 2. Java 8 or above
 3. Xio 12 or above ( https://github.com/xjdr/xio )
 4. Guava 19


### Download and build chicago ###

Clone the repo and build it with maven:
     
    mvn install
  

  
### Running Chicago cluster ###

Before starting the cluster, you need to have a Zookeeper instance.
Please refer to https://zookeeper.apache.org/doc/trunk/zookeeperStarted.html

Once you have a zookeeper running, modify the application.conf in config/ folder.
Fill in the following entries :
  1. `zookeeperCluster`
  2. `bindHost` with IP address of the machine for  admin, stats and db
   
Sample application.conf :

     chicago {
       application = ${chicago.applicationTemplate} {
         settings {
           zookeeperCluster = "10.24.25.188:2181,10.24.25.189:2181,10.25.145.56:2181,10.24.33.123:2181"
           dbPath = "/var/chicago/rocks.db"
           quorum = 3
           compactionSize = 60GB
           databaseMode = true
           encryptAtRest = true
           witnessList = [
             ""
           ]
         }
         servers {
           admin {
             settings {
               bindHost = 10.25.145.56
             }
           }
           stats {
             settings {
               bindHost = 10.25.145.56
             }
           }
           db {
             settings {
               bindHost = 10.25.145.56
             }
           }
         }
       }
     }
   
Start the server :

``` # bin/chiServer.sh & ```

By default Logs will go to `bin/chi.log` file.


## Chicago clients ##

Import the chicago jar to your java project and use the Chicago client:

```java
    ChicagoAsyncClient chicagoClient = new ChicagoAsyncClient(zookeeperConnectionString, quoromSize);
    chicagoClient.start();
    
    // To send a Key/Value 
    ListenableFuture<Boolean> clientResp = chicagoClient.write("ColFam".getBytes(), "Key".getBytes(), "Val".getBytes());
    
    Futures.addCallback(clientResp, new FutureCallback<Boolean>() {
      @Override
      public void onSuccess(@Nullable Boolean aBoolean) {
        System.out.println("Woot~!");
      }

      @Override
      public void onFailure(Throwable throwable) {
        System.out.println("So saaad");
      }
    });
    
    // For Reads
    ListenableFuture<byte[]> clientResp = chicagoClient.read("ColFam".getBytes(), "Key".getBytes());
    Futures.addCallback(clientResp, new FutureCallback<byte[]>() {
      @Override
      public void onSuccess(@Nullable byte[] bytes) {
        assertEquals("Val", new String(bytes));
      }

      @Override
      public void onFailure(Throwable t) {
        throw new SadFaceException(t);
      }
    });
        
    //To send value to a TimeSeries DB:
    chicagoClient.tsWrite(topic, value);
    
    //To stream a TimeSeries DB:
    chicagoClient.stream(topic, offset);
    
```
