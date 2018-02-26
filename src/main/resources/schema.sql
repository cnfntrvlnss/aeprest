create table if not exists testtask(
id int not null primary key,
name varchar(128) not null,
startTime timestamp,
endTime timestamp,
status varchar(10),
logPath varchar(512)
);

create table if not exists testtaskcase(
id int not null primary key auto_increment,
caseId int not null,
taskId int not null,
name varchar(128) not null,
scriptName varchar(256) not null,
srcPath varchar(512) not null,
scriptParam varchar(1024) not null,
elapsedTime int,
result varchar(10),
);

create index if not exists idx_testtaskcase_taskId on testtaskcase(taskId);