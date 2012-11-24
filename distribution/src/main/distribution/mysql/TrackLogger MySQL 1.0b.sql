##
#  Copyright 2012 the original author or authors.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
##

## 
# Authors: David Valeri
##
CREATE TABLE session (
  _id INTEGER PRIMARY KEY AUTO_INCREMENT,
  start_date DATETIME NOT NULL,
  last_modified_date DATETIME NOT NULL);

CREATE TABLE timing_entry(
  _id INTEGER PRIMARY KEY AUTO_INCREMENT,
  session_id INTEGER NOT NULL,
  synch_timestamp BIGINT NOT NULL,
  capture_timestamp BIGINT,
  lap INTEGER NOT NULL,
  lap_time BIGINT,
  split_index INTEGER NOT NULL,
  split_time BIGINT);

CREATE TABLE log_entry (
  _id INTEGER PRIMARY KEY AUTO_INCREMENT,
  session_id INTEGER NOT NULL,
  synch_timestamp BIGINT NOT NULL,
  accel_capture_timestamp BIGINT,
  longitudinal_accel FLOAT,
  lateral_accel FLOAT,
  vertical_accel FLOAT,
  location_capture_timestamp BIGINT,
  latitude DOUBLE,
  longitude DOUBLE,
  altitude DOUBLE,
  speed FLOAT,
  bearing FLOAT,
  ecu_capture_timestamp BIGINT,
  rpm BIGINT,
  map DOUBLE,
  mgp DOUBLE,
  tp DOUBLE,
  afr DOUBLE,
  mat DOUBLE,
  clt DOUBLE,
  ignition_advance DOUBLE,
  battery_voltage DOUBLE);