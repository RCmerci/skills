---
description: Interactive merge conflict resolution assistant
arguments: Optional file path to focus on specific conflict
---

# Merge Conflict Resolution

Help the user resolve git merge conflicts interactively.

## Process

1. **Identify Conflicts**
   Run `git status` to find files with merge conflicts.

2. **For Each Conflicted File**
   - Read the file and find conflict markers (`<<<<<<<`, `=======`, `>>>>>>>`)
   - Understand both versions (current branch and incoming changes)
   - Explain what each version does

3. **Guide Resolution**
   - Ask the user which changes to keep
   - Suggest a merged solution when appropriate
   - Make the edits to resolve the conflict

4. **Complete Resolution**
   - Stage the resolved file: `git add <file>`
   - Check if more conflicts exist
   - When all resolved, suggest completing the merge/rebase

## Conflict Marker Format

```
<<<<<<< HEAD (or current branch name)
Your current changes
=======
Incoming changes being merged
>>>>>>> branch-name (or commit hash)
```

## Output Format

For each conflict:

### File: `path/to/file.js`

**Current branch changes:**
```javascript
// Show the code from current branch
```

**Incoming changes:**
```javascript
// Show the code being merged in
```

**Explanation:**
[Describe what each version does and why they conflict]

**Suggested Resolution:**
```javascript
// Show recommended merged code
```

**Options:**
1. Keep current branch version
2. Keep incoming version
3. Use suggested merge
4. Custom resolution (describe your preference)

## Arguments

If `$ARGUMENTS` contains a file path, focus on that file first.

## Guidelines

- Explain the context of each conflicted section
- Consider the semantic meaning, not just line differences
- Suggest keeping both changes when they're additive
- Warn about potential issues in the resolution
- Test suggestions for syntax errors
