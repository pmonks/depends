# depends
![Depends](http://www.vamworld.com/file/view/depends.jpg/169661655/depends.jpg) "Because even adults need help dealing with sh*t from time to time."

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

## Developer Information

[GitHub project](https://github.com/pmonks/depends)

[Bug Tracker](https://github.com/pmonks/depends/issues)

Continuous Integration: TODO!


## License

Copyright © 2013 Peter Monks (pmonks@gmail.com)

Distributed under the Creative Commons Attribution-ShareAlike 3.0 Unported License.
