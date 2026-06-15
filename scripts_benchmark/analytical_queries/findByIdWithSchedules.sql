SELECT r.id, r.name, s.route_id, s.id, s.arrival_time, s.departure_time, s.sequence, st.id, l.id, l.latitude, l.longitude, st.name, st.stop_code, st.stop_type, st.zone_id
FROM catchit.route r
LEFT JOIN catchit.stopschedule s ON r.id = s.route_id
LEFT JOIN catchit.stop st ON st.id = s.stop_id
LEFT JOIN catchit.location l ON l.id = st.location_id
WHERE r.id = '2df47d4f-2315-4a48-b79a-59fe59079d3a';