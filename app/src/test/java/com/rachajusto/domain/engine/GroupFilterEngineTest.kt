package com.rachajusto.domain.engine

import com.rachajusto.data.seed.SeedDataFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GroupFilterEngineTest {

    @Test
    fun filter_matchesByNameDescriptionParticipantAndStatus() {
        val groups = SeedDataFactory.createSeedGroups(LocalDate.of(2026, 11, 20))

        // Accent-insensitive search on group name
        assertEquals(1, GroupFilterEngine.filter(groups, query = "jordao").size)
        // Search by participant name
        assertEquals(1, GroupFilterEngine.filter(groups, query = "matheus").size)
        // Two seed groups have pending transfers and one is fully settled
        assertEquals(2, GroupFilterEngine.filter(groups, statusFilter = GroupStatusFilter.PENDING).size)
        assertEquals(1, GroupFilterEngine.filter(groups, statusFilter = GroupStatusFilter.SETTLED).size)
    }
}
