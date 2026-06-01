import pandas as pd

RS_PATH = 'data/route_stops.csv'
SCH_PATH = 'data/schedule.csv'

def norm(s):
    return str(s).strip().upper()

rs = pd.read_csv(RS_PATH, dtype=str).fillna("")
sch = pd.read_csv(SCH_PATH, dtype=str).fillna("")

rs['stop_code_norm'] = rs['stop_code'].astype(str).apply(lambda v: norm(v))
sch['stop_code_norm'] = sch['stop_code'].astype(str).apply(lambda v: norm(v))

# show any route_stops rows with empty stop_code
empty_rs = rs[rs['stop_code_norm']=='']
if not empty_rs.empty:
    print('\nRows in route_stops with empty stop_code:')
    print(empty_rs.to_string(index=False))

print(f"route_stops rows: {len(rs)}, unique stop_code: {rs['stop_code_norm'].nunique()}, empty codes: {(rs['stop_code_norm']=='').sum()}")
print(f"schedule rows: {len(sch)}, unique stop_code: {sch['stop_code_norm'].nunique()}, empty codes: {(sch['stop_code_norm']=='').sum()}\n")

print("Top stop_code counts in route_stops:")
print(rs['stop_code_norm'].value_counts().head(30).to_string())
print("\nTop stop_code counts in schedule:")
print(sch['stop_code_norm'].value_counts().head(30).to_string())

# merged by normalized stop_code (what produced the large join)
merged = pd.merge(rs, sch, left_on='stop_code_norm', right_on='stop_code_norm', how='inner', suffixes=('_rs','_sch'))
print(f"\nMerged on stop_code_norm rows: {len(merged)}")

# Find problematic stop_codes that create large cross-products
rs_counts = rs['stop_code_norm'].value_counts()
sch_counts = sch['stop_code_norm'].value_counts()
counts = pd.DataFrame({'rs': rs_counts, 'sch': sch_counts}).fillna(0).astype(int)
counts['product'] = counts['rs'] * counts['sch']
counts = counts.sort_values('product', ascending=False)

print("\nTop stop_codes by rs_count * sch_count (cross-product contribution):")
print(counts.head(30).to_string())

# route_id alignment diagnostics
if 'route_id' in rs.columns and 'route_id' in sch.columns:
    print('\nSchedule distinct (route_id, stop_code) pairs vs schedule rows:')
    pairs = sch.groupby(['route_id','stop_code_norm']).size()
    print(f"schedule rows: {len(sch)}, unique (route_id,stop_code): {len(pairs)}")
    dup_pairs = pairs[pairs>1]
    print(f"pairs with multiple entries (sample 20):\n{dup_pairs.head(20).to_string()}\n")

    # pairs present in schedule not in route_stops
    rs_pairs = set((str(r).strip(), str(s).strip()) for r,s in rs[['route_id','stop_code_norm']].itertuples(index=False,name=None))
    sch_pairs = set((str(r).strip(), str(s).strip()) for r,s in sch[['route_id','stop_code_norm']].itertuples(index=False,name=None))
    only_in_schedule = sorted(list({p for p in sch_pairs if p not in rs_pairs}) )
    only_in_route = sorted(list({p for p in rs_pairs if p not in sch_pairs}) )
    print(f"pairs only in schedule (sample 20): {only_in_schedule[:20]}")
    print(f"pairs only in route_stops (sample 20): {only_in_route[:20]}\n")

    print("Suggestion: merge on both route_id and stop_code (normalized) to avoid cartesian products from schedule trip rows.")
    print("If schedule contains multiple trips per day, deduplicate schedule to one row per (route_id,stop_code) before merging, or aggregate times as needed.")
else:
    print('\nroute_id not present in both tables; consider normalizing stop_code text (strip/upper) and deduplicating schedule by stop_code per route.\n')

print('Done.')
