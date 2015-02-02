beersample-java2
================

A sample application for the Java SDK 2.0 and Couchbase Server 3.0

*Note: this is a rewrite of the previous SDK 2.0 tutorial material, for 2.1+ releases.
You can find the previous material on the __oldtutorial__ branch, see it*
[here on github](https://github.com/couchbaselabs/beersample-java2/tree/oldtutorial).

## What's needed?
 - The beersample sample bucket
 - The beer/brewery_beers view (built in in beersample sample)
 - An additional view beer/by_name with the following map function (you should copy the beer designdoc to dev in order
 to edit it and add this view):

```
    function (doc, meta) {
       if (doc.type == "beer") {
         emit(doc.name, doc.brewery_id)
       }
     }
```

## Building and running
Correctly configure the application for your couchbase installation by editing **`src/main/resources/application.yml`**.

To build a self-contained jar of the application, run the following Maven command:

    mvn clean package

To run the application and expose the REST API on `localhost:8080`, run the following command:

    java -jar target/beersample2-1.0-SNAPSHOT.jar

## REST API
The REST API is deployed on port 8080 and has the following routes:

### Beer Routes
 * `GET /beer/{id}`: retrieve the Beer with id {id} (one json object representing the beer)
 * `POST /beer`: with a jsonObject in body representing the beer data, creates a new beer
 * `PUT /beer/{id}`: with a jsonObject in body representing the updated beer data, updates a beer of id {id}
 * `DELETE /beer/{id}`: deletes the beer of id {id}
 * `GET /beer`: list all the beers, just outputting the beers `id` and `name` in an array of JSON objects
 * `GET /beer/search/{partOfName}`: list all the beers which name's contains {partOfName} (ignoring case). Each returned
 beer is represented as a JSON object with the beer's `id` and `name` and the whoe beer details under `detail`.

```
{
    "id": "theBeerId",
    "name": "The Beer",
    "detail": {
        "name": "The Beer",
        "category": "German Ale",
        ...
    }
}
```

### Brewery Routes
 * `GET /brewery/{id}`: retrieve the details of brewery {id}, along with the list of beers produced by this brewery (in
 a sub-array `beers`, one JSON object for each beer having the beer's id under `id` and the beer's detail under `beer`).

```
"beers": [
    {
        "id": "theBeerId",
        "beer": {
            "name": "someBeer",
            "category": "German Ale",
            ...
        }
    },
    ...
]
```
