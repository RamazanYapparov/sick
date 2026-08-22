---
name: create-siq-test-package
description: Create and validate SIQ development packages for concrete functional, regression, parser, media, or UI test scenarios in the sick repository. Use when Codex needs to translate a test scenario or code/UI change into a JSON recipe, select files from the local SIQ media library, invoke the official SIPackages-based generator, reuse an existing SIQ fixture, create a controlled malformed derivative, or load the resulting package in the application for verification.
---

# Create SIQ Test Package

Create the smallest package that proves the requested behavior. Use the repository's official
`SIPackages`-based dev tool; never assemble a canonical SIQ archive manually.

## Locate the tool

1. Resolve the repository root with `git rev-parse --show-toplevel`.
2. Read the applicable `AGENTS.md` files yourself.
3. Require `devtools/siq-package-generator/siq-tool`. If absent, report that the generator PR must
   be merged or checked out; do not substitute a custom serializer.
4. Read `devtools/siq-package-generator/recipe.schema.json` and only the closest example:
   `basic.json`, `media.example.json`, or `special-questions.example.json`.
5. Read [scenario-patterns.md](references/scenario-patterns.md) only for media, special-question,
   or malformed-package cases.

Do not add the tool to Gradle settings or production dependencies.

## Define the test case

1. Inspect the requested behavior, relevant code, tests, and current diff.
2. State the observable assertion: what must load, render, play, reject, or transition.
3. Prefer one focused package. Add boundary variants only when they exercise different logic.
4. Reuse an existing recipe or artifact when its structure and expected assertion match exactly.
5. Infer routine fixture details. Ask the user only when missing information changes the expected
   result or when no suitable licensed media exists.

Choose the fixture class:

- **Canonical**: generate exclusively through `siq-tool`.
- **Malformed**: generate a canonical baseline first, copy it under ignored `artifacts/`, and apply
  only the named ZIP/XML mutation. Keep the baseline intact and expect official validation to fail
  when the mutation violates the SIQ model.

## Select media

Run:

```bash
./devtools/siq-package-generator/siq-tool list-media
```

Use files under `devtools/siq-package-generator/media/` without modifying them. Inspect file type,
dimensions, duration, and size when relevant. Select the smallest file that exercises the required
format or playback behavior. Do not commit media files.

If exact semantic content is irrelevant, reuse a suitable existing file. If the required format is
missing, stop and name the expected media path instead of silently changing the scenario.

## Write the recipe

Use kebab-case scenario names. Store reusable regression recipes in
`devtools/siq-package-generator/cases/`; store one-off recipes in ignored
`devtools/siq-package-generator/artifacts/recipes/`.

Set explicit `id` and `date` for committed recipes. Use paths relative to `media/`. Include only the
rounds, themes, questions, answers, parameters, and media needed for the assertion. Never embed
absolute paths, credentials, remote tokens, or unrelated user data.

Validate the JSON syntax and review it against `recipe.schema.json` before generation.

## Generate and validate

Run from the repository root:

```bash
./devtools/siq-package-generator/siq-tool generate <recipe.json> \
  --output devtools/siq-package-generator/artifacts/<scenario>.siq \
  --force

./devtools/siq-package-generator/siq-tool validate \
  devtools/siq-package-generator/artifacts/<scenario>.siq

unzip -l devtools/siq-package-generator/artifacts/<scenario>.siq
```

Confirm the summary counts and every expected media entry. Treat validation as semantic; ZIP bytes
and timestamps need not be deterministic.

## Exercise the application

Choose verification proportional to the change:

- Parser or mapper: load the generated artifact through `SiqExtractor` and `SiqReader` in a focused
  automated test.
- Game logic: load the package, drive the relevant events, and assert state transitions.
- UI or media: run `./gradlew :composeApp:run`, load the artifact through the pack browser, and use
  available computer-control tooling to exercise the exact flow. Follow that tool's skill first.

Run `./gradlew :siq:test` after parser-facing work. Do not declare success from package generation
alone when the scenario targets application behavior.

## Preserve repository hygiene

Before handing off, run `git status --short --ignored`. Commit reusable recipe and skill changes
only. Keep `.dotnet/`, `.nuget/`, `media/` contents, `artifacts/`, extracted archives, logs, and
one-off test harnesses out of Git. Preserve unrelated user changes.

Report the recipe path, generated package path, selected media, validation result, application
assertions, and any untested limitation.

