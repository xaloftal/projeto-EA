import os
import pandas as pd

# Define paths
ROUTES_DIR = './data/routes/'
OUTPUT_FILE = './data/routes.csv'

# Get all route files
route_files = sorted([f for f in os.listdir(ROUTES_DIR) if f.endswith('.csv')])

# Create routes data
routes_data = []
for route_index, route_file in enumerate(route_files, start=1):
    route_id = route_file.replace('.csv', '')
    route_name = route_id
    
    routes_data.append({
        'id': route_index,
        'name': route_name
    })

# Create DataFrame and save
routes_df = pd.DataFrame(routes_data)
routes_df = routes_df[['id', 'name']]

routes_df.to_csv(OUTPUT_FILE, index=False)

print(f"✅ Routes salvo em: {OUTPUT_FILE}")
print(f"📊 Total de rotas: {len(routes_df)}")
print(f"\nPrimeiras 20 rotas:")
print(routes_df.head(20))
print(f"\n... (mostrando primeiras 20 de {len(routes_df)} rotas)")
