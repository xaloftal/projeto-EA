"""
generate_zones_csv.py
---------------------
Gera zones.csv com as coroas Andante inferidas a partir das coordenadas das stops.
 
Coroas (distância ao centro do Porto - Aliados):
    Z2:  0–3 km
    Z3:  3–6 km
    Z4:  6–10 km
    Z5: 10–17 km
 
Uso:
    python zones_generator.py
"""
 
import argparse
import csv
import math
from pathlib import Path
 
CENTER_LAT = 41.1496  # Aliados, Porto
CENTER_LON = -8.6109
 
ZONES = [
    {"name": "Z2", "min_km": 0,  "max_km": 3},
    {"name": "Z3", "min_km": 3,  "max_km": 6},
    {"name": "Z4", "min_km": 6,  "max_km": 10},
    {"name": "Z5", "min_km": 10, "max_km": 17},
]
 
def dist_km(lat: float, lon: float) -> float:
    R = 6371
    dlat = math.radians(lat - CENTER_LAT)
    dlon = math.radians(lon - CENTER_LON)
    a = (math.sin(dlat / 2) ** 2
         + math.cos(math.radians(CENTER_LAT)) * math.cos(math.radians(lat))
         * math.sin(dlon / 2) ** 2)
    return R * 2 * math.asin(math.sqrt(a))
 
def main():
    parser = argparse.ArgumentParser(description="Gera zones.csv com coroas Andante")
    parser.add_argument("--stops",  default="data/stops.csv",  help="CSV de stops de entrada")
    parser.add_argument("--output", default="data/zones.csv",  help="CSV de zonas de saída")
    args = parser.parse_args()
 
    base = Path(__file__).parent
    stops_path  = (base / args.stops).resolve()
    output_path = (base / args.output).resolve()
 
    # Contar stops por zona para feedback
    zone_counts = {z["name"]: 0 for z in ZONES}
    with open(stops_path, newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            d = dist_km(float(row["latitude"]), float(row["longitude"]))
            for z in ZONES:
                if z["min_km"] <= d < z["max_km"]:
                    zone_counts[z["name"]] += 1
                    break
 
    # Filtrar zonas que têm pelo menos uma stop
    active_zones = [z for z in ZONES if zone_counts[z["name"]] > 0]
 
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["name"])
        writer.writeheader()
        for z in active_zones:
            writer.writerow({"name": z["name"]})
 
    print(f"✅ CSV gerado: {output_path}  ({len(active_zones)} zonas)")
    for z in active_zones:
        print(f"  {z['name']}  ({z['min_km']}–{z['max_km']} km)  —  {zone_counts[z['name']]} stops")
 
if __name__ == "__main__":
    main()