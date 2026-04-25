import os
import pandas as pd

# Define paths
BUS_STOPS_FILE = '/home/gojalo/mei/pea/BusStopList.csv'
OUTPUT_FILE = '/home/gojalo/mei/pea/stops.csv'

# Load source stops
bus_stops_df = pd.read_csv(BUS_STOPS_FILE)
bus_stops_df = bus_stops_df.drop_duplicates(subset=['BUS_STOP']).reset_index(drop=True)

# Build stop export with sequential ids and nullable fields
stops_df = pd.DataFrame({
    'id': range(1, len(bus_stops_df) + 1),
    'name': bus_stops_df['BUS_STOP'],
    'stop_type': pd.NA,
    'location_id': pd.NA,
})

stops_df = stops_df[['id', 'name', 'stop_type', 'location_id']]
stops_df.to_csv(OUTPUT_FILE, index=False, na_rep='')

print(f"✅ Stops salvo em: {OUTPUT_FILE}")
print(f"📊 Total de paragens: {len(stops_df)}")
print("\nPrimeiras 20 paragens:")
print(stops_df.head(20))