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
    suspend fun addOrUpdateBpm(songAnalysis: SongAnalysisEntity)

    @Query("DELETE FROM songsanalysis WHERE song_id = :songId")
    suspend fun deleteBpm(songId: Long)

    @Query("SELECT bpm FROM songsAnalysis WHERE song_id = :songId")
    suspend fun getBPMBySongId(songId: Long): Double?

    @Query("SELECT song_id, bpm FROM songsAnalysis")
    suspend fun getBPMs(): List<SongAnalysisEntity>?

    @Query("DELETE FROM songsAnalysis")
    suspend fun deleteAll()
}
