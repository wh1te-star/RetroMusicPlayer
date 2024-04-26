import struct
import datetime
import sys

file_path = sys.argv[1]

with open(file_path, 'rb') as file:
    data = file.read()

format = 'Qdd'
record_size = struct.calcsize(format)
num_records = len(data) // record_size

records = []

for i in range(num_records):
    timestamp, latitude, longitude = struct.unpack_from(format, data, i * record_size)
    timestamp_dt = datetime.datetime.fromtimestamp(timestamp / 1000.0)
    record = f"{timestamp_dt.strftime('%Y/%m/%d %H:%M:%S.%f')[:-3]} {latitude}  {longitude}"
    records.append(record)

for record in records:
    print(record)

