package code.name.monkey.retromusic.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS SongsAnalysisEntity (
                song_id INTEGER PRIMARY KEY NOT NULL,
                bpm REAL,
                manualBPM REAL
            )
        """)
    }
}
