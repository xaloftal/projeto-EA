#!/usr/bin/env python3
"""
Analytical Benchmark for PostgreSQL Queries
Executa queries SQL múltiplas vezes, descarta warmup e calcula médias
"""

import subprocess
import json
import os
import sys
import re
import time
import statistics
from datetime import datetime
from pathlib import Path

class AnalyticalBenchmark:
    def __init__(self, queries_dir="analytical_queries", results_dir="results", 
                 warmup_runs=1, benchmark_runs=5):
        self.queries_dir = Path(queries_dir)
        self.results_dir = Path(results_dir)
        self.results_dir.mkdir(exist_ok=True)
        
        # Configuração do benchmark
        self.warmup_runs = warmup_runs      # Execuções de aquecimento (descartadas)
        self.benchmark_runs = benchmark_runs  # Execuções para média
        
        # Configuração do container
        self.container = "catchit-db"
        self.db_user = "postgres"
        self.db_name = "catchitdb"
        
    def run_query_once(self, sql_content, query_name):
        """Executa uma query SQL uma vez e retorna as métricas"""
        # Adicionar EXPLAIN se não existir
        if not sql_content.upper().startswith('EXPLAIN'):
            sql_content = f"EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)\n{sql_content}"
        
        cmd = [
            "docker", "exec", "-i", self.container,
            "psql", "-U", self.db_user, "-d", self.db_name,
            "-t", "-A", "-c", sql_content
        ]
        
        try:
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
            
            if result.returncode != 0:
                print(f"      ❌ Erro: {result.stderr[:100]}")
                return None
            
            output = result.stdout.strip()
            if output:
                try:
                    plan_data = json.loads(output)
                    return self._extract_metrics(plan_data, sql_content)
                except json.JSONDecodeError:
                    return self._extract_text_metrics(output, sql_content)
            return None
            
        except subprocess.TimeoutExpired:
            print(f"      ⏰ Timeout (120s)")
            return None
        except Exception as e:
            print(f"      ❌ Erro: {e}")
            return None
    
    def _extract_metrics(self, plan_data, sql):
        """Extrai métricas do plano JSON"""
        if not plan_data or len(plan_data) == 0:
            return None
        
        plan = plan_data[0].get("Plan", {})
        
        metrics = {
            "query": sql,
            "execution_time_ms": plan.get("Actual Total Time", 0),
            "planning_time_ms": plan_data[0].get("Planning Time", 0),
            "total_time_ms": plan_data[0].get("Planning Time", 0) + plan.get("Actual Total Time", 0),
            "plan_type": plan.get("Node Type", "Unknown"),
            "parallel_workers": plan.get("Workers Planned", 0),
            "actual_workers": plan.get("Workers Launched", 0),
            "hit_buffers": self._extract_hit_buffers(plan),
            "read_buffers": self._extract_read_buffers(plan),
            "temp_written": plan.get("Temp Written Blocks", 0),
            "rows_removed": self._count_rows_removed(plan),
            "actual_rows": plan.get("Actual Rows", 0),
            "actual_time_first_row": plan.get("Actual Total Time", 0)  # aproximação
        }
        
        return metrics
    
    def _extract_hit_buffers(self, plan):
        """Extrai buffers hit do plano"""
        total = 0
        if "Shared Hit Blocks" in plan:
            total += plan.get("Shared Hit Blocks", 0)
        for child in plan.get("Plans", []):
            total += self._extract_hit_buffers(child)
        return total
    
    def _extract_read_buffers(self, plan):
        """Extrai buffers read do plano"""
        total = 0
        if "Shared Read Blocks" in plan:
            total += plan.get("Shared Read Blocks", 0)
        for child in plan.get("Plans", []):
            total += self._extract_read_buffers(child)
        return total
    
    def _extract_text_metrics(self, output, sql):
        """Extrai métricas do formato texto"""
        metrics = {"query": sql}
        
        exec_match = re.search(r'Execution Time: ([\d.]+) ms', output)
        metrics["execution_time_ms"] = float(exec_match.group(1)) if exec_match else 0
        
        plan_match = re.search(r'Planning Time: ([\d.]+) ms', output)
        metrics["planning_time_ms"] = float(plan_match.group(1)) if plan_match else 0
        
        metrics["total_time_ms"] = metrics["planning_time_ms"] + metrics["execution_time_ms"]
        
        # Extrair tipo de plano
        if 'Seq Scan' in output:
            metrics["plan_type"] = "Seq Scan"
        elif 'Index Scan' in output:
            metrics["plan_type"] = "Index Scan"
        elif 'Bitmap Heap Scan' in output:
            metrics["plan_type"] = "Bitmap Scan"
        else:
            metrics["plan_type"] = "Other"
        
        # Extrair buffers
        hit_match = re.search(r'Buffers: shared hit=(\d+)', output)
        metrics["hit_buffers"] = int(hit_match.group(1)) if hit_match else 0
        
        read_match = re.search(r'Buffers: shared hit=\d+ read=(\d+)', output)
        metrics["read_buffers"] = int(read_match.group(1)) if read_match else 0
        
        metrics["full_plan"] = output
        return metrics
    
    def _count_rows_removed(self, plan):
        """Conta linhas removidas por filtros"""
        count = 0
        if "Rows Removed by Filter" in plan:
            count += plan.get("Rows Removed by Filter", 0)
        for child in plan.get("Plans", []):
            count += self._count_rows_removed(child)
        return count
    
    def run_query_multiple(self, sql_file):
        """Executa uma query múltiplas vezes e calcula estatísticas"""
        print(f"  📄 {sql_file.name}")
        
        with open(sql_file, 'r') as f:
            sql_content = f.read()
        
        # Remover EXPLAIN se existir (vamos adicionar depois)
        sql_content = re.sub(r'^EXPLAIN\s*(\([^)]+\))?\s*', '', sql_content, flags=re.IGNORECASE)
        sql_content = sql_content.strip()
        
        results = []
        
        total_runs = self.warmup_runs + self.benchmark_runs
        
        for run in range(total_runs):
            is_warmup = run < self.warmup_runs
            run_type = "🔥 Warmup" if is_warmup else "📊 Run"
            
            print(f"    {run_type} {run + 1}/{total_runs}...", end=" ", flush=True)
            
            metrics = self.run_query_once(sql_content, sql_file.stem)
            
            if metrics:
                results.append({
                    "run": run + 1,
                    "is_warmup": is_warmup,
                    "execution_time_ms": metrics.get("execution_time_ms", 0),
                    "planning_time_ms": metrics.get("planning_time_ms", 0),
                    "total_time_ms": metrics.get("total_time_ms", 0),
                    "plan_type": metrics.get("plan_type", "N/A"),
                    "hit_buffers": metrics.get("hit_buffers", 0),
                    "read_buffers": metrics.get("read_buffers", 0),
                    "rows_removed": metrics.get("rows_removed", 0)
                })
                print(f"✅ {metrics.get('execution_time_ms', 0):.2f} ms")
                time.sleep(0.5)  # Pequena pausa entre execuções
            else:
                print(f"❌ Falha")
        
        # Calcular estatísticas (apenas runs de benchmark, sem warmup)
        benchmark_results = [r for r in results if not r["is_warmup"]]
        
        if benchmark_results:
            exec_times = [r["execution_time_ms"] for r in benchmark_results]
            plan_times = [r["planning_time_ms"] for r in benchmark_results]
            total_times = [r["total_time_ms"] for r in benchmark_results]
            
            stats = {
                "name": sql_file.stem,
                "warmup_runs": self.warmup_runs,
                "benchmark_runs": len(benchmark_results),
                "execution_time_ms": {
                    "mean": statistics.mean(exec_times),
                    "median": statistics.median(exec_times),
                    "min": min(exec_times),
                    "max": max(exec_times),
                    "stddev": statistics.stdev(exec_times) if len(exec_times) > 1 else 0,
                    "values": exec_times
                },
                "planning_time_ms": {
                    "mean": statistics.mean(plan_times),
                    "median": statistics.median(plan_times),
                    "min": min(plan_times),
                    "max": max(plan_times),
                    "values": plan_times
                },
                "total_time_ms": {
                    "mean": statistics.mean(total_times),
                    "median": statistics.median(total_times),
                    "min": min(total_times),
                    "max": max(total_times),
                    "values": total_times
                },
                "plan_type": benchmark_results[0].get("plan_type", "N/A"),
                "hit_buffers_avg": statistics.mean([r["hit_buffers"] for r in benchmark_results]),
                "read_buffers_avg": statistics.mean([r["read_buffers"] for r in benchmark_results]),
                "all_runs": results
            }
            return stats
        return None
    
    def save_detailed_report(self, stats):
        """Guarda relatório detalhado da query"""
        if not stats:
            return
        
        output_file = self.results_dir / f"{stats['name']}_detailed.txt"
        
        with open(output_file, 'w') as f:
            f.write("=" * 80 + "\n")
            f.write(f"QUERY: {stats['name']}\n")
            f.write(f"DATA: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write("=" * 80 + "\n\n")
            
            f.write("CONFIGURAÇÃO:\n")
            f.write("-" * 40 + "\n")
            f.write(f"Warmup runs: {stats['warmup_runs']} (descartadas)\n")
            f.write(f"Benchmark runs: {stats['benchmark_runs']}\n\n")
            
            f.write("RESULTADOS POR EXECUÇÃO:\n")
            f.write("-" * 40 + "\n")
            f.write(f"{'Run':<6} {'Tipo':<10} {'Execution (ms)':<15} {'Planning (ms)':<15} {'Total (ms)':<15}\n")
            f.write("-" * 60 + "\n")
            
            for run in stats["all_runs"]:
                tipo = "Warmup" if run["is_warmup"] else "Benchmark"
                f.write(f"{run['run']:<6} {tipo:<10} {run['execution_time_ms']:<15.2f} {run['planning_time_ms']:<15.2f} {run['total_time_ms']:<15.2f}\n")
            
            f.write("\nESTATÍSTICAS FINAIS (apenas Benchmark runs):\n")
            f.write("-" * 40 + "\n")
            f.write(f"Plan Type: {stats['plan_type']}\n\n")
            
            f.write("Execution Time:\n")
            f.write(f"  Mean:   {stats['execution_time_ms']['mean']:.2f} ms\n")
            f.write(f"  Median: {stats['execution_time_ms']['median']:.2f} ms\n")
            f.write(f"  Min:    {stats['execution_time_ms']['min']:.2f} ms\n")
            f.write(f"  Max:    {stats['execution_time_ms']['max']:.2f} ms\n")
            f.write(f"  StdDev: {stats['execution_time_ms']['stddev']:.2f} ms\n")
            f.write(f"  Values: {[f'{v:.2f}' for v in stats['execution_time_ms']['values']]}\n\n")
            
            f.write("Planning Time:\n")
            f.write(f"  Mean:   {stats['planning_time_ms']['mean']:.2f} ms\n")
            f.write(f"  Median: {stats['planning_time_ms']['median']:.2f} ms\n")
            f.write(f"  Min:    {stats['planning_time_ms']['min']:.2f} ms\n")
            f.write(f"  Max:    {stats['planning_time_ms']['max']:.2f} ms\n")
            f.write(f"  Values: {[f'{v:.2f}' for v in stats['planning_time_ms']['values']]}\n\n")
            
            f.write("Cache Hit Ratio:\n")
            total_buffers = stats['hit_buffers_avg'] + stats['read_buffers_avg']
            if total_buffers > 0:
                hit_ratio = (stats['hit_buffers_avg'] / total_buffers) * 100
                f.write(f"  Hit Ratio: {hit_ratio:.1f}% (avg hit: {stats['hit_buffers_avg']:.0f}, avg read: {stats['read_buffers_avg']:.0f})\n")
            else:
                f.write("  Hit Ratio: N/A (no buffer data)\n")
        
        print(f"    💾 Relatório: {output_file}")
    
    def save_summary(self, all_stats):
        """Guarda resumo comparativo de todas as queries"""
        if not all_stats:
            return
        
        summary_file = self.results_dir / f"summary_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv"
        
        import csv
        with open(summary_file, 'w', newline='') as f:
            writer = csv.writer(f)
            writer.writerow([
                "Query", 
                "Execution Mean (ms)", "Execution Median (ms)", "Execution Min (ms)", "Execution Max (ms)", "Execution StdDev",
                "Planning Mean (ms)", "Total Mean (ms)",
                "Plan Type", "Cache Hit %", "Warmup Runs", "Benchmark Runs"
            ])
            
            for stats in all_stats:
                if stats:
                    total_buffers = stats['hit_buffers_avg'] + stats['read_buffers_avg']
                    hit_ratio = (stats['hit_buffers_avg'] / total_buffers * 100) if total_buffers > 0 else 0
                    
                    writer.writerow([
                        stats['name'],
                        f"{stats['execution_time_ms']['mean']:.2f}",
                        f"{stats['execution_time_ms']['median']:.2f}",
                        f"{stats['execution_time_ms']['min']:.2f}",
                        f"{stats['execution_time_ms']['max']:.2f}",
                        f"{stats['execution_time_ms']['stddev']:.2f}",
                        f"{stats['planning_time_ms']['mean']:.2f}",
                        f"{stats['total_time_ms']['mean']:.2f}",
                        stats['plan_type'],
                        f"{hit_ratio:.1f}",
                        stats['warmup_runs'],
                        stats['benchmark_runs']
                    ])
        
        print(f"\n📊 Resumo CSV: {summary_file}")
        return summary_file
    
    def print_summary_table(self, all_stats):
        """Imprime tabela resumo no terminal"""
        print("\n" + "=" * 90)
        print("RESUMO FINAL (médias após warmup)")
        print("=" * 90)
        print(f"{'Query':<35} {'Exec (ms)':<12} {'Med (ms)':<10} {'Min (ms)':<10} {'Max (ms)':<10} {'Plan Type':<15}")
        print("-" * 90)
        
        for stats in all_stats:
            if stats:
                name = stats['name'][:33]
                exec_mean = stats['execution_time_ms']['mean']
                exec_median = stats['execution_time_ms']['median']
                exec_min = stats['execution_time_ms']['min']
                exec_max = stats['execution_time_ms']['max']
                plan_type = stats['plan_type'][:13]
                
                # Cor based on performance
                if exec_mean < 10:
                    color = "\033[92m"  # Verde
                elif exec_mean < 100:
                    color = "\033[93m"  # Amarelo
                else:
                    color = "\033[91m"  # Vermelho
                
                print(f"{color}{name:<35} {exec_mean:<12.2f} {exec_median:<10.2f} {exec_min:<10.2f} {exec_max:<10.2f} {plan_type:<15}\033[0m")
        
        print("=" * 90)
        print("🔵 Legenda: Verde (<10ms) | Amarelo (10-100ms) | Vermelho (>100ms)")
        print("=" * 90)
    
    def run_all(self):
        """Executa todas as queries do diretório"""
        print("\n" + "=" * 60)
        print("ANALYTICAL BENCHMARK - PostgreSQL Query Analysis")
        print("=" * 60)
        print(f"📁 Queries dir: {self.queries_dir}")
        print(f"📁 Results dir: {self.results_dir}")
        print(f"🔥 Warmup runs: {self.warmup_runs} (descartadas)")
        print(f"📊 Benchmark runs: {self.benchmark_runs} (para média)")
        print("")
        
        # Verificar container
        result = subprocess.run(["docker", "ps", "--format", "{{.Names}}"], 
                               capture_output=True, text=True)
        if self.container not in result.stdout:
            print(f"❌ Container {self.container} não está a correr!")
            print("   Executa: docker start catchit-db")
            return
        
        # Encontrar ficheiros SQL
        sql_files = sorted(self.queries_dir.glob("*.sql"))
        
        if not sql_files:
            print(f"❌ Nenhum ficheiro .sql encontrado em {self.queries_dir}")
            return
        
        print(f"🔍 Encontradas {len(sql_files)} queries para analisar\n")
        
        all_stats = []
        
        for i, sql_file in enumerate(sql_files, 1):
            print(f"[{i}/{len(sql_files)}]")
            stats = self.run_query_multiple(sql_file)
            
            if stats:
                self.save_detailed_report(stats)
                all_stats.append(stats)
            else:
                print(f"  ❌ Falha ao processar {sql_file.name}")
            print()
        
        if all_stats:
            self.save_summary(all_stats)
            self.print_summary_table(all_stats)
        
        print("\n✅ Benchmark analítico concluído!")
        print(f"📁 Resultados guardados em: {self.results_dir}/")

def main():
    import argparse
    
    parser = argparse.ArgumentParser(description='Analytical Benchmark for PostgreSQL')
    parser.add_argument('--warmup', type=int, default=1, help='Número de runs de aquecimento (default: 1)')
    parser.add_argument('--runs', type=int, default=5, help='Número de runs para média (default: 5)')
    parser.add_argument('--queries-dir', type=str, default='analytical_queries', help='Diretório das queries SQL')
    parser.add_argument('--results-dir', type=str, default='results', help='Diretório de resultados')
    
    args = parser.parse_args()
    
    benchmark = AnalyticalBenchmark(
        queries_dir=args.queries_dir,
        results_dir=args.results_dir,
        warmup_runs=args.warmup,
        benchmark_runs=args.runs
    )
    benchmark.run_all()

if __name__ == "__main__":
    main()