#!/bin/sh
# Launch the real GUI under Xvfb and capture it with java.awt.Robot.
cd "$(dirname "$0")/.."
Xvfb :99 -screen 0 1280x1024x24 > /tmp/xvfb99.log 2>&1 &
XVFB_PID=$!
sleep 2
DISPLAY=:99 java -Dbank.data.dir=screenshot_data -cp out com.bank.Main > /tmp/bankapp.log 2>&1 &
APP_PID=$!
sleep 4
DISPLAY=:99 java -cp out CaptureScreen screenshots/main_window.png
kill $APP_PID 2>/dev/null
kill $XVFB_PID 2>/dev/null
ls -lh screenshots/main_window.png
cat /tmp/bankapp.log
