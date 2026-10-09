package com.devravix.attendancetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class TodayClass(
    val timetableId: Int,
    val subjectId: Int,
    val subjectName: String,
    val teacher: String,
    val shortName: String,
    val startTime: String,
    val endTime: String,
    val location: String,
    val status: AttendanceStatus? // Can be null if unmarked today
)

data class SubjectStats(
    val subjectId: Int,
    val subjectName: String,
    val presentCount: Int,
    val absentCount: Int,
    val massBunkCount: Int,
    val totalConducted: Int
)

@Dao
interface TrackerDao {

    @Query("""
        SELECT t.id as timetableId, s.id as subjectId, s.name as subjectName, s.teacher, s.shortName, 
               t.startTime, t.endTime, t.location, a.status 
        FROM timetables t
        INNER JOIN subjects s ON t.subjectId = s.id
        LEFT JOIN attendance_records a ON a.timetableId = t.id AND a.date = :date
        WHERE t.dayOfWeek = :dayOfWeek AND (t.`group` = 'ALL' OR t.`group` = :studentGroup)
        ORDER BY t.startTime ASC
    """)
    fun getTodayClasses(dayOfWeek: Int, studentGroup: String, date: LocalDate): Flow<List<TodayClass>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecordEntity)

    @androidx.room.Update
    suspend fun updateAttendance(record: AttendanceRecordEntity)

    @androidx.room.Delete
    suspend fun deleteAttendance(record: AttendanceRecordEntity)

    @Query("""
        SELECT a.* FROM attendance_records a
        INNER JOIN timetables t ON a.timetableId = t.id
        WHERE t.subjectId = :subjectId
    """)
    fun getAttendanceForSubject(subjectId: Int): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM subjects WHERE id = :subjectId")
    fun getSubjectById(subjectId: Int): Flow<SubjectEntity?>

    @Query("SELECT * FROM timetables WHERE subjectId = :subjectId")
    fun getTimetablesForSubject(subjectId: Int): Flow<List<TimetableEntity>>

    @Query("""
        SELECT 
            s.id as subjectId,
            s.name as subjectName,
            SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) as presentCount,
            SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) as absentCount,
            SUM(CASE WHEN a.status = 'MASS_BUNK' THEN 1 ELSE 0 END) as massBunkCount,
            SUM(CASE WHEN a.status IN ('PRESENT', 'ABSENT', 'MASS_BUNK') THEN 1 ELSE 0 END) as totalConducted
        FROM subjects s
        LEFT JOIN timetables t ON s.id = t.subjectId
        LEFT JOIN attendance_records a ON t.id = a.timetableId
        GROUP BY s.id
    """)
    fun getAllSubjectStats(): Flow<List<SubjectStats>>

    // Used for Seeding
    @Insert
    suspend fun insertSubjects(subjects: List<SubjectEntity>): List<Long>

    @Insert
    suspend fun insertTimetables(timetables: List<TimetableEntity>)

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun getSubjectCount(): Int
}