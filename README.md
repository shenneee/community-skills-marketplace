# Community Skills Marketplace

PRG2104 Final Project — SDG 1 (No Poverty), Tier C (AI-Integrated).

A desktop ScalaFX app where members list skills they can teach/do for
others and spend credits (earned by helping others) instead of money —
letting people without cash access services they need.

## How this addresses SDG 1
People with limited money can still trade what they know how to do.
Instead of paying cash for tutoring, repairs, or childcare, members
earn credits by offering their own skills and spend those credits on
others' — nobody needs to be short on money to get help.

## Requirements
- JDK 21+
- sbt 1.10+

## Run it
```
sbt run
```

## Compile check
Use `sbt clean compile` (not `sbt -Wunused clean compile`). `-Wunused:all` is
already configured in `build.sbt`'s `scalacOptions`, so it's applied
automatically — passing `-Wunused` on the sbt command line isn't needed, and
will actually error, since sbt parses it as its own CLI flag rather than a
compiler option.

## Test it
```
sbt test
```

## Project layout
- `src/main/scala/marketplace/model` — immutable domain model (Person hierarchy, SkillListing, Transaction, CreditLedger, Repository)
- `src/main/scala/marketplace/persistence` — CSV-based save/load, all IO wrapped in `Try`
- `src/main/scala/marketplace/ui` — ScalaFX screens (Browse, My Listings, New Listing, Moderate, Wallet & History)
- `src/main/scala/marketplace/MarketplaceService.scala` — business logic connecting the pieces above
- `src/test/scala` — ScalaTest unit tests
- `docs/dev_log.md` — dated development log
- `ai/` — AI interaction log and declaration

## Third-Party Libraries
| Library | License |
|---|---|
| ScalaFX | BSD 3-Clause |
| OpenJFX (JavaFX) | GPL v2 with Classpath Exception |
| ScalaTest | Apache License 2.0 |

Full citation detail (versions, source links) is in `docs/citations.md`.

## AI usage summary
AI tools (Claude, Antigravity, Codex, and Freebuff) were used throughout
development — for initial project scaffolding, UI styling and layout,
debugging (e.g. a session-logout bug traced to ComboBox selection
events), Coordinator/moderation feature implementation, and generating
and verifying the UML diagram against the final codebase. All AI output
was reviewed, tested, and adjusted before being kept; several fixes
(e.g. the session bug) were diagnosed independently before asking AI to
help confirm the cause. Full prompt-by-prompt detail is logged in
`ai/interaction_log.md`, and tool usage is declared in `ai/declaration.md`.

## Features (map to the 4+ required)
1. Browse all listings and request an exchange
2. Create a new listing (with input validation)
3. View your own listings
4. View credit balance and transaction history
5. Moderate listings as a Coordinator (retire listings in your moderated category)

## Known limitations / next steps
- Single-session "logged in" member (no real login screen yet)
- No listing editing yet (delete is supported)
