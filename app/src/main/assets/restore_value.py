import struct
import datetime
import sys
import webbrowser
import os

file_path = sys.argv[1]

with open(file_path, 'rb') as file:
    data = file.read()

format = 'Qddf'
record_size = struct.calcsize(format)
num_records = len(data) // record_size

coordinates = []
speeds = []

ind=0
mul=1000
for i in range(num_records):
    timestamp, latitude, longitude, speed = struct.unpack_from(format, data, i * record_size)
    timestamp_dt = datetime.datetime.fromtimestamp(timestamp / 1000.0)
    record = f"{timestamp_dt.strftime('%Y/%m/%d %H:%M:%S.%f')[:-3]} {latitude:.2f} {longitude:.2f} {int(speed)}"
    # print(f"time: {timestamp_dt}, latitude: {latitude:.2f}, longitude: {longitude:.2f}, speed: {speed}")
    if(ind % mul == 0):
        coordinates.append((latitude, longitude))
        speeds.append(speed)
    ind=ind+1

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
            iconUrl: 'ic_location_pin_red.svg',
            iconSize: [45, 55],
        }});
        var customIconBlue = L.icon({{
            iconUrl: 'ic_location_pin_blue.svg',
            iconSize: [45, 55],
        }});
        var coordinates = {coordinates_js_array};
        var speeds = {speeds_js_array};
        var index = 0
        coordinates.forEach(function(coord, index) {{
            if (speeds[index] > 27.7) {{
                L.marker([coord[0], coord[1]], {{icon: customIconRed}}).addTo(map);
            }} else {{
                L.marker([coord[0], coord[1]], {{icon: customIconBlue}}).addTo(map);
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
