beersample-java2
================

A sample application for the Java SDK 2.0 and Couchbase Server 3.0

## What's needed?
 - The beersample sample bucket
 - The beer/brewery_beers view (built in in beersample sample)
 - An additional view beer/by_name with the following map function (you should copy the beer designdoc to dev in order
 to edit it and add this view):
    `function (doc, meta) {
       if (doc.type == "beer") {
         emit(doc.name, doc.brewery_id)
       }
     }`
