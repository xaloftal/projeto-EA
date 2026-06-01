import io
import logging
import re
import unicodedata
import zipfile
from functools import lru_cache
from pathlib import Path

import pandas as pd
import requests
try:
    from routes_stops_scrapper import TRAIN_ROUTE_PDFS, fetch_pdf_text, extract_train_stop_names
except Exception:
    TRAIN_ROUTE_PDFS = {}
    def fetch_pdf_text(url):
        raise RuntimeError("routes_stops_scrapper not available")
    def extract_train_stop_names(text):
        return []


logging.basicConfig(level=logging.DEBUG, format="%(asctime)s - %(levelname)s - %(message)s")


BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"
ROUTE_STOPS_FILE = DATA_DIR / "route_stops_test.csv"
STOPS_FILE = DATA_DIR / "stops.csv"
OUTPUT_FILE = DATA_DIR / "schedule.csv"
ROUTE_GTFS_MAP_FILE = DATA_DIR / "route_gtfs_map.csv"

METRO_GTFS_ZIP = "https://www.metrodoporto.pt/metrodoporto/uploads/document/file/794/google_transit_31_03_2026.zip"
CP_GTFS_ZIP = "https://publico.cp.pt/gtfs/gtfs.zip"
# Community-enhanced CP GTFS (more descriptive route names)
CP_GTFS_ENHANCED = "https://github.com/joaodcp/cp-gtfs-enhanced/releases/download/2026.05.06/cp_gtfs_enhanced.zip"
STCP_ROUTE_SCHEDULE_URL = "https://stcp.pt/api/route/{route_id}/schedule"
TRANSITLAND_CP_FEED = "https://transit.land/feeds/f-eyc-cp/download"
# Transitland page lists a historic GTFS archive hosted by TransporLis — try as fallback
TRANSPORLIS_GTFS = "http://www.transporlis.pt/Portals/0/OpenData/gtfs/zip/3/gtfs_3.zip"

# Feature flags: control which providers are fetched
FETCH_TRAIN = True
FETCH_METRO = True
FETCH_BUS = True
# Prefer train PDF/webscraping before GTFS when True
TRAIN_FIRST = True

REQUEST_HEADERS = {
    "User-Agent": "Mozilla/5.0",
    "Accept": "application/json, text/plain, */*",
}


def repair_text(value):
    if pd.isna(value):
        return ""

    text = str(value)
    if any(marker in text for marker in ("Ã", "Â", "�")):
        try:
            text = text.encode("latin1").decode("utf-8")
        except (UnicodeEncodeError, UnicodeDecodeError):
            pass

    return text


def normalize_text(value):
    text = repair_text(value)
    text = unicodedata.normalize("NFKD", text)
    text = "".join(char for char in text if not unicodedata.combining(char))
    text = text.casefold()
    text = re.sub(r"[^\w\s]", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def read_csv(path):
    if not path.exists():
        raise FileNotFoundError(f"Missing required file: {path}")
    return pd.read_csv(path)


def parse_time_to_seconds(value):
    if value is None or pd.isna(value):
        return None

    text = str(value).strip()
    if not text:
        return None

    match = re.match(r"^(\d+):([0-5]\d)(?::([0-5]\d))?$", text)
    if not match:
        return None

    hours = int(match.group(1))
    minutes = int(match.group(2))
    seconds = int(match.group(3)) if match.group(3) is not None else 0
    return hours * 3600 + minutes * 60 + seconds


def seconds_to_time_string(seconds):
    if seconds is None or pd.isna(seconds):
        return None

    total_seconds = int(round(float(seconds)))
    if total_seconds < 0:
        total_seconds = 0

    hours, remainder = divmod(total_seconds, 3600)
    minutes, secs = divmod(remainder, 60)
    return f"{hours:02d}:{minutes:02d}:{secs:02d}"


def first_valid_time(*values):
    for value in values:
        seconds = parse_time_to_seconds(value)
        if seconds is not None:
            return seconds
    return None


def load_route_stops():
    route_stops = read_csv(ROUTE_STOPS_FILE)
    route_stops["route_id"] = route_stops["route_id"].astype(str)
    route_stops["stop_code"] = route_stops["stop_code"].astype(str)
    route_stops["sequence"] = pd.to_numeric(route_stops["sequence"], errors="coerce")
    route_stops["transport_type"] = route_stops["transport_type"].astype(str).str.upper()
    route_stops = route_stops.dropna(subset=["sequence"])
    return route_stops


def build_stop_lookup(stops_df):
    lookup = {}
    for _, row in stops_df.iterrows():
        transport_type = str(row.get("stop_type", "")).upper()
        stop_name = repair_text(row.get("name"))
        stop_code = repair_text(row.get("stop_code"))
        if not transport_type or not stop_name or not stop_code:
            continue

        transport_lookup = lookup.setdefault(transport_type, {})
        normalized_name = normalize_text(stop_name)
        transport_lookup.setdefault(normalized_name, stop_code)
        transport_lookup.setdefault(normalize_text(stop_code), stop_code)

    return lookup


def find_stop_code_by_name(stop_name, transport_type, stop_lookup):
    transport_lookup = stop_lookup.get(transport_type.upper(), {})
    normalized_name = normalize_text(stop_name)
    if not normalized_name:
        return None

    if normalized_name in transport_lookup:
        return transport_lookup[normalized_name]

    candidates = []
    for key, code in transport_lookup.items():
        if key == normalized_name or key in normalized_name or normalized_name in key:
            candidates.append((abs(len(key) - len(normalized_name)), code))

    if candidates:
        candidates.sort(key=lambda item: item[0])
        return candidates[0][1]

    return None


def route_sequence_map(route_stops):
    route_map = {}
    for route_id, group in route_stops.groupby("route_id"):
        ordered = group.sort_values(["sequence", "stop_code"])
        route_map[str(route_id)] = {
            "transport_type": str(ordered.iloc[0]["transport_type"]).upper(),
            "stop_codes": list(ordered["stop_code"].astype(str)),
            "sequence_by_stop_code": {
                str(row["stop_code"]): int(row["sequence"]) for _, row in ordered.iterrows()
            },
        }

    return route_map

@lru_cache(maxsize=1)
def fetch_zip_frames(zip_url):
    response = requests.get(zip_url, timeout=60, headers={"User-Agent": "Mozilla/5.0"})
    response.raise_for_status()

    frames = {}
    with zipfile.ZipFile(io.BytesIO(response.content)) as archive:
        for file_name in archive.namelist():
            if not file_name.endswith(".txt"):
                continue

            with archive.open(file_name) as handle:
                frames[Path(file_name).name] = pd.read_csv(handle)

    return frames


def fetch_frames_from_sources(sources):
    """Try multiple GTFS zip URLs in order and return the first usable frames dict and its source URL."""
    if isinstance(sources, str):
        try:
            frames = fetch_zip_frames(sources)
            return frames, sources
        except Exception:
            return {}, None

    for src in sources:
        try:
            frames = fetch_zip_frames(src)
            # basic validity check
            if frames and "routes.txt" in frames and not frames.get("routes.txt", pd.DataFrame()).empty:
                return frames, src
        except Exception as exc:
            logging.debug("GTFS source %s failed: %s", src, exc)

    return {}, None


@lru_cache(maxsize=64)
def fetch_json(url):
    response = requests.get(url, timeout=12, headers=REQUEST_HEADERS)
    response.raise_for_status()
    return response.json()


@lru_cache(maxsize=1)
def load_route_gtfs_mapping():
    if not ROUTE_GTFS_MAP_FILE.exists():
        return {}

    mapping_df = pd.read_csv(ROUTE_GTFS_MAP_FILE)
    if not {"route_id", "gtfs_route_id"}.issubset(mapping_df.columns):
        return {}

    mapping = {}
    for _, row in mapping_df.iterrows():
        route_key = str(row.get("route_id", "")).strip()
        gtfs_key = str(row.get("gtfs_route_id", "")).strip()
        if route_key and gtfs_key:
            mapping[route_key] = gtfs_key
    return mapping


def weekday_service_ids(calendar_frame):
    if calendar_frame is None or calendar_frame.empty:
        return set()
    required_columns = {"service_id", "monday", "tuesday", "wednesday", "thursday", "friday"}
    if not required_columns.issubset(calendar_frame.columns):
        return set()

    def truthy(val):
        return str(val).strip() in {"1", "True", "true", "1.0"}

    weekday_services = set()
    for _, row in calendar_frame.iterrows():
        try:
            days = [row.get(c) for c in ("monday", "tuesday", "wednesday", "thursday", "friday")]
            true_count = sum(1 for d in days if truthy(d))
            if true_count >= 3:
                weekday_services.add(str(row.get("service_id")))
        except Exception:
            continue

    return weekday_services


def build_gtfs_stop_code_lookup(stops_frame, transport_type, stop_lookup):
    gtfs_stop_code_lookup = {}
    for _, row in stops_frame.iterrows():
        stop_name = row.get("stop_name")
        if pd.isna(stop_name):
            continue

        stop_code = find_stop_code_by_name(stop_name, transport_type, stop_lookup)
        if stop_code:
            gtfs_stop_code_lookup[str(row.get("stop_id"))] = stop_code

    return gtfs_stop_code_lookup


def expand_gtfs_route_rows(routes_frame, selected_rows):
    if selected_rows.empty:
        return selected_rows

    first_row = selected_rows.iloc[0]
    route_short_name = str(first_row.get("route_short_name", "")).strip()
    route_long_name = str(first_row.get("route_long_name", "")).strip()
    route_id_value = str(first_row.get("route_id", "")).strip()

    expanded = selected_rows
    if route_short_name and "route_short_name" in routes_frame.columns:
        expanded = pd.concat(
            [expanded, routes_frame[routes_frame["route_short_name"].astype(str).str.strip() == route_short_name]],
            ignore_index=True,
        )
    if route_long_name and "route_long_name" in routes_frame.columns:
        expanded = pd.concat(
            [expanded, routes_frame[routes_frame["route_long_name"].astype(str).str.strip() == route_long_name]],
            ignore_index=True,
        )
    if route_id_value and "route_id" in routes_frame.columns:
        expanded = pd.concat(
            [expanded, routes_frame[routes_frame["route_id"].astype(str).str.strip() == route_id_value]],
            ignore_index=True,
        )

    return expanded.drop_duplicates().reset_index(drop=True)


def find_gtfs_route_rows(routes_frame, route_id):
    if routes_frame.empty:
        return routes_frame

    # If user provided an explicit mapping from our route_id -> GTFS route identifier, honor it first
    try:
        mapped = load_route_gtfs_mapping().get(str(route_id))
        if mapped:
            candidates = routes_frame[
                (routes_frame.get("route_id", pd.Series(dtype=str)).astype(str) == mapped)
                | (routes_frame.get("route_short_name", pd.Series(dtype=str)).astype(str) == mapped)
                | (routes_frame.get("route_long_name", pd.Series(dtype=str)).astype(str) == mapped)
            ]
            if not candidates.empty:
                return expand_gtfs_route_rows(routes_frame, candidates.iloc[[0]])
    except Exception:
        pass

    normalized_target = normalize_text(route_id)

    # Build alternative targets: strip common prefixes like 'CP-', take suffixes,
    # and try last token variants to improve matching against GTFS short/long names.
    alt_targets = [normalized_target]
    try:
        import re as _re

        stripped = _re.sub(r'^(cp[-_\s]+)', '', route_id, flags=_re.I).strip()
        if stripped:
            alt_targets.append(normalize_text(stripped))

        if "-" in route_id:
            tail = route_id.split("-")[-1]
            alt_targets.append(normalize_text(tail))

        if " " in route_id:
            last_tok = route_id.split()[-1]
            alt_targets.append(normalize_text(last_tok))

        alt_targets.append(normalize_text(route_id.replace('-', ' ')))
    except Exception:
        pass

    # Deduplicate
    alt_targets = list(dict.fromkeys([t for t in alt_targets if t]))

    candidate_rows = []

    for _, row in routes_frame.iterrows():
        score = None
        for column in ("route_short_name", "route_long_name", "route_desc", "route_id"):
            if column not in routes_frame.columns:
                continue

            value = row.get(column)
            if pd.isna(value):
                continue

            normalized_value = normalize_text(value)
            if not normalized_value:
                continue

            # exact or close match to any alt target
            for target in alt_targets:
                if normalized_value == target:
                    score = 0
                    break

                if target in normalized_value or normalized_value in target:
                    current_score = abs(len(normalized_value) - len(target)) + 1
                    score = current_score if score is None else min(score, current_score)

            if score == 0:
                break

        if score is not None:
            candidate_rows.append((score, row))

    if not candidate_rows:
        try:
            logging.debug("GTFS route lookup alt_targets for %s: %s", route_id, alt_targets)
            sample = []
            for col in ("route_short_name", "route_long_name", "route_id"):
                if col in routes_frame.columns:
                    sample.extend(list(routes_frame[col].dropna().astype(str).head(10)))
            logging.debug("GTFS route lookup: no candidates for %s, sample GTFS rows: %s", route_id, sample)
        except Exception:
            pass

        # Try looser token-based matching and fuzzy matching as a fallback
        try:
            import difflib

            tokens = [t for t in normalized_target.split() if t]
            fuzzy_candidates = []

            for _, row in routes_frame.iterrows():
                for column in ("route_short_name", "route_long_name", "route_desc", "route_id"):
                    if column not in routes_frame.columns:
                        continue

                    value = row.get(column)
                    if pd.isna(value):
                        continue

                    normalized_value = normalize_text(value)
                    if not normalized_value:
                        continue

                    # token intersection score (prefer more intersection)
                    val_tokens = set(normalized_value.split())
                    overlap = sum(1 for tk in tokens if tk in val_tokens)
                    if overlap > 0:
                        score = -overlap
                        fuzzy_candidates.append((score, row))
                        break

                    # fuzzy ratio
                    ratio = difflib.SequenceMatcher(None, normalized_target, normalized_value).ratio()
                    if ratio >= 0.65:
                        # lower score (better) for higher ratio
                        score = int((1.0 - ratio) * 100)
                        fuzzy_candidates.append((score, row))
                        break

            if fuzzy_candidates:
                fuzzy_candidates.sort(key=lambda item: item[0])
                return pd.DataFrame([fuzzy_candidates[0][1]])
        except Exception:
            pass

        return routes_frame.iloc[0:0]

    candidate_rows.sort(key=lambda item: item[0])
    return expand_gtfs_route_rows(routes_frame, pd.DataFrame([candidate_rows[0][1]]))


def route_stop_positions(route_info):
    return route_info["sequence_by_stop_code"]


def interpolate_timepoints(route_info, stop_times):
    positions = route_stop_positions(route_info)
    anchors = []

    for stop in stop_times:
        stop_code = stop.get("stop_code") or stop.get("stopCode") or stop.get("stop_id")
        stop_code = str(stop_code) if stop_code is not None else ""
        if stop_code not in positions:
            continue

        position = positions[stop_code]
        seconds = first_valid_time(stop.get("arrival_time"), stop.get("departure_time"))
        if seconds is None:
            continue

        anchors.append((position, seconds))

    if not anchors:
        return {}

    anchors = sorted({position: seconds for position, seconds in anchors}.items())
    route_positions = sorted(positions.values())

    if len(anchors) == 1:
        _, only_seconds = anchors[0]
        return {position: only_seconds for position in route_positions}

    interpolated = {}

    first_position, first_seconds = anchors[0]
    second_position, second_seconds = anchors[1]
    first_step = 0 if second_position == first_position else (second_seconds - first_seconds) / (second_position - first_position)

    for position in route_positions:
        if position < first_position:
            interpolated[position] = first_seconds + first_step * (position - first_position)

    for (left_position, left_seconds), (right_position, right_seconds) in zip(anchors, anchors[1:]):
        step = 0 if right_position == left_position else (right_seconds - left_seconds) / (right_position - left_position)
        for position in range(left_position, right_position + 1):
            interpolated[position] = left_seconds + step * (position - left_position)

    last_position, last_seconds = anchors[-1]
    prev_position, prev_seconds = anchors[-2]
    last_step = 0 if last_position == prev_position else (last_seconds - prev_seconds) / (last_position - prev_position)

    for position in route_positions:
        if position > last_position:
            interpolated[position] = last_seconds + last_step * (position - last_position)

    return interpolated


def build_bus_schedule(route_id, route_info):
    try:
        payload = fetch_json(STCP_ROUTE_SCHEDULE_URL.format(route_id=route_id))
    except Exception as exc:
        logging.warning("BUS %s: unable to load schedule endpoint (%s)", route_id, exc)
        return []

    trips = payload.get("schedule") or []
    records = []

    for trip in trips:
        service_id = str(trip.get("service_id", ""))
        if "uteis" not in normalize_text(service_id):
            continue

        interpolated = interpolate_timepoints(route_info, trip.get("stops", []))
        if not interpolated:
            continue

        # determine direction for this trip: inspect time trend along route positions
        positions_order = [route_info["sequence_by_stop_code"].get(sc) for sc in route_info["stop_codes"]]
        trip_times = [interpolated.get(pos) for pos in positions_order]
        inc = 0
        dec = 0
        last = None
        for t in trip_times:
            if t is None:
                last = None
                continue
            if last is not None:
                if t > last:
                    inc += 1
                elif t < last:
                    dec += 1
            last = t

        direction = 0 if inc >= dec else 1

        for stop_code in route_info["stop_codes"]:
            position = route_info["sequence_by_stop_code"].get(stop_code)
            if position is None:
                continue

            seconds = interpolated.get(position)
            if seconds is None:
                continue

            time_value = seconds_to_time_string(seconds)
            records.append(
                {
                    "arrival_time": time_value,
                    "departure_time": time_value,
                    "stop_code": stop_code,
                    "route_id": str(route_id),
                    "transport_type": "BUS",
                    "direction": int(direction),
                }
            )

    return records


def build_train_schedule_from_pdf(route_id, route_info, stop_lookup):
    """Fallback: scrape CP PDF timetable for a route and extract trip columns.

    Returns list of records with arrival_time, departure_time, stop_code, route_id, transport_type, direction
    """
    # Robust lookup for TRAIN_ROUTE_PDFS: try exact key, then normalized-key matching, then substring
    pdf_url = None
    try:
        if isinstance(TRAIN_ROUTE_PDFS, dict):
            # exact match
            if route_id in TRAIN_ROUTE_PDFS:
                pdf_url = TRAIN_ROUTE_PDFS.get(route_id)
                matched_key = route_id
            else:
                norm_target = normalize_text(route_id)
                matched_key = None
                for k, v in TRAIN_ROUTE_PDFS.items():
                    try:
                        if normalize_text(k) == norm_target or norm_target in normalize_text(k) or normalize_text(k) in norm_target:
                            pdf_url = v
                            matched_key = k
                            break
                    except Exception:
                        continue
            if matched_key:
                logging.debug("Using TRAIN_ROUTE_PDFS entry '%s' -> %s for route %s", matched_key, pdf_url, route_id)
    except Exception:
        pdf_url = None

    if not pdf_url:
        logging.debug("No PDF source configured for %s", route_id)
        return []

    # If the configured value is a web page (not a direct .pdf link), try to resolve a PDF link from the page
    resolved_pdf_url = pdf_url
    try:
        if pdf_url and not str(pdf_url).lower().endswith(".pdf"):
            logging.debug("Resolving PDF from page %s for route %s", pdf_url, route_id)
            try:
                page_resp = requests.get(pdf_url, timeout=15, headers=REQUEST_HEADERS)
                page_resp.raise_for_status()
                html = page_resp.text
                # find candidate PDF links
                hrefs = re.findall(r'href=["\']([^"\']+\.pdf)["\']', html, flags=re.I)
                if not hrefs:
                    hrefs = re.findall(r"(https?:\\/\\/[^\s']"+r"\.pdf)", html, flags=re.I)
                if hrefs:
                    # prefer links that contain route keywords
                    key = normalize_text(route_id).split()[-1]
                    chosen = None
                    for h in hrefs:
                        if key and key in h.lower():
                            chosen = h
                            break
                    if chosen is None:
                        chosen = hrefs[0]
                    # resolve relative URLs
                    from urllib.parse import urljoin

                    resolved_pdf_url = urljoin(pdf_url, chosen)
                    logging.debug("Resolved PDF %s -> %s", pdf_url, resolved_pdf_url)
            except Exception as exc:
                logging.debug("Error resolving PDF from page %s: %s", pdf_url, exc)

    except Exception:
        resolved_pdf_url = pdf_url

    try:
        text = fetch_pdf_text(resolved_pdf_url)
    except Exception as exc:
        logging.debug("Failed to fetch PDF for %s: %s", route_id, exc)
        return []

    # get a best-effort ordered list of stop names for this route
    pdf_stop_names = extract_train_stop_names(text)
    if not pdf_stop_names:
        logging.debug("No stop names extracted from PDF for %s", route_id)

    # build mapping from normalized stop name -> stop_code
    pdf_stop_to_code = {}
    for name in pdf_stop_names:
        code = find_stop_code_by_name(name, "TRAIN", stop_lookup)
        if code:
            pdf_stop_to_code[normalize_text(name)] = code

    # scan lines for rows that start with a stop name and contain time tokens
    lines = [ln.strip() for ln in text.splitlines() if ln.strip()]
    time_re = re.compile(r"\b\d{1,2}:\d{2}(?::\d{2})?\b")
    times_by_code = {}

    for line in lines:
        parts = re.split(r"\s{2,}", line)
        if not parts:
            continue
        head = parts[0]
        norm_head = normalize_text(head)
        if norm_head in pdf_stop_to_code:
            code = pdf_stop_to_code[norm_head]
            times = []
            for col in parts[1:]:
                found = time_re.findall(col)
                if found:
                    times.extend(found)
            if times:
                times_by_code[code] = times

    if not times_by_code:
        logging.debug("No time columns found in PDF for %s", route_id)
        return []

    # align trips by column index
    max_cols = max(len(v) for v in times_by_code.values())
    records = []

    # Only consider trips where at least half of route stops have a time
    for col_idx in range(max_cols):
        trip_times = []
        missing = 0
        for sc in route_info["stop_codes"]:
            times = times_by_code.get(sc) or []
            if col_idx < len(times):
                trip_times.append(times[col_idx])
            else:
                trip_times.append(None)
                missing += 1

        if missing > len(route_info["stop_codes"]) // 2:
            continue

        # infer direction by time trend
        seq_seconds = []
        for t in trip_times:
            if t is None:
                seq_seconds.append(None)
            else:
                seq_seconds.append(parse_time_to_seconds(t))

        inc = 0
        dec = 0
        last = None
        for s in seq_seconds:
            if s is None:
                last = None
                continue
            if last is not None:
                if s > last:
                    inc += 1
                elif s < last:
                    dec += 1
            last = s

        direction = 0 if inc >= dec else 1

        # create records
        for sc, t in zip(route_info["stop_codes"], trip_times):
            if not t:
                continue
            time_val = t if len(t.split(":")) == 3 else f"{t}:00"
            records.append({
                "arrival_time": time_val,
                "departure_time": time_val,
                "stop_code": sc,
                "route_id": str(route_id),
                "transport_type": "TRAIN",
                "direction": int(direction),
            })

    return records


def build_gtfs_schedule(route_id, route_info, gtfs_zip_url, transport_type, stop_lookup):
    # allow gtfs_zip_url to be a single url or an ordered iterable of urls
    frames = {}
    try:
        frames, _ = fetch_frames_from_sources(gtfs_zip_url)
    except Exception:
        frames = {}

    routes_frame = frames.get("routes.txt", pd.DataFrame())
    trips_frame = frames.get("trips.txt", pd.DataFrame())
    stop_times_frame = frames.get("stop_times.txt", pd.DataFrame())
    stops_frame = frames.get("stops.txt", pd.DataFrame())
    calendar_frame = frames.get("calendar.txt", pd.DataFrame())

    if routes_frame.empty or trips_frame.empty or stop_times_frame.empty or stops_frame.empty:
        return []

    matching_routes = find_gtfs_route_rows(routes_frame, route_id)
    if matching_routes.empty:
        logging.debug("%s %s: initial name-based lookup failed, attempting stop-overlap matching", transport_type, route_id)

        # Fallback: try to match GTFS routes by overlap of stops with our route_info
        try:
            route_stop_codes_set = set(route_info.get("stop_codes", []))
            candidate_scores = []
            gtfs_stop_code_map = build_gtfs_stop_code_lookup(stops_frame, transport_type, stop_lookup)

            for _, candidate in routes_frame.iterrows():
                cand_route_key = candidate.get("route_id")
                cand_route_key = str(cand_route_key)
                cand_trips = trips_frame[trips_frame["route_id"].astype(str) == str(cand_route_key)]
                if cand_trips.empty:
                    continue

                # pick first trip for sampling
                trip_id = cand_trips.iloc[0].get("trip_id")
                if pd.isna(trip_id):
                    continue

                cand_stop_times = stop_times_frame[stop_times_frame["trip_id"].astype(str) == str(trip_id)]
                if cand_stop_times.empty:
                    continue

                cand_codes = [gtfs_stop_code_map.get(str(sid)) for sid in cand_stop_times["stop_id"].astype(str)]
                cand_codes_set = set([c for c in cand_codes if c])
                overlap = len(route_stop_codes_set & cand_codes_set)

                denom = max(1, len(route_stop_codes_set))
                frac = overlap / denom

                candidate_scores.append((overlap, frac, candidate))

            # sort by overlap desc then frac desc
            candidate_scores.sort(key=lambda t: (-t[0], -t[1]))

            # pick the best strict match first (>=2 or >=30%), otherwise allow weaker matches
            selected = None
            for overlap, frac, candidate in candidate_scores:
                if overlap >= 2 or frac >= 0.3:
                    selected = (overlap, frac, candidate)
                    break

            if selected is None and candidate_scores:
                # choose best even if weak, but only if some overlap exists
                best_overlap, best_frac, best_candidate = candidate_scores[0]
                if best_overlap >= 1 or best_frac >= 0.1:
                    selected = (best_overlap, best_frac, best_candidate)

            if selected is not None:
                overlap, frac, chosen = selected
                logging.debug("GTFS stop-overlap matched %s -> %s (overlap=%d, frac=%.2f)", route_id, chosen.get("route_id"), overlap, frac)
                matching_routes = pd.DataFrame([chosen])
            else:
                # log top 3 candidates to help debugging
                top_sample = []
                for o, f, cand in candidate_scores[:3]:
                    top_sample.append(f"{cand.get('route_id')} (overlap={o}, frac={f:.2f})")
                logging.debug("No adequate stop-overlap match for %s, top candidates: %s", route_id, top_sample)
        except Exception as exc:
            logging.debug("GTFS stop-overlap matching error: %s", exc)
        # If still empty, try Transitland feed as a fallback
        if matching_routes.empty:
            try:
                logging.debug("%s %s: attempting Transitland GTFS fallback", transport_type, route_id)
                frames_tl = fetch_zip_frames(TRANSITLAND_CP_FEED)
                routes_frame_tl = frames_tl.get("routes.txt", pd.DataFrame())
                matching_routes = find_gtfs_route_rows(routes_frame_tl, route_id)
                if not matching_routes.empty:
                    logging.debug("Transitland GTFS matched route %s", route_id)
                    # swap frames to use Transitland frames
                    routes_frame = routes_frame_tl
                    trips_frame = frames_tl.get("trips.txt", pd.DataFrame())
                    stop_times_frame = frames_tl.get("stop_times.txt", pd.DataFrame())
                    stops_frame = frames_tl.get("stops.txt", pd.DataFrame())
                    calendar_frame = frames_tl.get("calendar.txt", pd.DataFrame())
            except Exception as exc:
                logging.debug("Transitland fallback failed: %s", exc)

        # Try TransporLis historic GTFS archive if still no match
        if matching_routes.empty:
            try:
                logging.debug("%s %s: attempting TransporLis historic GTFS fallback", transport_type, route_id)
                frames_tp = fetch_zip_frames(TRANSPORLIS_GTFS)
                routes_frame_tp = frames_tp.get("routes.txt", pd.DataFrame())
                matching_routes = find_gtfs_route_rows(routes_frame_tp, route_id)
                if not matching_routes.empty:
                    logging.debug("TransporLis GTFS matched route %s", route_id)
                    routes_frame = routes_frame_tp
                    trips_frame = frames_tp.get("trips.txt", pd.DataFrame())
                    stop_times_frame = frames_tp.get("stop_times.txt", pd.DataFrame())
                    stops_frame = frames_tp.get("stops.txt", pd.DataFrame())
                    calendar_frame = frames_tp.get("calendar.txt", pd.DataFrame())
            except Exception as exc:
                logging.debug("TransporLis fallback failed: %s", exc)

        if matching_routes.empty:
            logging.warning("%s %s: route not found in GTFS feed", transport_type, route_id)
            # If this is a TRAIN route, try PDF web-scraping fallback
            if transport_type and transport_type.upper() == "TRAIN":
                try:
                    logging.debug("%s %s: attempting PDF timetable scraping fallback", transport_type, route_id)
                    pdf_records = build_train_schedule_from_pdf(route_id, route_info, stop_lookup)
                    if pdf_records:
                        return pdf_records
                except Exception as exc:
                    logging.debug("PDF scraping fallback failed: %s", exc)

            return []

    weekday_services = weekday_service_ids(calendar_frame)
    # If calendar.txt didn't yield weekday services, try calendar_dates.txt as fallback
    if not weekday_services:
        try:
            cal_dates = frames.get("calendar_dates.txt", pd.DataFrame())
            if cal_dates is not None and not cal_dates.empty:
                inferred = set()
                import datetime as _dt

                for _, row in cal_dates.iterrows():
                    svc = str(row.get("service_id"))
                    date_val = row.get("date")
                    exc_type = row.get("exception_type")
                    if pd.isna(svc) or pd.isna(date_val):
                        continue
                    try:
                        d = _dt.datetime.strptime(str(int(date_val)), "%Y%m%d")
                        if d.weekday() < 5:
                            # exception_type 1 = added service; 2 = removed
                            if str(exc_type).strip() in {"1", "True", "true", "1.0"}:
                                inferred.add(svc)
                    except Exception:
                        continue

                if inferred:
                    weekday_services = inferred
        except Exception:
            pass

    matched_route_ids = (
        matching_routes["route_id"].astype(str).dropna().drop_duplicates().tolist()
        if "route_id" in matching_routes.columns
        else []
    )
    if not matched_route_ids:
        matched_route_ids = [str(route_id)]

    gtfs_stop_codes = build_gtfs_stop_code_lookup(stops_frame, transport_type, stop_lookup)
    route_positions = route_stop_positions(route_info)
    records = []

    def collect_records_for_route_key(gtfs_route_key):
        selected_trips = trips_frame[trips_frame["route_id"].astype(str) == str(gtfs_route_key)].copy()
        if selected_trips.empty:
            return []

        if weekday_services and "service_id" in selected_trips.columns:
            filtered = selected_trips[selected_trips["service_id"].astype(str).isin(weekday_services)]
            if not filtered.empty:
                selected_trips = filtered
            else:
                logging.debug("%s %s: no trips matched detected weekday service_ids for GTFS route %s; falling back to unfiltered trips", transport_type, route_id, gtfs_route_key)

        if selected_trips.empty:
            return []

        route_records = []

        for _, trip in selected_trips.iterrows():
            trip_id = trip.get("trip_id")
            trip_stop_times = stop_times_frame[stop_times_frame["trip_id"].astype(str) == str(trip_id)].copy()
            if trip_stop_times.empty:
                continue

            if "stop_sequence" in trip_stop_times.columns:
                trip_stop_times["stop_sequence"] = pd.to_numeric(trip_stop_times["stop_sequence"], errors="coerce")
                trip_stop_times = trip_stop_times.sort_values(["stop_sequence", "stop_id"])
            else:
                trip_stop_times = trip_stop_times.sort_values(["stop_id"])

            matched_stops = []
            for _, stop_time in trip_stop_times.iterrows():
                stop_id = stop_time.get("stop_id")
                stop_code = gtfs_stop_codes.get(str(stop_id))
                if not stop_code or stop_code not in route_positions:
                    continue

                seconds = first_valid_time(stop_time.get("arrival_time"), stop_time.get("departure_time"))
                if seconds is None:
                    continue

                matched_stops.append((route_positions[stop_code], stop_code, seconds))

            if len(matched_stops) < 2:
                continue

            # infer direction: prefer GTFS trip direction_id if available
            if "direction_id" in trip.index and not pd.isna(trip.get("direction_id")):
                try:
                    direction = int(trip.get("direction_id"))
                except Exception:
                    direction = 0
            else:
                # infer from stop_sequence ordering
                seq_order = []
                if "stop_sequence" in trip_stop_times.columns:
                    ordered = trip_stop_times.sort_values("stop_sequence")
                    for _, st in ordered.iterrows():
                        stop_id = st.get("stop_id")
                        stop_code = gtfs_stop_codes.get(str(stop_id))
                        if stop_code and stop_code in route_info["sequence_by_stop_code"]:
                            seq_order.append(route_info["sequence_by_stop_code"][stop_code])

                inc = 0
                dec = 0
                for a, b in zip(seq_order, seq_order[1:]):
                    if b > a:
                        inc += 1
                    elif b < a:
                        dec += 1
                direction = 0 if inc >= dec else 1

            matched_stops.sort(key=lambda item: item[0])
            for position, stop_code, seconds in matched_stops:
                if stop_code not in route_info["stop_codes"]:
                    continue

                time_value = seconds_to_time_string(seconds)
                route_records.append(
                    {
                        "arrival_time": time_value,
                        "departure_time": time_value,
                        "stop_code": stop_code,
                        "route_id": str(route_id),
                        "transport_type": transport_type,
                        "direction": int(direction),
                    }
                )

        return route_records

    for gtfs_route_key in matched_route_ids:
        records.extend(collect_records_for_route_key(gtfs_route_key))

    return records


def build_schedule(route_stops, stops_lookup):
    route_infos = route_sequence_map(route_stops)
    all_records = []

    bus_routes = route_stops[route_stops["transport_type"] == "BUS"]
    metro_routes = route_stops[route_stops["transport_type"] == "METRO"]
    train_routes = route_stops[route_stops["transport_type"] == "TRAIN"]
    # BUS
    if FETCH_BUS:
        for route_id in sorted(bus_routes["route_id"].unique(), key=lambda value: (len(str(value)), str(value))):
            route_key = str(route_id)
            logging.info("BUS route %s: loading weekday schedule", route_key)
            records = build_bus_schedule(route_key, route_infos[route_key])
            all_records.extend(records)
            logging.info("BUS route %s: %s rows", route_key, len(records))
    else:
        logging.info("Skipping BUS schedule fetch (FETCH_BUS=False)")

    # METRO
    if FETCH_METRO:
        for route_id in sorted(metro_routes["route_id"].unique()):
            route_key = str(route_id)
            logging.info("METRO route %s: loading weekday GTFS trips", route_key)
            records = build_gtfs_schedule(route_key, route_infos[route_key], METRO_GTFS_ZIP, "METRO", stops_lookup)
            all_records.extend(records)
            logging.info("METRO route %s: %s rows", route_key, len(records))
    else:
        logging.info("Skipping METRO schedule fetch (FETCH_METRO=False)")

    # TRAIN
    if FETCH_TRAIN:
        for route_id in sorted(train_routes["route_id"].unique()):
            route_key = str(route_id)
            logging.info("TRAIN route %s: loading weekday schedule (train-first=%s)", route_key, TRAIN_FIRST)
            records = []
            # Try PDF/webscraping first if configured
            if TRAIN_FIRST:
                try:
                    pdf_recs = build_train_schedule_from_pdf(route_key, route_infos[route_key], stops_lookup)
                    if pdf_recs:
                        logging.info("TRAIN route %s: recovered %s rows from PDF/webscrape", route_key, len(pdf_recs))
                        records = pdf_recs
                except Exception as exc:
                    logging.debug("TRAIN %s: PDF/webscrape attempt failed: %s", route_key, exc)

            # If nothing from PDF or TRAIN_FIRST disabled, fallback to GTFS
            if not records:
                try:
                    gtfs_recs = build_gtfs_schedule(route_key, route_infos[route_key], [CP_GTFS_ENHANCED, CP_GTFS_ZIP, TRANSITLAND_CP_FEED, TRANSPORLIS_GTFS], "TRAIN", stops_lookup)
                    records = gtfs_recs
                except Exception as exc:
                    logging.warning("TRAIN route %s: GTFS source unavailable (%s)", route_key, exc)
                    records = []

            all_records.extend(records)
            logging.info("TRAIN route %s: %s rows", route_key, len(records))
    else:
        logging.info("Skipping TRAIN schedule fetch (FETCH_TRAIN=False)")

    return pd.DataFrame(all_records)


def clean_output(schedule_df):
    if schedule_df.empty:
        return schedule_df

    schedule_df = schedule_df.dropna(subset=["arrival_time", "departure_time", "stop_code", "route_id"])
    schedule_df["route_id"] = schedule_df["route_id"].astype(str)
    schedule_df["stop_code"] = schedule_df["stop_code"].astype(str)
    schedule_df["transport_type"] = schedule_df["transport_type"].astype(str)
    # include direction in deduplication if present
    dedup_subset = ["arrival_time", "departure_time", "stop_code", "route_id"]
    if "direction" in schedule_df.columns:
        schedule_df["direction"] = schedule_df["direction"].astype(int)
        dedup_subset.append("direction")

    schedule_df = schedule_df.drop_duplicates(subset=dedup_subset, keep="first")
    schedule_df = schedule_df.sort_values(["transport_type", "route_id", "arrival_time", "departure_time", "stop_code"])
    cols = ["arrival_time", "departure_time", "stop_code", "route_id"]
    if "direction" in schedule_df.columns:
        cols.append("direction")
    return schedule_df[cols]


def main():
    route_stops = load_route_stops()
    stops_df = read_csv(STOPS_FILE)
    stops_lookup = build_stop_lookup(stops_df)

    logging.info("Loaded %s route-stop rows", len(route_stops))
    logging.info("Generating weekday schedules from real transport sources")

    schedule_df = build_schedule(route_stops, stops_lookup)
    schedule_df = clean_output(schedule_df)

    if schedule_df.empty:
        logging.warning("No schedule rows were generated")
        schedule_df = pd.DataFrame(columns=["arrival_time", "departure_time", "stop_code", "route_id", "direction"])

    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    schedule_df.to_csv(OUTPUT_FILE, index=False)

    logging.info("Saved %s rows to %s", len(schedule_df), OUTPUT_FILE)
    print(schedule_df.head(20).to_string(index=False))


if __name__ == "__main__":
    main()