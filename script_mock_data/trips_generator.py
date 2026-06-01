"""
trips_generator.py
------------------
Gera trips.csv com 5000 trips ativos (end_time vazio, start_time nas últimas 4 horas).

Uso:
    python trips_generator.py
    python trips_generator.py --output data/trips.csv --count 5000
"""

import argparse
import csv
import random
from datetime import datetime, timedelta
from pathlib import Path


def generate_trips(count: int) -> list[dict]:
    now = datetime.now()
    trips = []
    for _ in range(count):
        minutes_ago = random.randint(30, 240)
        start_time = now - timedelta(minutes=minutes_ago)
        trips.append({
            "start_time": start_time.strftime("%Y-%m-%d %H:%M:%S"),
            "end_time":   "",
        })
    return trips


def main():
    parser = argparse.ArgumentParser(description="Gera trips.csv com trips ativos")
    parser.add_argument("--output", default="data/trips.csv", help="Caminho do CSV de saída")
    parser.add_argument("--count",  type=int, default=5000,   help="Número de trips")
    args = parser.parse_args()

    output_path = (Path(__file__).parent / args.output).resolve()
    output_path.parent.mkdir(parents=True, exist_ok=True)

    trips = generate_trips(args.count)

    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["start_time", "end_time"])
        writer.writeheader()
        writer.writerows(trips)

    print(f"✅ CSV gerado: {output_path}  ({len(trips)} trips)")
    print(f"   start_time nas últimas 4 horas  |  end_time vazio = trip ativo")

if __name__ == "__main__":
    main()