/*
*
*	TABLES
*
*/


create table if not exists tbl_user(
	s_guid uuid default gen_random_uuid() primary key,
	s_code text not null unique,
	s_display text,
	s_description text,
	dt_created_at timestamp default now()
);

create table if not exists tbl_ticket(
	s_guid uuid default gen_random_uuid() primary key,
	s_code text not null unique,
	s_display text,
	s_description text,
	s_
);