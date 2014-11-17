/**
 * Copyright (C) 2014 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */
package com.couchbase.beersample.beers;

import java.util.Iterator;
import java.util.Map;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import com.couchbase.client.java.view.ViewRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST CRUD Controller for beers
 *
 * @author Simon Baslé
 * @since 1.0
 */
@RestController
@RequestMapping("/beer")
public class BeersController {

    private final Bucket bucket;

    @Autowired
    public BeersController(final Bucket bucket) {
        this.bucket = bucket;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getBeer(@PathVariable String id) {
        JsonDocument doc = bucket.get(id);
        if (doc != null) {
            return new ResponseEntity<String>(doc.content().toString(), HttpStatus.OK);
        } else {
            return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createBeer(@RequestBody Map<String, Object> beerData) {
        String id = "";
        try {
            JsonObject beer = parseBeer(beerData);
            id = "beer-" + beer.getString("name");
            bucket.insert(JsonDocument.create(id, beer));
            return new ResponseEntity<String>(id, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
        } catch (DocumentAlreadyExistsException e) {
            return new ResponseEntity<String>("Id " + id + " already exist", HttpStatus.CONFLICT);
        } catch (Exception e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{beerId}")
    public ResponseEntity<String> deleteBeer(@PathVariable String beerId) {
        JsonDocument deleted = bucket.remove(beerId);
        return new ResponseEntity<String>(""+deleted.cas(), HttpStatus.OK);
    }

    @RequestMapping(value = "/{beerId}", consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT)
    public ResponseEntity<String> updateBeer(@PathVariable String beerId, @RequestBody Map<String, Object> beerData) {
        try {
            JsonObject beer = parseBeer(beerData);
            bucket.replace(JsonDocument.create(beerId, beer));
            return new ResponseEntity<String>(beerId, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
        } catch (DocumentDoesNotExistException e) {
            return new ResponseEntity<String>("Id " + beerId + " does not exist", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private JsonObject parseBeer(Map<String, Object> beerData) {
        String type = (String) beerData.get("type");
        String name = (String) beerData.get("name");
        if (type == null || name == null || type.isEmpty() || name.isEmpty()) {
           throw new IllegalArgumentException();
        } else {
            JsonObject beer = JsonObject.create();
            for (Map.Entry<String, Object> entry : beerData.entrySet()) {
                beer.put(entry.getKey(), entry.getValue());
            }
            return beer;
        }
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> listBeers(@RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit) {
        ViewQuery query = ViewQuery.from("beer", "by_name");
        if (limit != null && limit > 0) {
            query.limit(limit);
        }
        if (offset != null && offset > 0) {
            query.skip(offset);
        }
        ViewResult result = bucket.query(query);
        if (!result.success()) {
            //TODO maybe detect type of error and change error code accordingly
            return new ResponseEntity<String>(result.error().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            JsonArray keys = JsonArray.create();
            Iterator<ViewRow> iter = result.rows();
            while(iter.hasNext()) {
                ViewRow row = iter.next();
                JsonObject beer = JsonObject.create();
                beer.put("name", row.key());
                beer.put("id", row.id());
                keys.add(beer);
            }
            return new ResponseEntity<String>(keys.toString(), HttpStatus.OK);
        }
    }
}
