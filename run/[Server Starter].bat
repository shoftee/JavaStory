@echo off
color b
cls

@title 1/4 Starting world server...

start /b launch_world.bat
ping 127.0.0.1 -w 20000 >nul

@title 2/4 Starting cash shop server...

start /b launch_CS.bat
ping 127.0.0.1 -w 20000 >nul

@title 3/4 Starting channel server(s)...

start /b launch_channel.bat
ping 127.0.0.1 -w 20000 >nul

@title 4/4 Starting login server...

start /b launch_login.bat
ping 127.0.0.1 -w 20000 >nul

@title Server Fully Active