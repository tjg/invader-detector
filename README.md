# industries.tjg/invader-detector

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

Let's make that image a bit clearer with
`--output-ascii-on-char "█" --output-ascii-off-char " "`:

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

Shell-shocked veterans appreciate the `--output-ascii-opaque-fill`:

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

The default match score is 70%. You suddenly see more with `--score-threshold 60`:

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

## License

Copyright © 2025 Tayssir John Gabbour
