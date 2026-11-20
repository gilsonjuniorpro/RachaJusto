package com.rachajusto.data.local

import com.rachajusto.data.seed.SeedDataFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

class FileJsonGroupStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun exportAndImportSnapshotJson_roundTripsValidGroupsAndRejectsCorruptJson() {
        val storage = FileJsonGroupStorage(File(tempFolder.root, "backup_test.json"))
        val seed = SeedDataFactory.createSeedGroups(LocalDate.of(2026, 11, 20))

        val snapshot = storage.exportSnapshotJson(seed)
        assertTrue(snapshot.contains("\"version\": 1"))

        val imported = storage.importSnapshotJson(snapshot)
        assertTrue(imported.isSuccess)
        assertEquals(seed.size, imported.getOrThrow().size)
        assertEquals(seed.size, storage.readGroups()?.size)

        val broken = storage.importSnapshotJson("{not valid json")
        assertTrue(broken.isFailure)
    }
}
