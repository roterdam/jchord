--
-- JSpider database creation script
--
-- WARNING: This JDBC storage provider is an alpha release.
-- It has only been tested on MySQL 3.
--
-- $Id: jspiderdb.sql,v 1.6 2003/04/09 17:08:02 vanrogu Exp $
--

--
-- MySQL dump 8.22
--
-- Host: localhost    Database: jspider
---------------------------------------------------------
-- Server version	3.23.55-nt

--
-- Table structure for table 'jspider_content'
--

CREATE TABLE jspider_content (
  id int(11) NOT NULL default '0',
  content blob
) TYPE=MyISAM;

--
-- Table structure for table 'jspider_cookie'
--

CREATE TABLE jspider_cookie (
  id int(11) NOT NULL auto_increment,
  site int(11) NOT NULL default '0',
  name varchar(255) NOT NULL default '',
  value varchar(255) NOT NULL default '',
  domain varchar(255) NOT NULL default '',
  path varchar(255) NOT NULL default '',
  expires varchar(255) default NULL,
  PRIMARY KEY  (id)
) TYPE=MyISAM;

--
-- Table structure for table 'jspider_decision'
--

CREATE TABLE jspider_decision (
  resource int(11) NOT NULL default '0',
  subject int(11) NOT NULL default '0',
  type int(11) NOT NULL default '0',
  comment longtext NOT NULL,
  PRIMARY KEY  (resource,subject)
) TYPE=MyISAM;

--
-- Table structure for table 'jspider_decision_step'
--

CREATE TABLE jspider_decision_step (
  resource int(11) NOT NULL default '0',
  subject int(11) NOT NULL default '0',
  sequence int(11) NOT NULL default '0',
  type int(11) NOT NULL default '0',
  rule longtext NOT NULL,
  decision int(11) NOT NULL default '0',
  comment longtext NOT NULL,
  PRIMARY KEY  (resource,subject,sequence)
) TYPE=MyISAM;

--
-- Table structure for table 'jspider_email_address'
--

CREATE TABLE jspider_email_address (
  id int(11) NOT NULL auto_increment,
  address varchar(255) NOT NULL default '',
  PRIMARY KEY  (id)
) TYPE=MyISAM;

--
-- Table structure for table 'jspider_email_address_reference'
--

CREATE TABLE jspider_email_address_reference (
  resource int(11) NOT NULL default '0',
  address int(11) NOT NULL default '0',
  count int(11) NOT NULL default '0',
  PRIMARY KEY  (resource,address)
) TYPE=MyISAM;

--
-- Table structure for table 'jspider_folder'
--

CREATE TABLE jspider_folder (
  id int(11) NOT NULL auto_increment,
  parent int(11) NOT NULL default '0',
  site int(11) NOT NULL default '0',
  name longtext NOT NULL,
  PRIMARY KEY  (id)
) TYPE=MyISAM;

--
-- Table structure for table 'jspider_resource'
--

CREATE TABLE jspider_resource (
  id int(11) NOT NULL auto_increment,
  url longtext NOT NULL,
  state int(11) NOT NULL default '0',
  httpstatus int(11) NOT NULL default '0',
  site int(11) NOT NULL default '0',
  timems int(11) NOT NULL default '0',
  mimetype varchar(255) default NULL,
  size int(11) NOT NULL default '0',
  folder int(11) NOT NULL default '0',
  PRIMARY KEY  (id)
) TYPE=MyISAM;

--
-- Table structure for table 'jspider_resource_reference'
--

CREATE TABLE jspider_resource_reference (
  referer int(11) NOT NULL default '0',
  referee int(11) NOT NULL default '0',
  count int(11) NOT NULL default '0',
  PRIMARY KEY  (referer,referee)
) TYPE=MyISAM;

--
-- Table structure for table 'jspider_site'
--

CREATE TABLE jspider_site (
  id int(11) NOT NULL auto_increment,
  host varchar(255) NOT NULL default '',
  port int(11) NOT NULL default '80',
  robotstxthandled tinyint(4) NOT NULL default '0',
  usecookies tinyint(4) NOT NULL default '0',
  useproxy tinyint(4) NOT NULL default '0',
  state int(11) NOT NULL default '0',
  obeyrobotstxt int(11) NOT NULL default '0',
  fetchrobotstxt int(11) NOT NULL default '0',
  basesite int(11) NOT NULL default '0',
  useragent varchar(255) NOT NULL default '',
  handle int(11) NOT NULL default '0',
  PRIMARY KEY  (id)
) TYPE=MyISAM;

