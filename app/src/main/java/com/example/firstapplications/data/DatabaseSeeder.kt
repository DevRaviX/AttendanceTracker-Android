package com.example.firstapplications.data

class DatabaseSeeder(private val dao: TrackerDao) {

    suspend fun seedDatabaseIfNeeded(group: StudentGroup) {
        if (dao.getSubjectCount() > 0) return // Already seeded

        val subjects = listOf(
            SubjectEntity(name = "Neural Network", teacher = "Dr. Dimple", shortName = "NN"),
            SubjectEntity(name = "Information Security", teacher = "Dr. Sood", shortName = "IS"),
            SubjectEntity(name = "Compiler Design", teacher = "Dr. Anshu / Dr. Suresh", shortName = "CD"),
            SubjectEntity(name = "Software Engg", teacher = "Dr. Sarika / Dr. Dimple", shortName = "SE"),
            SubjectEntity(name = "Mobile App Dev", teacher = "Dr. Munish", shortName = "MAD"),
            SubjectEntity(name = "Info Security Lab", teacher = "Dr. Sood", shortName = "IS Lab"),
            SubjectEntity(name = "Mobile App Dev Lab", teacher = "Dr. Sarika", shortName = "MAD Lab")
        )

        val ids = dao.insertSubjects(subjects)
        
        val nnId = ids[0].toInt()
        val isId = ids[1].toInt()
        val cdId = ids[2].toInt()
        val seId = ids[3].toInt()
        val madId = ids[4].toInt()
        val isLabId = ids[5].toInt()
        val madLabId = ids[6].toInt()

        val timetables = mutableListOf<TimetableEntity>()

        // Monday
        timetables.add(TimetableEntity(subjectId = nnId, dayOfWeek = 1, startTime = "10:20", endTime = "11:15", group = StudentGroup.ALL, location = "L1"))
        timetables.add(TimetableEntity(subjectId = isId, dayOfWeek = 1, startTime = "11:35", endTime = "12:30", group = StudentGroup.ALL, location = "L1"))
        
        // Tuesday
        timetables.add(TimetableEntity(subjectId = cdId, dayOfWeek = 2, startTime = "09:25", endTime = "10:20", group = StudentGroup.ALL, location = "L2"))
        timetables.add(TimetableEntity(subjectId = seId, dayOfWeek = 2, startTime = "10:20", endTime = "11:15", group = StudentGroup.ALL, location = "L2"))
        timetables.add(TimetableEntity(subjectId = madId, dayOfWeek = 2, startTime = "11:35", endTime = "12:30", group = StudentGroup.ALL, location = "L2"))

        // Wednesday
        timetables.add(TimetableEntity(subjectId = madId, dayOfWeek = 3, startTime = "09:25", endTime = "10:20", group = StudentGroup.ALL, location = "L1"))
        timetables.add(TimetableEntity(subjectId = seId, dayOfWeek = 3, startTime = "10:20", endTime = "11:15", group = StudentGroup.ALL, location = "L1"))
        timetables.add(TimetableEntity(subjectId = isId, dayOfWeek = 3, startTime = "11:35", endTime = "12:30", group = StudentGroup.ALL, location = "L1"))

        // Thursday
        timetables.add(TimetableEntity(subjectId = nnId, dayOfWeek = 4, startTime = "09:25", endTime = "10:20", group = StudentGroup.ALL, location = "L2"))
        timetables.add(TimetableEntity(subjectId = madId, dayOfWeek = 4, startTime = "10:20", endTime = "11:15", group = StudentGroup.ALL, location = "L2"))
        timetables.add(TimetableEntity(subjectId = cdId, dayOfWeek = 4, startTime = "11:35", endTime = "12:30", group = StudentGroup.ALL, location = "L2"))

        // Friday
        timetables.add(TimetableEntity(subjectId = nnId, dayOfWeek = 5, startTime = "08:30", endTime = "09:25", group = StudentGroup.ALL, location = "L2"))
        timetables.add(TimetableEntity(subjectId = cdId, dayOfWeek = 5, startTime = "09:25", endTime = "10:20", group = StudentGroup.ALL, location = "L2"))
        timetables.add(TimetableEntity(subjectId = seId, dayOfWeek = 5, startTime = "10:20", endTime = "11:15", group = StudentGroup.ALL, location = "L2"))
        timetables.add(TimetableEntity(subjectId = isId, dayOfWeek = 5, startTime = "11:35", endTime = "12:30", group = StudentGroup.ALL, location = "L2"))

        // Group Dependent Labs
        if (group == StudentGroup.G1_G3) {
            timetables.add(TimetableEntity(subjectId = isLabId, dayOfWeek = 1, startTime = "13:45", endTime = "16:30", group = StudentGroup.G1_G3, location = "LAB 3"))
            timetables.add(TimetableEntity(subjectId = madLabId, dayOfWeek = 4, startTime = "13:45", endTime = "16:30", group = StudentGroup.G1_G3, location = "LAB 3"))
        } else if (group == StudentGroup.G2_G4) {
            timetables.add(TimetableEntity(subjectId = madLabId, dayOfWeek = 2, startTime = "13:45", endTime = "16:30", group = StudentGroup.G2_G4, location = "Lab"))
            timetables.add(TimetableEntity(subjectId = isLabId, dayOfWeek = 3, startTime = "13:45", endTime = "16:30", group = StudentGroup.G2_G4, location = "Lab"))
        }

        dao.insertTimetables(timetables)
    }
}