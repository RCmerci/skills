#!/usr/bin/env node
/**
 * Audit recent agent sessions for a given cwd and surface likely “blind spots”
 * that cause back-and-forth (re-asking, re-planning, re-explaining).
 *
 * Focus: Codex sessions in ~/.codex/sessions (JSONL).
 *
 * Output: Markdown report to stdout.
 *
 * Usage:
 *   ./audit-sessions.js --agent codex [--cwd /path/to/repo] [--limit 20] [--scan 400]
 */

const fs = require("fs");
const path = require("path");
const os = require("os");

function die(msg) {
  process.stderr.write(`${msg}\n`);
  process.exit(1);
}

function parseArgs(argv) {
  const args = {
    agent: "codex",
    cwd: process.cwd(),
    limit: 20,
    scan: 400,
  };

  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--agent" && argv[i + 1]) args.agent = argv[++i];
    else if (a === "--cwd" && argv[i + 1]) args.cwd = argv[++i];
    else if (a === "--limit" && argv[i + 1]) args.limit = Number(argv[++i]);
    else if (a === "--scan" && argv[i + 1]) args.scan = Number(argv[++i]);
    else if (a === "-h" || a === "--help") args.help = true;
    else die(`Unknown argument: ${a}`);
  }

  if (!Number.isFinite(args.limit) || args.limit <= 0) die(`--limit must be > 0`);
  if (!Number.isFinite(args.scan) || args.scan <= 0) die(`--scan must be > 0`);
  return args;
}

function listJsonlFilesRecursively(rootDir) {
  const out = [];
  const queue = [rootDir];
  while (queue.length > 0) {
    const dir = queue.pop();
    let entries;
    try {
      entries = fs.readdirSync(dir, { withFileTypes: true });
    } catch {
      continue;
    }
    for (const ent of entries) {
      const full = path.join(dir, ent.name);
      if (ent.isDirectory()) queue.push(full);
      else if (ent.isFile() && ent.name.endsWith(".jsonl")) out.push(full);
    }
  }
  return out;
}

function readFirstJsonLine(filePath) {
  const fd = fs.openSync(filePath, "r");
  try {
    const buf = Buffer.alloc(16 * 1024);
    const n = fs.readSync(fd, buf, 0, buf.length, 0);
    if (n <= 0) return null;
    const chunk = buf.toString("utf8", 0, n);
    const firstNewline = chunk.indexOf("\n");
    const line = (firstNewline >= 0 ? chunk.slice(0, firstNewline) : chunk).trim();
    if (!line) return null;
    return JSON.parse(line);
  } finally {
    fs.closeSync(fd);
  }
}

function normalizeQuestion(s) {
  return s
    .toLowerCase()
    .replace(/`[^`]+`/g, "`<code>`")
    .replace(/\/users\/[^ \n\t]+/g, "<path>")
    .replace(/\b[0-9]+\b/g, "<n>")
    .replace(/\s+/g, " ")
    .trim();
}

function stripFences(s) {
  // Remove fenced code blocks; questions in code are usually noise.
  return s.replace(/```[\s\S]*?```/g, " ");
}

function extractQuestionSentences(assistantText) {
  const cleaned = stripFences(assistantText);
  const rawParts = cleaned.split("?");
  const questions = [];
  for (let i = 0; i < rawParts.length - 1; i++) {
    const part = rawParts[i];
    // take last ~180 chars before '?', since we split on '?'
    const tail = part.slice(Math.max(0, part.length - 180));
    const q = `${tail}?`
      .replace(/\s+/g, " ")
      .replace(/`[^`]+`/g, "`<code>`")
      .trim();

    if (q.length < 12 || q.length > 180) continue;
    // Heuristic: keep things that look like actual questions, not punctuation in prose.
    if (!/\b(what|why|how|which|where|when|can|could|should|would|do|does|did|want|need|ok)\b/i.test(q)) continue;
    questions.push(q);
  }
  return questions;
}

function extractCodexTranscript(sessionPath) {
  const raw = fs.readFileSync(sessionPath, "utf8");
  const lines = raw.split("\n").filter(Boolean);

  let meta = null;
  const messages = [];

  for (const line of lines) {
    let obj;
    try {
      obj = JSON.parse(line);
    } catch {
      continue;
    }

    if (obj.type === "session_meta") {
      meta = obj.payload || null;
      continue;
    }

    if (obj.type !== "event_msg") continue;
    const payload = obj.payload || {};
    const pt = payload.type;
    if (pt !== "user_message" && pt !== "agent_message") continue;

    const msg = payload.message || payload.text;
    if (typeof msg !== "string" || msg.trim().length === 0) continue;

    messages.push({
      role: pt === "user_message" ? "user" : "assistant",
      text: msg.trim(),
    });
  }

  return { meta, messages };
}

function countQuestions(s) {
  return extractQuestionSentences(s).length;
}

function looksPlanLike(s) {
  const t = s.trim();
  return (
    t.startsWith("Next ") ||
    t.startsWith("Next I") ||
    t.startsWith("I’m going to") ||
    t.startsWith("I'm going to") ||
    t.startsWith("I will ") ||
    t.startsWith("I’ll ") ||
    t.startsWith("Pulling up ") ||
    t.startsWith("Reading ") ||
    t.startsWith("Implementing ")
  );
}

function suggestTargetsForQuestion(q) {
  const s = q.toLowerCase();
  const targets = [];
  if (s.includes("test") || s.includes("lint") || s.includes("bb ") || s.includes("babashka")) {
    targets.push("AGENTS.md");
  }
  if (s.includes("spec") || s.includes("db sync") || s.includes("wrangler") || s.includes("cloudflare")) {
    targets.push("SPEC.md");
    targets.push("docs/agent-guide/db-sync/db-sync-guide.md");
  }
  if (s.includes("docs") || s.includes("agent-guide") || s.includes("where is") || s.includes("where's")) {
    targets.push("docs/agent-guide/");
  }
  if (targets.length === 0) targets.push("submodules/skills/ (new or improved skill)");
  return targets;
}

function main() {
  const args = parseArgs(process.argv.slice(2));
  if (args.help) {
    process.stdout.write(
      [
        "Usage:",
        "  ./audit-sessions.js --agent codex [--cwd /path/to/repo] [--limit 20] [--scan 400]",
        "",
        "Notes:",
        "  --scan controls how many of the most-recent session files (by mtime) to inspect for cwd matches.",
        "",
      ].join("\n"),
    );
    return;
  }

  if (args.agent !== "codex") {
    die(`Only --agent codex is supported right now (got: ${args.agent}).`);
  }

  const sessionsRoot = path.join(os.homedir(), ".codex", "sessions");
  if (!fs.existsSync(sessionsRoot)) die(`Codex sessions root not found: ${sessionsRoot}`);

  const all = listJsonlFilesRecursively(sessionsRoot)
    .map((p) => ({ path: p, mtimeMs: fs.statSync(p).mtimeMs }))
    .sort((a, b) => b.mtimeMs - a.mtimeMs);

  const candidates = all.slice(0, args.scan);
  const matching = [];

  for (const c of candidates) {
    let first;
    try {
      first = readFirstJsonLine(c.path);
    } catch {
      continue;
    }
    if (!first || first.type !== "session_meta") continue;
    const cwd = first.payload && first.payload.cwd;
    if (cwd === args.cwd) matching.push(c.path);
    if (matching.length >= args.limit) break;
  }

  if (matching.length === 0) {
    die(`No matching Codex sessions found for cwd=${args.cwd} (scanned ${candidates.length} recent session files).`);
  }

  const sessionReports = [];
  const questions = [];
  const userReframes = [];

  for (const p of matching) {
    const { meta, messages } = extractCodexTranscript(p);
    const agentText = messages.filter((m) => m.role === "assistant").map((m) => m.text).join("\n");
    const userText = messages.filter((m) => m.role === "user").map((m) => m.text).join("\n");

    const planLikeCount = messages
      .filter((m) => m.role === "assistant")
      .reduce((acc, m) => acc + (looksPlanLike(m.text) ? 1 : 0), 0);

    for (const m of messages) {
      if (m.role === "assistant") {
        for (const q of extractQuestionSentences(m.text)) {
          questions.push({ q, session: path.basename(p) });
        }
      } else {
        if (/\b(goal is|actually|no,|already)\b/i.test(m.text)) {
          userReframes.push({ s: m.text.slice(0, 220).replace(/\s+/g, " "), session: path.basename(p) });
        }
      }
    }

    sessionReports.push({
      path: p,
      when: meta && meta.timestamp ? String(meta.timestamp) : "(unknown)",
      messageCount: messages.length,
      userCount: messages.filter((m) => m.role === "user").length,
      agentCount: messages.filter((m) => m.role === "assistant").length,
      agentQuestionMarks: countQuestions(agentText),
      planLikeCount,
      userReframeCount: (userText.match(/\b(goal is|actually|no,|already)\b/gi) || []).length,
    });
  }

  // Cluster repeated questions by normalized form.
  const qCounts = new Map();
  const qExamples = new Map();
  for (const p of questions) {
    const key = normalizeQuestion(p.q);
    qCounts.set(key, (qCounts.get(key) || 0) + 1);
    if (!qExamples.has(key)) qExamples.set(key, p.q);
  }

  const topQuestions = Array.from(qCounts.entries())
    .sort((a, b) => b[1] - a[1])
    .slice(0, 10)
    .map(([k, n]) => ({
      n,
      norm: k,
      example: qExamples.get(k) || k,
      targets: suggestTargetsForQuestion(qExamples.get(k) || k),
    }));

  // Report
  process.stdout.write(`# Codex Session Audit (cwd = ${args.cwd})\n\n`);
  process.stdout.write(`Scanned: ${Math.min(args.scan, all.length)} recent session files\n\n`);
  process.stdout.write(`Included sessions (most recent first): ${matching.length}\n\n`);

  process.stdout.write(`## Session Summary\n\n`);
  process.stdout.write(`| When (session_meta) | Messages | User | Agent | Agent ? count | Plan-like msgs | User reframes |\n`);
  process.stdout.write(`|---|---:|---:|---:|---:|---:|---:|\n`);
  for (const r of sessionReports) {
    process.stdout.write(
      `| ${r.when} | ${r.messageCount} | ${r.userCount} | ${r.agentCount} | ${r.agentQuestionMarks} | ${r.planLikeCount} | ${r.userReframeCount} |\n`,
    );
  }

  process.stdout.write(`\n## Repeated Agent Questions (Likely Blind Spots)\n\n`);
  if (topQuestions.length === 0) {
    process.stdout.write(`No agent question patterns found (no assistant->user adjacency with '?').\n`);
  } else {
    for (const q of topQuestions) {
      process.stdout.write(`- (${q.n}x) ${q.example.replace(/\n/g, " ")}\n`);
      process.stdout.write(`  Suggested doc/skill targets: ${q.targets.join(", ")}\n`);
    }
  }

  process.stdout.write(`\n## User Reframes (Possible Symptom Of Misalignment)\n\n`);
  if (userReframes.length === 0) {
    process.stdout.write(`No obvious reframes detected (keywords: goal is / actually / no, / already).\n`);
  } else {
    // Show up to 10; these are for humans to inspect, not for clustering.
    for (const r of userReframes.slice(0, 10)) {
      process.stdout.write(`- ${r.s}\n`);
    }
  }

  process.stdout.write(`\n## Next Step Template (Turn Blind Spots Into Assets)\n\n`);
  process.stdout.write(`Pick one repeated pattern above, then:\n`);
  process.stdout.write(`- If it’s a repo-specific “where/how” question: patch the relevant doc (AGENTS.md / SPEC.md / docs/agent-guide)\n`);
  process.stdout.write(`- If it’s a reusable workflow: create or improve a skill under submodules/skills/\n`);
  process.stdout.write(`- Re-run this audit after the change and confirm the pattern disappears in future sessions\n`);
}

main();
