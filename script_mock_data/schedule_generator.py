import pandas as pd
import os

# Define paths
ROUTES_DIR = './data/routes' 
STOPS_FILE = './data/stops.csv'
OUTPUT_FILE = './data/schedule.csv'

# Load stop mapping generated from BusStopList.csv
stops_df = pd.read_csv(STOPS_FILE)
stop_id_map = dict(zip(stops_df['name'], stops_df['id']))

print("📍 Bus stops carregados:", len(stop_id_map))

# List to store all schedules
all_schedules = []

# Process each route file
route_files = sorted([f for f in os.listdir(ROUTES_DIR) if f.endswith('.csv')])
print(f"📂 Encontradas {len(route_files)} rotas")

for route_file in route_files:
    route_path = os.path.join(ROUTES_DIR, route_file)
    route_id = route_file.replace('.csv', '')
    
    try:
        # Read route CSV
        route_df = pd.read_csv(route_path)
        
        # Process each stop in the route
        for idx, row in route_df.iterrows():
            stop_stn = row['Stop_stn']
            ride_time = row['Ride_time']
            sub = row['sub']
            
            # Find the FID that matches this stop_stn
            stop_id = None
            stop_id = stop_id_map.get(stop_stn)
            
            if stop_id is not None:
                # Parse the time
                try:
                    # ride_time is the arrival time (absolute time from start of day)
                    arrival_time = str(ride_time).strip()
                    
                    # Calculate departure_time (departure = arrival + some buffer, typically same as arrival)
                    # or you can add a small delay. Here, using arrival as departure
                    departure_time = arrival_time
                    
                    schedule_entry = {
                        'id': f"{route_id}_{stop_id}_{idx}",
                        'arrival_time': arrival_time,
                        'departure_time': departure_time,
                        'sequence': idx,
                        'stop_id': stop_id,
                        'route_id': route_id
                    }
                    all_schedules.append(schedule_entry)
                except Exception as e:
                    print(f"⚠️ Erro ao processar {route_id} - Stop {stop_stn}: {e}")
            else:
                print(f"⚠️ Stop {stop_stn} não encontrado no mapa de stops (rota: {route_id})")
    
    except Exception as e:
        print(f"❌ Erro ao ler rota {route_file}: {e}")

# Create DataFrame and save
if all_schedules:
    schedule_df = pd.DataFrame(all_schedules)
    schedule_df = schedule_df[['id', 'arrival_time', 'departure_time', 'sequence', 'stop_id', 'route_id']]
    
    schedule_df.to_csv(OUTPUT_FILE, index=False)
    print(f"\n✅ Schedule salvo em: {OUTPUT_FILE}")
    print(f"📊 Total de entradas: {len(schedule_df)}")
    print(f"\nPrimeiras 10 linhas:")
    print(schedule_df.head(10))
else:
    print("❌ Nenhum schedule foi gerado!")
