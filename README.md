# dependency-reader

Reads dependencies from compiled Java code (.class files).

## Installation

Checkout the source from [GitHub](https://github.com/pmonks/dependency-reader).

## Running / usage

```shell
 lein run -- [arguments]
```

    Switches               Default  Desc                                             
    --------               -------  ----                                             
    -e, --no-edn, --edn    false    Produce EDN output instead of JSON (the default) 
    -h, --no-help, --help  false    Show help                                        

    Args           Desc
    ----           ----
    source         Returns the dependencies of all class files in the given location (which may be a .class file, a directory or an archive).

## Developer Information

[GitHub project](https://github.com/pmonks/dependency-reader)

[Bug Tracker](https://github.com/pmonks/dependency-reader/issues)

Continuous Integration: TODO!


## License

Copyright © 2013 Peter Monks (pmonks@gmail.com)

Distributed under the Creative Commons Attribution-ShareAlike 3.0 Unported License.
