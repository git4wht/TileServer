# On Linux install
* System V Init
```
sudo ln -s /data/GIS/TileServer.jar /etc/init.d/GisService
```
* systemd
```
cat<<EOF>>/etc/systemd/system/GisService.service
[Unit]
Description=GisService A Spring Boot application
After=syslog.target
 
[Service]
WorkingDirectory=/data/GIS/
ExecStart=/data/GIS/TileServer.jar
SuccessExitStatus=143 
 
[Install] 
WantedBy=multi-user.target
EOF
```
# On Windows install
* Java Service Wrapper
```
 GisService.exe install
```
* Apache Commons Daemon
