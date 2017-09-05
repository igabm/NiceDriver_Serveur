FROM tomcat:8-jre8

MAINTAINER "Matteo <matteo.redaelli@gmail.com>"

ADD server.xml /usr/local/tomcat/conf/
ADD tomcat-users.xml /usr/local/tomcat/conf/
ADD context.xml /usr/local/tomcat/webapps/host-manager/META-INF/
ADD context.xml /usr/local/tomcat/webapps/manager/META-INF/
#ADD ojdbc6.jar /usr/local/tomcat/lib/
RUN wget 'https://github.com/igabm/NiceDriver_Serveur/raw/master/deploy/NiceDriver_Serveur.war' -O /usr/local/tomcat/webapps/NiceDriver_Serveur.war

EXPOSE 8080
