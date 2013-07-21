# depends
![Project Logo](https://github.com/pmonks/depends/wiki/images/depends.jpg)

Reads dependencies from compiled Java code (.class files) and writes them out in a variety of different formats.

## Installation

Checkout the source from [GitHub](https://github.com/pmonks/depends).

## Running / usage

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

Examples:
```shell
 # Parse a single .class file and write the dependencies to stdout in EDN format
 lein run -- -e /path/to/myclassfile.class

 # Parse all .class files in the specified directory and write the dependencies to stdout in JSON format
 lein run -- -j /path/to/folder/full/of/crap

 # Parse all .class files in the specified JAR file and write the dependencies to a Neo4J server running on localhost
 lein run -- -n http://localhost:7474/db/data/ /path/to/myjarfile.jar

 # Recursively parse all .class files in the specified EAR file (including those within JARs within WARs within the EAR) and don't write the results anywhere
 # Nonsensical, but possible
 lein run -- /path/to/myenterpriseturdbucket.ear
```

## Developer Information

[GitHub project](https://github.com/pmonks/depends)

[Bug Tracker](https://github.com/pmonks/depends/issues)

Continuous Integration: TODO!


## License

Copyright © 2013 Peter Monks (pmonks@gmail.com)

Distributed under the Creative Commons Attribution-ShareAlike 3.0 Unported License.
