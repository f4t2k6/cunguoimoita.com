#!/bin/sh
mkdir -p /usr/local/tomcat/conf/Catalina/localhost
cat > /usr/local/tomcat/conf/Catalina/localhost/ROOT.xml <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<Context>
    <Resource name="jdbc/cunguoimoita"
              auth="Container"
              type="javax.sql.DataSource"
              factory="org.apache.tomcat.jdbc.pool.DataSourceFactory"
              driverClassName="org.postgresql.Driver"
              url="${DB_URL}"
              username="${DB_USER}"
              password="${DB_PASSWORD}"
              maxActive="5"
              maxIdle="2"
              minIdle="0"
              initialSize="0"
              maxWait="10000"
              validationQuery="SELECT 1"
              testOnBorrow="true"
              validationInterval="30000"
              testWhileIdle="true"
              timeBetweenEvictionRunsMillis="60000"
              minEvictableIdleTimeMillis="120000"
              defaultAutoCommit="true"
              rollbackOnReturn="true"
              jdbcInterceptors="ConnectionState"/>

    <Parameter name="mail.username" value="${MAIL_USERNAME}" override="false"/>
    <Parameter name="mail.password" value="${MAIL_PASSWORD}" override="false"/>

    <Parameter name="app.baseUrl" value="${APP_BASE_URL}" override="false"/>
</Context>
EOF
exec catalina.sh run
