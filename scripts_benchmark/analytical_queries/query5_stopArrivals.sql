-- searchRoutes - Pesquisa de rotas entre duas paragens
SELECT DISTINCT 
    r.id as route_id,
    r.name as route_name,
    s1.id as from_stop_id,
    s1.name as from_stop_name,
    s2.id as to_stop_id,
    s2.name as to_stop_name,
    MIN(ss1.departure_time) as departure_time,
    MIN(ss2.arrival_time) as arrival_time
FROM catchit.route r
JOIN catchit.route_stop rs1 ON r.id = rs1.route_id
JOIN catchit.stop s1 ON s1.id = rs1.stop_id
JOIN catchit.route_stop rs2 ON r.id = rs2.route_id
JOIN catchit.stop s2 ON s2.id = rs2.stop_id
JOIN catchit.stopschedule ss1 ON ss1.stop_id = s1.id AND ss1.route_id = r.id
JOIN catchit.stopschedule ss2 ON ss2.stop_id = s2.id AND ss2.route_id = r.id
WHERE rs1.sequence < rs2.sequence
GROUP BY r.id, r.name, s1.id, s1.name, s2.id, s2.name
ORDER BY departure_time;