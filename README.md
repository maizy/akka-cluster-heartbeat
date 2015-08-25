# akka-cluster-heartbeat - akka hello world in a hard way

Trying to implement minimal akka cluster app with a simple purpose.


## Dev

```
cd akka-cluster-heartbeat
sbt 'run cmd --option'
```

### Tests

```
sbt test
```

If you run tests in the IntelliJ IDEA, don't forget to run multi-jvm tests manually:
```
sbt multi-jvm:test
```


## Building

```
sbt assembly
```


## Usage

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

Just kill the process by SIGTERM or SIGINT.
```
kill -INT $pid
kill $pid
```

or just `Ctrl+C` if you have an interactive run.


## License

MIT

Copyright (c) 2015 [Nikita Kovaliov](https://github.com/maizy), [dev.maizy.ru](http://dev.maizy.ru/)

See [LICENSE.txt](LICENSE.txt) for details.
