import pandas as pd

# Merge schedule with route metadata safely by joining on both route_id and normalized stop_code.

def norm_code(s):
	return str(s).strip().upper()

rs = pd.read_csv('data/route_stops.csv', dtype=str).fillna('')
sch = pd.read_csv('data/schedule.csv', dtype=str).fillna('')

rs['stop_code_norm'] = rs['stop_code'].apply(norm_code)
sch['stop_code_norm'] = sch['stop_code'].apply(norm_code)

rs['route_id'] = rs['route_id'].astype(str)
sch['route_id'] = sch['route_id'].astype(str)

print(f"route_stops: {len(rs)} rows, unique route+stop pairs: {rs.groupby(['route_id','stop_code_norm']).ngroups}")
print(f"schedule: {len(sch)} rows, unique route+stop pairs: {sch.groupby(['route_id','stop_code_norm']).ngroups}")

# detect duplicates in route_stops (same route_id+stop_code repeated)
dup_rs = rs.groupby(['route_id','stop_code_norm']).size().reset_index(name='count').query('count>1')
if not dup_rs.empty:
	print('\nroute_stops has duplicated (route_id,stop_code) entries (sample 20):')
	print(dup_rs.head(20).to_string(index=False))

# keep every schedule line, and collapse duplicate route_stops rows only for metadata enrichment
rs_meta = rs.drop_duplicates(subset=['route_id', 'stop_code_norm'], keep='first')
merged = pd.merge(sch, rs_meta, on=['route_id','stop_code_norm'], how='left', suffixes=('_sch','_rs'))
print(f"\nMerged schedule -> route_stops on (route_id, stop_code_norm): {len(merged)} rows")
print(f"Schedule rows preserved: {len(merged) == len(sch)}")

# report top contributors to merged size
top_keys = merged.groupby('stop_code_norm').size().sort_values(ascending=False).head(20)
print('\nTop stop_code_norm contributors to merged rows:')
print(top_keys.to_string())

# show any route_stops rows that still don't match schedule by (route_id,stop_code_norm)
rs_pairs = set((r,s) for r,s in rs_meta[['route_id','stop_code_norm']].itertuples(index=False,name=None))
sch_pairs = set((r,s) for r,s in sch[['route_id','stop_code_norm']].itertuples(index=False,name=None))
only_in_rs = sorted([p for p in rs_pairs if p not in sch_pairs])
only_in_sch = sorted([p for p in sch_pairs if p not in rs_pairs])
print(f"\nPairs only in route_stops (sample 20): {only_in_rs[:20]}")
print(f"Pairs only in schedule (sample 20): {only_in_sch[:20]}")

print('\nIf you want to attach trip times as metadata (one row per route-stop), deduplicate schedule first.')



# check null
null_rows = merged[merged.isnull().any(axis=1)]
print("\nMerged data with nulls: count=", len(null_rows))
if not null_rows.empty:
	print(null_rows.head().to_string())
 
 # check the number of lines in merged vs schedule
print(f"\nMerged rows: {len(merged)}, Schedule rows: {len(sch)}")

# save corrected versions
print("\n--- Saving corrected files ---")

# remove the temporary normalization columns before saving
rs_meta_save = rs_meta.drop(columns=['stop_code_norm'])
merged_save = merged.drop(columns=['stop_code_norm'])

rs_meta_save.to_csv('data/route_stops_dedup.csv', index=False)
print(f"✓ Saved deduplicated route_stops: data/route_stops_dedup.csv ({len(rs_meta_save)} rows)")

merged_save.to_csv('data/schedule_enriched.csv', index=False)
print(f"✓ Saved enriched schedule: data/schedule_enriched.csv ({len(merged_save)} rows)")