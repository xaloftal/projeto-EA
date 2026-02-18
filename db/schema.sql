/*
*
*	TABLES
*
*/

create table if not exists tbl_transport(
/*
*	If its bus, metrobus, metro, train...
*/
	s_guid uuid default gen_random_uuid() primary key,
	s_code text not null unique,
	s_display text,
	s_description text
);

create table if not exists tbl_stop(
/*
* 	Physical stop where people get in or off the vehicles
*/
	s_guid uuid default gen_random_uuid() primary key,
	s_code text not null unique,
	s_display text,
	s_description text,
	s_transport_guid uuid references tbl_transport(s_guid)
);

create table if not exists tbl_vehicle(
	s_guid uuid default gen_random_uuid() primary key,
	s_code text not null unique,
	s_display text,
	s_description text,
	i_capacity integer,
	s_transport_guid uuid references tbl_transport(s_guid)
);


create table if not exists tbl_user(
/*
*	User account
*/
	s_guid uuid default gen_random_uuid() primary key,
	s_code text not null unique,
	s_display text,
	s_description text,
	f_balance float,
	s_password text, /* to be seen how we do this*/
	b_is_admin boolean,
	dt_created_at timestamp default now()
);

create table if not exists tbl_ticket(
	s_guid uuid default gen_random_uuid() primary key,
	s_code text not null unique,
	s_display text,
	s_description text,
	s_from_guid uuid references tbl_stop(s_guid),
	s_to_guid uuid references tbl_stop (s_guid),
	b_is_pass boolean default false,
	b_is_two_way boolean default false,
	f_total float,
	dt_purchased timestamp default now(),
	dt_used timestamp,
	dt_expiration_date timestamp default now() + interval '1 week',
	b_is_valid boolean default true,
	s_vehicle_guid uuid references tbl_vehicle(s_guid)
);

create table if not exists tbl_poi(
	s_guid uuid default gen_random_uuid() primary key,
	s_user_guid uuid references tbl_user(s_guid),
	s_stop_guid uuid references tbl_stop(s_guid),
	s_display text,
	s_description text
);


create table if not exists tbl_route(
	s_guid uuid default gen_random_uuid() primary key,
	s_code text not null unique,
	s_display text,
	s_description text
	
);

create table if not exists tbl_route_stop(
	s_guid uuid default gen_random_uuid() primary key,
	s_stop_guid uuid references tbl_stop(s_guid),
	s_route_guid uuid references tbl_route(s_guid)
	
);

create table if not exists tbl_schedule(
	s_guid uuid default gen_random_uuid() primary key,
	s_route_stop_guid uuid references tbl_route_stop(s_guid),
	dt_time timestamp
);

create table if not exists tbl_vehicle_route(
	s_vehicle_guid uuid references tbl_vehicle(s_guid),
	s_route_guid uuid references tbl_route(s_guid),
	b_is_active boolean,

	primary key (s_vehicle_guid, s_route_guid)
);


create table if not exists tbl_system_analytics(
	s_guid uuid default gen_random_uuid() primary key,
	dt_date date default now()::date,
	
);

/*
drop table if exists tbl_vehicle_route cascade;
drop table if exists tbl_schedule cascade;
drop table if exists tbl_transport cascade;
drop table if exists tbl_stop cascade;
drop table if exists tbl_route_stop cascade;
drop table if exists tbl_route cascade;
drop table if exists tbl_poi cascade;
drop table if exists tbl_ticket cascade;
drop table if exists tbl_user cascade;
drop table if exists tbl_vehicle cascade;
*/

