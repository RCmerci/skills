---
name: logseq-electron-debug
description: "Run Logseq Electron dev build with a remote debugging port and attach Chrome DevTools (CDP) reliably."
---

# Logseq Electron Debug Skill

Use this when you need to inspect the Electron renderer (DOM, network, localStorage, Logseq APIs like `logseq.api.*`).

Prefer launching Electron from `static/` (Electron Forge) instead of the root `yarn dev-electron-app` (gulp wrapper). The wrapper can be noisy and can surface false-negative exit codes even when the app launched.

## Quick Start (Remote Debugging Port)

1. Ensure the dev app is built:

```bash
cd /Users/brz/repos/logseq
yarn watch
```

2. Start Electron with a remote debugging port:

```bash
cd /Users/brz/repos/logseq/static
yarn electron:dev -- -- --remote-debugging-port=9333
```

If you want to bypass Yarn and see the real command:

```bash
cd /Users/brz/repos/logseq/static
./node_modules/.bin/electron-forge start -- --remote-debugging-port=9333
```

3. Verify the port is live:

```bash
curl http://127.0.0.1:9333/json/list
```

4. Attach Chrome:

- Open `chrome://inspect/#devices`
- Add `localhost:9333`
- Click “inspect”

## Common Pitfall: `yarn dev-electron-app` Does Not Forward Flags

`yarn dev-electron-app` runs via `gulp electron`, and it hard-calls `yarn electron:dev` without a way to pass through `--remote-debugging-port=...` unless you edit the gulp task (see `/Users/brz/repos/logseq/gulpfile.js`).

If you need a debug port, start via `static/` as shown above.

## Notes

- If a desktop Logseq instance is already running, quit it first. Electron single-instance behavior can make it look like flags were applied when they were not.
- Pick a port that is not in use (`9333` is usually safe).
