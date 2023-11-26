#java -cp "../out/production/pd_23_24:resources/db/sqlite-jdbc-3.42.0.1.jar;" model.server.Server 5001 resources/db/ resources/files

javac -d out/ -sourcepath ../src ../src/model/server/Server.java
cd ../out/production/pd_23_24
java model.server.Server 5001 resources/db/ resources/files