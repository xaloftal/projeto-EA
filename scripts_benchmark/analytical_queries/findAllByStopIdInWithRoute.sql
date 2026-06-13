SELECT ss.id, ss.route_id, ss.stop_id, ss.arrival_time, ss.departure_time, ss.sequence,
r.id, r.name, s.id, s.name, s.stop_code, s.stop_type, s.zone_id, s.location_id, l.id, l.latitude, l.longitude
FROM catchit.stopschedule ss
JOIN catchit.route r ON r.id = ss.route_id
JOIN catchit.stop s ON s.id = ss.stop_id
LEFT JOIN catchit.location l ON l.id = s.location_id
WHERE s.id IN (
    '4da643a2-77ef-461d-9643-237e4ffaa3c5',
    '6c9142ab-5a66-4658-a574-57b5b9142eaf',
    'd558ac6b-a2e6-4d7f-bf78-3ae793750956'
);