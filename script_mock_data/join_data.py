import pandas as pd

zones = pd.read_csv('data/zones.csv')
route_stops = pd.read_csv('data/route_stops.csv')


# merge stops and routes and drop duplicate column of zone_code
merged_stop_zone = pd.merge(route_stops, zones, left_on='zone_code', right_on='zone_code', how='inner')
merged_stop_zone = merged_stop_zone.rename(columns={'zone_code_x': 'zone_code'})

#rename route_id to route_code to match with other dataframes
merged_stop_zone = merged_stop_zone.rename(columns={'route_id': 'route_code'})



#number of lines in stop
print("route_stops: " + str(route_stops.shape[0]))
print("merged: " + str(merged_stop_zone.shape[0]))
print(merged_stop_zone.head())

#save merged dataframe to csv
merged_stop_zone.to_csv('data/stops_routes_zones.csv', index=False)