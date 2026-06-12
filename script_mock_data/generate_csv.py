import csv
import os
import sys

def parse_gtfs_and_generate_csv(gtfs_dir, output_csv):
    # GTFS files needed
    routes_file = os.path.join(gtfs_dir, 'routes.txt')
    trips_file = os.path.join(gtfs_dir, 'trips.txt')
    stop_times_file = os.path.join(gtfs_dir, 'stop_times.txt')
    stops_file = os.path.join(gtfs_dir, 'stops.txt')

    for file in [routes_file, trips_file, stop_times_file, stops_file]:
        if not os.path.exists(file):
            print(f"Error: Could not find {file}")
            sys.exit(1)

    # 1. Parse routes
    routes = {}
    print("Parsing routes.txt...")
    with open(routes_file, 'r', encoding='utf-16') as f:
        reader = csv.DictReader(f, delimiter='\t')
        for row in reader:
            route_id = row['route_id']
            # Default to BUS if type not found
            route_type = row.get('route_type', '3')
            transport_type = 'BUS'
            if route_type == '0': transport_type = 'METRO'
            elif route_type == '1': transport_type = 'METRO'
            elif route_type == '2': transport_type = 'BUS'

            routes[route_id] = {
                'route_code': row.get('route_short_name', route_id),
                'transport_type': transport_type,
                'hex_color': '#' + row.get('route_color', '637939') if row.get('route_color') else '#637939'
            }

    # 2. Pick a representative trip for each route (e.g. longest trip by stop count)
    print("Parsing trips.txt and stop_times.txt to find representative trips...")
    
    def clean_time(time_str):
        if not time_str or not time_str.strip():
            return None
        if ':' not in time_str:
            try:
                fraction = float(time_str)
                total_seconds = int(round(fraction * 24 * 3600))
                hours = total_seconds // 3600
                minutes = (total_seconds % 3600) // 60
                seconds = total_seconds % 60
                return f"{hours:02d}:{minutes:02d}:{seconds:02d}"
            except ValueError:
                pass
        return time_str

    trip_to_route = {}
    trip_to_direction = {}
    with open(trips_file, 'r', encoding='utf-16') as f:
        reader = csv.DictReader(f, delimiter='\t')
        for row in reader:
            trip_to_route[row['trip_id']] = row['route_id']
            trip_to_direction[row['trip_id']] = row.get('direction_id', '0')

    trip_stop_counts = {}
    trip_stops = {} # trip_id -> list of dicts with sequence, stop_id, arrival_time, departure_time
    with open(stop_times_file, 'r', encoding='utf-16') as f:
        reader = csv.DictReader(f, delimiter='\t')
        for row in reader:
            trip_id = row['trip_id']
            if trip_id not in trip_stops:
                trip_stops[trip_id] = []
            trip_stops[trip_id].append({
                'sequence': int(row['stop_sequence']),
                'stop_id': row['stop_id'],
                'arrival_time': clean_time(row.get('arrival_time', '')),
                'departure_time': clean_time(row.get('departure_time', ''))
            })

    route_best_trip = {} # route_id -> trip_id
    route_max_stops = {} # route_id -> count
    
    for trip_id, stops_list in trip_stops.items():
        route_id = trip_to_route.get(trip_id)
        if not route_id: continue
        
        count = len(stops_list)
        if route_id not in route_max_stops or count > route_max_stops[route_id]:
            route_max_stops[route_id] = count
            route_best_trip[route_id] = trip_id

    # 3. Parse stops
    print("Parsing stops.txt...")
    stops = {}
    with open(stops_file, 'r', encoding='utf-16') as f:
        reader = csv.DictReader(f, delimiter='\t')
        for row in reader:
            stops[row['stop_id']] = {
                'stop_name': row.get('stop_name', ''),
                'latitude': row.get('stop_lat', ''),
                'longitude': row.get('stop_lon', ''),
                'zone_code': row.get('zone_id', 'PRT1'),
                'zone_id': '12' # Placeholder, logic might be needed to map zone_code to a numeric zone_id
            }

    # 4. Generate CSV
    print(f"Generating {output_csv}...")
    headers = [
        'route_code', 'stop_code', 'stop_name', 'latitude', 'longitude',
        'zone_code', 'sequence', 'transport_type', 'zone_id', 'hex_color'
    ]

    with open(output_csv, 'w', encoding='utf-8', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=headers)
        writer.writeheader()

        for route_id, trip_id in route_best_trip.items():
            route_info = routes.get(route_id)
            if not route_info: continue

            # Sort stops for this trip by sequence
            stops_in_trip = sorted(trip_stops[trip_id], key=lambda x: x['sequence'])
            
            for i, st in enumerate(stops_in_trip):
                stop_info = stops.get(st['stop_id'])
                if not stop_info: continue

                writer.writerow({
                    'route_code': route_info['route_code'],
                    'stop_code': st['stop_id'],
                    'stop_name': stop_info['stop_name'],
                    'latitude': stop_info['latitude'],
                    'longitude': stop_info['longitude'],
                    'zone_code': stop_info['zone_code'],
                    'sequence': i + 1, # Re-sequence from 1
                    'transport_type': route_info['transport_type'],
                    'zone_id': stop_info['zone_id'],
                    'hex_color': route_info['hex_color']
                })

    # 5. Generate schedule.csv
    schedule_csv = output_csv.replace('stops_routes_zones_gtfs.csv', 'schedule_gtfs.csv')
    if schedule_csv == output_csv:
        schedule_csv = output_csv + '_schedule.csv'

    print(f"Generating {schedule_csv}...")
    sched_headers = ['arrival_time', 'departure_time', 'stop_code', 'route_id', 'direction']
    with open(schedule_csv, 'w', encoding='utf-8', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=sched_headers)
        writer.writeheader()

        # Pre-compute valid stops for each route to avoid KeyErrors during seeding
        route_valid_stops = {}
        for r_id, b_trip_id in route_best_trip.items():
            route_valid_stops[r_id] = {st['stop_id'] for st in trip_stops[b_trip_id]}

        seen_schedules = set()

        for trip_id, stops_list in trip_stops.items():
            route_id = trip_to_route.get(trip_id)
            if not route_id: continue
            
            route_info = routes.get(route_id)
            if not route_info: continue
            
            best_trip_id = route_best_trip.get(route_id)
            if trip_to_direction.get(trip_id) != trip_to_direction.get(best_trip_id):
                continue
                
            valid_stops = route_valid_stops.get(route_id)
            if not valid_stops: continue

            direction = trip_to_direction.get(trip_id, '0')

            for st in sorted(stops_list, key=lambda x: x['sequence']):
                if st['stop_id'] in valid_stops:
                    if not st['arrival_time'] or not st['departure_time']:
                        continue
                    
                    sched_key = (route_info['route_code'], st['stop_id'], st['arrival_time'])
                    if sched_key in seen_schedules:
                        continue
                    seen_schedules.add(sched_key)

                    writer.writerow({
                        'arrival_time': st['arrival_time'],
                        'departure_time': st['departure_time'],
                        'stop_code': st['stop_id'],
                        'route_id': route_info['route_code'],
                        'direction': direction
                    })

    print("Done!")

if __name__ == '__main__':
    # Default directories, modify as needed
    gtfs_directory = './gtfs'
    output_filename = './stops_routes_zones_gtfs.csv'
    
    if len(sys.argv) > 1:
        gtfs_directory = sys.argv[1]
    if len(sys.argv) > 2:
        output_filename = sys.argv[2]
        
    if not os.path.exists(gtfs_directory):
        print(f"Error: GTFS directory '{gtfs_directory}' does not exist.")
        print("Usage: python generate_csv.py [path_to_gtfs_dir] [output_csv_path]")
        sys.exit(1)
        
    parse_gtfs_and_generate_csv(gtfs_directory, output_filename)
