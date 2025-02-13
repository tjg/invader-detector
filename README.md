# Invader Detector

<img src="./doc/images/invaders-sneak-peek.png" 
     alt="Two invaders eyeing each other">

[Background.](https://github.com/tjg/invader-detector/blob/main/doc/spec.md)
This app takes in an ASCII representation of space invaders and a
radar sample, and does pattern recognition to locate them.

## Walkthrough

If you're using this program, sadly you know we have no time to waste.

```sh
cd /path/to/invader-detector

./detect-invaders.sh \
  --radar-sample-file resources/spec-radar-sample-2-guys.txt \
  --invader-files resources/spec-invader-1.txt:resources/spec-invader-2.txt \
  --print-ascii

---╭84%─────╮------o-╭91%────────╮-
-oo│---oo---│-o--o-o-│--o-----o--│-
-o-│--oooo--│---oo---│-------o---│-
---│oooooooo│--o-----│--oooo-oo--│-
---│oo-oo--o│-o--o---│----ooo-oo-│-
o--│-ooooooo│--------│o--oooooo-o│-
o--│--o--ooo│--o----o│o-o-ooooo-o│-
---│-o-oo---│-----o-o│o-o-----o-o│-
---│oo--oo-o│-----o--│---oo-oo---│-
---╰────────╯------o-╰───────────╯-
---o----o------o-----╰────────╯──╯-
```

Let's clarify that image with
`--output-ascii-on-char "█"` and
`--output-ascii-off-char " "`:

```sh
./detect-invaders.sh \
  --radar-sample-file resources/spec-radar-sample-2-guys.txt \
  --invader-files resources/spec-invader-1.txt:resources/spec-invader-2.txt \
  --print-ascii \
  --output-ascii-on-char "█" --output-ascii-off-char " "

   ╭84%─────╮      █ ╭91%────────╮ 
 ██│   ██   │ █  █ █ │  █     █  │ 
 █ │  ████  │   ██   │       █   │ 
   │████████│  █     │  ████ ██  │ 
   │██ ██  █│ █  █   │    ███ ██ │ 
█  │ ███████│        │█  ██████ █│ 
█  │  █  ███│  █    █│█ █ █████ █│ 
   │ █ ██   │     █ █│█ █     █ █│ 
   │██  ██ █│     █  │   ██ ██   │ 
   ╰────────╯      █ ╰───────────╯ 
   █    █      █     ╰────────╯──╯ 
```

If you prefer images, try `--save-images two-invaders.png`:

```sh
./detect-invaders.sh \
  --radar-sample-file resources/spec-radar-sample-2-guys.txt \
  --invader-files resources/spec-invader-1.txt:resources/spec-invader-2.txt \
  --save-images two-invaders.png
```

![Two invaders side-by-side](doc/images/two-invaders.png)

Shell-shocked veterans who've seen too many invaders appreciate `--output-ascii-opaque-fill`:

```sh
./detect-invaders.sh \
  --radar-sample-file resources/spec-radar-sample-2-guys.txt \
  --invader-files resources/spec-invader-1.txt:resources/spec-invader-2.txt \
  --print-ascii \
  --output-ascii-on-char "█" \
  --output-ascii-off-char " " \
  --output-ascii-opaque-fill

   ╭84%─────╮      █ ╭91%────────╮ 
 ██│        │ █  █ █ │           │ 
 █ │        │   ██   │           │ 
   │        │  █     │           │ 
   │        │ █  █   │           │ 
█  │        │        │           │ 
█  │        │  █    █│           │ 
   │        │     █ █│           │ 
   │        │     █  │           │ 
   ╰────────╯      █ ╰───────────╯ 
   █    █      █     ╰────────╯──╯ 
```

The default match score is 70%. The radar gets crowded with
`--score-threshold 60`:

```sh
./detect-invaders.sh \
  --radar-sample-file resources/spec-radar-sample-2-guys.txt \
  --invader-files resources/spec-invader-1.txt:resources/spec-invader-2.txt \
  --print-ascii \
  --output-ascii-on-char "█" \
  --output-ascii-off-char " " \
  --output-ascii-opaque-fill \
  --score-threshold 60

│6╭╭84%─────╮─╮─╮╮ █ ╭91%────────╮─
│ ││        │ │ ││ █ │           │ 
│ ││        │ │ ││   │           │ 
│ ││        │ │ ││   │           │ 
│ ││        │ │ ││   │           │ 
│ ││        │ │ ││   │           │ 
│ ││        │ │ ││  █│           │ 
│ ││        │ │ ││█ █│           │ 
╰─││        │ │ ││█  │           │ 
  ╰╰────────╯─╯─╯╯ █ ╰───────────╯─
  ╰────────╯─╯ █     ╰────────╯──╯─
```

So far, we've focused on human UIs. But naturally we'll need more
precise data:

```sh
./detect-invaders.sh \
  --radar-sample-file resources/spec-radar-sample-2-guys.txt \
  --invader-files resources/spec-invader-1.txt:resources/spec-invader-2.txt \
  --print-matches

({:invader-id 0,
  :bbox {:x 22, :y 1, :width 11, :height 8},
  :score 90.9090909090909}
 {:invader-id 1,
  :bbox {:x 4, :y 1, :width 8, :height 8},
  :score 84.375}
 {:invader-id 1,
  :bbox {:x 22, :y 2, :width 8, :height 8},
  :score 71.875}
 {:invader-id 1,
  :bbox {:x 25, :y 2, :width 8, :height 8},
  :score 70.3125})
```

Let's try a bigger radar sample:

```
./detect-invaders.sh \
  --radar-sample-file resources/spec-radar-sample.txt \
  --invader-files resources/spec-invader-1.txt:resources/spec-invader-2.txt \
  --save-images spec-locations-threshold-70.png
```

<img src="./doc/images/spec-locations-threshold-70.png"
     alt="Invader locations with 70% score-threshold" width="400"/>


## Usage

There's quite a few options. Also, you can use multiple output
switches together in a single commandline.

```sh
./detect-invaders.sh --help
Detect invaders in radar samples.

Usage: ./invader-detector.sh [options]

Options:
      --radar-sample-file FILE                       Radar sample file
      --invader-files FILES                          Invader files separated by colons
      --input-on-chars CHARS        o,O              Characters denoting 'on', separated by commas
      --input-off-chars CHARS       -                Characters denoting 'off', separated by commas
      --input-lenient-parsing       true             Be lenient when interpreting input files.

      --max-results COUNT                            Maximum number of matches
      --score-threshold PERCENT     70               Minimum match score to include in results. Number from 0 to 100

      --print-ascii                                  Print ascii to screen
      --save-ascii FILE                              Output text file
      --save-images FILES                            Output image files, separated by colons.
      --print-matches                                Print matches to screen
      --save-matches FILE                            File with EDN-encoded matches
      --invader-colors COLORS       #4300ff,#44f20d  Colors to highlight invaders. Recycled if fewer colors than invaders.
      --output-ascii-on-char CHAR   o                For ascii output, character denoting 'on'.
      --output-ascii-off-char CHAR  -                For ascii output, character denoting 'off'.
      --output-ascii-opaque-fill                     For ascii output, make bounding boxes blank inside.
  -h, --help
```

## Design

### Where to start?

One place is
[user.clj](https://github.com/tjg/invader-detector/blob/c6126b53561284ec4b5a710cebd25e32f60cfda1/dev/src/user.clj#L34),
a developer sandbox that's likely more convenient than the CLI. Simply
evaluating the whole buffer will print results in the REPL, as well as
save images and matches to a temp dir.

It calls
[run.clj](https://github.com/tjg/invader-detector/blob/c6126b53561284ec4b5a710cebd25e32f60cfda1/src/industries/tjg/invader_detector/run.clj#L154),
which coordinates the sources/processors/sinks pipeline:

![Source/processor/sink diagram](doc/images/invader-detector-IO.png)

`pixel-matrix` is a 2D vector representing a radar sample or pattern:

```clojure
[[0 0 0 1 1 0 0 0]
 [0 0 1 1 1 1 0 0]
 [0 1 1 1 1 1 1 0]
 [1 1 0 1 1 0 1 1]
 [1 1 1 1 1 1 1 1]
 [0 0 1 0 0 1 0 0]
 [0 1 0 1 1 0 1 0]
 [1 0 1 0 0 1 0 1]]
```

`scorebox` is a map representing a bounding box in the radar sample,
with a score that estimates the likelihood that an invader's in the
bounding box:

```clojure
{:score 5/8
 :bbox {:x -1, :y -1, :width 6, :height 8}}
```

### Testing

`clj -X:test` runs tests.

Two testing frameworks:
- [Rich Comment Tests](https://github.com/matthewdowney/rich-comment-tests):
  Helps illustrate sourcecode.
- [Expectations](https://github.com/clojure-expectations/clojure-test):
  More expressive than `clojure.test`, but compatible with its tooling.

### Performance

> *“The real problem is that programmers have spent far too much time
> worrying about efficiency in the wrong places and at the wrong
> times; premature optimization is the root of all evil (or at least
> most of it) in programming.”*
>
> — Donald Knuth, ["Computer Programming as an Art"](https://dl.acm.org/doi/10.1145/361604.361612)

This currently uses a O(N²) algorithm. If performance optimization's
needed, [Criterium](https://github.com/hugoduncan/criterium) (a handy
benchmarking library) is included in the `:deps` alias.

There's many optimization opportunities to consider, if performance is
ever needed: improved algorithms, batching, judicious parallelism, CPU
cache-friendly datastructures, bit-vector comparisons, pre-processing,
etc.

## License

Copyright © 2025 Tayssir John Gabbour
