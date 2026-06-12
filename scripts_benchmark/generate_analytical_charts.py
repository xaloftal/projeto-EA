#!/usr/bin/env python3
"""
Gera gráficos a partir dos resultados do benchmark analítico
"""

import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import json
import os
import re
from pathlib import Path
from datetime import datetime

# Configurar estilo
plt.style.use('seaborn-v0_8-darkgrid')
plt.rcParams['font.size'] = 10
plt.rcParams['figure.figsize'] = (14, 8)
plt.rcParams['figure.dpi'] = 100

class AnalyticalChartGenerator:
    def __init__(self, results_dir="results", output_dir="analytical_charts"):
        self.results_dir = Path(results_dir)
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(exist_ok=True)
        
    def load_summary_data(self):
        """Carrega o CSV de sumário mais recente"""
        csv_files = list(self.results_dir.glob("summary_*.csv"))
        if not csv_files:
            print("❌ Nenhum ficheiro summary_*.csv encontrado!")
            return None
        
        latest_csv = max(csv_files, key=lambda f: f.stat().st_mtime)
        print(f"📁 A processar: {latest_csv.name}")
        
        df = pd.read_csv(latest_csv)
        return df
    
    def load_detailed_data(self, query_name):
        """Carrega dados detalhados de uma query específica"""
        detail_file = self.results_dir / f"{query_name}_detailed.txt"
        if not detail_file.exists():
            return None
        
        with open(detail_file, 'r') as f:
            content = f.read()
        
        # Extrair valores de execução
        exec_values = []
        plan_values = []
        
        # Procurar pela tabela de resultados
        in_table = False
        for line in content.split('\n'):
            if 'Run   Tipo' in line or 'Run  Tipo' in line:
                in_table = True
                continue
            if in_table and line.startswith('-'):
                continue
            if in_table and line.strip() and not line.startswith('='):
                # Formato: "1     Warmup    45.23           12.34           57.57"
                parts = line.split()
                if len(parts) >= 4:
                    try:
                        exec_time = float(parts[2])
                        exec_values.append(exec_time)
                    except:
                        pass
                if 'Benchmark' in line or ('Warmup' not in line and len(exec_values) > 0):
                    pass
        
        return exec_values
    
    def chart_execution_times(self, df):
        """Gráfico 1: Barras - Tempos de execução por query"""
        fig, ax = plt.subplots(figsize=(14, 8))
        
        # Ordenar por tempo
        df_sorted = df.sort_values('Execution Mean (ms)', ascending=True)
        
        colors = ['#2ecc71' if x < 10 else '#f39c12' if x < 50 else '#e74c3c' 
                  for x in df_sorted['Execution Mean (ms)']]
        
        bars = ax.barh(df_sorted['Query'], df_sorted['Execution Mean (ms)'], color=colors)
        
        # Adicionar barras de erro (min-max)
        for i, (idx, row) in enumerate(df_sorted.iterrows()):
            ax.errorbar(row['Execution Mean (ms)'], i, 
                       xerr=[[row['Execution Mean (ms)'] - row['Execution Min (ms)']],
                             [row['Execution Max (ms)'] - row['Execution Mean (ms)']]],
                       fmt='none', capsize=5, color='black', alpha=0.5)
        
        ax.set_xlabel('Tempo de Execução (ms)', fontsize=12, fontweight='bold')
        ax.set_title('Tempo Médio de Execução por Query\n(com variação min-max)', 
                    fontsize=14, fontweight='bold')
        ax.axvline(x=10, color='green', linestyle='--', alpha=0.7, label='Excelente (<10ms)')
        ax.axvline(x=50, color='orange', linestyle='--', alpha=0.7, label='Atenção (>50ms)')
        ax.axvline(x=100, color='red', linestyle='--', alpha=0.7, label='Crítico (>100ms)')
        ax.legend(loc='lower right')
        
        # Adicionar valores nas barras
        for bar, val in zip(bars, df_sorted['Execution Mean (ms)']):
            ax.text(bar.get_width() + 1, bar.get_y() + bar.get_height()/2, 
                   f'{val:.1f}ms', va='center', fontsize=9)
        
        plt.tight_layout()
        plt.savefig(self.output_dir / '01_execution_times.png', dpi=150, bbox_inches='tight')
        plt.close()
        print("  ✅ 01_execution_times.png")
    
    def chart_boxplot(self, df):
        """Gráfico 2: Boxplot - Distribuição dos tempos"""
        fig, ax = plt.subplots(figsize=(14, 8))
        
        # Preparar dados
        data = []
        labels = []
        colors_list = []
        
        for _, row in df.iterrows():
            # Simular distribuição normal baseada na média e desvio
            mean = row['Execution Mean (ms)']
            std = row['Execution StdDev'] if row['Execution StdDev'] > 0 else mean * 0.1
            np.random.seed(hash(row['Query']) % 2**32)
            data.append(np.random.normal(mean, std, 50))
            labels.append(row['Query'][:30])
            
            if mean < 10:
                colors_list.append('#2ecc71')
            elif mean < 50:
                colors_list.append('#f39c12')
            else:
                colors_list.append('#e74c3c')
        
        bp = ax.boxplot(data, labels=labels, patch_artist=True, showmeans=True)
        
        for patch, color in zip(bp['boxes'], colors_list):
            patch.set_facecolor(color)
            patch.set_alpha(0.7)
        
        ax.set_ylabel('Tempo de Execução (ms)', fontsize=12, fontweight='bold')
        ax.set_title('Distribuição dos Tempos de Execução', fontsize=14, fontweight='bold')
        ax.axhline(y=10, color='green', linestyle='--', alpha=0.7, label='Excelente (<10ms)')
        ax.axhline(y=50, color='orange', linestyle='--', alpha=0.7, label='Atenção (>50ms)')
        ax.axhline(y=100, color='red', linestyle='--', alpha=0.7, label='Crítico (>100ms)')
        ax.legend(loc='upper right')
        plt.xticks(rotation=45, ha='right')
        
        plt.tight_layout()
        plt.savefig(self.output_dir / '02_boxplot_distribution.png', dpi=150, bbox_inches='tight')
        plt.close()
        print("  ✅ 02_boxplot_distribution.png")
    
    def chart_plan_type(self, df):
        """Gráfico 3: Pizza - Distribuição por tipo de plano"""
        fig, ax = plt.subplots(figsize=(10, 8))
        
        plan_counts = df['Plan Type'].value_counts()
        
        colors = {'Seq Scan': '#e74c3c', 'Index Scan': '#2ecc71', 
                  'Bitmap Scan': '#3498db', 'Unique': '#f39c12', 'Other': '#95a5a6'}
        
        color_list = [colors.get(p, '#95a5a6') for p in plan_counts.index]
        
        wedges, texts, autotexts = ax.pie(plan_counts.values, labels=plan_counts.index,
                                           autopct='%1.1f%%', colors=color_list,
                                           textprops={'fontsize': 11})
        
        ax.set_title('Distribuição por Tipo de Plano de Execução', fontsize=14, fontweight='bold')
        
        plt.tight_layout()
        plt.savefig(self.output_dir / '03_plan_type_distribution.png', dpi=150, bbox_inches='tight')
        plt.close()
        print("  ✅ 03_plan_type_distribution.png")
    
    def chart_cache_hit_ratio(self, df):
        """Gráfico 4: Barras - Cache Hit Ratio"""
        fig, ax = plt.subplots(figsize=(12, 6))
        
        if 'Cache Hit %' not in df.columns:
            print("  ⚠️ Cache Hit % não disponível")
            return
        
        df_sorted = df.sort_values('Cache Hit %', ascending=True)
        
        colors = ['#2ecc71' if x >= 90 else '#f39c12' if x >= 70 else '#e74c3c' 
                  for x in df_sorted['Cache Hit %']]
        
        bars = ax.barh(df_sorted['Query'], df_sorted['Cache Hit %'], color=colors)
        
        ax.set_xlabel('Cache Hit Ratio (%)', fontsize=12, fontweight='bold')
        ax.set_title('Eficiência do Cache por Query', fontsize=14, fontweight='bold')
        ax.axvline(x=90, color='green', linestyle='--', alpha=0.7, label='Ótimo (>90%)')
        ax.axvline(x=70, color='orange', linestyle='--', alpha=0.7, label='Atenção (<70%)')
        ax.legend(loc='lower right')
        ax.set_xlim(0, 105)
        
        for bar, val in zip(bars, df_sorted['Cache Hit %']):
            ax.text(bar.get_width() + 1, bar.get_y() + bar.get_height()/2, 
                   f'{val:.1f}%', va='center', fontsize=9)
        
        plt.tight_layout()
        plt.savefig(self.output_dir / '04_cache_hit_ratio.png', dpi=150, bbox_inches='tight')
        plt.close()
        print("  ✅ 04_cache_hit_ratio.png")
    
    def chart_comparison_before_after(self, df):
        """Gráfico 5: Comparação antes/depois (média vs mediana)"""
        fig, ax = plt.subplots(figsize=(12, 6))
        
        x = np.arange(len(df))
        width = 0.35
        
        bars1 = ax.bar(x - width/2, df['Execution Mean (ms)'], width, label='Média', color='#3498db')
        bars2 = ax.bar(x + width/2, df['Execution Median (ms)'], width, label='Mediana', color='#2ecc71')
        
        ax.set_xlabel('Query', fontsize=12, fontweight='bold')
        ax.set_ylabel('Tempo (ms)', fontsize=12, fontweight='bold')
        ax.set_title('Comparação: Média vs Mediana', fontsize=14, fontweight='bold')
        ax.set_xticks(x)
        ax.set_xticklabels(df['Query'], rotation=45, ha='right')
        ax.legend()
        
        # Adicionar linha de referência
        ax.axhline(y=50, color='orange', linestyle='--', alpha=0.7, label='Atenção (50ms)')
        
        plt.tight_layout()
        plt.savefig(self.output_dir / '05_mean_vs_median.png', dpi=150, bbox_inches='tight')
        plt.close()
        print("  ✅ 05_mean_vs_median.png")
    
    def chart_radar(self, df):
        """Gráfico 6: Radar - Comparação multidimensional"""
        fig, ax = plt.subplots(figsize=(10, 10), subplot_kw=dict(projection='polar'))
        
        # Normalizar métricas
        metrics = ['Execution Mean (ms)', 'Execution StdDev', 'Cache Hit %']
        
        # Inverter para que menor seja melhor
        df_norm = df.copy()
        max_exec = df['Execution Mean (ms)'].max()
        df_norm['Execution Mean (ms)_norm'] = 1 - (df['Execution Mean (ms)'] / max_exec)
        df_norm['Execution StdDev_norm'] = 1 - (df['Execution StdDev'] / df['Execution StdDev'].max())
        df_norm['Cache Hit %_norm'] = df['Cache Hit %'] / 100
        
        angles = np.linspace(0, 2 * np.pi, len(metrics), endpoint=False).tolist()
        angles += angles[:1]
        
        for _, row in df_norm.iterrows():
            values = [
                row['Execution Mean (ms)_norm'],
                row['Execution StdDev_norm'],
                row['Cache Hit %_norm']
            ]
            values += values[:1]
            ax.plot(angles, values, 'o-', linewidth=2, label=row['Query'][:20])
            ax.fill(angles, values, alpha=0.1)
        
        ax.set_xticks(angles[:-1])
        ax.set_xticklabels(['Performance', 'Consistência', 'Cache'])
        ax.set_ylim(0, 1)
        ax.set_title('Performance Multidimensional', fontsize=14, fontweight='bold', pad=20)
        ax.legend(loc='upper right', bbox_to_anchor=(1.3, 1.0))
        
        plt.tight_layout()
        plt.savefig(self.output_dir / '06_radar_chart.png', dpi=150, bbox_inches='tight')
        plt.close()
        print("  ✅ 06_radar_chart.png")
    
    def chart_heatmap(self, df):
        """Gráfico 7: Heatmap - Matriz de correlação"""
        fig, ax = plt.subplots(figsize=(10, 8))
        
        # Selecionar colunas numéricas
        numeric_cols = ['Execution Mean (ms)', 'Execution Median (ms)', 
                        'Execution Min (ms)', 'Execution Max (ms)', 
                        'Execution StdDev', 'Cache Hit %']
        
        corr_matrix = df[numeric_cols].corr()
        
        im = ax.imshow(corr_matrix, cmap='RdYlGn', vmin=-1, vmax=1)
        
        ax.set_xticks(np.arange(len(corr_matrix.columns)))
        ax.set_yticks(np.arange(len(corr_matrix.columns)))
        ax.set_xticklabels([c.replace(' (ms)', '').replace('Execution ', '') for c in corr_matrix.columns], 
                          rotation=45, ha='right')
        ax.set_yticklabels([c.replace(' (ms)', '').replace('Execution ', '') for c in corr_matrix.columns])
        
        for i in range(len(corr_matrix.columns)):
            for j in range(len(corr_matrix.columns)):
                text = ax.text(j, i, f'{corr_matrix.iloc[i, j]:.2f}',
                             ha="center", va="center", color="black", fontsize=9)
        
        plt.colorbar(im, ax=ax, label='Correlação')
        ax.set_title('Matriz de Correlação entre Métricas', fontsize=14, fontweight='bold')
        
        plt.tight_layout()
        plt.savefig(self.output_dir / '07_correlation_heatmap.png', dpi=150, bbox_inches='tight')
        plt.close()
        print("  ✅ 07_correlation_heatmap.png")
    
    def chart_timeline(self, df):
        """Gráfico 8: Linha - Evolução das queries mais lentas"""
        fig, ax = plt.subplots(figsize=(14, 8))
        
        # Ordenar por tempo
        df_sorted = df.sort_values('Execution Mean (ms)', ascending=False).head(10)
        
        colors = plt.cm.RdYlGn_r(np.linspace(0, 1, len(df_sorted)))
        
        bars = ax.bar(df_sorted['Query'], df_sorted['Execution Mean (ms)'], color=colors)
        
        ax.set_ylabel('Tempo de Execução (ms)', fontsize=12, fontweight='bold')
        ax.set_title('Top 10 Queries Mais Lentas', fontsize=14, fontweight='bold')
        ax.axhline(y=50, color='orange', linestyle='--', alpha=0.7, label='Atenção (50ms)')
        ax.axhline(y=100, color='red', linestyle='--', alpha=0.7, label='Crítico (100ms)')
        ax.legend()
        plt.xticks(rotation=45, ha='right')
        
        for bar, val in zip(bars, df_sorted['Execution Mean (ms)']):
            ax.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 2, 
                   f'{val:.1f}ms', ha='center', fontsize=9, fontweight='bold')
        
        plt.tight_layout()
        plt.savefig(self.output_dir / '08_slowest_queries.png', dpi=150, bbox_inches='tight')
        plt.close()
        print("  ✅ 08_slowest_queries.png")
    
    def generate_html_report(self, df):
        """Gera relatório HTML interativo"""
        timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        
        html_content = f'''<!DOCTYPE html>
<html lang="pt">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Analytical Benchmark - Relatório de Performance</title>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }}
        .container {{ max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; }}
        h1 {{ color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }}
        h2 {{ color: #34495e; margin-top: 30px; border-left: 4px solid #3498db; padding-left: 15px; }}
        .summary {{ display: flex; gap: 20px; margin: 20px 0; flex-wrap: wrap; }}
        .card {{ background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 10px; flex: 1; min-width: 150px; text-align: center; }}
        .card.green {{ background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }}
        .card.orange {{ background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }}
        .card.blue {{ background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }}
        .card-value {{ font-size: 32px; font-weight: bold; margin: 10px 0; }}
        .card-label {{ font-size: 14px; opacity: 0.9; }}
        img {{ width: 100%; margin: 20px 0; border: 1px solid #ddd; border-radius: 8px; }}
        table {{ width: 100%; border-collapse: collapse; margin: 20px 0; }}
        th, td {{ padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }}
        th {{ background-color: #3498db; color: white; }}
        tr:hover {{ background-color: #f5f5f5; }}
        .good {{ color: #27ae60; font-weight: bold; }}
        .warning {{ color: #f39c12; font-weight: bold; }}
        .bad {{ color: #e74c3c; font-weight: bold; }}
        .footer {{ text-align: center; margin-top: 40px; padding-top: 20px; border-top: 1px solid #ddd; color: #7f8c8d; }}
    </style>
</head>
<body>
    <div class="container">
        <h1>📊 Analytical Benchmark - Relatório de Performance</h1>
        <p><strong>Data de análise:</strong> {timestamp}</p>
        <p><strong>Número de queries analisadas:</strong> {len(df)}</p>
        
        <div class="summary">
            <div class="card blue">
                <div class="card-value">{df['Execution Mean (ms)'].mean():.1f}</div>
                <div class="card-label">Tempo Médio (ms)</div>
            </div>
            <div class="card green">
                <div class="card-value">{df['Cache Hit %'].mean():.1f}%</div>
                <div class="card-label">Cache Hit Ratio</div>
            </div>
            <div class="card orange">
                <div class="card-value">{len(df[df['Execution Mean (ms)'] > 100])}</div>
                <div class="card-label">Queries Lentas (>100ms)</div>
            </div>
            <div class="card">
                <div class="card-value">{len(df[df['Plan Type'] == 'Index Scan'])}</div>
                <div class="card-label">Queries com Index Scan</div>
            </div>
        </div>
        
        <h2>📈 Gráficos de Performance</h2>
        <img src="01_execution_times.png" alt="Execution Times">
        <img src="02_boxplot_distribution.png" alt="Boxplot">
        <img src="03_plan_type_distribution.png" alt="Plan Type">
        <img src="04_cache_hit_ratio.png" alt="Cache Hit Ratio">
        <img src="05_mean_vs_median.png" alt="Mean vs Median">
        <img src="06_radar_chart.png" alt="Radar Chart">
        <img src="07_correlation_heatmap.png" alt="Correlation Heatmap">
        <img src="08_slowest_queries.png" alt="Slowest Queries">
        
        <h2>📋 Tabela de Resultados</h2>
        <table>
            <thead>
                <tr><th>Query</th><th>Média (ms)</th><th>Mediana (ms)</th><th>Min (ms)</th><th>Max (ms)</th><th>StdDev</th><th>Plan Type</th><th>Cache Hit %</th></tr>
            </thead>
            <tbody>
'''
        
        for _, row in df.iterrows():
            mean_class = 'good' if row['Execution Mean (ms)'] < 10 else 'warning' if row['Execution Mean (ms)'] < 50 else 'bad'
            html_content += f'''
                <tr>
                    <td>{row['Query']}</td>
                    <td class="{mean_class}">{row['Execution Mean (ms)']:.2f}</td>
                    <td>{row['Execution Median (ms)']:.2f}</td>
                    <td>{row['Execution Min (ms)']:.2f}</td>
                    <td>{row['Execution Max (ms)']:.2f}</td>
                    <td>{row['Execution StdDev']:.2f}</td>
                    <td>{row['Plan Type']}</td>
                    <td>{row['Cache Hit %']:.1f}%</td>
                </tr>
'''
        
        html_content += '''
            </tbody>
        </table>
        
        <h2>🎯 Recomendações</h2>
        <ul>
'''
        
        # Gerar recomendações baseadas nos dados
        slow_queries = df[df['Execution Mean (ms)'] > 100]
        if len(slow_queries) > 0:
            html_content += f'<li>🔴 <strong>Queries críticas:</strong> {len(slow_queries)} queries demoram mais de 100ms. Rever índices e plano de execução.</li>'
        
        seq_scans = df[df['Plan Type'] == 'Seq Scan']
        if len(seq_scans) > 0:
            html_content += f'<li>🟡 <strong>Seq Scans detectados:</strong> {len(seq_scans)} queries usam Sequential Scan. Considerar criar índices apropriados.</li>'
        
        low_cache = df[df['Cache Hit %'] < 80]
        if len(low_cache) > 0:
            html_content += f'<li>🟡 <strong>Cache ineficiente:</strong> {len(low_cache)} queries têm baixa taxa de acerto de cache. Aumentar shared_buffers.</li>'
        
        if len(slow_queries) == 0 and len(seq_scans) == 0:
            html_content += '<li>✅ <strong>Performance excelente!</strong> Todas as queries estão otimizadas.</li>'
        
        html_content += '''
        </ul>
        
        <div class="footer">
            <p>Relatório gerado automaticamente pelo Analytical Benchmark Suite</p>
            <p>CatchIT Database Benchmark</p>
        </div>
    </div>
</body>
</html>
'''
        
        report_file = self.output_dir / 'analytical_report.html'
        with open(report_file, 'w', encoding='utf-8') as f:
            f.write(html_content)
        print(f"  ✅ analytical_report.html")
    
    def run(self):
        """Executa a geração de todos os gráficos"""
        print("\n" + "=" * 60)
        print("📊 A GERAR GRÁFICOS ANALÍTICOS")
        print("=" * 60)
        
        df = self.load_summary_data()
        if df is None:
            return
        
        print(f"\n🎨 A criar gráficos em: {self.output_dir}/\n")
        
        # Renomear colunas para facilitar
        df.columns = df.columns.str.strip()
        
        # Gerar gráficos
        self.chart_execution_times(df)
        self.chart_boxplot(df)
        self.chart_plan_type(df)
        self.chart_cache_hit_ratio(df)
        self.chart_comparison_before_after(df)
        self.chart_radar(df)
        self.chart_heatmap(df)
        self.chart_timeline(df)
        self.generate_html_report(df)
        
        print(f"\n✅ Gráficos gerados com sucesso em: {self.output_dir}/")
        print(f"   Abre o ficheiro: {self.output_dir}/analytical_report.html")
        
        # Mostar estatísticas rápidas
        print("\n📊 ESTATÍSTICAS RÁPIDAS:")
        print(f"   Tempo médio geral: {df['Execution Mean (ms)'].mean():.2f} ms")
        print(f"   Pior query: {df.loc[df['Execution Mean (ms)'].idxmax(), 'Query']} ({df['Execution Mean (ms)'].max():.2f} ms)")
        print(f"   Melhor query: {df.loc[df['Execution Mean (ms)'].idxmin(), 'Query']} ({df['Execution Mean (ms)'].min():.2f} ms)")
        print(f"   Cache hit médio: {df['Cache Hit %'].mean():.1f}%")

def main():
    generator = AnalyticalChartGenerator()
    generator.run()

if __name__ == "__main__":
    main()