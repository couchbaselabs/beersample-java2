beersample-java2
================

A sample application for the Java SDK 2.0 and Couchbase Server 3.0

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

## REST API
The REST API is deployed on port 8080 and has the following routes:

### Beer Routes
 * `GET /beer/{id}`: retrieve the Beer with id {id} (one json object representing the beer)
 * `POST /beer`: with a jsonObject in body representing the beer data, creates a new beer
 * `PUT /beer/{id}`: with a jsonObject in body representing the updated beer data, updates a beer of id {id}
 * `DELETE /beer/{id}`: deletes the beer of id {id}
 * `GET /beer`: list all the beers, just outputting the beers `id` and `name` in an array of JSON objects

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