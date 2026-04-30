import pickle
from pathlib import Path
import pandas as pd
import json
import hashlib
import math
import argparse
import time


PICKLE_FILE = "BusRoutes (1).pickle"
OUTPUT_DIR = "./data/routes"
MAP_FILE = "routes_map.html"


def load_bus_routes(pickle_file=PICKLE_FILE):
    with open(pickle_file, "rb") as file:
        pickle_data = pickle.load(file)

    if not isinstance(pickle_data, dict):
        raise TypeError("O conteúdo do pickle não é um dicionário.")

    return pickle_data


def route_to_dataframe(route_data):
    if isinstance(route_data, pd.DataFrame):
        return route_data
    if isinstance(route_data, dict):
        return pd.json_normalize(route_data)
    if isinstance(route_data, list):
        return pd.DataFrame(route_data)
    return pd.DataFrame({"value": [route_data]})


def save_routes_as_csv(bus_routes, output_dir=OUTPUT_DIR):
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)

    for route_id, route_data in bus_routes.items():
        route_file = output_path / f"{route_id}.csv"
        df = route_to_dataframe(route_data)
        df.to_csv(route_file, index=False)

    return output_path


def route_color(route_id):
    digest = hashlib.md5(route_id.encode("utf-8")).hexdigest()
    hue = int(digest[:8], 16) % 360
    return f"hsl({hue}, 78%, 45%)"


def format_duration(total_seconds):
    total_seconds = int(max(0, total_seconds))
    hours = total_seconds // 3600
    minutes = (total_seconds % 3600) // 60
    seconds = total_seconds % 60
    return f"{hours:02d}:{minutes:02d}:{seconds:02d}"


def _schedule_seconds_from_ride_time(series):
    ride_time = pd.to_datetime(series.astype(str), format="%H:%M:%S", errors="coerce")
    if ride_time.isna().all():
        return None

    seconds = (
        ride_time.dt.hour.fillna(0).astype(int) * 3600
        + ride_time.dt.minute.fillna(0).astype(int) * 60
        + ride_time.dt.second.fillna(0).astype(int)
    ).astype(float)

    corrected = []
    day_offset = 0
    prev = None
    for value in seconds.tolist():
        if prev is not None and value + day_offset < prev:
            day_offset += 24 * 3600
        current = value + day_offset
        corrected.append(current)
        prev = current

    return pd.Series(corrected)


def prepare_route_schedule(route_df):
    df = route_to_dataframe(route_df).copy()
    if "Stop_stn" not in df.columns:
        raise ValueError("A rota nao tem a coluna 'Stop_stn'.")

    df = df[df["Stop_stn"].notna()].reset_index(drop=True)
    if df.empty:
        raise ValueError("A rota nao contem paragens validas.")

    base_seconds = None
    if "sub" in df.columns:
        sub_seconds = pd.to_timedelta(df["sub"], errors="coerce").dt.total_seconds()
        if sub_seconds.notna().sum() >= 2:
            base_seconds = sub_seconds.ffill().fillna(0)

    if base_seconds is None and "Ride_time" in df.columns:
        ride_seconds = _schedule_seconds_from_ride_time(df["Ride_time"])
        if ride_seconds is not None:
            base_seconds = ride_seconds

    if base_seconds is None:
        # Fallback conservador: 90s entre paragens quando nao ha metadados temporais.
        base_seconds = pd.Series([idx * 90 for idx in range(len(df))], dtype=float)

    base_seconds = base_seconds.astype(float)
    base_seconds = base_seconds - float(base_seconds.iloc[0])
    intervals = base_seconds.diff().fillna(0)
    intervals = intervals.clip(lower=0)

    schedule = []
    for idx, row in df.iterrows():
        stop = str(row["Stop_stn"])
        wait_seconds = float(intervals.iloc[idx])
        elapsed_seconds = float(base_seconds.iloc[idx])
        ride_time = str(row["Ride_time"]) if "Ride_time" in df.columns else "N/A"
        schedule.append(
            {
                "stop": stop,
                "ride_time": ride_time,
                "wait_seconds": wait_seconds,
                "elapsed_seconds": elapsed_seconds,
            }
        )

    return schedule


def simulate_route(route_id, bus_routes, speedup=120.0, max_wait=2.0):
    if speedup <= 0:
        raise ValueError("speedup tem de ser maior que 0.")

    if route_id not in bus_routes:
        available = ", ".join(sorted(bus_routes.keys())[:15])
        raise KeyError(f"Rota '{route_id}' nao encontrada. Exemplos: {available}")

    schedule = prepare_route_schedule(bus_routes[route_id])
    total_stops = len(schedule)
    total_simulated = schedule[-1]["elapsed_seconds"] if schedule else 0

    print(f"Simulacao iniciada para rota: {route_id}")
    print(f"Paragens: {total_stops} | Tempo simulado total: {format_duration(total_simulated)}")
    print(f"Aceleracao: x{speedup:.1f} | Espera real maxima por troco: {max_wait:.2f}s")
    print("-" * 72)

    real_start = time.perf_counter()
    for idx, stop_info in enumerate(schedule, start=1):
        if idx > 1:
            simulated_leg = stop_info["wait_seconds"]
            real_leg = min(simulated_leg / speedup, max_wait)
            print(
                f"A circular para {stop_info['stop']} "
                f"(troco: {format_duration(simulated_leg)} sim | {real_leg:.2f}s real)"
            )
            time.sleep(real_leg)

        elapsed = stop_info["elapsed_seconds"]
        print(
            f"[{idx:02d}/{total_stops}] Chegada a {stop_info['stop']} "
            f"| hora_planeada={stop_info['ride_time']} "
            f"| tempo_decorrido={format_duration(elapsed)}"
        )

    real_elapsed = time.perf_counter() - real_start
    print("-" * 72)
    print(
        f"Simulacao terminada. Rota {route_id} concluida "
        f"em {real_elapsed:.2f}s reais (equivale a {format_duration(total_simulated)} simulados)."
    )


def synthetic_stop_coord(stop_id, center_lat=38.7223, center_lon=-9.1393):
    digest = hashlib.md5(stop_id.encode("utf-8")).hexdigest()
    angle_seed = int(digest[:8], 16) / 0xFFFFFFFF
    radius_seed = int(digest[8:16], 16) / 0xFFFFFFFF

    angle = angle_seed * 2 * math.pi
    # Espalha as paragens numa area bem maior para reduzir sobreposicao visual.
    radius = 0.03 + radius_seed * 0.22

    lat = center_lat + radius * math.cos(angle)
    lon = center_lon + radius * math.sin(angle)
    return [round(lat, 6), round(lon, 6)]


def build_route_map_data(bus_routes):
    stop_routes = {}
    route_stops = {}
    route_tracks = []

    for route_id, route_data in bus_routes.items():
        df = route_to_dataframe(route_data)
        if "Stop_stn" not in df.columns:
            continue

        stops = df["Stop_stn"].dropna().astype(str).tolist()
        stops = [stop for stop in stops if stop]
        if len(stops) < 2:
            continue

        route_stops[route_id] = stops
        for stop in set(stops):
            stop_routes.setdefault(stop, set()).add(route_id)

    nodes_data = []
    for stop in sorted(stop_routes):
        usage = len(stop_routes[stop])
        shared = usage > 1
        coord = synthetic_stop_coord(stop)
        nodes_data.append(
            {
                "id": stop,
                "name": stop,
                "coord": coord,
                "usage": usage,
                "title": f"{stop} | usada por {usage} rota(s)",
                "shared": shared,
            }
        )

    edges_data = []
    for route_id, stops in route_stops.items():
        route_df = route_to_dataframe(bus_routes[route_id]).copy()
        route_df = route_df[route_df["Stop_stn"].notna()].reset_index(drop=True)
        schedule = prepare_route_schedule(route_df)

        coords = [synthetic_stop_coord(stop) for stop in stops if stop in stop_routes]
        if len(coords) < 2:
            continue

        simplified = [coords[0]]
        for point in coords[1:]:
            if point != simplified[-1]:
                simplified.append(point)

        if len(simplified) < 2:
            continue

        segment_seconds = [max(1.0, float(item["wait_seconds"])) for item in schedule[1:]]
        if len(segment_seconds) < len(simplified) - 1:
            segment_seconds.extend([90.0] * (len(simplified) - 1 - len(segment_seconds)))
        segment_seconds = segment_seconds[: len(simplified) - 1]

        route_tracks.append(
            {
                "routeId": route_id,
                "color": route_color(route_id),
                "coords": simplified,
                "segmentSeconds": segment_seconds,
                "title": f"Rota {route_id}",
            }
        )

        edges_data.append(
            {
                "routeId": route_id,
                "color": route_color(route_id),
                "coords": simplified,
                "title": f"Rota {route_id}",
            }
        )

    return nodes_data, edges_data, route_tracks, len(route_stops)


def save_routes_map(bus_routes, map_file=MAP_FILE):
    nodes_data, edges_data, route_tracks, route_count = build_route_map_data(bus_routes)

    html = f"""<!DOCTYPE html>
<html lang=\"pt\">
<head>
    <meta charset=\"UTF-8\" />
    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />
    <title>Mapa de Rotas</title>
    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />
    <script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>
    <style>
        body {{ margin: 0; font-family: sans-serif; background: #f4f7f9; }}
        #toolbar {{
            padding: 12px 16px;
            background: #0f172a;
            color: #e2e8f0;
            display: flex;
            gap: 10px;
            flex-wrap: wrap;
            align-items: center;
        }}
        #toolbar select, #toolbar button, #toolbar input {{ padding: 6px 10px; }}
        #map {{ width: 100%; height: calc(100vh - 64px); }}
    </style>
</head>
<body>
    <div id=\"toolbar\">
        <span>Mapa de Rotas | Rotas: {route_count} | Paragens unicas: {len(nodes_data)}</span>
        <label for=\"routeSelect\">Rota ativa</label>
        <select id=\"routeSelect\"></select>
        <button id=\"playBtn\">Play</button>
        <button id=\"pauseBtn\">Pause</button>
        <button id=\"resetBtn\">Reset</button>
        <label for=\"speedRange\">Velocidade</label>
        <input id=\"speedRange\" type=\"range\" min=\"20\" max=\"800\" value=\"300\" step=\"10\" />
        <span id=\"speedLabel\">x300</span>
        <span id=\"statusText\"></span>
    </div>
    <div id=\"map\"></div>
    <script>
        const nodes = {json.dumps(nodes_data, ensure_ascii=False)};
        const tracks = {json.dumps(route_tracks, ensure_ascii=False)};

        const map = L.map('map', {{ zoomControl: true }}).setView([38.7223, -9.1393], 10);

        L.tileLayer('https://tile.openstreetmap.org/{{z}}/{{x}}/{{y}}.png', {{
            maxZoom: 19,
            attribution: '&copy; OpenStreetMap contributors'
        }}).addTo(map);

        const stopsLayer = L.layerGroup().addTo(map);
        const activeLayer = L.layerGroup().addTo(map);

        const routeSelect = document.getElementById('routeSelect');
        const playBtn = document.getElementById('playBtn');
        const pauseBtn = document.getElementById('pauseBtn');
        const resetBtn = document.getElementById('resetBtn');
        const speedRange = document.getElementById('speedRange');
        const speedLabel = document.getElementById('speedLabel');
        const statusText = document.getElementById('statusText');

        function setStatus(text) {{
            statusText.textContent = text;
        }}

        function getSpeed() {{
            const value = Number(speedRange.value || 300);
            speedLabel.textContent = `x${{value}}`;
            return value;
        }}

        tracks.forEach(track => {{
            const option = document.createElement('option');
            option.value = track.routeId;
            option.textContent = track.routeId;
            routeSelect.appendChild(option);
        }});

        nodes.forEach(stop => {{
            const marker = L.circleMarker(stop.coord, {{
                radius: stop.shared ? 5 : 3,
                color: stop.shared ? '#b45309' : '#0369a1',
                fillColor: stop.shared ? '#f59e0b' : '#0ea5e9',
                fillOpacity: 0.9,
                weight: 1
            }});
            marker.bindTooltip(stop.title);
            marker.addTo(stopsLayer);
        }});

        let activeTrack = null;
        let activePolyline = null;
        let busMarker = null;
        let animationFrame = null;
        let playing = false;
        let playStartReal = null;
        let pausedSimElapsed = 0;

        function cumulativeDurations(track) {{
            const cumulative = [0];
            let total = 0;
            track.segmentSeconds.forEach(seconds => {{
                total += Math.max(1, Number(seconds || 1));
                cumulative.push(total);
            }});
            return cumulative;
        }}

        function positionAt(track, segmentIndex, progress) {{
            const start = track.coords[segmentIndex];
            const end = track.coords[segmentIndex + 1];
            return [
                start[0] + (end[0] - start[0]) * progress,
                start[1] + (end[1] - start[1]) * progress
            ];
        }}

        function renderTrack(track) {{
            activeLayer.clearLayers();
            activeTrack = track;
            const polyline = L.polyline(track.coords, {{
                color: track.color,
                weight: 6,
                opacity: 0.95
            }}).addTo(activeLayer);
            activePolyline = polyline;
            busMarker = L.circleMarker(track.coords[0], {{
                radius: 8,
                color: '#111827',
                weight: 2,
                fillColor: '#f97316',
                fillOpacity: 1.0
            }}).addTo(activeLayer);
            map.fitBounds(polyline.getBounds(), {{ padding: [40, 40] }});
            map.zoomIn(1);
            pausedSimElapsed = 0;
            playStartReal = null;
            playing = false;
            setStatus(`Rota ${{track.routeId}} pronta para simulacao.`);
        }}

        function currentSimElapsed() {{
            if (!activeTrack) return 0;
            if (!playing || playStartReal === null) return pausedSimElapsed;
            return pausedSimElapsed + ((performance.now() - playStartReal) / 1000) * getSpeed();
        }}

        function tick() {{
            if (!playing || !activeTrack || !busMarker) return;

            const cumulative = cumulativeDurations(activeTrack);
            const simElapsed = currentSimElapsed();
            const totalSim = cumulative[cumulative.length - 1] || 0;

            if (simElapsed >= totalSim) {{
                busMarker.setLatLng(activeTrack.coords[activeTrack.coords.length - 1]);
                setStatus(`Rota ${{activeTrack.routeId}} concluida.`);
                playing = false;
                animationFrame = null;
                return;
            }}

            let segmentIndex = 0;
            while (segmentIndex < cumulative.length - 1 && simElapsed >= cumulative[segmentIndex + 1]) {{
                segmentIndex += 1;
            }}

            const segmentStart = cumulative[segmentIndex];
            const segmentEnd = cumulative[segmentIndex + 1] || (segmentStart + 1);
            const progress = Math.min(1, Math.max(0, (simElapsed - segmentStart) / Math.max(1, segmentEnd - segmentStart)));
            const position = positionAt(activeTrack, segmentIndex, progress);
            busMarker.setLatLng(position);
            setStatus(`Rota ${{activeTrack.routeId}} | troco ${{segmentIndex + 1}}/${{activeTrack.coords.length - 1}} | ${{(progress * 100).toFixed(0)}}%`);

            animationFrame = requestAnimationFrame(tick);
        }}

        function play() {{
            if (!activeTrack) return;
            if (playing) return;
            playing = true;
            playStartReal = performance.now();
            setStatus(`A simular a rota ${{activeTrack.routeId}}...`);
            animationFrame = requestAnimationFrame(tick);
        }}

        function pause() {{
            if (!playing) return;
            pausedSimElapsed = currentSimElapsed();
            playing = false;
            playStartReal = null;
            if (animationFrame) cancelAnimationFrame(animationFrame);
            animationFrame = null;
            setStatus(`Simulacao da rota ${{activeTrack.routeId}} em pausa.`);
        }}

        function reset() {{
            if (!activeTrack) return;
            renderTrack(activeTrack);
            setStatus(`Rota ${{activeTrack.routeId}} reiniciada.`);
        }}

        routeSelect.addEventListener('change', event => {{
            const track = tracks.find(item => item.routeId === event.target.value);
            if (track) renderTrack(track);
        }});
        playBtn.addEventListener('click', play);
        pauseBtn.addEventListener('click', pause);
        resetBtn.addEventListener('click', reset);
        speedRange.addEventListener('input', () => {{
            getSpeed();
            if (playing) {{
                pausedSimElapsed = currentSimElapsed();
                playStartReal = performance.now();
            }}
        }});

        if (tracks.length > 0) {{
            routeSelect.value = tracks[0].routeId;
            renderTrack(tracks[0]);
            getSpeed();
        }}
    </script>
</body>
</html>
"""

    map_path = Path(map_file)
    map_path.write_text(html, encoding="utf-8")
    return map_path


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Processamento e simulacao de rotas de autocarro")
    parser.add_argument("--simulate", metavar="ROUTE_ID", help="Simula em tempo real uma rota especifica")
    parser.add_argument(
        "--speedup",
        type=float,
        default=120.0,
        help="Fator de aceleracao da simulacao (default: 120.0)",
    )
    parser.add_argument(
        "--max-wait",
        type=float,
        default=2.0,
        help="Espera maxima real por troco em segundos (default: 2.0)",
    )
    args = parser.parse_args()

    bus_routes = load_bus_routes()

    if args.simulate:
        simulate_route(args.simulate, bus_routes, speedup=args.speedup, max_wait=args.max_wait)
        raise SystemExit(0)

    output_path = save_routes_as_csv(bus_routes)
    map_path = save_routes_map(bus_routes)
    print(f"Pickle carregado com sucesso. Total de rotas: {len(bus_routes)}")
    print(f"Ficheiros gerados em: {output_path.resolve()}")
    print(f"Mapa gerado em: {map_path.resolve()}")