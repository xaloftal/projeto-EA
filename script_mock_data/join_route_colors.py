# DO NOT RUN AGAIN
# just to document how we join the route colors to the stops data

import pandas as pd

stops = pd.read_csv('data/stops.csv', encoding='latin-1')
colors = pd.read_excel('data/routes_colors.csv')

# mapping of zone name and id

zone = {
    'ESP1': 1,
    'ESP2': 2,
    'GDM1': 3,
    'GDM2': 4,
    'MAI1': 5,
    'MAI2': 6,
    'MAI3': 7,
    'MAI4': 8,
    'MTS1': 9,
    'MTS2': 10,
    'PRD1': 11,
    'PRT1': 12,
    'PRT2': 13,
    'PRT3': 14,
    'PV_VC': 15,
    'STZ1': 16,
    'TRF1': 17,
    'VCD3': 18,
    'VCD8': 19,
    'VLG1': 20,
    'VLG2': 21,
    'VLG3': 22,
    'VNG1': 23,
    'VNG2': 24,
    'VNG3': 25,
    'VNG4': 26,
    'VNG5': 27,
}

# add zone id to stops, set null to 28
stops['zone_id'] = stops['zone_code'].map(zone).fillna(28).astype(int)
stops['zone_code'] = stops['zone_code'].fillna('OUT')

stops.to_csv('data/stops_revised.csv', index=False)


zones = stops[['zone_id', 'zone_code']].drop_duplicates()

df = pd.merge(zones, colors, on='zone_id', how='left')

df.to_csv('data/zones_1.csv', index=False)