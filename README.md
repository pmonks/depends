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
    -f, --file                      Print the dependencies for a single .class file.                                
    -d, --directory                 Print the dependencies for all .class files recursively in the given directory. 
    -h, --no-help, --help  false    Show help                                                                       
    
    Args                   Desc
    ----                   ----
    file-or-directory-name The .class filename or directory containing .class files to print dependency information for.

## Developer Information

[GitHub project](https://github.com/pmonks/dependency-reader)

[Bug Tracker](https://github.com/pmonks/dependency-reader/issues)

Continuous Integration: TODO!


## License

Copyright © 2013 Peter Monks (pmonks@gmail.com)

Distributed under the Creative Commons Attribution-ShareAlike 3.0 Unported License.
