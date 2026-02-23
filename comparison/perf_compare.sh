#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: comparison/perf_compare.sh [--runs N] [--output FILE] [--compare FILE]

Runs the Kotlin perf snapshot multiple times, summarizes medians across runs,
and optionally compares against a previously saved summary JSON.

Options:
  --runs N       Number of full perf runs to execute (default: 3)
  --output FILE  Where to write summary JSON (default: /tmp/qs_perf_<ts>.json)
  --compare FILE Compare current summary against a previous summary JSON
EOF
}

runs=3
output=""
compare=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --runs)
      if [[ $# -lt 2 ]]; then
        echo "Missing value for --runs" >&2
        exit 2
      fi
      runs="${2:-}"
      shift 2
      ;;
    --output)
      if [[ $# -lt 2 ]]; then
        echo "Missing value for --output" >&2
        exit 2
      fi
      output="${2:-}"
      shift 2
      ;;
    --compare)
      if [[ $# -lt 2 ]]; then
        echo "Missing value for --compare" >&2
        exit 2
      fi
      compare="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if ! [[ "$runs" =~ ^[0-9]+$ ]] || [[ "$runs" -le 0 ]]; then
  echo "--runs must be a positive integer" >&2
  exit 2
fi

if [[ -z "$output" ]]; then
  output="/tmp/qs_perf_$(date +%Y%m%d_%H%M%S).json"
fi

if [[ -n "$compare" && ! -f "$compare" ]]; then
  echo "--compare file does not exist: $compare" >&2
  exit 2
fi

tmpdir="$(mktemp -d)"
cleanup() { rm -rf "$tmpdir"; }
trap cleanup EXIT

raw_jsonl="$tmpdir/raw.jsonl"

for run in $(seq 1 "$runs"); do
  echo "Running perf snapshot ($run/$runs) ..."
  snapshot_file="$tmpdir/snapshot_$run.txt"
  ./gradlew -q :comparison:run --args perf >"$snapshot_file"
  python3 - "$run" "$snapshot_file" >>"$raw_jsonl" <<'PY'
import json
import re
import sys

run = int(sys.argv[1])
path = sys.argv[2]

enc_re = re.compile(
    r"^\s*depth=\s*(\d+):\s*([0-9.]+)\s*ms/op\s*\|\s*([0-9.]+|n/a)\s*MiB/op\s*\|\s*len=(\d+)\s*$"
)
dec_re = re.compile(
    r"^\s*count=\s*(\d+),\s*comma=(true|false)\s*,\s*utf8=(true|false)\s*,\s*len=\s*(\d+):\s*([0-9.]+)\s*ms/op\s*\|\s*([0-9.]+|n/a)\s*KiB/op\s*\|\s*keys=(\d+)\s*$"
)

with open(path, "r", encoding="utf-8") as f:
    for line in f:
        m = enc_re.match(line)
        if m:
            depth, ms, alloc, out_len = m.groups()
            rec = {
                "run": run,
                "kind": "encode",
                "case": {"depth": int(depth), "len": int(out_len)},
                "ms_per_op": float(ms),
                "alloc_unit": "MiB",
                "alloc_per_op": None if alloc == "n/a" else float(alloc),
            }
            print(json.dumps(rec))
            continue
        m = dec_re.match(line)
        if m:
            count, comma, utf8, value_len, ms, alloc, keys = m.groups()
            rec = {
                "run": run,
                "kind": "decode",
                "case": {
                    "count": int(count),
                    "comma": comma == "true",
                    "utf8": utf8 == "true",
                    "value_len": int(value_len),
                    "keys": int(keys),
                },
                "ms_per_op": float(ms),
                "alloc_unit": "KiB",
                "alloc_per_op": None if alloc == "n/a" else float(alloc),
            }
            print(json.dumps(rec))
            continue
        if "ms/op" in line:
            print(
                f"[perf_compare] warning: run={run} unmatched benchmark line: {line.rstrip()}",
                file=sys.stderr,
            )
PY
done

python3 - "$raw_jsonl" "$output" "$compare" <<'PY'
import json
import statistics
import sys
from collections import defaultdict

raw_path, out_path, compare_path = sys.argv[1], sys.argv[2], sys.argv[3]

records = []
with open(raw_path, "r", encoding="utf-8") as f:
    for line in f:
        line = line.strip()
        if line:
            records.append(json.loads(line))

groups = defaultdict(list)
for rec in records:
    key = (rec["kind"], json.dumps(rec["case"], sort_keys=True))
    groups[key].append(rec)

summary_cases = []
for (kind, case_json), items in sorted(groups.items()):
    ms_values = [x["ms_per_op"] for x in items]
    alloc_values = [x["alloc_per_op"] for x in items if x["alloc_per_op"] is not None]
    summary_cases.append(
        {
            "kind": kind,
            "case": json.loads(case_json),
            "runs": len(items),
            "ms_per_op_median": statistics.median(ms_values),
            "ms_per_op_values": ms_values,
            "alloc_unit": items[0]["alloc_unit"],
            "alloc_per_op_median": statistics.median(alloc_values) if alloc_values else None,
            "alloc_per_op_values": alloc_values,
        }
    )

summary = {
    "runs": len({r["run"] for r in records}),
    "cases": summary_cases,
}

with open(out_path, "w", encoding="utf-8") as f:
    json.dump(summary, f, indent=2, sort_keys=True)

print(f"\nSaved summary: {out_path}")
print("\nCurrent medians:")
for c in summary_cases:
    if c["kind"] == "encode":
        label = f"encode depth={c['case']['depth']},len={c['case']['len']}"
    else:
        label = (
            "decode "
            f"count={c['case']['count']},comma={str(c['case']['comma']).lower()},"
            f"utf8={str(c['case']['utf8']).lower()},len={c['case']['value_len']}"
        )
    alloc = "n/a" if c["alloc_per_op_median"] is None else f"{c['alloc_per_op_median']:.3f} {c['alloc_unit']}"
    print(f"  {label:55}  ms={c['ms_per_op_median']:.3f}  alloc={alloc}")

if compare_path:
    with open(compare_path, "r", encoding="utf-8") as f:
        base = json.load(f)
    base_map = {
        (c["kind"], json.dumps(c["case"], sort_keys=True)): c
        for c in base.get("cases", [])
    }
    print(f"\nDelta vs baseline: {compare_path}")
    for c in summary_cases:
        k = (c["kind"], json.dumps(c["case"], sort_keys=True))
        b = base_map.get(k)
        if not b:
            continue
        ms_delta = None
        if b["ms_per_op_median"] != 0:
            ms_delta = ((c["ms_per_op_median"] / b["ms_per_op_median"]) - 1.0) * 100.0
        alloc_delta = None
        if (
            b.get("alloc_per_op_median") is not None
            and c.get("alloc_per_op_median") is not None
            and b["alloc_per_op_median"] != 0
        ):
            alloc_delta = ((c["alloc_per_op_median"] / b["alloc_per_op_median"]) - 1.0) * 100.0

        if c["kind"] == "encode":
            label = f"encode depth={c['case']['depth']},len={c['case']['len']}"
        else:
            label = (
                "decode "
                f"count={c['case']['count']},comma={str(c['case']['comma']).lower()},"
                f"utf8={str(c['case']['utf8']).lower()},len={c['case']['value_len']}"
            )
        ms_txt = "n/a" if ms_delta is None else f"{ms_delta:+.2f}%"
        alloc_txt = "n/a" if alloc_delta is None else f"{alloc_delta:+.2f}%"
        print(f"  {label:55}  ms={ms_txt:>8}  alloc={alloc_txt:>8}")
PY
