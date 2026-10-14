-- Derived database .index/ekw.db (SQLite). Everything here can be rebuilt from Git + the analyzer,
-- except the support-check cache and the run ledger index, which only save money and time.

-- ---- snapshots -----------------------------------------------------------------------------------------------
CREATE TABLE snapshot (
  system       TEXT NOT NULL,
  commit_sha   TEXT NOT NULL,
  commit_time  TEXT NOT NULL,
  analyzed_at  TEXT NOT NULL,
  analyzer     TEXT NOT NULL,                       -- ekw-analyzer/<ver>+jdt-<ver>
  current      INTEGER NOT NULL DEFAULT 0,          -- 1 for the commit the bundle reflects
  PRIMARY KEY (system, commit_sha)
);

CREATE TABLE file (
  system TEXT NOT NULL, commit_sha TEXT NOT NULL, path TEXT NOT NULL,
  lines INTEGER NOT NULL, sha256 TEXT NOT NULL, triage TEXT NOT NULL,
  PRIMARY KEY (system, commit_sha, path),
  FOREIGN KEY (system, commit_sha) REFERENCES snapshot (system, commit_sha)
);

-- ---- code facts ----------------------------------------------------------------------------------------------
CREATE TABLE module (
  system TEXT NOT NULL, commit_sha TEXT NOT NULL, name TEXT NOT NULL,
  group_id TEXT, artifact_id TEXT, java_release TEXT, root TEXT NOT NULL,
  PRIMARY KEY (system, commit_sha, name)
);

CREATE TABLE symbol (
  system TEXT NOT NULL, commit_sha TEXT NOT NULL,
  id TEXT NOT NULL,                                 -- java:fqcn or java:fqcn#method(params)
  owner_id TEXT,                                    -- declaring type for members
  kind TEXT NOT NULL,                               -- CLASS INTERFACE ENUM RECORD ANNOTATION METHOD CONSTRUCTOR FIELD ...
  simple_name TEXT NOT NULL,
  module TEXT, signature TEXT, modifiers TEXT,
  path TEXT NOT NULL, start_line INTEGER NOT NULL, end_line INTEGER NOT NULL,
  body_hash TEXT, javadoc TEXT, is_test INTEGER NOT NULL DEFAULT 0,
  rank REAL,
  PRIMARY KEY (system, commit_sha, id)
);
CREATE INDEX symbol_name ON symbol (system, commit_sha, simple_name);
CREATE INDEX symbol_body ON symbol (system, commit_sha, body_hash);

CREATE TABLE supertype (
  system TEXT NOT NULL, commit_sha TEXT NOT NULL, type_id TEXT NOT NULL, super_id TEXT NOT NULL,
  relation TEXT NOT NULL,                           -- EXTENDS IMPLEMENTS
  PRIMARY KEY (system, commit_sha, type_id, super_id)
);

CREATE TABLE call_site (
  system TEXT NOT NULL, commit_sha TEXT NOT NULL,
  caller_id TEXT NOT NULL, callee_id TEXT,          -- callee NULL when unresolved
  path TEXT NOT NULL, line INTEGER NOT NULL, col INTEGER NOT NULL,
  flags TEXT NOT NULL DEFAULT '',                   -- comma list: IN_LAMBDA,CHAINED,...
  unresolved_reason TEXT,
  PRIMARY KEY (system, commit_sha, path, line, col)
);
CREATE INDEX call_callee ON call_site (system, commit_sha, callee_id);
CREATE INDEX call_caller ON call_site (system, commit_sha, caller_id);

CREATE TABLE type_ref (
  system TEXT NOT NULL, commit_sha TEXT NOT NULL, from_id TEXT NOT NULL, to_id TEXT NOT NULL,
  count INTEGER NOT NULL,
  PRIMARY KEY (system, commit_sha, from_id, to_id)
);

CREATE TABLE annotation (
  system TEXT NOT NULL, commit_sha TEXT NOT NULL, target_id TEXT NOT NULL, annotation_type TEXT NOT NULL,
  values_json TEXT NOT NULL DEFAULT '{}', path TEXT NOT NULL, line INTEGER NOT NULL
);
CREATE INDEX annotation_type_idx ON annotation (system, commit_sha, annotation_type);

CREATE TABLE config_key (
  system TEXT NOT NULL, commit_sha TEXT NOT NULL, key TEXT NOT NULL, default_value TEXT,
  how TEXT NOT NULL,                                -- getter, @Value, properties-file, constant
  path TEXT NOT NULL, line INTEGER NOT NULL
);
CREATE INDEX config_key_idx ON config_key (system, commit_sha, key);

CREATE TABLE error_code (
  system TEXT NOT NULL, commit_sha TEXT NOT NULL, code TEXT NOT NULL, constant_id TEXT,
  level TEXT, path TEXT NOT NULL, line INTEGER NOT NULL
);
CREATE INDEX error_code_idx ON error_code (system, commit_sha, code);

CREATE TABLE spi_registration (
  system TEXT NOT NULL, commit_sha TEXT NOT NULL, interface_id TEXT NOT NULL, name TEXT NOT NULL,
  impl_id TEXT NOT NULL, path TEXT NOT NULL, line INTEGER NOT NULL
);

CREATE TABLE dependency (
  system TEXT NOT NULL, commit_sha TEXT NOT NULL, module TEXT NOT NULL,
  group_id TEXT NOT NULL, artifact_id TEXT NOT NULL, version TEXT, scope TEXT, optional INTEGER NOT NULL,
  resolved INTEGER NOT NULL, path TEXT NOT NULL, line INTEGER NOT NULL
);

CREATE TABLE produced_artifact (                    -- feeds cross-system dependency edges and catalog cards
  system TEXT NOT NULL, commit_sha TEXT NOT NULL, group_id TEXT NOT NULL, artifact_id TEXT NOT NULL,
  module TEXT NOT NULL,
  PRIMARY KEY (system, commit_sha, group_id, artifact_id)
);

CREATE TABLE component (                            -- grouping proposed by the writer, validated by code
  system TEXT NOT NULL, slug TEXT NOT NULL, title TEXT NOT NULL, type_id TEXT NOT NULL,
  PRIMARY KEY (system, slug, type_id)
);

-- ---- documents and tickets -----------------------------------------------------------------------------------
CREATE TABLE doc_section (
  source TEXT NOT NULL, doc_id TEXT NOT NULL, anchor TEXT NOT NULL, heading TEXT NOT NULL,
  start_line INTEGER NOT NULL, end_line INTEGER NOT NULL, sha256 TEXT NOT NULL, kind TEXT NOT NULL,
  language TEXT,
  PRIMARY KEY (source, doc_id, anchor)
);

CREATE TABLE entity_link (                          -- doc text -> code entity, found by the deterministic linker
  source TEXT NOT NULL, doc_id TEXT NOT NULL, anchor TEXT NOT NULL, mention TEXT NOT NULL,
  system TEXT, target_id TEXT, target_kind TEXT NOT NULL,   -- symbol, config_key, error_code, artifact, module
  ambiguous INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE ticket (
  source TEXT NOT NULL, id TEXT NOT NULL, url TEXT NOT NULL, title TEXT NOT NULL, state TEXT,
  labels TEXT, language TEXT, updated_at TEXT, triage TEXT NOT NULL,
  PRIMARY KEY (source, id)
);

-- ---- pages, claims, checks ------------------------------------------------------------------------------------
CREATE TABLE page (
  path TEXT PRIMARY KEY, type TEXT NOT NULL, status TEXT NOT NULL, tier TEXT NOT NULL,
  system TEXT, commit_sha TEXT, input_hash TEXT, evidence_state TEXT, stale_after TEXT, rank REAL
);

CREATE TABLE citation (                             -- one row per sources[] entry of a page
  page_path TEXT NOT NULL REFERENCES page (path) ON DELETE CASCADE,
  source_id TEXT NOT NULL, kind TEXT NOT NULL,
  system TEXT, commit_sha TEXT, path TEXT, start_line INTEGER, end_line INTEGER,
  symbol TEXT, sha256 TEXT, resource TEXT NOT NULL,
  PRIMARY KEY (page_path, source_id)
);

CREATE TABLE concept_mention (                      -- x-ekw.concepts of source-like pages; drives the >=2 rule
  slug TEXT NOT NULL, page_path TEXT NOT NULL REFERENCES page (path) ON DELETE CASCADE,
  PRIMARY KEY (slug, page_path)
);

CREATE TABLE support_check (                        -- cache: never pay twice for the same claim and evidence
  key TEXT PRIMARY KEY,                             -- sha256(sentence | sorted fingerprints | prompt id@version)
  verdict TEXT NOT NULL, reason TEXT, missing TEXT, model TEXT NOT NULL, checked_at TEXT NOT NULL
);

CREATE TABLE lint_finding (
  rule_id TEXT NOT NULL, severity TEXT NOT NULL, page_path TEXT, message TEXT NOT NULL,
  first_seen TEXT NOT NULL, last_seen TEXT NOT NULL, needs_person INTEGER NOT NULL, resolved INTEGER NOT NULL DEFAULT 0
);

-- ---- runs and money -------------------------------------------------------------------------------------------
CREATE TABLE run (
  id TEXT PRIMARY KEY, kind TEXT NOT NULL,          -- ingest, lint, query, eval
  source_id TEXT, started_at TEXT NOT NULL, finished_at TEXT, started_by TEXT NOT NULL,
  limit_usd REAL NOT NULL, estimate_usd REAL, spent_usd REAL NOT NULL DEFAULT 0,
  status TEXT NOT NULL, partial INTEGER NOT NULL DEFAULT 0, branch TEXT,
  trigger TEXT NOT NULL DEFAULT 'MANUAL',           -- MANUAL, ASSISTANT, SCHEDULE, WEBHOOK
  job_id TEXT,                                      -- groups runs of one scheduled job, for the nightly total
  first_build INTEGER NOT NULL DEFAULT 0, auto_merged INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX run_job ON run (job_id);

CREATE TABLE model_call (
  run_id TEXT NOT NULL REFERENCES run (id), seq INTEGER NOT NULL,
  role TEXT NOT NULL, prompt TEXT NOT NULL, model TEXT NOT NULL, served_by TEXT NOT NULL,
  input_tokens INTEGER NOT NULL, output_tokens INTEGER NOT NULL,
  cache_read_tokens INTEGER NOT NULL, cache_write_tokens INTEGER NOT NULL,
  reserved_usd REAL NOT NULL, cost_usd REAL NOT NULL, latency_ms INTEGER NOT NULL, stop TEXT NOT NULL,
  work_item TEXT, batch INTEGER NOT NULL DEFAULT 0, policy TEXT NOT NULL,
  PRIMARY KEY (run_id, seq)
);
