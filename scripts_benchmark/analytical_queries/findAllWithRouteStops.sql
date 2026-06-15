SELECT r.id, r.name, rs.id, rs.sequence, st.id, st.name, st.stop_code, st.stop_type, st.zone_id
FROM catchit.route r
LEFT JOIN catchit.route_stop rs ON r.id = rs.route_id
LEFT JOIN catchit.stop st ON st.id = rs.stop_id
ORDER BY r.name ASC, rs.sequence ASC;