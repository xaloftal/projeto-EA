#!/usr/bin/env python3
import pandas as pd
import matplotlib.pyplot as plt
import glob
import os
from datetime import datetime

plt.style.use('default')
plt.rcParams['font.size'] = 10
plt.rcParams['figure.figsize'] = (14, 8)

def load_csv_data(filename):
    try:
        df = pd.read_csv(filename, encoding='utf-8')
        df['Throughput(ops/s)'] = df['Throughput(ops/s)'].apply(lambda x: max(0, x) if x > 0 else 0)
        df['Latência_Média(ms)'] = df['Latência_Média(ms)'].apply(lambda x: max(0, x))
        return df
    except Exception as e:
        print(f"Erro: {e}")
        return None

def grafico_throughput(df, output_dir):
    fig, ax = plt.subplots(figsize=(14, 8))
    df_plot = df[df['Throughput(ops/s)'] > 0].sort_values('Throughput(ops/s)', ascending=True)
    if df_plot.empty:
        print("  ⚠️ Sem dados de throughput")
        return
    cores = ['#2ecc71' if x > 500 else '#f39c12' if x > 200 else '#e74c3c' for x in df_plot['Throughput(ops/s)']]
    bars = ax.barh(df_plot['Teste'], df_plot['Throughput(ops/s)'], color=cores)
    ax.set_xlabel('Throughput (ops/s)', fontsize=12, fontweight='bold')
    ax.set_title('Throughput por Operação', fontsize=16, fontweight='bold')
    ax.axvline(x=200, color='orange', linestyle='--', alpha=0.7, label='Meta (200 ops/s)')
    ax.axvline(x=500, color='green', linestyle='--', alpha=0.7, label='Ideal (500 ops/s)')
    ax.legend(loc='lower right')
    for bar, val in zip(bars, df_plot['Throughput(ops/s)']):
        ax.text(bar.get_width() + 5, bar.get_y() + bar.get_height()/2, f'{val:.0f}', va='center', fontsize=9)
    plt.tight_layout()
    plt.savefig(f'{output_dir}/throughput.png', dpi=150, bbox_inches='tight')
    plt.close()
    print("  ✅ throughput.png")

def grafico_latencia(df, output_dir):
    fig, ax = plt.subplots(figsize=(14, 8))
    df_plot = df[df['Latência_Média(ms)'] > 0].sort_values('Latência_Média(ms)', ascending=False)
    if df_plot.empty:
        print("  ⚠️ Sem dados de latência")
        return
    cores = ['#2ecc71' if x < 10 else '#f39c12' if x < 30 else '#e74c3c' for x in df_plot['Latência_Média(ms)']]
    bars = ax.barh(df_plot['Teste'], df_plot['Latência_Média(ms)'], color=cores)
    ax.set_xlabel('Latência Média (ms)', fontsize=12, fontweight='bold')
    ax.set_title('Latência Média por Operação', fontsize=16, fontweight='bold')
    ax.axvline(x=10, color='green', linestyle='--', alpha=0.7, label='Excelente (<10ms)')
    ax.axvline(x=30, color='orange', linestyle='--', alpha=0.7, label='Aceitável (<30ms)')
    ax.legend(loc='lower right')
    for bar, val in zip(bars, df_plot['Latência_Média(ms)']):
        ax.text(bar.get_width() + 0.5, bar.get_y() + bar.get_height()/2, f'{val:.1f}ms', va='center', fontsize=9)
    plt.tight_layout()
    plt.savefig(f'{output_dir}/latencia.png', dpi=150, bbox_inches='tight')
    plt.close()
    print("  ✅ latencia.png")

def grafico_p95(df, output_dir):
    fig, ax = plt.subplots(figsize=(14, 8))
    df_plot = df[df['P95(ms)'] > 0].sort_values('P95(ms)', ascending=False)
    if df_plot.empty:
        print("  ⚠️ Sem dados de P95")
        return
    cores = ['#2ecc71' if x < 15 else '#f39c12' if x < 40 else '#e74c3c' for x in df_plot['P95(ms)']]
    bars = ax.barh(df_plot['Teste'], df_plot['P95(ms)'], color=cores)
    ax.set_xlabel('Latência P95 (ms)', fontsize=12, fontweight='bold')
    ax.set_title('Latência P95 (95% das operações)', fontsize=14, fontweight='bold')
    ax.axvline(x=15, color='green', linestyle='--', alpha=0.7, label='Excelente (<15ms)')
    ax.axvline(x=40, color='orange', linestyle='--', alpha=0.7, label='Aceitável (<40ms)')
    ax.legend(loc='lower right')
    for bar, val in zip(bars, df_plot['P95(ms)']):
        ax.text(bar.get_width() + 0.5, bar.get_y() + bar.get_height()/2, f'{val:.0f}ms', va='center', fontsize=9)
    plt.tight_layout()
    plt.savefig(f'{output_dir}/p95.png', dpi=150, bbox_inches='tight')
    plt.close()
    print("  ✅ p95.png")

def grafico_sucesso(df, output_dir):
    fig, ax = plt.subplots(figsize=(12, 6))
    df['Taxa_Sucesso'] = df['Sucesso'] / (df['Sucesso'] + df['Falhas']) * 100
    df_plot = df.sort_values('Taxa_Sucesso', ascending=True)
    cores = ['#2ecc71' if x == 100 else '#e74c3c' for x in df_plot['Taxa_Sucesso']]
    bars = ax.barh(df_plot['Teste'], df_plot['Taxa_Sucesso'], color=cores)
    ax.set_xlabel('Taxa de Sucesso (%)', fontsize=12, fontweight='bold')
    ax.set_title('Taxa de Sucesso por Operação', fontsize=16, fontweight='bold')
    ax.set_xlim(0, 105)
    ax.axvline(x=99, color='green', linestyle='--', alpha=0.7, label='Meta (99%)')
    ax.legend(loc='lower right')
    for bar, val in zip(bars, df_plot['Taxa_Sucesso']):
        ax.text(bar.get_width() + 0.5, bar.get_y() + bar.get_height()/2, f'{val:.0f}%', va='center', fontsize=9)
    plt.tight_layout()
    plt.savefig(f'{output_dir}/sucesso.png', dpi=150, bbox_inches='tight')
    plt.close()
    print("  ✅ sucesso.png")

def main():
    # Procurar CSV em várias localizações
    csv_files = []
    
    # Procurar na diretoria atual
    csv_files.extend(glob.glob("benchmark_ticketing_*.csv"))
    
    # Procurar na diretoria pai (raiz do projeto)
    csv_files.extend(glob.glob("../benchmark_ticketing_*.csv"))
    
    # Procurar em todo o projeto
    for root, dirs, files in os.walk(".."):
        for file in files:
            if file.startswith("benchmark_ticketing_") and file.endswith(".csv"):
                csv_files.append(os.path.join(root, file))
    
    if not csv_files:
        print("❌ Nenhum ficheiro benchmark_ticketing_*.csv encontrado!")
        print("   Diretorias procuradas:")
        print(f"   - {os.getcwd()}")
        print(f"   - {os.path.dirname(os.getcwd())}")
        print("\n   Execute primeiro o benchmark: ./run-ticketing-benchmark.sh")
        return
    
    # Usar o mais recente
    latest_csv = max(csv_files, key=os.path.getctime)
    print(f"📁 A processar: {latest_csv}")
    
    df = load_csv_data(latest_csv)
    if df is None:
        return
    
    output_dir = f"graficos_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
    os.makedirs(output_dir, exist_ok=True)
    print(f"📁 A criar gráficos em: {output_dir}/")
    
    grafico_throughput(df, output_dir)
    grafico_latencia(df, output_dir)
    grafico_p95(df, output_dir)
    grafico_sucesso(df, output_dir)
    
    print(f"\n✅ Gráficos gerados com sucesso em: {output_dir}/")
    print(f"   - throughput.png")
    print(f"   - latencia.png")
    print(f"   - p95.png")
    print(f"   - sucesso.png")

if __name__ == "__main__":
    main()
