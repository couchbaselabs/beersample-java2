beersample-java2
================

A sample application for the Java SDK 2.0 and Couchbase Server 3.0.

# Building and Running
The sample application needs the `beer-sample` sample bucket to be loaded. Additionally, the sample application needs
additional views to be created on this bucket: `beer/by_name` and `brewery/by_name`. Refer below for the code of these
views.

> beer/by_name

    function(doc, meta) {
      if (doc.type === "beer") {
          emit(doc.name, doc.brewery_id);
      }
    }

*Note: the beer design document already exist when loading sample, so it should be copied to development and this view
should then be added to it before re-publishing.*

> brewery/by_name

    function(doc, meta) {
      if (doc.type === "beer") {
          emit(doc.name, doc.brewery_id);
      }
    }


Once the views for the application have been created, application itself can be packaged and run:

**from Maven**
*Make sure Maven uses Java 8, see `mvn -version` for a quick check.*

Open a command line in the project's folder and run:

    mvn clean package

Then run the application (which will be visible on `localhost:8081`):

    java -jar target/beersample-0.0.1-SNAPSHOT.jar

