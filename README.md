# industries.tjg/invader-detector

## Installation

Download from https://github.com/industries.tjg/invader-detector

## Usage

FIXME: explanation

Run the project directly, via `:exec-fn`:

    $ clojure -X:run-x
    Hello, Clojure!

Run the project, overriding the name to be greeted:

    $ clojure -X:run-x :name '"Someone"'
    Hello, Someone!

Run the project directly, via `:main-opts` (`-m industries.tjg.invader-detector`):

    $ clojure -M:run-m
    Hello, World!

Run the project, overriding the name to be greeted:

    $ clojure -M:run-m Via-Main
    Hello, Via-Main!

Run the project's tests (they'll fail until you edit them):

    $ clojure -T:build test

Run the project's CI pipeline and build an uberjar (this will fail until you edit the tests to pass):

    $ clojure -T:build ci

Run that uberjar:

    $ java -jar target/industries.tjg/invader-detector-0.1.0-SNAPSHOT.jar


## License

Copyright © 2025 Tayssir John Gabbour
