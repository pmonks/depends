[![Build Status](https://travis-ci.org/pmonks/depends.svg?branch=master)](https://travis-ci.org/pmonks/depends)
[![Open Issues](https://img.shields.io/github/issues/pmonks/depends.svg)](https://github.com/pmonks/depends/issues)
[![License](https://img.shields.io/github/license/pmonks/depends.svg)](https://github.com/pmonks/depends/blob/master/LICENSE)
[![Dependencies Status](http://jarkeeper.com/pmonks/depends/status.svg)](http://jarkeeper.com/pmonks/depends)

# depends

Reads dependencies from compiled Java code (.class files) and writes them out in a variety of different formats.

Why might that be useful, I hear you ask?

Welp the personal driver for this project was to support some work I was doing to validate 3rd party extensions
to an open source Java product.  Basically I needed to make sure that that 3rd party code didn't use the product's
private APIs, didn't attempt threading, didn't use ```Process.exec()``` or ```System.out/err```, didn't employ
dodgy ```ClassLoader``` tricks etc. etc.

I started out using grep but quickly ran into a couple of walls, including:
 1. not necessarily having access to the source code for these extensions;
 2. that approach requires manual followup due to Java's rather complex aliasing rules (e.g. is the code
    ```new Thread()``` referring to ```java.lang.Thread``` or ```org.sewing.Thread```??).

To do this I'm jamming the output into Neo4J then running a couple of Cypher queries to determine if the code
violates any of these rules.

Other possible uses of the data this tool produces include:
 1. Visualising the dependencies of a code base (particularly one you're unfamiliar with)
 2. Calculating certains kinds of software quality metrics (note however that tools like Structure101 already
    do this kind of thing)
 3. Faffing by looking at shiny graphy baubles

Notes:
 * The tool reads dependencies that are present in the compiled .class files.  Amongst other things this means
   that types referred to only via generics (e.g. ```Map<ClassA, ClassB>```) won't be listed in the
   dependency graph.
 * I still consider myself an utter Clojure n00b, so don't look at this code for best (or even mediocre)
   practices.  Any comments, feedback, criticism is welcome (email address below).

## Getting the bits

### Dependencies
 * [Oracle JDK 1.7+](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (one of the libraries
   used by depends requires JDK 1.7 or newer, and depends is tested on Oracle JDKs only)
 * [Leiningen v2.5+](http://leiningen.org/#install)
 * [Neo4j v2.0+](http://www.neo4j.org/download) (optional - only required if you intend to send output to a
   Neo4J database)

Note: these dependencies should be installed via your OS package manager, where possible.  On Mac OSX, I
strongly recommend [Homebrew](http://brew.sh/).

### Command line usage

If you wish to use `depends` as a command line app, checkout the source from [GitHub](https://github.com/pmonks/depends).

#### Running the command line app

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

#### Examples:
```shell
 # Parse a single .class file and write the dependencies to stdout in EDN format
 lein run -- -e /path/to/myclassfile.class

 # Parse all .class files in the specified directory and write the dependencies to stdout in JSON format
 lein run -- -j /path/to/folder/full/of/crap

 # Parse all .class files in the specified JAR file and write the dependencies to a Neo4J server running on localhost
 lein run -- -n http://localhost:7474/db/data/ /path/to/myjarfile.jar

 # Recursively parse all .class files in the specified EAR file (including those within JARs within WARs within the EAR) and drop the results on the floor
 # Nonsensical, but possible (I'm too lazy to do proper command line argument validaton *sigh*)
 lein run -- /path/to/myenterpriseturdbucket.ear

 # Recursively parse all .class files in the specified ZIP file (including any embedded JARs, WARs, EARs or ZIPs) and write the results to stdout in both EDN and JSON format, and to a Neo4J server running on another server
 # Why anyone would actually want to do all this in one shot is beyond me...
 lein run -- -e -j -n http://neo4j.veryimportantcompany.com:7474/db/data/ /path/to/yourzipisdown.zip
```

### Library usage

If you wish to use `depends` as a library, it's available as a Maven artifact from [Clojars](https://clojars.org/org.clojars.pmonks/depends):

[![version](https://clojars.org/org.clojars.pmonks/depends/latest-version.svg)](https://clojars.org/org.clojars.pmonks/depends)

The library's functionality is provided in the `depends.reader` and `depends.neo4jwriter` namespaces.
`depends.reader` contains the logic that analyses .class files and returns the results as data structures.
`depends.neo4jwriter` writes those data structures into a Neo4J database.

TODO: provide more detailed usage instructions for the library.

## Developer Information

[GitHub project](https://github.com/pmonks/depends)

[Bug Tracker](https://github.com/pmonks/depends/issues)

[![endorse](https://api.coderwall.com/pmonks/endorsecount.png)](https://coderwall.com/pmonks)

## License

Copyright © 2013-2016 Peter Monks (pmonks@gmail.com)

Distributed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html) either version 1.0 or (at your option) any later version.
