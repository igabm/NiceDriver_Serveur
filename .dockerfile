FROM tomcat:8-jre8

MAINTAINER "Matteo <matteo.redaelli@gmail.com>"

ADD server.xml /usr/local/tomcat/conf/
ADD tomcat-users.xml /usr/local/tomcat/conf/
ADD ojdbc6.jar /usr/local/tomcat/lib/
ADD bips.war /usr/local/tomcat/webapps/
