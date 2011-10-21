@echo off
color b
cls

@title 1/3 Starting world server(s)...

start /b launch_worlds.bat
ping 127.0.0.1 -w 20000 >nul

@title 2/3 Starting channel server(s)...

start /b launch_channels.bat
ping 127.0.0.1 -w 20000 >nul

@title 3/3 Starting login server...

start /b launch_login.bat
ping 127.0.0.1 -w 20000 >nul

@title Server Fully Active