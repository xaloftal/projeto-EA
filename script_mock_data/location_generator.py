import csv
import sys
import os

def generate_location_csv(stops_csv_path: str, output_path: str = "locations.csv"):
    if not os.path.exists(stops_csv_path):
        print(f"Erro: ficheiro '{stops_csv_path}' não encontrado.")
        sys.exit(1)

    seen = set()
    rows = [["id", "latitude", "longitude"]]

    with open(stops_csv_path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            loc_id = row["location_id"].strip()
            lat    = row["latitude"].strip()
            lon    = row["longitude"].strip()
            if loc_id and loc_id not in seen:
                seen.add(loc_id)
                rows.append([loc_id, lat, lon])

    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerows(rows)

    print(f"location.csv gerado com {len(rows) - 1} entradas em '{output_path}'")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python gen_location_csv.py <stops.csv> [output_path]")
        print("Exemplo: python gen_location_csv.py stops.csv location.csv")
        sys.exit(1)

    stops_path = sys.argv[1]
    output     = sys.argv[2] if len(sys.argv) > 2 else "location.csv"
    generate_location_csv(stops_path, output)