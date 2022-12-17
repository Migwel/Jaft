# Run using podman
First, build the jar executable using maven
```shell
$ mvn verify
```

This should create a jar file in the `target` directory.
Now, we can build our image using podman

```shell
$ podman build -t jaft .
```

The building phase is done. Now we can run it.
To make our life easier, we're running all our instance in the same pod (this makes the networking easier in dev/test phase)

```shell
$ podman pod create -p 8080-8082:8080-8082
$ podman pod ps
POD ID        NAME           STATUS   CREATED        INFRA ID      # OF CONTAINERS
5498222d2b83  eager_euler    Created  9 seconds ago  8729cb94cefe  1
$ podman run --pod eager_euler   -dt localhost/jaft --currentServerId=0 --server.port=8080 
$ podman run --pod eager_euler   -dt localhost/jaft --currentServerId=1 --server.port=8081 
$ podman run --pod eager_euler   -dt localhost/jaft --currentServerId=2 --server.port=8082 
```
