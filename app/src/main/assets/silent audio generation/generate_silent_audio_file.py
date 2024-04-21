import os
import sys
from pydub import AudioSegment
from mutagen.id3 import ID3, TRCK, TIT2, TPE1, TALB, APIC, TPE2, TDRC
from mutagen.mp3 import MP3

def length_to_ms(length):
    minutes, seconds = map(int, length.split(':'))
    return (minutes * 60 + seconds) * 1000

def create_silent_audio(duration_ms, file_path):
    silent_audio = AudioSegment.silent(duration=duration_ms)
    silent_audio.export(file_path, format="mp3")
    return file_path

def add_metadata_to_mp3(file_path, track_number, title, artist, album, album_artist, release_year, image_path):
    audio = MP3(file_path, ID3=ID3)
    
    audio.clear()
    audio.tags.add(TRCK(encoding=3, text=track_number))
    audio.tags.add(TIT2(encoding=3, text=title))
    audio.tags.add(TPE1(encoding=3, text=artist))
    audio.tags.add(TALB(encoding=3, text=album))
    audio.tags.add(TPE2(encoding=3, text=album_artist))
    audio.tags.add(TDRC(encoding=3, text=release_year))
    
    with open(image_path, 'rb') as img_file:
        audio.tags.add(
            APIC(
                encoding=3,
                mime='image/jpeg',
                type=3,
                desc=u'Cover',
                data=img_file.read()
            )
        )
    
    audio.save()
    print(f"Metadata added to {file_path}")

def find_album_art(album):
    for ext in ['.jpg', '.png']:
        image_path = f"{album}{ext}"
        if os.path.exists(image_path):
            return image_path
    return None

def generate_silent_audio_files(tracklist_file, output_dir):
    with open(tracklist_file, 'r', encoding='utf-8') as file:
        for line in file:
            if line.strip():
                track_number, title, artist, album, album_artist, release_year, length = line.strip().split('\t')
                
                duration_ms = length_to_ms(length)
                
                file_path = os.path.join(output_dir, f"{str(track_number).zfill(2)}_{title}.mp3")
                create_silent_audio(duration_ms, file_path)
                
                image_path = find_album_art(album)
                if image_path:
                    add_metadata_to_mp3(file_path, track_number, title, artist, album, album_artist, release_year, image_path)
                else:
                    print(f"Album art not found for {album}. Skipping metadata addition for {file_path}.")

    print("Silent audio files processed.")

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage: python script.py <tracklist_file> <output_dir>")
        sys.exit(1)

    tracklist_file = sys.argv[1]
    output_dir = sys.argv[2]

    generate_silent_audio_files(tracklist_file, output_dir)
