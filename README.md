# User Service API

## API Documentation

You can find it [here](https://editor.swagger.io/?url=https%3A%2F%2Fraw.githubusercontent.com%2Fquiquedev%2Fuser-service%2Fmaster%2Fopenapi.yaml).

## Local run

### Requirements

You must have [docker-compose](https://docs.docker.com/compose/install) installed in your machine.

### How To

```
git clone git@github.com:quiquedev/user-service.git
cd user-service
docker-compose up --build
```

The service will be listening on the port `8080`.

## About

### Technologies

#### Language

[Scala 2.13.2](https://www.scala-lang.org/download/2.13.2.html)

#### Libraries

Implementation takes advantage of the benefits of [purely functional programming](https://en.wikipedia.org/wiki/Purely_functional_programming)
by using the following libraries in production:

* [cats](https://typelevel.org/cats) as the main functional programming library
* [cats-effect](https://typelevel.org/cats-effect) as io type 
* [http4s](https://http4s.org) as http server
* [doobie](https://tpolecat.github.io/doobie) as jdbc layer
* [circe](https://circe.github.io/circe) as json library
* [pureconfig](https://pureconfig.github.io) as configuration loader

Other non functional libraries:

* [flyway](https://flywaydb.org) for database version control

For testing:

* [scalatest](https://www.scalatest.org) as testing tool
* [testcontainers-scala](https://github.com/testcontainers/testcontainers-scala) for dockerized test environments
* [sbt-scoverage](https://github.com/scoverage/sbt-scoverage) to measure test coverage

#### Persistence

[PostgreSQL](https://www.postgresql.org) is the storage type I decided to use so that we could have:

* normalized data
* version control
* json support

#### API 

Neither frameworks like [Consumer-Driven Contracts](https://martinfowler.com/articles/consumerDrivenContracts.html) nor
libraries like [Avro Schemas](https://docs.oracle.com/database/nosql-12.1.3.0/GettingStartedGuide/avroschemas.html)
or [Bean Validation](https://beanvalidation.org/1.0/spec) have been used to materialize the API and the *DTO*'s requirements.

Specification has been documented with [OpenAPI](https://swagger.io/specification) and validation has been done programmatically
in an elegant way by using [Validated](https://typelevel.org/cats/datatypes/validated.html).

Size constraints, i.e.: maximum number of characters of *lastName*, have been introduced, in order to avoid to the possibility of
blowing up the service and the persistence engine. No constraints in terms of format have been used on *mail* and *number*
but could be easily introduced. The configuration of these constraints, i.e.: maximum size of 5, have been written programmatically 
in the code for simplicity.

## Code

### [Tagless final](https://scalac.io/tagless-final-pattern-for-scala-code)
This functional programming pattern is used in production code with the main purpose of abstracting out the *IO* type used.
The effect type is specified [only in one place](src/main/scala/info/quiquedev/userservice/Main.scala) so that we can change
it easily.

### Simplified [Entity-control-boundary](https://en.wikipedia.org/wiki/Entity-control-boundary)
Each endpoint uses exclusively an [use case](src/main/scala/info/quiquedev/userservice/usecases/UserUsecases.scala) 
which encapsulate the required business logic.
The relation *endpoint-use_case* is `1:1` with the purpose of introducing orthogonality to totally decouple business use cases.

### [Data Transfer Object](https://en.wikipedia.org/wiki/Data_transfer_object)
 
*DTO* and [model](https://en.wikipedia.org/wiki/Data_model) are completely separated but still keep a almost `1:1` relationship
i.e. for the representation of an `User` we have the [DTO](src/main/scala/info/quiquedev/userservice/routes/dtos/UserDto.scala)
and its corresponding [model](src/main/scala/info/quiquedev/userservice/usecases/model/User.scala).

This design allow us to decouple the [service domain](https://en.wikipedia.org/wiki/Domain-driven_design) from the data model.

### DTO And Data Model Validation

The validation is only performed on `DTO level` since the relation between *DTO* and *data model* is almost `1:1` and there is
not a complex business logic. However we compensate the lack of *data model* validation with **exhaustive DTO validation tests**.

## Test Coverage
* Statement coverage:  **89.07%** 
* Branch coverage: **100%**




