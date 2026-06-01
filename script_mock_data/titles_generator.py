"""
titles_generator.py
-------------------
Gera titles.csv com 10000 titles (50% tickets, 50% cards).

Estados e datas consistentes:
    UNUSED    — valid_from no passado, valid_until no futuro (ainda não usado)
    ACTIVE    — valid_from no passado, valid_until no futuro (passes/cards ativos)
    USED      — valid_from e valid_until no passado
    EXPIRED   — valid_until no passado

Uso:
    python titles_generator.py
    python titles_generator.py --output data/titles.csv --count 10000
"""

import argparse
import csv
import random
from collections import Counter
from datetime import datetime, timedelta
from pathlib import Path

STATES = ["UNUSED", "ACTIVE", "USED", "EXPIRED"]


def random_dates(state: str):
    now = datetime.now()

    if state in ("UNUSED", "ACTIVE"):
        valid_from  = now - timedelta(days=random.randint(0, 60))
        valid_until = now + timedelta(days=random.randint(1, 365))
    elif state == "USED":
        valid_from  = now - timedelta(days=random.randint(30, 365))
        valid_until = now - timedelta(days=random.randint(1, 29))
    else:  # EXPIRED
        valid_from  = now - timedelta(days=random.randint(60, 730))
        valid_until = now - timedelta(days=random.randint(1, 59))

    created_at = valid_from - timedelta(days=random.randint(1, 10))
    return (
        created_at.strftime("%Y-%m-%d %H:%M:%S"),
        valid_from.strftime("%Y-%m-%d %H:%M:%S"),
        valid_until.strftime("%Y-%m-%d %H:%M:%S"),
    )


def generate_titles(count: int) -> list[dict]:
    titles = []
    half = count // 2

    for i in range(count):
        title_type = "ticket" if i < half else "card"
        state      = random.choice(STATES)
        created_at, valid_from, valid_until = random_dates(state)

        if title_type == "ticket":
            price = round(random.uniform(1.50, 30.00), 2)
        else:
            price = round(random.uniform(10.00, 40.00), 2)

        titles.append({
            "title_type":  title_type,
            "state_name":  state,
            "price":       price,
            "created_at":  created_at,
            "valid_from":  valid_from,
            "valid_until": valid_until,
        })

    return titles


def main():
    parser = argparse.ArgumentParser(description="Gera titles.csv")
    parser.add_argument("--output", default="data/titles.csv", help="Caminho do CSV de saída")
    parser.add_argument("--count",  type=int, default=10000,   help="Número de titles")
    args = parser.parse_args()

    output_path = (Path(__file__).parent / args.output).resolve()
    output_path.parent.mkdir(parents=True, exist_ok=True)

    titles = generate_titles(args.count)

    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["title_type", "state_name", "price", "created_at", "valid_from", "valid_until"])
        writer.writeheader()
        writer.writerows(titles)

    type_counts  = Counter(t["title_type"] for t in titles)
    state_counts = Counter(t["state_name"] for t in titles)

    print(f"✅ CSV gerado: {output_path}  ({len(titles)} titles)")
    print(f"\nPor tipo:")
    for k, v in type_counts.items():
        print(f"  {k:8s} — {v}")
    print(f"\nPor estado:")
    for k, v in state_counts.items():
        print(f"  {k:10s} — {v}")

if __name__ == "__main__":
    main()