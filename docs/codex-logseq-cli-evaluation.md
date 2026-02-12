# Codex CLI 使用 Logseq CLI 的场景化评测计划

Goal: 在固定图 `clojure-docs-20260210-v1` 上通过 3 个贴近真实用户工作流的复杂场景，系统找出 `logseq-cli` 命令设计中不合理之处，并产出可执行的 `logseq-cli` skill 改进建议。

Architecture: 评测采用“任务回放 + 命令轨迹 + 结果打分”的方式，要求每个场景都包含多步检索、证据聚合、答案生成与结果核验。
Architecture: 每个场景定义输入上下文、Codex 应执行的 Logseq 操作、输出格式要求、失败模式与量化评分，避免只测单条 query 的玩具任务。
Architecture: 评测输出必须包含两类产物，即 `logseq-cli` 命令不合理点清单，以及对应的 `logseq-cli` skill 改进方案。
Architecture: 所有评测均绑定同一图 `clojure-docs-20260210-v1`，确保跨场景结果可比较且可复现。

Tech Stack: codex-cli, logseq-cli, Datascript query, shell transcript capture.

Related: Relates to `docs/agent-guide/001-clojuredocs-logseq-import.md`.

## Problem statement

本计划的目的不是泛化评估“是否可用”，而是定位 `logseq-cli` 命令层面的不合理设计，并据此反推 `logseq-cli` skill 的改进项。

如果只用“查一个页面”或“跑一个 count query”做测试，无法暴露检索召回不足、证据链断裂、查询属性误用、答案幻觉、以及多轮会话状态漂移等真实风险。

因此本计划定义 3 个高复杂度、用户真实会执行的场景，每个场景都包含业务目标、决策压力与可量化验收标准，并且强制产出“命令问题 -> skill 改进”的闭环结论。

## 评测总规则

- 固定 graph: `clojure-docs-20260210-v1`.
- 记录完整 transcript: 用户提示词、Codex 中间推理摘要、实际 logseq-cli 命令、命令输出摘要、最终回答。
- 每个场景至少执行 2 轮对话，以验证 Codex 在上下文延续下的稳定性。
- 每个场景打分满分 100，低于 80 视为不通过。

## 场景 1: API 选型与证据链回答

### 用户真实目标

用户在写 Clojure 数据处理代码时，需要在多个候选函数之间做选型，并要求“给出可追溯证据，不要只给结论”。

典型请求示例: “我要处理一个可能很大的嵌套集合，要求尽量保持惰性并控制内存。请在 `map`、`mapcat`、`flatten`、`sequence`、`into` 之间给建议，并说明 trade-off。”

### Codex 需要完成的 Logseq 操作

- 查询候选 var 页面的文档块、arglists、examples、notes。
- 沿 `see-also` 关系扩展至少一层，补全相邻 API 候选。
- 拉取并汇总每个候选 API 的“用途、限制、典型写法”证据。
- 生成带引用的对比结论。

### 建议核验命令样式

```bash
logseq query --repo clojure-docs-20260210-v1 --output edn \
  --query '[:find (pull ?p [:db/id :block/title :block/uuid]) ...
           :where
           [?p :block/tags :user.class/ClojureDocsVar]]'
```

```bash
logseq show --repo clojure-docs-20260210-v1 --page "clojure.core/mapcat" --level 3
```

### 评分细则

| 维度 | 权重 | 通过标准 |
| --- | --- | --- |
| 证据覆盖 | 30 | 最终结论引用至少 4 个 API，且每个 API 至少关联 1 条示例或注释证据。 |
| 选型正确性 | 30 | 对惰性、内存、可读性 trade-off 的描述与图中材料一致，无明显冲突。 |
| 可追溯性 | 20 | 每条关键结论可回溯到页面或 block 证据。 |
| 交互质量 | 20 | 回答结构清晰，能针对用户约束给出可执行建议。 |

### 常见失败模式

- 只比较 API 名称，不给图内证据。
- 忽略 `see-also` 导致候选遗漏。
- 把示例代码语义解释反了，出现“看似合理但不成立”的结论。

## 场景 2: 生产问题排查与修复建议

### 用户真实目标

用户带着线上问题来问诊，希望 Codex 从文档图中找出可能根因并给最小修复方案，而不是泛泛而谈。

典型请求示例: “我们在处理 map 合并时出现字段丢失，怀疑和 `merge`、`merge-with`、`update` 的使用有关。请结合 ClojureDocs 证据给出排查路径和修复建议。”

### Codex 需要完成的 Logseq 操作

- 先定位核心函数页面，再扩展到相关函数与反例。
- 聚合 notes 中的坑点、边界条件与版本语义差异。
- 输出“排查顺序 + 最小修复 + 回归测试建议”。
- 在第二轮追问中，基于上一轮证据继续收敛，不丢失上下文。

### 建议核验命令样式

```bash
logseq show --repo clojure-docs-20260210-v1 --page "clojure.core/merge-with" --level 4
```

```bash
logseq query --repo clojure-docs-20260210-v1 --output edn \
  --query '[:find ?title ?u
           :where
           [?b :block/title ?title]
           [?b :block/uuid ?u]
           [(clojure.string/includes? ?title "merge")]]'
```

### 评分细则

| 维度 | 权重 | 通过标准 |
| --- | --- | --- |
| 根因假设质量 | 35 | 提出至少 2 个可验证根因，并与图中函数语义关联。 |
| 修复可执行性 | 30 | 给出明确改法和最小回归测试点，不是抽象建议。 |
| 多轮稳定性 | 20 | 第二轮追问能延续第一轮证据，不自相矛盾。 |
| 误报控制 | 15 | 不把无关函数当主因，不过度外推。 |

### 常见失败模式

- 给出教科书式建议但不贴合用户故障。
- 第二轮答复遗忘第一轮已确认前提。
- 把“可能相关”直接说成“确定根因”。

## 场景 3: 专题知识页自动构建与回写

### 用户真实目标

用户希望把一次技术研究结果沉淀为可复用页面，要求 Codex 不仅会查，还要会将结果结构化写回图中。

典型请求示例: “帮我做一页 `Clojure Lazy Sequence Pitfalls`，包含 6 个高频坑、对应函数、示例和规避建议，并打标签方便后续检索。”

### Codex 需要完成的 Logseq 操作

- 查询并筛选候选函数与证据块。
- 生成专题页面结构草案。
- 通过 `logseq add page` 与 `logseq add block` 写入页面和分节内容。
- 必要时用 `logseq update` 调整 block 层级与标签。
- 完成后再次 `show` 核验最终树结构与标签完整性。

### 建议核验命令样式

```bash
logseq add page --repo clojure-docs-20260210-v1 --page "Clojure Lazy Sequence Pitfalls"
```

```bash
logseq add block --repo clojure-docs-20260210-v1 \
  --target-page-name "Clojure Lazy Sequence Pitfalls" \
  --content "Pitfall 1: ..." --tags '["ClojureDocs" "LazySeq"]'
```

```bash
logseq show --repo clojure-docs-20260210-v1 --page "Clojure Lazy Sequence Pitfalls" --level 4
```

### 评分细则

| 维度 | 权重 | 通过标准 |
| --- | --- | --- |
| 写入正确性 | 35 | 页面创建成功，至少 6 个坑点 block 落地，结构层级符合请求。 |
| 内容证据性 | 30 | 每个坑点都能关联到图内函数页面或示例。 |
| 可检索性 | 20 | 标签和标题设计支持后续 query 命中。 |
| 幂等与安全 | 15 | 重复执行时不产生灾难性重复或结构破坏。 |

### 常见失败模式

- 能回答但不会回写，停在“建议文本”。
- 回写成功但没有结构化分节，后续不可维护。
- 重跑后重复插入大量同名 block。

## 统一验收与输出模板

每次场景执行后，统一输出以下评测记录。

| 字段 | 内容 |
| --- | --- |
| graph | `clojure-docs-20260210-v1` |
| scenario | `scenario-1` / `scenario-2` / `scenario-3` |
| run-id | 时间戳 + 执行人 |
| transcript | 用户请求与 Codex 最终回复 |
| logseq-commands | 实际执行命令列表 |
| command-issues | `logseq-cli` 命令不合理点清单（触发条件、错误表现、影响范围、复现命令）。 |
| score | 四个维度分项 + 总分 |
| verdict | pass / fail |
| issues | 失败原因与改进建议 |
| skill-improvement-plan | 针对 `logseq-cli` skill 的改进建议清单（见文末“最终总结要求”）。 |

## 执行顺序建议

1. 先跑场景 1，验证检索与证据链能力。
2. 再跑场景 2，验证问题定位与多轮稳定性。
3. 最后跑场景 3，验证写入与知识沉淀闭环。

## 结论标准

- 3 个场景总平均分大于等于 85。
- 任一场景不得低于 80。
- 场景 3 的写入正确性不得低于 30/35。
- 必须形成一份去重后的 `logseq-cli` 命令不合理点清单，并按严重级别排序（`critical` / `major` / `minor`）。
- 每个命令不合理点都必须至少映射 1 条 `logseq-cli` skill 改进建议，且可在 `skills/logseq-cli/SKILL.md` 中落地。

满足以上标准后，可认为本轮评测达成目标，即完成“发现命令问题”与“提出 skill 改进建议”两项核心产出。

## 最终总结要求（必须包含 logseq-cli skill 改进建议）

每次完成 3 个场景后，最终总结必须包含一个 `Improve logseq-cli skill` 小节。

该小节必须是“可执行改进方案”，不能只写泛化结论。

每条建议都必须包含以下字段。

| 字段 | 要求 |
| --- | --- |
| issue | 明确问题现象，引用具体场景与失败样例。 |
| root-cause | 推测根因，指向 skill 内容缺口（命令示例、查询规范、错误处理、输出约定等）。 |
| change | 要修改的 skill 内容与位置，例如 `skills/logseq-cli/SKILL.md` 的具体章节。 |
| patch-outline | 计划新增或改写的要点，至少 3 条，要求可直接落文档。 |
| expected-impact | 预期提升指标，例如成功率、平均命令数、重试次数、错误率。 |
| validation | 如何验证改进有效，需绑定到 3 个场景中的至少 1 个回归用例。 |
| priority | `P0` / `P1` / `P2`。 |

建议数量要求如下。

- 至少 5 条改进建议。
- 其中至少 2 条为 `P0`。
- 至少 1 条专门针对 Datascript 查询易错点。
- 至少 1 条专门针对写入操作（`add`/`update`）的幂等与安全。

最终总结建议使用如下模板。

```markdown
## Improve logseq-cli skill

### Suggestion 1 (P0)
- issue:
- root-cause:
- change:
- patch-outline:
  - ...
  - ...
  - ...
- expected-impact:
- validation:

### Suggestion 2 (P0)
...
```

若最终总结缺少该小节，或建议不满足以上结构与数量要求，则本轮评测报告视为不完整。
