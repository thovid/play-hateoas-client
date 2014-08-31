Play HATEOAS client
=================

This is a simple playframework module for working with HATEOAS services.

Getting the module
------------------

The module can be included by adding the resolvers

```
resolvers += Resolver.url("thovid play modules on github (releases)", url("http://thovid.github.com/releases/"))(Resolver.ivyStylePatterns)

resolvers += Resolver.url("thovid play modules on github (snapshots)", url("http://thovid.github.com/snapshots/"))(Resolver.ivyStylePatterns)
```

the the build file and using the dependency

```
"de.thovid" %% "play-hateoas-client" % "1.0-SNAPSHOT"
```

Supported versions
------------------

Currently, only playframework 2.2.x and scala 2.10 are supported. The module is in an early stage of development. The current version is 1.0-SNAPSHOT.


Using the module
------------------

The module expects service calls to return links in the format

```
{
  "some_content": "xzy",
  "links": [
    {
	  "rel": "self",
	  "path": "http://some.url/stuff/123"
	},
	{
	  "rel": "update",
	  "path": "http://some.url/other-stuff/123"
	
	}
  ]
}
```

Example usage:

1) Simple get:
```
val result = HATEOAS.client
  .at("http://localhost/samples/1")
  .get()
  .asJson {
    case (200, json) => name(json)
  }
```

gets the content of the url as json (of type ```play.api.libs.json.JsValue```). The result is of type ```Future[Either[String, A]]```, where ```A``` is the type of the partial function provided to the ```asJson``` method.

If an error occurred, or if the partial function is not defined for the result (for example, if the status code is not ```200``` in the example above), e ```Left[String]``` containing the error message is returned.

2) Get following a link:

```
val result = HATEOAS.client  
  .at(s"http://localhost/samples")
  .following("self", selectedBy("samples" -> "id", "2"))
  .get()
  .asJson {
    case (OK, json) => name(json)
  }
```

The code above makes two calls: First a GET to the provided url. It expects the response to contain a json array named samples containing objects with attribute id. It selects the entry with id = "2" and executes a GET on the link named "self" provided with this entry. The result of the second GET is handed over to the partial function parsing the result.

Licence
------------------
APACHE 2 Licence 
http://www.apache.org/licenses/LICENSE-2.0.txt