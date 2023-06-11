# Demo Pack
This is a simple Spring Boot application developed to demonstrate how to work with our API. 

We use auto-generated client to interact with our backend.
You can find API specification here `src/main/resources/specs/swagger-purchase-apis.yaml`,
which is a source for the generated client (for more information please see pom.xml)

## How to build
In the project directory run `mvn clean package` -
it will build executable jar `target/demo-1.0-SNAPSHOT.jar`

## How to run
In the project directory run `java -jar target/demo-1.0-SNAPSHOT.jar` -
it will launch the application on http://localhost:8083 (by default) 

## How to configure
All configuration settings are located here: `src/main/resources/application.yaml`
certificate is here: `../secret/dpg-demo-pack.dpg.db.com.p12`
and certificate password is here: `../secret/dpg-demo-pack.dpg.db.com.txt`
