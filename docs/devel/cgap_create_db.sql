create database cmview_cgap;
use cmview_cgap;
create table nbhstrings( graph_id int, num int, nbhstring varchar(40)); 
create table edges( graph_id int,  accession_code char(4), cid varchar(6), i_num int, i_res char(1), i_sstype char(1), j_num int, j_res char(1), j_sstype char(1), dist_ij float, j_atom varchar(2), x float, y float, z float, r float, theta float, phi float );
create table rvecs10( nbstring varchar(10), rvector char(20), support int(10), distance float(8,6), tauA float(8,6), tauB float(8,6) );
load data local infile "cullpdb_20_edges.txt" into table edges;
load data local infile "cullpdb_20_nbhstrings.txt" into table nbhstrings;
load data local infile "rvecs10.txt" into table rvecs10;
