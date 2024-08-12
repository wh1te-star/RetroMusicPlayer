import struct
import datetime
import sys
import webbrowser
import os

file_path = sys.argv[1]

with open(file_path, 'rb') as file:
    data = file.read()

format = 'Qdddfffff'
record_size = struct.calcsize(format)
num_records = len(data) // record_size

coordinates = []
speeds = []

ind = 0
mul = 100
for i in range(num_records):
    timestamp, latitude, longitude, altitude, bearing, speed, acceleroX, acceleroY, acceleroZ = struct.unpack_from(format, data, i * record_size)
    timestamp_dt = datetime.datetime.fromtimestamp(timestamp / 1000.0)
    record = f"{timestamp_dt.strftime('%Y/%m/%d %H:%M:%S.%f')[:-3]} {latitude:.2f} {longitude:.2f} {altitude:.2f} {bearing:.2f} {int(speed)} {acceleroX {acceleroY}} {acceleroZ}"
    if ind % mul == 0:
        coordinates.append((latitude, longitude))
        speeds.append(speed)
    ind += 1

coordinates_js_array = str(coordinates).replace('(', '[').replace(')', ']')
speeds_js_array = str(speeds).replace('(', '').replace(')', '')

html_content = f"""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Map with Pinned Icons</title>
    <link rel="stylesheet" href="https://unpkg.com/leaflet/dist/leaflet.css" />
    <style>
        #map {{
            height: 100vh;
        }}
    </style>
</head>
<body>
    <div id="map"></div>
    <script src="https://unpkg.com/leaflet/dist/leaflet.js"></script>
    <script>
        var map = L.map('map').setView([{coordinates[0][0]}, {coordinates[0][1]}], 15);
        L.tileLayer('https://{{s}}.tile.openstreetmap.org/{{z}}/{{x}}/{{y}}.png', {{
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        }}).addTo(map);
        var customIconRed = L.icon({{
            iconUrl: 'location_pin/ic_location_pin_red.svg',
            iconSize: [45, 55],
        }});
        var customIconBlue = L.icon({{
            iconUrl: 'location_pin/ic_location_pin_blue.svg',
            iconSize: [45, 55],
        }});
        var customIconGreen = L.icon({{
            iconUrl: 'location_pin/ic_location_pin_green.svg',
            iconSize: [45, 55],
        }});
        var customIconCyan = L.icon({{
            iconUrl: 'location_pin/ic_location_pin_cyan.svg',
            iconSize: [45, 55],
        }});
        var customIconMagenta = L.icon({{
            iconUrl: 'location_pin/ic_location_pin_magenta.svg',
            iconSize: [45, 55],
        }});
        var customIconYellow = L.icon({{
            iconUrl: 'location_pin/ic_location_pin_yellow.svg',
            iconSize: [45, 55],
        }});
        var coordinates = {coordinates_js_array};
        var speeds = {speeds_js_array};
        var index = 0
        coordinates.forEach(function(coord, index) {{
            if (speeds[index] < 8.3) {{
                L.marker([coord[0], coord[1]], {{icon: customIconGreen}}).addTo(map);
            }} else if (speeds[index] < 16.6) {{
                L.marker([coord[0], coord[1]], {{icon: customIconBlue}}).addTo(map);
            }} else if (speeds[index] < 22.2) {{
                L.marker([coord[0], coord[1]], {{icon: customIconCyan}}).addTo(map);
            }} else if (speeds[index] < 27.7) {{
                L.marker([coord[0], coord[1]], {{icon: customIconMagenta}}).addTo(map);
            }} else if (speeds[index] < 33.3) {{
                L.marker([coord[0], coord[1]], {{icon: customIconYellow}}).addTo(map);
            }} else {{
                L.marker([coord[0], coord[1]], {{icon: customIconRed}}).addTo(map);
            }}
            index++;
        }});
    </script>
</body>
</html>
"""

current_dir = os.path.dirname(os.path.abspath(__file__))
html_file_path = os.path.join(current_dir, 'map.html')

with open(html_file_path, 'w') as html_file:
    html_file.write(html_content)

webbrowser.open(f'file://{html_file_path}')
