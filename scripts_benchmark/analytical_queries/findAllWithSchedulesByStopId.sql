SELECT r.id, r.name, s.route_id, s.id, s.arrival_time, s.departure_time, s.sequence, st.id, l.id, l.latitude, l.longitude, st.name, st.stop_code, st.stop_type, st.zone_id
FROM catchit.route r
JOIN catchit.stopschedule s ON r.id = s.route_id
JOIN catchit.stop st ON st.id = s.stop_id
LEFT JOIN catchit.location l ON l.id = st.location_id
WHERE st.id = '2613de1b-e68f-4ebf-8f34-b36ba21c5e42';