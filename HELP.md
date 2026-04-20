# Getting Started

### Running Redis (required for caching)

This app uses Redis for caching claim lookups and RAG retrieval results. Start it via Docker before running the app:

```bash
docker run -d -p 6379:6379 --name langchain-redis redis
```

To stop it:
```bash
docker stop langchain-redis
```

To restart it later:
```bash
docker start langchain-redis
```

### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.5/gradle-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.0.5/gradle-plugin/packaging-oci-image.html)
* [Spring Web Services](https://docs.spring.io/spring-boot/4.0.5/reference/io/webservices.html)
* [Spring Reactive Web](https://docs.spring.io/spring-boot/4.0.5/reference/web/reactive.html)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/4.0.5/reference/actuator/index.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Producing a SOAP web service](https://spring.io/guides/gs/producing-web-service/)
* [Building a Reactive RESTful Web Service](https://spring.io/guides/gs/reactive-rest-service/)
* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)

### Additional Links
These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)

