import io
import logging
import re
import time
import os
from pathlib import Path
import unicodedata
import zipfile
from functools import lru_cache

import pandas as pd
import requests
from pypdf import PdfReader

# Setup logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

routes = pd.read_csv("data/routes.csv")
stops = pd.read_csv("data/stops.csv")
zones = pd.read_csv("data/zones.csv")


def normalize_text(value):
    value = repair_text(value)
    value = unicodedata.normalize("NFKD", value)
    value = "".join(char for char in value if not unicodedata.combining(char))
    value = value.casefold()
    value = re.sub(r"[^\w\s]", " ", value)
    value = re.sub(r"\s+", " ", value).strip()
    return value


def repair_text(value):
    value = "" if pd.isna(value) else str(value)
    if any(marker in value for marker in ("Ã", "Â", "�")):
        try:
            value = value.encode("latin1").decode("utf-8")
        except (UnicodeEncodeError, UnicodeDecodeError):
            pass
    return value


def coerce_float(value):
    if pd.isna(value):
        return None

    text = str(value).strip()
    if not text:
        return None

    try:
        return float(text)
    except (TypeError, ValueError):
        return None


zone_code_lookup = {
    str(row["zone_id"]): repair_text(row["zone_code"])
    for _, row in zones.iterrows()
    if not pd.isna(row.get("zone_id")) and repair_text(row.get("zone_code"))
}


def resolve_zone_code(zone_code=None, zone_id=None):
    zone_text = repair_text(zone_code).strip().upper() if not pd.isna(zone_code) else ""
    if zone_text:
        return zone_text

    if pd.isna(zone_id):
        return "OUT"

    zone_id_text = repair_text(zone_id).strip()
    if not zone_id_text:
        return "OUT"

    if zone_id_text in zone_code_lookup:
        return zone_code_lookup[zone_id_text]

    try:
        zone_id_key = str(int(float(zone_id_text)))
    except (TypeError, ValueError):
        zone_id_key = zone_id_text

    return zone_code_lookup.get(zone_id_key, "OUT")


def build_stop_metadata_lookups(stops_df):
    lookup_by_name = {}
    lookup_by_code = {}

    for _, row in stops_df.iterrows():
        canonical_name = repair_text(row.get("name"))
        stop_code = repair_text(row.get("stop_code"))
        metadata = {
            "stop_name": canonical_name,
            "stop_code": stop_code,
            "latitude": coerce_float(row.get("latitude")),
            "longitude": coerce_float(row.get("longitude")),
            "zone_code": resolve_zone_code(row.get("zone_code"), row.get("zone_id")),
        }

        if canonical_name:
            lookup_by_name[normalize_text(canonical_name)] = metadata

        if stop_code:
            lookup_by_code[stop_code] = metadata

    return lookup_by_name, lookup_by_code


all_stop_lookup_by_name, all_stop_lookup_by_code = build_stop_metadata_lookups(stops)


metro_stops = stops[stops["stop_type"].astype(str).str.upper() == "METRO"].copy()
metro_stop_lookup_by_name, metro_stop_lookup_by_code = build_stop_metadata_lookups(metro_stops)

metro_stop_names = sorted(metro_stop_lookup_by_name.keys(), key=len, reverse=True)

METRO_ROUTE_PDFS = {
    "A": "https://appassets.mvtdev.com/map/191/l/1904/184573499.pdf",
    "B": "https://appassets.mvtdev.com/map/191/l/1904/184573500.pdf",
    "C": "https://appassets.mvtdev.com/map/191/l/1904/184573501.pdf",
    "D": "https://appassets.mvtdev.com/map/191/l/1904/184573502.pdf",
    "E": "https://appassets.mvtdev.com/map/191/l/1904/184573503.pdf",
    "F": "https://appassets.mvtdev.com/map/191/l/1904/184573504.pdf",
}

METRO_GTFS_ZIP = "https://www.metrodoporto.pt/metrodoporto/uploads/document/file/794/google_transit_31_03_2026.zip"

# Non-official enhanced CP GTFS (community-maintained releases)
CP_GTFS_ZIP = "https://github.com/joaodcp/cp-gtfs-enhanced/releases/download/2026.05.06/cp_gtfs_enhanced.zip"

train_stops = stops[stops["stop_type"].astype(str).str.upper() == "TRAIN"].copy()
train_stop_lookup_by_name, train_stop_lookup_by_code = build_stop_metadata_lookups(train_stops)
train_stop_names = sorted(train_stop_lookup_by_name.keys(), key=len, reverse=True)

TRAIN_ROUTE_PDFS = {
    "CP-Minho": "https://www.cp.pt/info/documents/d/cp/comboios-urbanos-porto-braga",
    "CP-Guimarães": "https://www.cp.pt/info/documents/d/cp/comboios-urbanos-porto-guimaraes",
    "CP-Aveiro": "https://www.cp.pt/info/documents/d/cp/comboios-urbanos-porto-aveiro",
}


@lru_cache(maxsize=1)
def fetch_zip_frames(zip_url):
    response = requests.get(zip_url, timeout=30, headers={"User-Agent": "Mozilla/5.0"})
    response.raise_for_status()

    with zipfile.ZipFile(io.BytesIO(response.content)) as archive:
        frames = {}
        for file_name in archive.namelist():
            if not file_name.endswith(".txt"):
                continue

            with archive.open(file_name) as handle:
                frames[file_name] = pd.read_csv(handle)

    return frames


def get_best_matching_stop_name(value, stop_lookup):
    normalized_value = normalize_text(value)
    if not normalized_value:
        return None, None

    if normalized_value in stop_lookup:
        return stop_lookup[normalized_value]

    for stop_key, stop_data in stop_lookup.items():
        if stop_key in normalized_value or normalized_value in stop_key:
            return stop_data

    return None, None


def get_best_matching_stop_metadata(value, stop_lookup):
    normalized_value = normalize_text(value)
    if not normalized_value:
        return None

    if normalized_value in stop_lookup:
        return stop_lookup[normalized_value]

    for stop_key, stop_data in stop_lookup.items():
        if stop_key in normalized_value or normalized_value in stop_key:
            return stop_data

    return None


def resolve_stop_metadata(stop_name=None, stop_code=None, latitude=None, longitude=None, zone_code=None, zone_id=None, stop_lookup_by_name=None, stop_lookup_by_code=None):
    metadata = {
        "stop_name": repair_text(stop_name) if stop_name is not None else None,
        "stop_code": repair_text(stop_code) if stop_code is not None else None,
        "latitude": coerce_float(latitude),
        "longitude": coerce_float(longitude),
        "zone_code": resolve_zone_code(zone_code, zone_id),
    }

    candidate = None
    if metadata["stop_code"] and stop_lookup_by_code:
        candidate = stop_lookup_by_code.get(metadata["stop_code"])

    if candidate is None and metadata["stop_name"] and stop_lookup_by_name:
        candidate = get_best_matching_stop_metadata(metadata["stop_name"], stop_lookup_by_name)

    if candidate:
        metadata["stop_name"] = metadata["stop_name"] or candidate["stop_name"]
        if not metadata["stop_code"]:
            metadata["stop_code"] = candidate["stop_code"]
        if metadata["latitude"] is None:
            metadata["latitude"] = candidate["latitude"]
        if metadata["longitude"] is None:
            metadata["longitude"] = candidate["longitude"]
        if metadata["zone_code"] == "OUT" and candidate["zone_code"]:
            metadata["zone_code"] = candidate["zone_code"]

    metadata["zone_code"] = metadata["zone_code"] or "OUT"
    return metadata

def extract_known_stop_names(pdf_text, stop_names, stop_lookup, start_marker=None, stop_markers=None):
    lines = [line.strip() for line in pdf_text.splitlines() if line.strip()]

    extracted_stop_names = []
    seen_stops = set()
    capture = start_marker is None
    stop_markers = stop_markers or ()

    for line in lines:
        if start_marker and line.startswith(start_marker):
            capture = True
            continue

        if capture and any(line.startswith(marker) for marker in stop_markers):
            break

        if not capture:
            continue

        normalized_line = normalize_text(line)
        for stop_key in stop_names:
            if stop_key in seen_stops:
                continue

            if normalized_line == stop_key or normalized_line.startswith(f"{stop_key} ") or normalized_line.startswith(f"{stop_key}-"):
                canonical_name = stop_lookup[stop_key]["stop_name"]
                extracted_stop_names.append(canonical_name)
                seen_stops.add(stop_key)

    return extracted_stop_names


def extract_train_stop_names(pdf_text):
    return extract_known_stop_names(
        pdf_text,
        train_stop_names,
        train_stop_lookup_by_name,
        start_marker=None,
        stop_markers=(),
    )


def extract_metro_stop_names_from_gtfs(route_id):
    frames = fetch_zip_frames(METRO_GTFS_ZIP)
    routes_frame = frames.get("routes.txt", pd.DataFrame())
    trips_frame = frames.get("trips.txt", pd.DataFrame())
    stop_times_frame = frames.get("stop_times.txt", pd.DataFrame())
    stops_frame = frames.get("stops.txt", pd.DataFrame())

    if routes_frame.empty or trips_frame.empty or stop_times_frame.empty or stops_frame.empty:
        return []

    if "route_short_name" in routes_frame.columns:
        selected_routes = routes_frame[routes_frame["route_short_name"].astype(str).str.upper() == route_id.upper()]
    else:
        selected_routes = routes_frame[routes_frame.get("route_id", pd.Series(dtype=str)).astype(str).str.upper() == route_id.upper()]

    if selected_routes.empty:
        return []

    route_key = selected_routes.iloc[0].get("route_id", route_id)
    selected_trips = trips_frame[trips_frame["route_id"].astype(str) == str(route_key)]

    if selected_trips.empty:
        return []

    preferred_trips = selected_trips
    if "direction_id" in selected_trips.columns:
        direction_zero = selected_trips[selected_trips["direction_id"].fillna(0).astype(int) == 0]
        if not direction_zero.empty:
            preferred_trips = direction_zero

    trip_id = preferred_trips.iloc[0]["trip_id"]
    selected_stop_times = stop_times_frame[stop_times_frame["trip_id"].astype(str) == str(trip_id)].copy()

    if selected_stop_times.empty:
        return []

    selected_stop_times["stop_sequence"] = pd.to_numeric(selected_stop_times["stop_sequence"], errors="coerce")
    selected_stop_times = selected_stop_times.sort_values(["stop_sequence", "stop_id"])

    merged = selected_stop_times.merge(stops_frame, on="stop_id", how="left", suffixes=("", "_gtfs"))

    stop_metadata = []
    seen_stop_names = set()
    for _, row in merged.iterrows():
        stop_name = repair_text(row.get("stop_name"))
        metadata = resolve_stop_metadata(
            stop_name=stop_name,
            latitude=row.get("stop_lat", row.get("latitude")),
            longitude=row.get("stop_lon", row.get("longitude")),
            zone_code=row.get("zone_code"),
            zone_id=row.get("zone_id"),
            stop_lookup_by_name=metro_stop_lookup_by_name,
            stop_lookup_by_code=metro_stop_lookup_by_code,
        )
        normalized_name = normalize_text(metadata["stop_name"] or stop_name)
        if normalized_name in seen_stop_names:
            continue
        stop_metadata.append(metadata)
        seen_stop_names.add(normalized_name)

    return stop_metadata


def extract_train_stop_names_from_gtfs(route_id):
    frames = fetch_zip_frames(CP_GTFS_ZIP)
    routes_frame = frames.get("routes.txt", pd.DataFrame())
    trips_frame = frames.get("trips.txt", pd.DataFrame())
    stop_times_frame = frames.get("stop_times.txt", pd.DataFrame())
    stops_frame = frames.get("stops.txt", pd.DataFrame())

    if routes_frame.empty or trips_frame.empty or stop_times_frame.empty or stops_frame.empty:
        return []

    rid_norm = str(route_id).casefold()

    # Try matching by short name, long name or route_id (case-insensitive)
    candidates = pd.DataFrame()
    if "route_short_name" in routes_frame.columns:
        candidates = routes_frame[routes_frame["route_short_name"].astype(str).str.casefold() == rid_norm]

    if candidates.empty and "route_long_name" in routes_frame.columns:
        candidates = routes_frame[routes_frame["route_long_name"].astype(str).str.casefold().str.contains(rid_norm, na=False)]

    if candidates.empty:
        candidates = routes_frame[routes_frame.get("route_id", pd.Series(dtype=str)).astype(str).str.casefold() == rid_norm]

    if candidates.empty:
        # try contains in short name as a last resort
        if "route_short_name" in routes_frame.columns:
            candidates = routes_frame[routes_frame["route_short_name"].astype(str).str.casefold().str.contains(rid_norm, na=False)]

    if candidates.empty:
        return []

    route_key = candidates.iloc[0].get("route_id")
    selected_trips = trips_frame[trips_frame["route_id"].astype(str) == str(route_key)]
    if selected_trips.empty:
        return []

    preferred_trips = selected_trips
    if "direction_id" in selected_trips.columns:
        direction_zero = selected_trips[selected_trips["direction_id"].fillna(0).astype(int) == 0]
        if not direction_zero.empty:
            preferred_trips = direction_zero

    trip_id = preferred_trips.iloc[0]["trip_id"]
    selected_stop_times = stop_times_frame[stop_times_frame["trip_id"].astype(str) == str(trip_id)].copy()
    if selected_stop_times.empty:
        return []

    selected_stop_times["stop_sequence"] = pd.to_numeric(selected_stop_times.get("stop_sequence", selected_stop_times.get("stop_times_sequence", pd.Series())), errors="coerce")
    selected_stop_times = selected_stop_times.sort_values(["stop_sequence", "stop_id"])

    merged = selected_stop_times.merge(stops_frame, on="stop_id", how="left", suffixes=("", "_gtfs"))

    stop_metadata = []
    seen_stop_names = set()
    for _, row in merged.iterrows():
        stop_name = repair_text(row.get("stop_name"))
        metadata = resolve_stop_metadata(
            stop_name=stop_name,
            latitude=row.get("stop_lat", row.get("latitude")),
            longitude=row.get("stop_lon", row.get("longitude")),
            zone_code=row.get("zone_code"),
            zone_id=row.get("zone_id"),
            stop_lookup_by_name=train_stop_lookup_by_name,
            stop_lookup_by_code=train_stop_lookup_by_code,
        )
        normalized_name = normalize_text(metadata["stop_name"] or stop_name)
        if normalized_name in seen_stop_names:
            continue
        stop_metadata.append(metadata)
        seen_stop_names.add(normalized_name)

    return stop_metadata


@lru_cache(maxsize=16)
def fetch_pdf_text(pdf_url):
    response = requests.get(pdf_url, timeout=20, headers={"User-Agent": "Mozilla/5.0"})
    response.raise_for_status()

    reader = PdfReader(io.BytesIO(response.content))
    return "\n".join(page.extract_text() or "" for page in reader.pages)


def extract_metro_stop_names(pdf_text):
    return extract_known_stop_names(
        pdf_text,
        metro_stop_names,
        metro_stop_lookup_by_name,
        start_marker="Horários e mapa de metro da linha",
        stop_markers=("©",),
    )

# **********************************
#                       BUS
# **********************************

# where code is numeric
bus_routes = routes[routes['code'].apply(lambda x: str(x).isnumeric())]

def _load_scraped_bus_routes(checkpoint_path="data/scraped_bus_routes.txt"):
    seen = set()
    # read explicit checkpoint file
    try:
        if os.path.exists(checkpoint_path):
            with open(checkpoint_path, "r", encoding="utf-8") as fh:
                for line in fh:
                    line = line.strip()
                    if line:
                        seen.add(line)
    except Exception:
        pass

    # also read existing combined output if present for extra safety
    combined_path = Path("data/route_stops_test.csv")
    if combined_path.exists():
        try:
            df_existing = pd.read_csv(combined_path)
            if "route_id" in df_existing.columns and "transport_type" in df_existing.columns:
                bus_ids = df_existing[df_existing["transport_type"].astype(str).str.upper() == "BUS"]["route_id"].astype(str).unique()
                seen.update(map(str, bus_ids))
        except Exception:
            pass

    return seen


def _append_scraped_bus_route(route_id, checkpoint_path="data/scraped_bus_routes.txt"):
    try:
        os.makedirs(os.path.dirname(checkpoint_path), exist_ok=True)
        with open(checkpoint_path, "a", encoding="utf-8") as fh:
            fh.write(f"{route_id}\n")
    except Exception:
        pass


def _load_bus_stop_codes_from_schedule(schedule_path="data/schedule_scrapped_1.csv"):
    schedule_file = Path(schedule_path)
    if not schedule_file.exists():
        return {}

    try:
        schedule_df = pd.read_csv(schedule_file)
    except Exception:
        return {}

    required_columns = {"route_id", "stop_code"}
    if not required_columns.issubset(schedule_df.columns):
        return {}

    if "direction" in schedule_df.columns:
        schedule_df = schedule_df[schedule_df["direction"].fillna(0).astype(str) == "0"]

    schedule_df = schedule_df.dropna(subset=["route_id", "stop_code"])
    bus_stop_codes = {}
    for route_id, group in schedule_df.groupby(schedule_df["route_id"].astype(str), sort=False):
        ordered_codes = []
        seen_codes = set()
        for stop_code in group["stop_code"].astype(str):
            stop_code = repair_text(stop_code).strip()
            if not stop_code or stop_code in seen_codes:
                continue
            ordered_codes.append(stop_code)
            seen_codes.add(stop_code)
        if ordered_codes:
            bus_stop_codes[str(route_id)] = ordered_codes

    return bus_stop_codes


def scrap_bus_routes(delay=1.5, max_attempts=3, backoff_factor=1.0, checkpoint_path="data/scraped_bus_routes.txt"):
    """
    Scrapes bus routes and stops from STCP API.
    Returns a dataframe with: route_id, stop_code, stop_name, latitude, longitude, zone_code, sequence, transport_type
    
    Args:
        delay (float): Delay in seconds between API requests to avoid rate limiting
        
    Returns:
        pd.DataFrame: DataFrame with columns [route_id, stop_code, stop_name, latitude, longitude, zone_code, sequence, transport_type]
    """
    url = "https://stcp.pt/api/route/{route_id}/stops/direction?direction_id=0"

    # List to store all route-stop relationships
    route_stops_list = []

    # Determine which routes are already scraped
    already_scraped = _load_scraped_bus_routes(checkpoint_path=checkpoint_path)
    real_bus_stop_codes = _load_bus_stop_codes_from_schedule()

    total = len(bus_routes)
    for idx, route_id in enumerate(bus_routes['code'], 1):
        route_id_str = str(route_id)
        if route_id_str in already_scraped:
            logging.info(f"[{idx}/{total}] Route {route_id}: Skipping (already scraped)")
            continue

        attempt = 0
        success = False
        while attempt < max_attempts and not success:
            attempt += 1
            try:
                logging.info(f"[{idx}/{total}] Route {route_id}: attempt {attempt}/{max_attempts}")
                response = requests.get(url.format(route_id=route_id), timeout=10)

                if response.status_code == 200:
                    data = response.json()
                    stops = data.get('stops', [])
                    logging.info(f"[{idx}/{total}] Route {route_id}: Found {len(stops)} stops")

                    for stop in stops:
                        sequence = stop.get('stop_sequence')
                        sequence_index = None
                        try:
                            sequence_index = int(float(sequence)) - 1 if sequence is not None else None
                        except (TypeError, ValueError):
                            sequence_index = None

                        schedule_codes = real_bus_stop_codes.get(route_id_str, [])
                        schedule_stop_code = None
                        if sequence_index is not None and 0 <= sequence_index < len(schedule_codes):
                            schedule_stop_code = schedule_codes[sequence_index]

                        source_stop_code = repair_text(stop.get('stop_code'))
                        resolved_stop_code = schedule_stop_code or source_stop_code

                        metadata = resolve_stop_metadata(
                            stop_name=stop.get('stop_name'),
                            stop_code=resolved_stop_code,
                            latitude=stop.get('stop_lat', stop.get('latitude')),
                            longitude=stop.get('stop_lon', stop.get('longitude')),
                            zone_code=stop.get('zone_code'),
                            zone_id=stop.get('zone_id'),
                            stop_lookup_by_name=all_stop_lookup_by_name,
                            stop_lookup_by_code=all_stop_lookup_by_code,
                        )

                        if not metadata['stop_code'] and resolved_stop_code:
                            metadata['stop_code'] = resolved_stop_code

                        route_stops_list.append({
                            'route_id': route_id,
                            'stop_code': metadata['stop_code'],
                            'stop_name': metadata['stop_name'],
                            'latitude': metadata['latitude'],
                            'longitude': metadata['longitude'],
                            'zone_code': metadata['zone_code'],
                            'sequence': stop.get('stop_sequence'),
                            'transport_type': 'BUS'
                        })

                    # mark success and persist checkpoint
                    success = True
                    _append_scraped_bus_route(route_id_str, checkpoint_path=checkpoint_path)

                    # polite delay between successful requests
                    time.sleep(delay)
                else:
                    logging.warning(f"Route {route_id}: Failed with status code {response.status_code}")
                    # on non-200 we may retry
                    if attempt < max_attempts:
                        backoff = backoff_factor * (2 ** (attempt - 1))
                        logging.info(f"Route {route_id}: retrying after {backoff}s")
                        time.sleep(backoff)

            except requests.exceptions.RequestException as e:
                logging.error(f"Route {route_id}: Request Error - {str(e)}")
                if attempt < max_attempts:
                    backoff = backoff_factor * (2 ** (attempt - 1))
                    logging.info(f"Route {route_id}: retrying after {backoff}s")
                    time.sleep(backoff)
            except Exception as e:
                logging.error(f"Route {route_id}: Unexpected error - {str(e)}")
                break

        if not success:
            logging.warning(f"Route {route_id}: Failed after {max_attempts} attempts")

    # Create dataframe from the collected data
    if route_stops_list:
        df_bus_routes = pd.DataFrame(route_stops_list)
        logging.info(f"Total BUS route-stop relationships collected: {len(df_bus_routes)}")
        return df_bus_routes
    else:
        logging.warning("No BUS data collected from API")
        return pd.DataFrame(columns=['route_id', 'stop_code', 'stop_name', 'latitude', 'longitude', 'zone_code', 'sequence', 'transport_type'])


# **********************************
#                       METRO
# **********************************

metro_routes = routes[routes['code'].isin(METRO_ROUTE_PDFS.keys())]

def scrap_metro_routes(delay=0.5):
    """
    Scrapes metro routes and stops from the linked route-map PDF.
    Returns a dataframe with: route_id, stop_code, stop_name, latitude, longitude, zone_code, sequence, transport_type
    
    Args:
        delay (float): Delay in seconds between API requests
        
    Returns:
        pd.DataFrame: DataFrame with columns [route_id, stop_code, stop_name, latitude, longitude, zone_code, sequence, transport_type]
    """
    route_stops_list = []
    
    logging.info(f"Starting to scrape {len(metro_routes)} metro routes")
    
    for idx, route_id in enumerate(metro_routes['code'], 1):
        try:
            stop_metadata_list = extract_metro_stop_names_from_gtfs(route_id)

            if not stop_metadata_list:
                pdf_url = METRO_ROUTE_PDFS.get(route_id)
                if not pdf_url:
                    logging.warning(f"Metro {route_id}: No Metro source configured")
                    continue

                pdf_text = fetch_pdf_text(pdf_url)
                stop_names = extract_metro_stop_names(pdf_text)
                stop_metadata_list = [
                    resolve_stop_metadata(
                        stop_name=stop_name,
                        stop_lookup_by_name=metro_stop_lookup_by_name,
                        stop_lookup_by_code=metro_stop_lookup_by_code,
                    )
                    for stop_name in stop_names
                ]

            if not stop_metadata_list:
                logging.warning(f"Metro {route_id}: No stops extracted from source")
                continue

            logging.info(f"[{idx}/{len(metro_routes)}] Metro {route_id}: Found {len(stop_metadata_list)} stops")

            for sequence, metadata in enumerate(stop_metadata_list, 1):
                route_stops_list.append({
                    'route_id': route_id,
                    'stop_code': metadata['stop_code'],
                    'stop_name': metadata['stop_name'],
                    'latitude': metadata['latitude'],
                    'longitude': metadata['longitude'],
                    'zone_code': metadata['zone_code'],
                    'sequence': sequence,
                    'transport_type': 'METRO'
                })

            time.sleep(delay)

        except Exception as e:
            logging.error(f"Metro {route_id}: Error - {str(e)}")

    if route_stops_list:
        df_metro_routes = pd.DataFrame(route_stops_list)
        logging.info(f"Total METRO route-stop relationships collected: {len(df_metro_routes)}")
        return df_metro_routes

    logging.warning("No METRO data collected")
    return pd.DataFrame(columns=['route_id', 'stop_code', 'stop_name', 'latitude', 'longitude', 'zone_code', 'sequence', 'transport_type'])



# **********************************
#                       TRAIN
# **********************************

train_routes = routes[routes['code'].isin(TRAIN_ROUTE_PDFS.keys())]

def scrap_train_routes(delay=0.5):
    """
    Scrapes train routes and stops from CP PDF timetables.
    Returns a dataframe with: route_id, stop_code, stop_name, latitude, longitude, zone_code, sequence, transport_type
    
    Args:
        delay (float): Delay in seconds between API requests
        
    Returns:
        pd.DataFrame: DataFrame with columns [route_id, stop_code, stop_name, latitude, longitude, zone_code, sequence, transport_type]
    """
    route_stops_list = []
    
    logging.info(f"Starting to scrape {len(train_routes)} train routes")
    
    for idx, route_id in enumerate(train_routes['code'], 1):
        try:
            # Prefer GTFS-based extraction if available (community-maintained CP GTFS)
            stop_metadata_list = extract_train_stop_names_from_gtfs(route_id)

            # Fallback to PDF scraping if GTFS didn't yield results
            if not stop_metadata_list:
                pdf_url = TRAIN_ROUTE_PDFS.get(route_id)
                if not pdf_url:
                    logging.warning(f"Train {route_id}: No source configured")
                    continue

                pdf_text = fetch_pdf_text(pdf_url)
                stop_names = extract_train_stop_names(pdf_text)
                stop_metadata_list = [
                    resolve_stop_metadata(
                        stop_name=stop_name,
                        stop_lookup_by_name=train_stop_lookup_by_name,
                        stop_lookup_by_code=train_stop_lookup_by_code,
                    )
                    for stop_name in stop_names
                ]

                if not stop_metadata_list:
                    logging.warning(f"Train {route_id}: No stops extracted from PDF")
                    continue

            logging.info(f"[{idx}/{len(train_routes)}] Train {route_id}: Found {len(stop_metadata_list)} stops")

            for sequence, metadata in enumerate(stop_metadata_list, 1):
                route_stops_list.append({
                    'route_id': route_id,
                    'stop_code': metadata['stop_code'],
                    'stop_name': metadata['stop_name'],
                    'latitude': metadata['latitude'],
                    'longitude': metadata['longitude'],
                    'zone_code': metadata['zone_code'],
                    'sequence': sequence,
                    'transport_type': 'TRAIN'
                })

            time.sleep(delay)

        except Exception as e:
            logging.error(f"Train {route_id}: Error - {str(e)}")
    
    if route_stops_list:
        df_train_routes = pd.DataFrame(route_stops_list)
        logging.info(f"Total TRAIN route-stop relationships collected: {len(df_train_routes)}")
        return df_train_routes
    else:
        logging.warning("No TRAIN data collected")
        return pd.DataFrame(columns=['route_id', 'stop_code', 'stop_name', 'latitude', 'longitude', 'zone_code', 'sequence', 'transport_type'])


# **********************************
#                   MAIN EXECUTION
# **********************************

def combine_all_transport():
    """
    Combines bus, metro, and train route-stop data into a single dataframe.
    
    Returns:
        pd.DataFrame: Combined dataframe with all transport types
    """
    logging.info("="*80)
    logging.info("Starting data collection from all transport providers")
    logging.info("="*80)
    
    # Scrap all transport types
    df_bus = scrap_bus_routes()
    df_metro = scrap_metro_routes(delay=0.5)
    df_train = scrap_train_routes(delay=0.5)
    
    # Combine all dataframes
    df_all = pd.concat([df_bus, df_metro, df_train], ignore_index=True)
    
    logging.info("="*80)
    logging.info(f"TOTAL route-stop relationships: {len(df_all)}")
    logging.info(f"  - BUS: {len(df_bus)}")
    logging.info(f"  - METRO: {len(df_metro)}")
    logging.info(f"  - TRAIN: {len(df_train)}")
    logging.info("="*80)
    
    return df_all


if __name__ == "__main__":
    # Execute the scraping and combine results
    df_route_stops = combine_all_transport()
    
    # Save to CSV
    output_file = "data/route_stops_test.csv"
    df_route_stops.to_csv(output_file, index=False)
    logging.info(f"Data saved to {output_file}")
    
    # Display summary
    print("\nDataFrame Summary:")
    print(df_route_stops.head(20))
    print(f"\nShape: {df_route_stops.shape}")
    print(f"\nColumns: {df_route_stops.columns.tolist()}")
    print(f"\nTransport types: {df_route_stops['transport_type'].unique()}")
    print(f"\nSample data:\n{df_route_stops.sample(min(10, len(df_route_stops)))}")