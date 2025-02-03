#!/bin/sh

if ! command -v clojure >/dev/null 2>&1; then
  echo "Error: 'clojure' command not found. Please install Clojure CLI:"
  echo "https://clojure.org/guides/install_clojure"
  exit 1
fi

clojure -M:run-m "$@"
