/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package code.name.monkey.retromusic.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy

@Dao
interface SongAnalysisDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(songAnalysisEntity: SongAnalysisEntity)

    @Query("SELECT * FROM SongsAnalysisEntity WHERE song_id = :songId")
    suspend fun find(songId: Long): SongAnalysisEntity?

    @Query("DELETE FROM SongsAnalysisEntity WHERE song_id = :songId")
    suspend fun delete(songId: Long)

    @Query("SELECT song_id, bpm, manualBPM FROM SongsAnalysisEntity")
    suspend fun getAll(): List<SongAnalysisEntity>?

    @Query("DELETE FROM SongsAnalysisEntity")
    suspend fun deleteAll()

    suspend fun updateBpm(songId: Long, bpm: Double?) {
        val foundRecord = find(songId)
        if (foundRecord != null) {
            editBPM(songId, bpm)
        } else {
            insert(SongAnalysisEntity(songId, bpm, null))
        }
    }

    @Query("UPDATE SongsAnalysisEntity SET bpm = :bpm WHERE song_id = :songId")
    suspend fun editBPM(songId: Long, bpm: Double?)

    @Query("SELECT bpm FROM SongsAnalysisEntity WHERE song_id = :songId")
    suspend fun getBPM(songId: Long): Double?

    suspend fun deleteBpm(songId: Long) {
        val foundRecord = find(songId)
        if (foundRecord != null) {
            editBPM(songId, null)
        } else {
            delete(songId)
        }
    }
}
