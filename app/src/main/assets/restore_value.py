import struct
import datetime
import sys
import webbrowser
import os

file_path = sys.argv[1]

with open(file_path, 'rb') as file:
    data = file.read()

format = 'Qdd'
record_size = struct.calcsize(format)
num_records = len(data) // record_size

coordinates = []

for i in range(num_records):
    timestamp, latitude, longitude = struct.unpack_from(format, data, i * record_size)
    timestamp_dt = datetime.datetime.fromtimestamp(timestamp / 1000.0)
    record = f"{timestamp_dt.strftime('%Y/%m/%d %H:%M:%S.%f')[:-3]} {latitude}  {longitude}"
    coordinates.append((latitude, longitude))

coordinates_js_array = str(coordinates).replace('(', '[').replace(')', ']')

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
        var coordinates = {coordinates_js_array};
        coordinates.forEach(function(coord) {{
            L.marker(coord).addTo(map);
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
