# akka-cluster-heartbeat - akka hello world in a hard way

Trying to implement minimal akka cluster app with a simple purpose.

## Dev

```
cd akka-cluster-heartbeat
sbt 'run cmd --option'
```


## Production run

### Building

```
sbt assembly
```


### Usage

```
java -jar target/scala-2.11/akka-cluster-heartbeat-assembly-*.jar COMMAND [OPTIONS]
```

Commands:
* node - work with cluster node
* emulator - start emulator

For options see `java -jar akka-cluster-heartbeat-assembly.jar --help`

### Start cluster

```
java -jar akka-cluster-heartbeat-assembly.jar node --port 2550
```

### Add node

```
java -jar akka-cluster-heartbeat-assembly.jar node --port 2551 --role stat
```

### Remove node

**TODO**


## License

MIT

See [LICENSE.txt](LICENSE.txt) for details.
