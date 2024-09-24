package code.name.monkey.retromusic.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE SongEntity_new (
                song_key INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                playlist_creator_id INTEGER NOT NULL,
                id INTEGER NOT NULL,
                title TEXT NOT NULL,
                track_number INTEGER NOT NULL,
                year INTEGER NOT NULL,
                duration INTEGER NOT NULL,
                data TEXT NOT NULL,
                date_modified INTEGER NOT NULL,
                album_id INTEGER NOT NULL,
                album_name TEXT NOT NULL,
                artist_id INTEGER NOT NULL,
                artist_name TEXT NOT NULL,
                composer TEXT,
                album_artist TEXT
            )
        """)

        database.execSQL("""
            INSERT INTO SongEntity_new (song_key, playlist_creator_id, id, title, track_number, year, duration, data, date_modified, album_id, album_name, artist_id, artist_name, composer, album_artist)
            SELECT song_key, playlist_creator_id, id, title, track_number, year, duration, data, date_modified, album_id, album_name, artist_id, artist_name, composer, album_artist
            FROM SongEntity
        """)

        database.execSQL("DROP TABLE SongEntity")

        database.execSQL("ALTER TABLE SongEntity_new RENAME TO SongEntity")

        database.execSQL("CREATE UNIQUE INDEX index_SongEntity_playlist_creator_id_id ON SongEntity(playlist_creator_id, id)")
    }
}
