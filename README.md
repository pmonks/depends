# depends
![Project Logo](https://github.com/pmonks/depends/wiki/images/depends.jpg)

Reads dependencies from compiled Java code (.class files) and writes them out in a variety of different formats.

## Installation

Checkout the source from [GitHub](https://github.com/pmonks/depends).

## Running / usage

For now depends is source only, so...

```shell
 lein deps
 # Wait while Maven downloads teh internetz
 lein run -- [switches] source
```
    Switches               Default  Desc
    --------               -------  ----
    -j, --no-json, --json  false    Write JSON to stdout
    -e, --no-edn, --edn    false    Write EDN to stdout
    -n, --neo4j            false    Write to the specified Neo4J server
    -h, --no-help, --help  false    Show help

    Args           Desc
    ----           ----
    source         Returns the dependencies of all class files in the given location (which may be a .class file, a directory or an archive). Must be provided.

Eventually I plan to provide a precompiled executable uberjar as well.

### Examples:
```shell
 # Parse a single .class file and write the dependencies to stdout in EDN format
 lein run -- -e /path/to/myclassfile.class

 # Parse all .class files in the specified directory and write the dependencies to stdout in JSON format
 lein run -- -j /path/to/folder/full/of/crap

 # Parse all .class files in the specified JAR file and write the dependencies to a Neo4J server running on localhost
 lein run -- -n http://localhost:7474/db/data/ /path/to/myjarfile.jar

 # Recursively parse all .class files in the specified EAR file (including those within JARs within WARs within the EAR) and don't write the results anywhere
 # Nonsensical, but possible (I'm too lazy to do proper command line argument validaton *sigh*)
 lein run -- /path/to/myenterpriseturdbucket.ear

 # Recursively parse all .class files in the specified ZIP file (including any embedded JARs, WARs, EARs or ZIPs) and write the results to stdout in both EDN and JSON format, and to a Neo4J server running on another server
 # Why anyone would actually want to do all this in one shot is beyond me...
 lein run -- -e -j -n http://neo4j.veryimportantcompany.com:7474/db/data/ /path/to/yourzipisdown.zip
```

## Developer Information

[GitHub project](https://github.com/pmonks/depends)

[Bug Tracker](https://github.com/pmonks/depends/issues)

Continuous Integration: TODO!


## License

Copyright © 2013 Peter Monks (pmonks@gmail.com)

Distributed under the Creative Commons Attribution-ShareAlike 3.0 Unported License.
