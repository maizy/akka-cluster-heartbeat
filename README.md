# akka-cluster-heartbeat - akka hello world in a hard way

Trying to implement minimal akka cluster app with a simple purpose.

## Dev

```
cd akka-cluster-heartbeat
sbt -Dlogback.configurationFile=dev-configs/logback-dev.xml -Dconfig.file=dev-configs/dev.conf run
```


## Production run

### Building

```
sbt assembly
```


### Launch node

```
java -jar target/scala-2.11/akka-cluster-heartbeat-assembly-*.jar
```
**TODO**


### Add node

**TODO**


### Remove node

**TODO**


## Licence

MIT
