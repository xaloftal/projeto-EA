"""
generate_users_csv.py
---------------------
Gera users.csv com 5000 utilizadores.

Campos:
    name          — nome aleatório
    email         — email único derivado do nome
    password_hash — hash BCrypt da password gerada aleatoriamente
    balance       — float aleatório entre 0 e 500

Uso:
    python generate_users_csv.py
    python generate_users_csv.py --output data/users.csv --count 5000

Dependências:
    pip install bcrypt
"""

import argparse
import csv
import random
import string
import unicodedata
from pathlib import Path

try:
    import bcrypt
except ImportError:
    print("❌ Instala bcrypt:  pip install bcrypt")
    import sys; sys.exit(1)

FIRST_NAMES = [
    "Ana","João","Maria","Pedro","Sofia","Miguel","Inês","Rui","Beatriz","Tiago",
    "Carolina","André","Mariana","Diogo","Francisca","Luís","Catarina","Bruno",
    "Marta","Ricardo","Sara","Nuno","Filipa","Gonçalo","Leonor","Vasco","Helena",
    "Rodrigo","Daniela","Afonso","Raquel","Hugo","Mónica","Sérgio","Patrícia",
    "Vítor","Joana","Alexandre","Cláudia","Paulo","Teresa","Marco","Susana",
    "Renato","Carla","Fernando","Vanessa","Jorge","Diana","Eduardo",
]

LAST_NAMES = [
    "Silva","Santos","Ferreira","Pereira","Oliveira","Costa","Rodrigues","Martins",
    "Jesus","Sousa","Fernandes","Gonçalves","Gomes","Lopes","Marques","Alves",
    "Almeida","Ribeiro","Pinto","Carvalho","Teixeira","Moreira","Correia","Mendes",
    "Nunes","Soares","Vieira","Barbosa","Rocha","Cunha","Pires","Macedo","Simões",
    "Mota","Figueiredo","Cardoso","Monteiro","Ramos","Freitas","Coelho","Cruz",
    "Azevedo","Melo","Campos","Abreu","Fonseca","Dias","Borges","Reis","Tavares",
]


def random_password(length: int = 12) -> str:
    chars = string.ascii_letters + string.digits + "!@#$%"
    return "".join(random.choices(chars, k=length))


def strip_accents(s: str) -> str:
    return "".join(c for c in unicodedata.normalize("NFD", s)
                   if unicodedata.category(c) != "Mn")


def generate_users(count: int) -> list[dict]:
    users = []
    seen_emails = set()

    print(f"A gerar {count} utilizadores (isto pode demorar alguns segundos devido ao BCrypt)...")

    while len(users) < count:
        first = random.choice(FIRST_NAMES)
        last  = random.choice(LAST_NAMES)

        base_email = f"{strip_accents(first).lower()}.{strip_accents(last).lower()}"
        email = f"{base_email}@catchit.pt"
        suffix = 1
        while email in seen_emails:
            email = f"{base_email}{suffix}@catchit.pt"
            suffix += 1
        seen_emails.add(email)

        plain  = random_password()
        hashed = bcrypt.hashpw(plain.encode(), bcrypt.gensalt()).decode()

        users.append({
            "name":          f"{first} {last}",
            "email":         email,
            "password_hash": hashed,
            "balance":       round(random.uniform(0, 500), 2),
        })

        if len(users) % 500 == 0:
            print(f"  {len(users)}/{count}...")

    return users


def main():
    parser = argparse.ArgumentParser(description="Gera users.csv com hashes BCrypt")
    parser.add_argument("--output", default="data/users.csv", help="Caminho do CSV de saída")
    parser.add_argument("--count",  type=int, default=5000,   help="Número de utilizadores")
    args = parser.parse_args()

    output_path = (Path(__file__).parent / args.output).resolve()
    output_path.parent.mkdir(parents=True, exist_ok=True)

    users = generate_users(args.count)

    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["name", "email", "password_hash", "balance"])
        writer.writeheader()
        writer.writerows(users)

    print(f"✅ CSV gerado: {output_path}  ({len(users)} utilizadores)")
    print(f"   Balance mín: {min(u['balance'] for u in users):.2f} €")
    print(f"   Balance máx: {max(u['balance'] for u in users):.2f} €")
    print("\nPrimeiros 3 utilizadores:")
    for u in users[:3]:
        print(f"  {u['name']:<25} {u['email']:<35} balance={u['balance']}")

if __name__ == "__main__":
    main()