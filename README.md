# Invader Detector

<img src="./doc/images/invaders-sneak-peek.png" alt="Two invaders eyeing each other">

## Walkthrough

If you're using this program, sadly you know we have no time to waste.

```
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

```
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

If you prefer visuals, try `--save-images two-invaders.png`:

```
./detect-invaders.sh \
  --radar-sample-file resources/spec-radar-sample-2-guys.txt \
  --invader-files resources/spec-invader-1.txt:resources/spec-invader-2.txt \
  --save-images two-invaders.png
```

![Banner](doc/images/two-invaders.png)

Shell-shocked veterans who've seen too many invaders appreciate `--output-ascii-opaque-fill`:

```
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

```
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

```
./detect-invaders.sh \
  --radar-sample-file resources/spec-radar-sample.txt \
  --invader-files resources/spec-invader-1.txt:resources/spec-invader-2.txt \
  --print-matches

({:invader-id 0,
  :bbox {:x 60, :y 13, :width 11, :height 8},
  :score 90.9090909090909}
 {:invader-id 1,
  :bbox {:x 42, :y 0, :width 8, :height 8},
  :score 87.5}
 {:invader-id 0,
  :bbox {:x 74, :y 1, :width 11, :height 8},
  :score 87.5}
 {:invader-id 0,
  :bbox {:x 85, :y 12, :width 11, :height 8},
  :score 86.36363636363636}
 {:invader-id 1,
  :bbox {:x 82, :y 41, :width 8, :height 8},
  :score 85.9375}
 {:invader-id 1,
  :bbox {:x 16, :y 28, :width 8, :height 8},
  :score 85.9375}
 ...
```

## Usage

There's quite a few options:

```
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

## License

Copyright © 2025 Tayssir John Gabbour
