import argparse
import csv
from pathlib import Path

VEHICLE_TYPES = [
    {"type": "BUS",   "capacity": 60,  "count": 30},
    {"type": "METRO", "capacity": 200, "count": 30},
    {"type": "TRAIN", "capacity": 350, "count": 30},
]

def main():
    parser = argparse.ArgumentParser(description="Gera vehicles.csv")
    parser.add_argument("--output", default="data/vehicles.csv", help="Caminho do CSV de saída")
    args = parser.parse_args()

    output_path = (Path(__file__).parent / args.output).resolve()
    output_path.parent.mkdir(parents=True, exist_ok=True)

    rows = []
    for spec in VEHICLE_TYPES:
        for _ in range(spec["count"]):
            rows.append({"capacity": spec["capacity"], "type": spec["type"]})

    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["capacity", "type"])
        writer.writeheader()
        writer.writerows(rows)

    print(f"✅ CSV gerado: {output_path}  ({len(rows)} veículos)")
    for spec in VEHICLE_TYPES:
        print(f"  {spec['type']:6s} — {spec['count']} veículos (capacidade {spec['capacity']})")

if __name__ == "__main__":
    main()