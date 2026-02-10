# Skills Repo Reorganization Implementation Plan

Goal: Reorganize this repository so all skill packages live under one `skills/` directory, and expose both `link-skills` and `unlink-skills` as `bb` tasks.

Architecture: Move each top-level skill package directory into `/Users/rcmerci/gh-repos/skills/skills/` while keeping non-skill project code at repository root.
Architecture: Refactor link management into testable Clojure functions with thin Babashka entry scripts for link and unlink operations.
Architecture: Wire both operations into `/Users/rcmerci/gh-repos/skills/bb.edn` tasks and validate behavior with filesystem-focused tests and command-level checks.

Tech Stack: Babashka, Clojure, `babashka.fs`, `bb.edn` tasks, `clojure.test`.

Related: Builds on `docs/agent-guide/001-clojuredocs-logseq-import.md`.

## Problem statement

The repository currently stores skill package directories at the root level, which makes root-level automation ambiguous and mixes product code with skill content.

The current `/Users/rcmerci/gh-repos/skills/link_skills.clj` links every non-hidden root directory, so it can accidentally link non-skill folders like `docs`, `src`, and `test`.

There is no `bb` task entry for skill linking, and there is no `unlink-skills` workflow for safe cleanup.

This plan uses `@planning-documents`, `@writing-plans`, and `@test-driven-development`.

## Testing Plan

I will add unit behavior tests for skill directory discovery to ensure only directories containing `SKILL.md` are treated as linkable skills.

I will add unit behavior tests for link creation to ensure links are created per skill and non-symlink destination conflicts fail fast without deletion.

I will add unit behavior tests for unlink behavior to ensure only links owned by this repo are removed and unrelated links are preserved.

I will add command-level checks for `bb tasks`, `bb run link-skills`, and `bb run unlink-skills` using temporary directories so local home directories are not mutated during validation.

I will add migration validation checks ensuring all moved skill directories remain discoverable under `/Users/rcmerci/gh-repos/skills/skills/` and no root-level skill directory remains.

NOTE: I will write *all* tests before I add any implementation behavior.

## Current and target layout

Current layout stores each skill directly under `/Users/rcmerci/gh-repos/skills/<skill-name>/SKILL.md`.

Target layout stores each skill under `/Users/rcmerci/gh-repos/skills/skills/<skill-name>/SKILL.md`.

```text
/Users/rcmerci/gh-repos/skills
├── bb.edn
├── link_skills.clj
├── unlink_skills.clj
├── src/
├── test/
├── docs/
└── skills/
    ├── planning-documents/
    ├── writing-plans/
    ├── test-driven-development/
    └── ...
```

## Implementation tasks

1. Create a new test namespace `/Users/rcmerci/gh-repos/skills/test/skills_repo/skill_linking_test.clj`.
2. Write a failing test that discovery returns only directories containing `SKILL.md` from a configurable source root.
3. Run `bb test --focus skills-repo.skill-linking-test/discovers-only-skill-directories` and confirm RED failure is behavior-related.
4. Write a failing test that link operation creates one symlink per discovered skill in each destination root.
5. Run focused test and confirm RED failure is due to missing link behavior.
6. Write a failing test that existing non-symlink destination paths are not deleted and trigger an explicit failure.
7. Run focused test and confirm RED failure is due to unsafe conflict handling.
8. Write a failing test that unlink removes only symlinks pointing to this repo skill directories.
9. Run focused test and confirm RED failure is due to missing unlink behavior.
10. Write a failing test that unlink leaves unrelated symlinks untouched.
11. Run focused test and confirm RED failure is due to over-broad deletion behavior.
12. Update `/Users/rcmerci/gh-repos/skills/src/clojuredocs_to_logseq/test_runner.clj` to include `skills-repo.skill-linking-test`.
13. Implement minimal reusable linking functions in `/Users/rcmerci/gh-repos/skills/src/skills_repo/skill_linking.clj`.
14. Implement skill discovery from `/Users/rcmerci/gh-repos/skills/skills` using `SKILL.md` presence as the selection rule.
15. Implement conflict checks so existing non-symlink paths in destination roots fail with clear error output.
16. Implement unlink behavior that removes only symlinks whose resolved target is one of the source skill directories.
17. Refactor `/Users/rcmerci/gh-repos/skills/link_skills.clj` to call shared logic and default source root to `/Users/rcmerci/gh-repos/skills/skills`.
18. Create `/Users/rcmerci/gh-repos/skills/unlink_skills.clj` with the same destination root defaults and shared unlink logic.
19. Add environment override support for destination roots to allow safe test-time temp directories.
20. Run `bb test --focus skills-repo.skill-linking-test/discovers-only-skill-directories` and confirm GREEN.
21. Run `bb test --focus skills-repo.skill-linking-test/creates-symlinks-for-each-skill` and confirm GREEN.
22. Run `bb test --focus skills-repo.skill-linking-test/fails-on-non-symlink-conflict` and confirm GREEN.
23. Run `bb test --focus skills-repo.skill-linking-test/unlinks-only-owned-symlinks` and confirm GREEN.
24. Run `bb test --focus skills-repo.skill-linking-test/preserves-unrelated-symlinks` and confirm GREEN.
25. Create `/Users/rcmerci/gh-repos/skills/skills/` directory.
26. Move each existing skill directory from repository root into `/Users/rcmerci/gh-repos/skills/skills/` using `git mv`.
27. Run `find /Users/rcmerci/gh-repos/skills -maxdepth 2 -name SKILL.md -type f | sort` and confirm results are under `/Users/rcmerci/gh-repos/skills/skills/`.
28. Update `/Users/rcmerci/gh-repos/skills/bb.edn` to add `link-skills` task calling `bb link_skills.clj`.
29. Update `/Users/rcmerci/gh-repos/skills/bb.edn` to add `unlink-skills` task calling `bb unlink_skills.clj`.
30. Run `bb tasks | rg \"link-skills|unlink-skills\"` and confirm both tasks are listed with docs.
31. Run full suite with `bb test` and confirm all tests are green.
32. Run `bb run link-skills` with temporary destination roots and confirm expected symlink creation output.
33. Run `bb run unlink-skills` with temporary destination roots and confirm only owned links are removed.
34. Run `git status --short` and confirm only intended file moves and script or config changes are present.

## Validation commands and expected outputs

Run the following from `/Users/rcmerci/gh-repos/skills`.

```bash
bb tasks | rg "link-skills|unlink-skills"
```

Expected output includes two lines containing `link-skills` and `unlink-skills`.

```bash
find /Users/rcmerci/gh-repos/skills/skills -mindepth 2 -maxdepth 2 -name SKILL.md | wc -l
```

Expected output is `12` for the current repository state after migration.

```bash
bb test
```

Expected output exits with status code `0` and no failing tests.

## Edge cases

A destination root can already contain real directories with the same skill name, and the plan must fail safely instead of deleting user data.

A destination root can contain unrelated symlinks, and unlink must not remove links that do not resolve to this repo skill source directories.

A developer can run scripts from a non-root working directory, so source root resolution must be anchored to script location or explicit configuration.

A skill directory can temporarily miss `SKILL.md` during partial edits, and discovery should skip it instead of creating broken links.

The migration can be partially applied in a dirty working tree, so each move should be verifiable before running link tasks.

## Questions requiring clarity

Should `link-skills` hard-fail when one destination root is unavailable, or continue with a per-root warning and non-zero summary status.

Should the migration include a compatibility period where old root-level skill paths are kept as symlinks, or should it be a strict move-only cutover.

## Testing Details

Tests will focus on filesystem behavior outcomes such as discovered skill set, created symlink targets, and safe unlink scope, rather than internal helper structure.

The command-level checks will assert observable CLI behavior from `bb` tasks and not only direct function calls.

Migration checks will assert repository structure after `git mv` to prevent regressions where skills remain at root.

## Implementation Details
- Keep source skill discovery rule as `directory contains SKILL.md` to avoid accidental linking of project folders.
- Use absolute resolved paths when comparing unlink ownership to avoid false positives from relative symlink targets.
- Keep destination defaults as `~/.codex/skills` and `~/.config/opencode/skills`.
- Add destination override environment variable for safe tests with temporary directories.
- Keep `link_skills.clj` and `unlink_skills.clj` as thin entry scripts that delegate to shared source code.
- Use explicit non-zero exits on unsafe conflicts so automation can fail fast.
- Keep `bb.edn` task docs concise and action-oriented.
- Ensure moved skill directories preserve existing internal `references/`, `scripts/`, and `assets/` subfolders.
- Keep full test run green before and after directory migration.
- Avoid touching unrelated project code under `/Users/rcmerci/gh-repos/skills/src/clojuredocs_to_logseq`.

## Question

Decision: `unlink-skills` removes only symlinks that resolve to skill directories in this repository.

---
